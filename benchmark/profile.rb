$LOAD_PATH.unshift File.expand_path(File.dirname(__FILE__) + '/..')
$LOAD_PATH.unshift File.expand_path(File.dirname(__FILE__) + '/../lib')

require 'gson'
require 'optparse'

options = {
  :times => 1_000,
  :file => File.expand_path(File.dirname(__FILE__) + '/subjects/ohai.json'),
  :type => :decode
}

OptionParser.new do |opts|
  opts.banner = "Usage: profile.rb [options]"
  opts.on("-t", "--type TYPE", [:decode, :encode], "Type of the operation (default: #{options[:type]}") do |v|
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

json = File.read(options[:file])
hash = Gson::Decoder.new.decode(json)

case options[:type]
when :decode, "decode"
  decoder = Gson::Decoder.new
  options[:times].times do
    decoder.decode(json)
  end
when :encode, "encode"
  encoder = Gson::Encoder.new
  options[:times].times do
    encoder.encode(hash)
  end
else
  raise "Unknown type: #{options[:type].inspect}"
end
