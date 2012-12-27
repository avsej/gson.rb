These tests originated at https://github.com/kr/okjson

Except following changes:

* GSON generates floats with uppercase 'E'
* It doesn't renders Bignum's as numbers, it renders Infinity. See
  `*bignum*.json` tests
* GSON understands escaping illegal characters like single quote and
  silently skip the backslash. Therefore these tests were renamed:
  * `invalid15.json` to `decode-invalid15.json{,.exp}`
  * `invalid16.json` to `decode-invalid16.json{,.exp}`
  * `invalid17.json` to `decode-invalid17.json{,.exp}`
* GSON understands verbatim tabs and line breaks in the JSON string.
  Therefore these tests were renamed:
  * `invalid25.json` to `decode-invalid25.json{,.exp}`
  * `invalid26.json` to `decode-invalid26.json{,.exp}`
  * `invalid27.json` to `decode-invalid27.json{,.exp}`
  * `invalid28.json` to `decode-invalid28.json{,.exp}`
