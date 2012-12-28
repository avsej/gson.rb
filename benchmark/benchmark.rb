$LOAD_PATH.unshift File.expand_path(File.dirname(__FILE__) + '/..')
$LOAD_PATH.unshift File.expand_path(File.dirname(__FILE__) + '/../lib')

require 'rubygems'
require 'benchmark'
require 'gson'
require 'optparse'
begin
  require 'json'
rescue LoadError
end

options = {
  :times => 1_000,
  :file => File.expand_path(File.dirname(__FILE__) + '/subjects/ohai.json'),
  :type => :both
}

OptionParser.new do |opts|
  opts.banner = "Usage: benchmark.rb [options]"
  opts.on("-t", "--type TYPE", [:decode, :encode, :both], "Type of the operation (default: #{options[:type]}") do |v|
    options[:verbose] = v
  end
  opts.on("-n", "--times NUMBER", Integer, "Number of iterations (default: #{options[:times]})") do |v|
    options[:times] = v
  end
  opts.on("-f", "--file FILE", "File to use as JSON source (default: #{options[:file]})") do |v|
    options[:file] = v
  end
  opts.on_tail("-?", "--help", "Show this message") do
    puts opts
    exit
  end
end.parse!

options[:json] = File.read(options[:file])
options[:hash] = Gson::Decoder.new.decode(options[:json])
options[:json_stream] = File.open(options[:file])

def encode(options)
  puts "\n## ENCODING JSON"
  encoder = Gson::Encoder.new
  Benchmark.bmbm do |x|
    x.report("Gson::Encoder#encode (to a String)") do
      options[:times].times do
        encoder.encode(options[:hash])
      end
    end
    if defined?(JSON)
      x.report("JSON.generate") do
        options[:times].times do
          JSON.generate(options[:hash])
        end
      end
    end
  end
end

def decode(options)
  puts "\n\n## DECODING JSON"
  decoder = Gson::Decoder.new
  Benchmark.bmbm do |x|
    x.report("Gson::Decoder#decode (from an IO)") do
      options[:times].times do
        options[:json_stream].rewind
        decoder.decode(options[:json_stream])
      end
    end
    x.report("Gson::Decoder#decode (from a String)") do
      options[:times].times do
        decoder.decode(options[:json])
      end
    end
    if defined?(JSON)
      x.report("JSON.parse") do
        options[:times].times do
          JSON.parse(options[:json], :max_nesting => false)
        end
      end
    end
  end
end

case options[:type]
when :decode, "decode"
  decode(options)
when :encode, "encode"
  encode(options)
when :both, "both"
  encode(options)
  decode(options)
else
  raise "Unknown type: #{options[:type].inspect}"
end


options[:json_stream].close
