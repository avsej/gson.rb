/*
 *     Copyright 2012 Couchbase, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package gson_ext;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.regex.Pattern;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.stringio.RubyStringIO;
import org.jruby.java.addons.IOJavaAddons;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "Gson::Decoder")
public class Decoder extends RubyObject {

    private static class MemoizedReader extends Reader {

        private Reader origin = null;
        private StringBuilder memory = new StringBuilder(1024);

        public MemoizedReader() {
        }

        @Override
        public void close() throws IOException {
            origin.close();
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int result = origin.read(cbuf, off, len);
            if (result != -1) {
                memory.append(cbuf, off, result);
            }
            return result;
        }

        public void setOrigin(Reader origin) {
            this.origin = origin;
        }

        public void retainMemory(int tailSize) {
            memory.delete(0, memory.length()-tailSize);
        }

        public String getMemory() {
            return memory.toString();
        }

        private void clearMemory() {
            memory.setLength(0);
        }

    }

    private static class SequenceReader extends Reader {

        private Reader r1 = null;
        private Reader r2 = null;
        private boolean r1Exhausted = false;

        public SequenceReader(Reader r1, Reader r2) {
            this.r1 = r1;
            this.r2 = r2;
        }

        @Override
        public void close() throws IOException {
            r1.close();
            r2.close();
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (r1Exhausted) {
                return r2.read(cbuf, off, len);
            } else {
                int result = r1.read(cbuf, off, len);
                if (result < len) {
                    r1Exhausted = true;
                    if (result == -1) {
                        return r2.read(cbuf, off, len);
                    } else {
                        return result + r2.read(cbuf, off+result, len-result);
                    }
                } else {
                    return result;
                }
            }
        }

    }

    static final long serialVersionUID = 2328444027137249699L;
    private boolean lenient = true;
    private boolean symbolizeKeys = false;
    private static Pattern floatPattern = Pattern.compile("[.eE]");
    private Block onDocumentCallback = null;
    private LinkedList<IRubyObject> builderStack = new LinkedList<IRubyObject>();
    private MemoizedReader decodeMemory = new MemoizedReader();
    private JsonReader reader;
    private int nestedArrayLevel = 0;
    private int nestedHashLevel = 0;

    public Decoder(final Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        Ruby ruby = context.getRuntime();
        if (args.length < 1) {
            return context.nil;
        }
        if (!(args[0] instanceof RubyHash)) {
            throw ruby.newArgumentError("expected Hash for options argument");
        }
        RubyHash options = (RubyHash)args[0];
        RubySymbol name = ruby.newSymbol("lenient");
        if (options.containsKey(name)) {
            this.lenient = options.op_aref(context, name).isTrue();
        }
        name = ruby.newSymbol("symbolize_keys");
        if (options.containsKey(name)) {
            this.symbolizeKeys = options.op_aref(context, name).isTrue();
        }
        return context.nil;
    }

    @JRubyMethod(name = "lenient?")
    public IRubyObject isLenient(ThreadContext context) {
        return RubyBoolean.newBoolean(context.getRuntime(), this.lenient);
    }

    @JRubyMethod(name = "symbolize_keys?")
    public IRubyObject isSymbolizeKeys(ThreadContext context) {
        return RubyBoolean.newBoolean(context.getRuntime(), this.symbolizeKeys);
    }

    @JRubyMethod(name = "on_document")
    public IRubyObject onDocument(ThreadContext context, Block block) {
        if (block.isGiven()) {
            onDocumentCallback = block;
            return block.getProcObject();
        } else {
            throw context.getRuntime().newArgumentError("No block given. This method requires a block");
        }
    }

    @JRubyClass(name="Gson::DecodeError", parent="StandardError")
    public static class DecodeError {

        public static RaiseException newDecodeError(Ruby ruby, String message) {
            RubyClass errorClass = ruby.getModule("Gson").getClass("DecodeError");
            return new RaiseException(RubyException.newException(ruby, errorClass, message), true);
        }

    }

    private Reader prepareSourceReader(Ruby ruby, ThreadContext context, IRubyObject source) {
        if (source instanceof RubyString) {
            return new StringReader(source.toString());
        } else if ((source instanceof RubyIO) || (source instanceof RubyStringIO)) {
            IRubyObject stream = IOJavaAddons.AnyIO.any_to_inputstream(context, source);
            return new InputStreamReader((InputStream)stream.toJava(InputStream.class));
        } else {
            throw ruby.newArgumentError("Unsupported source. This method accepts String or IO");
        }
    }

    private void appendValue(ThreadContext context, IRubyObject value) {
        if (builderStack.size() > 0) {
            IRubyObject head = builderStack.peek();
            if (head instanceof RubyString || head instanceof RubySymbol) {
                builderStack.pop();
                RubyHash hash = (RubyHash)builderStack.peek();
                hash.op_aset(context, head, value);
                if (value instanceof RubyArray || value instanceof RubyHash) {
                    builderStack.push(value);
                }
            } else if (head instanceof RubyArray) {
                ((RubyArray)head).append(value);
                if (value instanceof RubyArray || value instanceof RubyHash) {
                    builderStack.push(value);
                }
            } else if (head instanceof RubyHash) {
                builderStack.push(value);
            }
        } else {
            builderStack.push(value);
        }
    }

    private void checkAndFireCallback(ThreadContext context) {
        if (onDocumentCallback != null) {
            if (builderStack.size() == 1 && nestedArrayLevel == 0 && nestedHashLevel == 0) {
                onDocumentCallback.yield(context, builderStack.pop());
            }
        }
    }

    private boolean isStartWith(String actual, String expected) {
        if (actual.length() >= expected.length() && actual.substring(0, expected.length()).equals(expected)) {
            return true;
        }
        return false;
    }

    private void decodeChunk(ThreadContext context, IRubyObject source) {
        Ruby ruby = context.getRuntime();

        Reader sourceReader = prepareSourceReader(ruby, context, source);
        String memory = decodeMemory.getMemory();
        if (memory.length() > 0) {
            Reader memoryReader = new StringReader(memory);
            sourceReader = new SequenceReader(memoryReader, sourceReader);
        }
        decodeMemory.clearMemory();
        decodeMemory.setOrigin(sourceReader);

        if (reader == null) {
            reader = new JsonReader(decodeMemory);
        } else {
            reader.recoverAfterBreak(decodeMemory);
        }
        reader.setLenient(this.lenient);
        reader.setChunkMode(onDocumentCallback != null);

        if (memory.length() > 0) {
            try {
                reader.peek();
            } catch(IOException ex) {
                //TODO: try to avoid allowing every "End of input" exception
                if (onDocumentCallback == null && !isStartWith(ex.getMessage(), "End of input at line")) {
                    throw DecodeError.newDecodeError(ruby, ex.getMessage());
                }
            }
        }

        IRubyObject val;
        try {
            while (true) {
                JsonToken token = reader.peek();
                switch (token) {
                    case END_ARRAY:
                        reader.endArray();
                        nestedArrayLevel -= 1;
                        if (builderStack.size() > 1) {
                            builderStack.pop();
                        }
                        checkAndFireCallback(context);

                        break;
                    case END_OBJECT:
                        reader.endObject();
                        nestedHashLevel -= 1;
                        if (builderStack.size() > 1) {
                            builderStack.pop();
                        }
                        checkAndFireCallback(context);

                        break;
                    case NAME:
                        if (this.symbolizeKeys) {
                            val = RubySymbol.newSymbol(ruby, reader.nextName());
                        } else {
                            val = RubyString.newString(ruby, reader.nextName());
                        }
                        appendValue(context, val);

                        break;
                    case BEGIN_OBJECT:
                        reader.beginObject();
                        nestedHashLevel += 1;
                        appendValue(context, RubyHash.newHash(ruby));

                        break;
                    case BEGIN_ARRAY:
                        reader.beginArray();
                        nestedArrayLevel += 1;
                        appendValue(context, RubyArray.newArray(ruby));

                        break;
                    case STRING:
                        val = RubyString.newString(ruby, reader.nextString());
                        appendValue(context, val);

                        break;
                    case NUMBER:
                        String tmp = reader.nextString();
                        if (floatPattern.matcher(tmp).find()) {
                            val = RubyNumeric.str2fnum(ruby, RubyString.newString(ruby, tmp));
                        } else {
                            val = RubyNumeric.str2inum(ruby, RubyString.newString(ruby, tmp), 10);
                        }
                        appendValue(context, val);

                        break;
                    case BOOLEAN:
                        val = RubyBoolean.newBoolean(ruby, reader.nextBoolean());
                        appendValue(context, val);

                        break;
                    case NULL:
                        reader.nextNull();
                        appendValue(context, context.nil);

                        break;
                    case END_DOCUMENT:
                        return;
                    default:
                        throw DecodeError.newDecodeError(ruby, String.format("Unknown token: %s", token.toString()));
                }

                reader.prepareForBreak();
                decodeMemory.retainMemory(reader.getUnreadBufferLength());
            }
        } catch (MalformedJsonException ex) {
            if (onDocumentCallback != null && (isStartWith(ex.getMessage(), "Unterminated string at line") ||
                                               isStartWith(ex.getMessage(), "Unterminated array at line") ||
                                               isStartWith(ex.getMessage(), "Unterminated object at line") ||//TODO: try to avoid allowing this
                                               isStartWith(ex.getMessage(), "Unterminated escape sequence at line"))) {
                return;
            } else {
                throw DecodeError.newDecodeError(ruby, ex.getMessage());
            }
        } catch (IOException ex) {
            if (onDocumentCallback != null && isStartWith(ex.getMessage(), "End of input at line")) {
                return;
            } else {
                throw DecodeError.newDecodeError(ruby, ex.getMessage());
            }
        } catch (Exception ex) {
            throw DecodeError.newDecodeError(ruby, ex.getMessage());
        }
    }

    @JRubyMethod
    public IRubyObject decode(ThreadContext context, IRubyObject source) {
        decodeChunk(context, source);
        if (onDocumentCallback != null) {
            checkAndFireCallback(context);
            return context.nil;
        }
        return builderStack.pop();
    }

}
