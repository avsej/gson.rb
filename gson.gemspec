# -*- encoding: utf-8 -*-
# Author:: Couchbase <info@couchbase.com>
# Copyright:: 2012 Couchbase, Inc.
# License:: Apache License, Version 2.0
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'gson/version'

Gem::Specification.new do |gem|
  gem.name          = "gson"
  gem.version       = Gson::VERSION
  gem.author        = "Sergey Avseyev"
  gem.email         = "sergey.avseyev@gmail.com"
  gem.description   = %q{Ruby wrapper for GSON. https://code.google.com/p/google-gson/}
  gem.summary       = %q{Ruby wrapper for GSON}
  gem.homepage      = "https://github.com/avsej/gson.rb"

  gem.files         = `git ls-files`.split($/)
  gem.executables   = gem.files.grep(%r{^bin/}).map{ |f| File.basename(f) }
  gem.test_files    = gem.files.grep(%r{^(test|spec|features)/})
  gem.require_paths = ["lib"]

  gem.add_development_dependency 'rake-compiler', '>= 0.7.5'
  gem.add_development_dependency 'minitest', '>= 4.7.5'
end
