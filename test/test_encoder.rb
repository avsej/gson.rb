require 'minitest/autorun'
require 'gson'
require 'stringio'

class TestEncoder < MiniTest::Unit::TestCase

  def test_it_lenient_by_default
    encoder = Gson::Encoder.new
    assert encoder.lenient?
  end

  def test_it_serializes_nils_by_default
    encoder = Gson::Encoder.new
    assert encoder.serialize_nils?
  end

  def test_it_isnt_html_safe_by_default
    encoder = Gson::Encoder.new
    refute encoder.html_safe?
  end

  def test_it_doesnt_prettify_output_by_default
    encoder = Gson::Encoder.new
    assert_equal "", encoder.indent
  end

  def test_it_generates_html_safe_content_if_needed
    source = {:avatar => '<img src="http://example.com/avatar.png">'}

    encoder = Gson::Encoder.new(:html_safe => true)
    expected = '{"avatar":"\u003cimg src\u003d\"http://example.com/avatar.png\"\u003e"}'
    assert_equal expected, encoder.encode(source)

    encoder = Gson::Encoder.new(:html_safe => false)
    expected = '{"avatar":"<img src=\"http://example.com/avatar.png\">"}'
    assert_equal expected, encoder.encode(source)
  end

  def test_it_might_be_configured_to_skip_nils
    source = {"foo" => "bar", "baz" => nil}

    encoder = Gson::Encoder.new(:serialize_nils => true)
    expected = '{"foo":"bar","baz":null}'
    assert_equal expected, encoder.encode(source)

    encoder = Gson::Encoder.new(:serialize_nils => false)
    expected = '{"foo":"bar"}'
    assert_equal expected, encoder.encode(source)
  end

  def test_it_can_prettify_the_output
    source = {"foo" => "bar", "ary" => [1, 2, 3]}

    encoder = Gson::Encoder.new(:indent => "    ")
    expected = <<EOJ.chomp
{
    "foo": "bar",
    "ary": [
        1,
        2,
        3
    ]
}
EOJ
    assert_equal expected, encoder.encode(source)

    encoder = Gson::Encoder.new(:indent => nil)
    expected = '{"foo":"bar","ary":[1,2,3]}'
    assert_equal expected, encoder.encode(source)

    encoder = Gson::Encoder.new(:indent => "")
    expected = '{"foo":"bar","ary":[1,2,3]}'
    assert_equal expected, encoder.encode(source)
  end

  def test_in_lenient_mode_it_allows_primitives_as_top_level_value
    source = 1

    encoder = Gson::Encoder.new(:lenient => true)
    expected = '1'
    assert_equal expected, encoder.encode(source)

    encoder = Gson::Encoder.new(:lenient => false)
    assert_raises Gson::EncodeError do
      encoder.encode(source)
    end
  end

  def test_in_lenient_mode_it_allows_nans_to_be_serialized
    source = {"foo" => 1/0.0}

    encoder = Gson::Encoder.new(:lenient => true)
    expected = '{"foo":Infinity}'
    assert_equal expected, encoder.encode(source)

    encoder = Gson::Encoder.new(:lenient => false)
    assert_raises Gson::EncodeError do
      encoder.encode(source)
    end
  end

end
