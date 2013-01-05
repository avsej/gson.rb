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

  class Custom
    attr_accessor :foo, :bar

    def initialize(foo, bar)
      @foo = foo
      @bar = bar
    end

    def as_json(options = {})
      {:foo => @foo, :bar => @bar}
    end
  end

  def test_it_dumps_custom_objects_wich_implement_as_json
    encoder = Gson::Encoder.new
    expected = '{"foo":1,"bar":2}'
    assert_equal expected, encoder.encode(Custom.new(1, 2))
  end

  def test_it_converts_unknown_objects_to_string
      time = Time.at(1355218745).utc

      # time does not respond to to_json method
      def time.respond_to?(method, *args)
        return false if method == :to_json
        super
      end

      encoder = Gson::Encoder.new
      decoder = Gson::Decoder.new

      dumped_json = encoder.encode(time)
      expected = if RUBY_VERSION > '1.9'
                   '2012-12-11 09:39:05 UTC'
                 else
                   'Tue Dec 11 09:39:05 UTC 2012'
                 end
      assert_equal(expected, decoder.decode(dumped_json))
  end

end
