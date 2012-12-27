require 'minitest/autorun'
require 'gson'

class TestOkjson < MiniTest::Unit::TestCase

  TEST_DIR = File.expand_path("../okjson/t", __FILE__)

  def test_valid
    encoder = Gson::Encoder.new(:lenient => false)
    decoder = Gson::Decoder.new(:lenient => false)
    files = Dir["#{TEST_DIR}/valid*.json"]
    files.each do |file|
      there = decoder.decode(File.read(file))
      back = encoder.encode(there)
      assert_equal File.read("#{file}.exp").chomp, back, "#{file} failed"
    end
  end

  def test_encode
    encoder = Gson::Encoder.new(:lenient => false)
    files = Dir["#{TEST_DIR}/encode*.json"]
    files.each do |file|
      json = encoder.encode(eval(File.read(file)))
      assert_equal File.read("#{file}.exp").chomp, json, "#{file} failed"
    end
  end

  def test_decode
    decoder = Gson::Decoder.new(:lenient => false)
    files = Dir["#{TEST_DIR}/decode*.json"]
    files.each do |file|
      obj = decoder.decode(File.read(file)).inspect
      assert_equal File.read("#{file}.exp").chomp, obj, "#{file} failed"
    end
  end

  def test_decode_error
    decoder = Gson::Decoder.new(:lenient => false)
    files = Dir["#{TEST_DIR}/invalid*.json"]
    files.each do |file|
      assert_raises(Gson::DecodeError, "#{file} failed") do
        decoder.decode(File.read(file))
      end
    end
  end

  def test_encode_error
    encoder = Gson::Encoder.new(:lenient => false)
    files = Dir["#{TEST_DIR}/err*.json"]
    files.each do |file|
      assert_raises(Gson::EncodeError, "#{file} failed") do
        encoder.encode(eval(File.read(file)))
      end
    end
  end

end
