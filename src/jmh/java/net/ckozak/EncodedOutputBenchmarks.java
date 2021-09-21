package net.ckozak;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@Measurement(iterations = 4, time = 4)
@Warmup(iterations = 2, time = 4)
@Fork(1)
@Threads(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class EncodedOutputBenchmarks {

    public enum Message {
        ASCII() {
            @Override
            String message() {
                return """
            This is a simple ASCII message. Lorem ipsum dolor sit amet, consectetur adipiscing elit,
            sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
            """;
            }
        },
        UNICODE() {
            @Override
            String message() {
                return """
            This is a message with unicode 😊 Lorem ipsum dolor sit amet, consectetur adipiscing elit,
            sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
            """;
            }
        };

        abstract String message();
    }

    @Param
    public Message msg;

    @Param ({ "UTF-8" })
    public String charsetName;

    public String message;
    public Charset charset;
    public OutputStream outputStream;
    public OutputStreamWriter outputStreamWriter;

    @Setup
    public void setup(Blackhole blackhole) throws Exception {
        charset = Charset.forName(charsetName);
        outputStream = new OutputStream() {
            @Override
            public void write(int b) {
                blackhole.consume(b);
            }

            @Override
            public void write(byte[] buf) {
                blackhole.consume(buf);
            }

            @Override
            public void write(byte[] buf, int off, int len) {
                blackhole.consume(buf);
                blackhole.consume(off);
                blackhole.consume(len);
            }
        };
        outputStreamWriter = new OutputStreamWriter(outputStream, charset);
        message = msg.message();
    }

    @TearDown
    public void tearDown() throws Exception {
        if (outputStream != null) {
            outputStream.close();
        }
        if (outputStreamWriter != null) {
            outputStreamWriter.close();
        }
    }

    @Benchmark
    public void outputStreamWriter() throws Exception {
        outputStreamWriter.write(message);
    }

    @Benchmark
    public void getBytesToOutputStream() throws Exception {
        outputStream.write(message.getBytes(charset));
    }

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()
                .include(EncodedOutputBenchmarks.class.getSimpleName())
                .build())
                .run();
    }
}
