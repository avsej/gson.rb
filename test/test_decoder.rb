require 'minitest/autorun'
require 'gson'
require 'stringio'
require 'json/pure'

class TestDecoder < MiniTest::Unit::TestCase

  def test_it_lenient_by_default
    decoder = Gson::Decoder.new
    assert decoder.lenient?
  end

  def test_it_doesnt_symbolize_keys_by_default
    decoder = Gson::Decoder.new
    refute decoder.symbolize_keys?
  end

  def test_it_symbolizes_keys_if_required
    decoder = Gson::Decoder.new(:symbolize_keys => true)
    expected = {:foo => "bar"}
    assert_equal expected, decoder.decode('{"foo":"bar"}')
  end

  def test_it_accepts_io_objects
    path = File.expand_path("../okjson/t/valid-object-single.json", __FILE__)
    File.open(path) do |io|
      decoder = Gson::Decoder.new
      expected = {"a" => "b"}
      assert_equal expected, decoder.decode(io)
    end
  end

  def test_it_accepts_stringio_objects
    io = StringIO.new('{"a":"b"}')
    decoder = Gson::Decoder.new
    expected = {"a" => "b"}
    assert_equal expected, decoder.decode(io)
  end

  def test_it_can_use_callback_to_return_documents
    decoder = Gson::Decoder.new
    actual = nil
    decoder.on_document do |doc|
      actual = doc
    end
    expected = {"x" => "y"}
    assert_equal nil, decoder.decode('{"x":"y"}')
    assert_equal expected, actual
  end

  def test_it_can_parse_documents_by_chunks_with_callback
    decoder = Gson::Decoder.new
    actual = nil
    decoder.on_document do |doc|
      actual = doc
    end

    expected = { 'foo' => 'bar', 'num' => 111 }
    ['{"foo":', '"bar', '"', ',"num":11', '1}'].each do |chunk|
      assert_equal nil, decoder.decode(chunk)
    end
    assert_equal expected, actual
  end

  def test_it_can_parse_multiple_documents_with_callback
    decoder = Gson::Decoder.new
    actual = nil
    decoder.on_document do |doc|
      actual = doc
    end

    decoder.decode('{"x":1.1')
    assert_equal nil, actual

    decoder.decode('5}{"y":"z')
    assert_equal({"x"=>1.15}, actual)

    decoder.decode('"}')
    assert_equal({'y'=>'z'}, actual)
  end

  def test_in_lenient_mode_it_allows_top_level_value_of_any_type
    assert_valid_in_lenient_mode(1, "1")
  end

  def test_in_lenient_mode_it_allows_source_to_start_with_non_execute_prefix
    nep = %Q|)]}'\n|
    assert_valid_in_lenient_mode({"foo"=>"bar"}, nep + '{"foo":"bar"}')
  end

  def test_in_lenient_mode_it_allows_source_to_include_multiple_top_level_values
    assert_valid_in_lenient_mode(1, '1 2 3')
    assert_valid_in_lenient_mode({"foo"=>"bar"}, '{"foo":"bar"}{"bar":"foo"}')
  end

  def test_in_lenient_mode_it_allows_names_to_be_unquoted
    assert_valid_in_lenient_mode({"foo"=>"bar"}, '{foo:"bar"}')
  end

  def test_in_lenient_mode_it_allows_names_to_be_single_quoted
    assert_valid_in_lenient_mode({"foo"=>"bar"}, %Q({'foo':"bar"}))
  end

  def test_in_lenient_mode_it_allows_strings_to_be_unquoted
    assert_valid_in_lenient_mode({"foo"=>"bar"}, '{"foo":bar}')
  end

  def test_in_lenient_mode_it_allows_strings_to_be_single_quoted
    assert_valid_in_lenient_mode({"foo"=>"bar"}, %Q({"foo":'bar'}))
  end

  def test_in_lenient_mode_it_allows_semicolons_as_array_separators
    assert_valid_in_lenient_mode({"foo"=>[1,2,3,4]}, '{"foo":[1;2;3;4]}')
  end

  def test_in_lenient_mode_it_allows_hashrockets_as_name_value_separators
    assert_valid_in_lenient_mode({"foo" => "bar"}, '{"foo"=>"bar"}')
  end

  def assert_valid_in_lenient_mode(expected, source)
    decoder = Gson::Decoder.new(:lenient => true)
    assert_equal expected, decoder.decode(source)

    decoder = Gson::Decoder.new(:lenient => false)
    assert_raises Gson::DecodeError do
      decoder.decode(source)
    end
  end

  def test_it_can_parse_valid_documents_by_chunks
    pass_fixtures = File.expand_path("../fixtures/pass*.json", __FILE__)
    Dir[pass_fixtures].each do |f|
      decoder = Gson::Decoder.new
      actual = nil
      decoder.on_document do |document|
        actual = document
      end

      file_content = File.read(f)

      file_content.split('').each { |part| decoder.decode(part) }
      assert_equal actual, JSON.parse(file_content), "#{f} failed"
    end
  end

  def test_it_cannot_parse_invalid_documents_by_chunks
    fail_fixtures = File.expand_path("../fixtures/fail*.json", __FILE__)
    encoder = Gson::Encoder.new(:lenient => false)
    Dir[fail_fixtures].each do |f|
      decoder = Gson::Decoder.new
      decoder.on_document {}

      assert_raises Gson::DecodeError, "#{f} failed".to_sym do
        File.read(f).split('').each { |part| decoder.decode(part) }
      end
    end
  end

end
