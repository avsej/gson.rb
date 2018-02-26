# gson.rb

Ruby wrapper for [google-gson][1] library

[![Build Status](https://travis-ci.org/avsej/gson.rb.png)][2]

## Installation

Add this line to your application's Gemfile:

    gem 'gson'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install gson

## Usage

### Encoding

    Gson::Encoder.new.encode({"abc" => [123, -456.789]})
    => "{\"abc\":[123,-456.789]}"

`Gson::Decoder#decode` also accept optional IO or StringIO object:

    File.open("/tmp/gson.json", "w+") do |io|
      Gson::Encoder.new.encode({"foo" => "bar"}, io)
    end
    File.read("/tmp/gson.json")
    => "{\"foo\":\"bar\"}"

Additional encoder options:

* `:html_safe`, default `false`, force encoder to wrte JSON that is
  safe for inclusion in HTML and XML documents

        source = {:avatar => '<img src="http://example.com/avatar.png">'}
        Gson::Encoder.new(:html_safe => true).encode(source)
        => "{\"avatar\":\"\\u003cimg src\\u003d\\\"http://example.com/avatar.png\\\"\\u003e\"}"

* `:serialize_nils`, default `true`, force encoder to write object
  members if their value is `nil`. This has no impact on array
  elements.

* `:indent`, default `""`, a string containing a full set of spaces
  for a single level of indentation. `nil` or `""` (empty string)
  means not pretty printing.

* `:lenient`, default `true`, configure encoder to relax its syntax
  rules. Setting it to lenient permits the following:

  * top-level values of any type. With strict writing, the top-level
    value must be an object or an array

  * numbers may be NaNs or infinities

### Decoding

    Gson::Decoder.new.decode('{"abc":[123,-456.789e0]}')
    => {"abc"=>[123, -456.789]}

`Gson::Decoder#decode` also accept IO or StringIO objects:

    Gson::Decoder.new.decode(File.open("valid-object-single.json"))
    => {"a"=>"b"}

Additional decoder options:

* `:symbolize_keys`, default `false`, force all property names decoded
  to ruby symbols instead of strings.

        Gson::Decoder.new(:symbolize_keys => true).decode('{"a":"b"}')
        => {:a=>"b"}

* `:lenient`, default `true`, configure this parser to be  be liberal
  in what it accepts:

  * streams that start with the non-execute prefix, `")]}'\n"`

  * streams that include multiple top-level values. With strict
    parsing, each stream must contain exactly one top-level value

  * top-level values of any type. With strict parsing, the top-level
    value must be an object or an array

  * numbers may be NaNs or infinities

  * end of line comments starting with `//` or`#` and ending with a
    newline character

  * C-style comments starting with `/*` and ending with `*/`. Such
    comments may not be nested

  * names that are unquoted or `'single quoted'`

  * strings that are unquoted or `'single quoted'`

  * array elements separated by `;` instead of `,`

  * names and values separated by `=` or `=>` instead of `:`

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

[1]: https://code.google.com/p/google-gson/
[2]: https://travis-ci.org/avsej/gson.rb
