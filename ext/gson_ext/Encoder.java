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

import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyInteger;
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
import org.jruby.util.IOOutputStream;

@JRubyClass(name = "Gson::Encoder")
public class Encoder extends RubyObject {

    static final long serialVersionUID = 5035506147333315973L;
    private boolean htmlSafe = false;
    private boolean lenient = true;
    private boolean serializeNulls = true;
    private String indent = "";
    private IRubyObject options;

    public Encoder(final Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    @JRubyMethod(name = "lenient?")
    public IRubyObject isLenient(ThreadContext context) {
        return RubyBoolean.newBoolean(context.getRuntime(), this.lenient);
    }

    @JRubyMethod(name = "html_safe?")
    public IRubyObject isHtmlSafe(ThreadContext context) {
        return RubyBoolean.newBoolean(context.getRuntime(), this.htmlSafe);
    }

    @JRubyMethod(name = "serialize_nils?")
    public IRubyObject isSerializeNulls(ThreadContext context) {
        return RubyBoolean.newBoolean(context.getRuntime(), this.serializeNulls);
    }

    @JRubyMethod(name = "indent")
    public IRubyObject getIndent(ThreadContext context) {
        return RubyString.newString(context.getRuntime(), this.indent);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        Ruby ruby = context.getRuntime();
        if (args.length < 1 || args[0].isNil()) {
            this.options = RubyHash.newHash(ruby);
            return context.nil;
        }
        if (!(args[0] instanceof RubyHash)) {
            throw ruby.newArgumentError("expected Hash for options argument");
        }
        this.options = args[0];
        RubyHash options = (RubyHash)this.options;
        RubySymbol name = ruby.newSymbol("lenient");
        if (options.containsKey(name)) {
            this.lenient = options.op_aref(context, name).isTrue();
        }
        name = ruby.newSymbol("serialize_nils");
        if (options.containsKey(name)) {
            this.serializeNulls = options.op_aref(context, name).isTrue();
        }
        name = ruby.newSymbol("html_safe");
        if (options.containsKey(name)) {
            this.htmlSafe = options.op_aref(context, name).isTrue();
        }
        name = ruby.newSymbol("indent");
        if (options.containsKey(name)) {
            IRubyObject val = options.op_aref(context, name);
            if (val.isNil()) {
                this.indent = "";
            } else {
                this.indent = val.checkStringType().asJavaString();
            }
        }
        return context.nil;
    }

    @JRubyClass(name="Gson::EncodeError", parent="StandardError")
    public static class EncodeError {

        public static RaiseException newEncodeError(Ruby ruby, String message) {
            RubyClass errorClass = ruby.getModule("Gson").getClass("EncodeError");
            return new RaiseException(RubyException.newException(ruby, errorClass, message), true);
        }

    }

    @JRubyMethod(required = 1, optional = 1)
    public IRubyObject encode(ThreadContext context, IRubyObject[] args) {
        Ruby ruby = context.getRuntime();
        Writer out = null;

        if (args.length < 2 || args[1].isNil()) {
            out = new StringWriter();
        } else {
            IRubyObject io = args[1];
            if ((io instanceof RubyIO) || (io instanceof RubyStringIO)) {
                IRubyObject stream = IOJavaAddons.AnyIO.any_to_outputstream(context, io);
                out = new OutputStreamWriter((OutputStream)stream.toJava(OutputStream.class));
            } else {
                throw ruby.newArgumentError("Unsupported source. This method accepts IO");
            }
        }

        JsonWriter writer = new JsonWriter(out);
        writer.setLenient(this.lenient);
        writer.setHtmlSafe(this.htmlSafe);
        writer.setIndent(this.indent);
        writer.setSerializeNulls(this.serializeNulls);
        try {
            encodeValue(writer, context, args[0]);
        } catch (Exception ex) {
            throw EncodeError.newEncodeError(ruby, ex.getMessage());
        }
        if (out instanceof StringWriter) {
            return ruby.newString(out.toString());
        } else {
            return context.nil;
        }
    }

    private void encodeValue(JsonWriter writer, ThreadContext context, IRubyObject val)
        throws IOException {
        Ruby ruby = context.getRuntime();

        if (val.isNil()) {
            writer.nullValue();
        } else if (val instanceof RubyHash) {
            writer.beginObject();
            for (Object obj : ((RubyHash)val).directEntrySet()) {
                RubyHash.RubyHashEntry item = (RubyHash.RubyHashEntry)obj;
                writer.name(item.getKey().toString());
                encodeValue(writer, context, (IRubyObject)item.getValue());
            }
            writer.endObject();
        } else if (val instanceof RubyArray) {
            writer.beginArray();
            for (IRubyObject item : ((RubyArray)val).toJavaArray()) {
                encodeValue(writer, context, item);
            }
            writer.endArray();
        } else if (val instanceof RubyString || val instanceof RubySymbol) {
            writer.value(val.toString());
        } else if (val instanceof RubyInteger) {
            try {
                writer.value(((RubyInteger)val).getLongValue());
            } catch (RaiseException ex) {
                if ("RangeError".equals(ex.getException().getType().getName())) {
                    writer.value(((RubyInteger)val).getBigIntegerValue());
                } else {
                    throw ex;
                }
            }
        } else if (val instanceof RubyFloat) {
            writer.value((Double)((RubyFloat)val).getDoubleValue());
        } else if (val instanceof RubyBoolean) {
            writer.value(val.isTrue());
        } else if (val.respondsTo("as_json")) {
            encodeValue(writer, context, val.callMethod(context, "as_json", this.options));
        } else if (val.respondsTo("to_s")) {
            writer.value(val.toString());
        } else {
            writer.value(val.anyToString().toString());
        }
        writer.flush();
    }

}
