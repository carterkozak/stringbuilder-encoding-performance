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

Initial results on f806c2c6256c14a6b303fa38eba83e7c196ac937

```
Benchmark                                                      (charsetName)                          (message)  (timesToAppend)  Mode  Cnt    Score    Error  Units
EncoderBenchmarks.charsetEncoder                                       UTF-8     This is a simple ASCII message                3  avgt    4   94.929 ±  1.609  ns/op
EncoderBenchmarks.charsetEncoder                                       UTF-8  This is a message with unicode 😊                3  avgt    4  176.099 ± 14.827  ns/op
EncoderBenchmarks.charsetEncoderWithAllocation                         UTF-8     This is a simple ASCII message                3  avgt    4  117.231 ±  5.081  ns/op
EncoderBenchmarks.charsetEncoderWithAllocation                         UTF-8  This is a message with unicode 😊                3  avgt    4  197.668 ± 16.875  ns/op
EncoderBenchmarks.charsetEncoderWithAllocationWrappingBuilder          UTF-8     This is a simple ASCII message                3  avgt    4  284.429 ± 12.907  ns/op
EncoderBenchmarks.charsetEncoderWithAllocationWrappingBuilder          UTF-8  This is a message with unicode 😊                3  avgt    4  419.487 ±  5.926  ns/op
EncoderBenchmarks.toStringGetBytes                                     UTF-8     This is a simple ASCII message                3  avgt    4   45.050 ±  7.921  ns/op
EncoderBenchmarks.toStringGetBytes                                     UTF-8  This is a message with unicode 😊                3  avgt    4  164.701 ± 38.575  ns/op
```
