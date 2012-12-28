These benchmarks originated at https://github.com/brianmario/yajl-ruby

Benchmark using (ohai.json) 32444 bytes of JSON data 1000 times

## ENCODING JSON

    Rehearsal ----------------------------------------------------------------------
    Gson::Encoder#encode (to a String)   1.410000   0.030000   1.440000 (  0.872000)
    JSON.generate                        1.200000   0.000000   1.200000 (  0.514000)
    ------------------------------------------------------------- total: 2.640000sec

                                            user     system      total        real
    Gson::Encoder#encode (to a String)   0.700000   0.010000   0.710000 (  0.611000)
    JSON.generate                        0.800000   0.010000   0.810000 (  0.436000)


## DECODING JSON

    Rehearsal ------------------------------------------------------------------------
    Gson::Decoder#decode (from an IO)      3.130000   0.020000   3.150000 (  1.823000)
    Gson::Decoder#decode (from a String)   1.170000   0.010000   1.180000 (  1.129000)
    JSON.parse                             1.580000   0.000000   1.580000 (  1.332000)
    --------------------------------------------------------------- total: 5.910000sec

                                              user     system      total        real
    Gson::Decoder#decode (from an IO)      1.350000   0.000000   1.350000 (  1.236000)
    Gson::Decoder#decode (from a String)   1.120000   0.000000   1.120000 (  1.101000)
    JSON.parse                             1.300000   0.000000   1.300000 (  1.198000)
