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
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "Gson::Decoder")
public class Decoder extends RubyObject {

    static final long serialVersionUID = 2328444027137249699L;
    private boolean lenient = true;
    private boolean symbolizeKeys = false;
    private static Pattern floatPattern = Pattern.compile("[.eE]");

    public Decoder(final Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    @JRubyMethod(name = "lenient?")
    public IRubyObject isLenient(ThreadContext context) {
        return RubyBoolean.newBoolean(context.getRuntime(), this.lenient);
    }

    @JRubyMethod(name = "symbolize_keys?")
    public IRubyObject isSymbolizeKeys(ThreadContext context) {
        return RubyBoolean.newBoolean(context.getRuntime(), this.symbolizeKeys);
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

    @JRubyClass(name="Gson::DecodeError", parent="StandardError")
    public static class DecodeError {

        public static RaiseException newDecodeError(Ruby ruby, String message) {
            RubyClass errorClass = ruby.getModule("Gson").getClass("DecodeError");
            return new RaiseException(RubyException.newException(ruby, errorClass, message), true);
        }

    }

    @JRubyMethod
    public IRubyObject decode(ThreadContext context, IRubyObject arg) {
        Ruby ruby = context.getRuntime();
        RingBuffer buffer = new RingBuffer();

        if (arg instanceof RubyString) {
            buffer.write(arg.toString().toCharArray());
        } else if ((arg instanceof RubyIO) || (arg instanceof RubyStringIO)) {
            IRubyObject stream = IOJavaAddons.AnyIO.any_to_inputstream(context, arg);
            buffer.setExternalSource(new InputStreamReader((InputStream)stream.toJava(InputStream.class)));
        } else {
            throw ruby.newArgumentError("Unsupported source. This method accepts String or IO");
        }

        LinkedList<IRubyObject> stack = new LinkedList<IRubyObject>();
        LinkedList<IRubyObject> res = new LinkedList<IRubyObject>();
        JsonReader reader = new JsonReader(buffer.getReader());
        reader.setLenient(this.lenient);

        IRubyObject head, key, val;
        RubyHash obj;
        RubyArray ary;
        int i;
        try {
            while (true) {
                JsonToken token = reader.peek();
                switch (token) {
                    case END_ARRAY:
                        reader.endArray();
                        val = stack.pop();
                        if (stack.isEmpty()) {
                            res.add(val);
                        }
                        break;
                    case END_OBJECT:
                        reader.endObject();
                        val = stack.pop();
                        if (stack.isEmpty()) {
                            res.add(val);
                        }
                        break;
                    case NAME:
                        if (this.symbolizeKeys) {
                            val = RubySymbol.newSymbol(ruby, reader.nextName());
                        } else {
                            val = RubyString.newString(ruby, reader.nextName());
                        }
                        stack.push(val);
                        break;
                    case BEGIN_OBJECT:
                    case BEGIN_ARRAY:
                    case STRING:
                    case NUMBER:
                    case BOOLEAN:
                    case NULL:
                        head = stack.peek();
                        switch (token) {
                            case BEGIN_ARRAY:
                                reader.beginArray();
                                val = RubyArray.newArray(ruby);
                                break;
                            case BEGIN_OBJECT:
                                reader.beginObject();
                                val = RubyHash.newHash(ruby);
                                break;
                            case STRING:
                                val = RubyString.newString(ruby, reader.nextString());
                                break;
                            case NUMBER:
                                String tmp = reader.nextString();
                                if (floatPattern.matcher(tmp).find()) {
                                    val = RubyNumeric.str2fnum(ruby, RubyString.newString(ruby, tmp));
                                } else {
                                    val = RubyNumeric.str2inum(ruby, RubyString.newString(ruby, tmp), 10);
                                }
                                break;
                            case BOOLEAN:
                                val = RubyBoolean.newBoolean(ruby, reader.nextBoolean());
                                break;
                            case NULL:
                                reader.nextNull();
                                val = context.nil;
                                break;
                            default:
                                throw DecodeError.newDecodeError(ruby, String.format("Unknown token: %s", token.toString()));
                        }
                        if (head instanceof RubyString || head instanceof RubySymbol) {
                            key = stack.pop();
                            obj = (RubyHash)stack.peek();
                            obj.op_aset(context, key, val);
                        } else if (head instanceof RubyArray) {
                            ary = (RubyArray)stack.peek();
                            ary.append(val);
                        }
                        switch (token) {
                            case BEGIN_ARRAY:
                            case BEGIN_OBJECT:
                                stack.push(val);
                                break;
                            default:
                                if (head == null) {
                                    res.add(val);
                                }
                        }
                        break;
                    case END_DOCUMENT:
                        switch (res.size()) {
                            case 0:
                                return context.nil;
                            case 1:
                                return res.pop();
                            default:
                                return ruby.newArray(res);
                        }
                    default:
                        throw DecodeError.newDecodeError(ruby, String.format("Unknown token: %s", token.toString()));
                }
            }
        } catch (Exception ex) {
            throw DecodeError.newDecodeError(ruby, ex.getClass().getName() + ": " + ex.getMessage());
        }
    }
}
