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
    public ByteBuffer unsafeAccess() throws CharacterCodingException {
        ByteBuffer byteBuffer = this.byteBuffer;
        StringBuilder stringBuilder = this.stringBuilder;
        byteBuffer.clear();
        if (!EncoderHelper.unsafeEncodeUTF8(stringBuilder, byteBuffer)) {
            CharBuffer charBuffer = this.charBuffer;
            try {
                int limit = stringBuilder.length();
                stringBuilder.getChars(0, limit, charBuffer.array(), charBuffer.arrayOffset());
                charBuffer.position(0);
                charBuffer.limit(limit);
                EncoderHelper.encodeTo(encoder, charBuffer, byteBuffer);
            } finally {
                charBuffer.clear();
            }
        }
        byteBuffer.flip();
//        System.out.println("Result is: " + new String(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.arrayOffset() + byteBuffer.limit(), charset));
        return byteBuffer;
    }

    @Benchmark
    public ByteBuffer encoderStringCharArray() throws CharacterCodingException {
        return encoder.encode(CharBuffer.wrap(stringBuilder.toString().toCharArray()));
    }

    @Benchmark
    public ByteBuffer charsetEncoderWithAllocation() throws CharacterCodingException {
        CharBuffer charBuffer = this.charBuffer;
        StringBuilder stringBuilder = this.stringBuilder;
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
        return encoder.encode(CharBuffer.wrap(stringBuilder));
    }

    @Benchmark
    public ByteBuffer charsetEncoder() throws CharacterCodingException {
        ByteBuffer byteBuffer = this.byteBuffer;
        CharBuffer charBuffer = this.charBuffer;
        StringBuilder stringBuilder = this.stringBuilder;
        byteBuffer.clear();
        try {
            int limit = stringBuilder.length();
            stringBuilder.getChars(0, limit, charBuffer.array(), charBuffer.arrayOffset());
            charBuffer.position(0);
            charBuffer.limit(limit);
            EncoderHelper.encodeTo(encoder, charBuffer, byteBuffer);
            byteBuffer.flip();
//            System.out.println("Result is: " + new String(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.arrayOffset() + byteBuffer.limit(), charset));
            return byteBuffer;
        } finally {
            charBuffer.clear();
        }
    }

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder()
                .include(EncoderBenchmarks.class.getSimpleName())
                .build())
                .run();
    }
}
