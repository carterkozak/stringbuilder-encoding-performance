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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@Measurement(iterations = 4, time = 4)
@Warmup(iterations = 2, time = 4)
@Fork(1)
@Threads(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class EncoderBenchmarks {

    @Param ({
            "This is a simple ASCII message",
            "This is a message with unicode \uD83D\uDE0A"
    })
    public String message;

    // avoid clever inlining the string bytes directly into a result.
    @Param ({ "3" })
    public int timesToAppend;

    @Param ({ "UTF-8" })
    public String charsetName;

    public Charset charset;
    public CharsetEncoder encoder;

    public StringBuilder stringBuilder = new StringBuilder();
    public ByteBuffer byteBuffer = ByteBuffer.allocate(8192);
    public CharBuffer charBuffer = CharBuffer.allocate(8192);

    @Setup
    public void setup() {
        charset = Charset.forName(charsetName);
        encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        for (int i = 0; i < timesToAppend; i++) {
            stringBuilder.append(message);
        }
    }

    @Benchmark
    public byte[] toStringGetBytes() {
        byte[] result = stringBuilder.toString().getBytes(charset);
//        System.out.println("Result is: " + new String(result, charset));
        return result;
    }

    @Benchmark
    public ByteBuffer charsetEncoderWithAllocation() throws CharacterCodingException {
        byteBuffer.clear();
        try {
            int limit = stringBuilder.length();
            stringBuilder.getChars(0, limit, charBuffer.array(), charBuffer.arrayOffset());
            charBuffer.position(0);
            charBuffer.limit(limit);
            return encoder.encode(charBuffer);
        } finally {
            charBuffer.clear();
        }
    }

    @Benchmark
    public ByteBuffer charsetEncoderWithAllocationWrappingBuilder() throws CharacterCodingException {
        byteBuffer.clear();
        try {
            return encoder.encode(CharBuffer.wrap(stringBuilder));
        } finally {
            charBuffer.clear();
        }
    }

    @Benchmark
    public ByteBuffer charsetEncoder() throws CharacterCodingException {
        byteBuffer.clear();
        try {
            int limit = stringBuilder.length();
            stringBuilder.getChars(0, limit, charBuffer.array(), charBuffer.arrayOffset());
            charBuffer.position(0);
            charBuffer.limit(limit);
            encodeTo(encoder, charBuffer, byteBuffer);
//            System.out.println("Result is: " + new String(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.arrayOffset() + byteBuffer.limit(), charset));
            return byteBuffer;
        } finally {
            charBuffer.clear();
        }
    }

    /** Encode {@link CharBuffer} {@code in} into {@link ByteBuffer} {@code out}. */
    public static void encodeTo(CharsetEncoder encoder, CharBuffer in, ByteBuffer out)
            throws CharacterCodingException
    {
        if (in.remaining() == 0) {
            return;
        }
        encoder.reset();
        for (;;) {
            CoderResult cr = in.hasRemaining() ?
                    encoder.encode(in, out, true) : CoderResult.UNDERFLOW;
            if (cr.isUnderflow())
                cr = encoder.flush(out);
            if (cr.isUnderflow())
                break;
            // throw if overflow
            cr.throwException();
        }
        out.flip();
    }

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()
                .include(EncoderBenchmarks.class.getSimpleName())
                .build())
                .run();
    }
}
