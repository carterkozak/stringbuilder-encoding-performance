## Developing:

### Intellij Idea
Open via Gradle integration with Intellij

### Eclipse
`./gradlew eclipse` + import

## Running

You can run the benchmarks from the command line with:

`./gradlew jmh`

Or build a jmh jar (based on instructions from the [jmh-gradle-plugin](https://github.com/melix/jmh-gradle-plugin)) which produces `build/libs/stringbuilder-encoding-performance-jmh.jar`:

`./gradlew jmhJar`

Alternatively via the main method in your IDE.

## Existing results

Initial results on e37e7651509adf66c78bee041f62719ca2ef1cb1

### Java 11
```
Benchmark                                                      (charsetName)                          (message)  (timesToAppend)  Mode  Cnt    Score    Error  Units
EncoderBenchmarks.charsetEncoder                                       UTF-8     This is a simple ASCII message                3  avgt    4   62.079 ±  4.741  ns/op
EncoderBenchmarks.charsetEncoder                                       UTF-8  This is a message with unicode 😊                3  avgt    4  189.813 ±  6.188  ns/op
EncoderBenchmarks.charsetEncoderWithAllocation                         UTF-8     This is a simple ASCII message                3  avgt    4   82.448 ± 18.847  ns/op
EncoderBenchmarks.charsetEncoderWithAllocation                         UTF-8  This is a message with unicode 😊                3  avgt    4  199.352 ±  9.499  ns/op
EncoderBenchmarks.charsetEncoderWithAllocationWrappingBuilder          UTF-8     This is a simple ASCII message                3  avgt    4  402.621 ± 22.275  ns/op
EncoderBenchmarks.charsetEncoderWithAllocationWrappingBuilder          UTF-8  This is a message with unicode 😊                3  avgt    4  462.582 ± 37.584  ns/op
EncoderBenchmarks.toStringGetBytes                                     UTF-8     This is a simple ASCII message                3  avgt    4   21.845 ±  0.973  ns/op
EncoderBenchmarks.toStringGetBytes                                     UTF-8  This is a message with unicode 😊                3  avgt    4  150.277 ±  8.610  ns/op
```

### Java 17 EA (`zulu17.0.81-ea-jdk17.0.0-ea.35-linux_x64`)
```
Benchmark                                                      (charsetName)                          (message)  (timesToAppend)  Mode  Cnt    Score    Error  Units
EncoderBenchmarks.charsetEncoder                                       UTF-8     This is a simple ASCII message                3  avgt    4   68.529 ±  4.686  ns/op
EncoderBenchmarks.charsetEncoder                                       UTF-8  This is a message with unicode 😊                3  avgt    4  142.830 ± 60.421  ns/op
EncoderBenchmarks.charsetEncoderWithAllocation                         UTF-8     This is a simple ASCII message                3  avgt    4   75.724 ±  2.927  ns/op
EncoderBenchmarks.charsetEncoderWithAllocation                         UTF-8  This is a message with unicode 😊                3  avgt    4  151.600 ± 28.248  ns/op
EncoderBenchmarks.charsetEncoderWithAllocationWrappingBuilder          UTF-8     This is a simple ASCII message                3  avgt    4  287.453 ± 18.314  ns/op
EncoderBenchmarks.charsetEncoderWithAllocationWrappingBuilder          UTF-8  This is a message with unicode 😊                3  avgt    4  344.048 ±  3.704  ns/op
EncoderBenchmarks.toStringGetBytes                                     UTF-8     This is a simple ASCII message                3  avgt    4   23.662 ±  0.792  ns/op
EncoderBenchmarks.toStringGetBytes                                     UTF-8  This is a message with unicode 😊                3  avgt    4  122.379 ±  5.602  ns/op
```

### Why doesn't this match my expectations?

`StringBuilder.toString().getBytes(StandardCharsets.UTF_8)` is a common pattern, and it's fantastic that the performance is much better than it used to be! However, it requires allocation and may strain the garbage collector depending on usage patterns outside of logging. In log4j, we try to avoid unnecessary allocation because it makes performance less uniform and predictable, and some of our consumers require zero-garbage steady state logging.

By reusing a few buffers with a pattern similar to the `EncoderBenchmarks.charsetEncoder` benchmark, we can avoid allocation overhead, however the performance is worse than `toString().getBytes(charset)` for compact strings, and roughly equivalent for Strings that use multi-byte characters.

### Other oddities?

Our approach of writing characters from the StringBuilder into a (reused) HeapCharBuffer looked odd at first glance, however if I wrap the StringBuilder itself in a `CharBuffer.wrap` (StringCharBuffer) our throughput drops in half. This is true both if I wrap the `StringBuilder` or the result of `stringBuilder.toString()`. Perhaps there's some specialization we could apply to encoded string wrappers (e.g. StringBuilder, StringBuffer, String) to better match `String.getBytes(charset)` performance?

I haven't verified the performance yet (I may have time to write benchmarks later), but it looks like this would limit the performance of `OutputStreamWriter` compared to `OutputStream.write(charSequence.toString().getBytes(cs))`.