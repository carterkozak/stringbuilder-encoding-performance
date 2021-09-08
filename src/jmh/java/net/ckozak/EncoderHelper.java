package net.ckozak;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

final class EncoderHelper {

    /** Encode {@link CharBuffer} {@code in} into {@link ByteBuffer} {@code out}. */
    static void encodeTo(CharsetEncoder encoder, CharBuffer in, ByteBuffer out)
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
    }

    private static final sun.misc.Unsafe unsafe;
    private static final long coderOffset;
    private static final long valueOffset;
    private static final long countOffset;

    static {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (sun.misc.Unsafe) field.get(null);
            Class<?> abstractStringBuilder = Class.forName("java.lang.AbstractStringBuilder");
            coderOffset = unsafe.objectFieldOffset(abstractStringBuilder.getDeclaredField("coder"));
            valueOffset = unsafe.objectFieldOffset(abstractStringBuilder.getDeclaredField("value"));
            countOffset = unsafe.objectFieldOffset(abstractStringBuilder.getDeclaredField("count"));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static boolean unsafeEncodeUTF8(StringBuilder buf, ByteBuffer destination) {
        byte coder = unsafe.getByte(buf, coderOffset);
        if (coder == 0) {
            int len = unsafe.getInt(buf, countOffset);
            byte[] value = (byte[]) unsafe.getObject(buf, valueOffset);
            return unsafeEncodeUTF8FastPath(coder, value, len, destination);
        }
        return false;
    }

    static boolean unsafeEncodeUTF8FastPath(byte coder, byte[] val, int valLength, ByteBuffer destination) {
        if (coder == 0 &&
                // Cannot access the StringCoder.hasNegatives intrinsic
                !hasNegatives(val, 0, valLength)) {
            destination.put(val, 0, valLength);
            return true;
        }
        return false;
    }

    static boolean hasNegatives(byte[] ba, int off, int len) {
        for (int i = off; i < off + len; i++) {
            if (ba[i] < 0) {
                return true;
            }
        }
        return false;
    }

    private EncoderHelper() {}
}
