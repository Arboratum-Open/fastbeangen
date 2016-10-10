package com.arboratum.beangen.util;

import lombok.Getter;

/**
 * The RandomSequence is designed to be fast.  It is not thread safe.
 *
 * It is using the XORShift* (https://en.wikipedia.org/wiki/Xorshift#xorshift.2A) to regenerate a pseudo-random sequence of 64 bit (long) number.
 * The provided seed is mixed by applying repeatedly the formula X = (X + PRIME1) * PRIME2 until the X contains at least 20 bits sets
 *
 * Created by gpicron on 08/08/2016.
 */
@Getter
public class RandomSequence {
    private final long seed;
    private long register;

    public RandomSequence(long id) {
        this.seed = id;
        if (id < 0) throw new IllegalArgumentException("only positive id are supported");
        do {
            id += 17l;
            id ^= (id << 21);
            id ^= (id >>> 35);
            id ^= (id << 4);
        } while (Long.bitCount(id) < 20);


        register = id;
    }


    private final long shift(long x) {
        x ^= (x >> 12);
        x ^= (x << 25);
        x ^= (x >> 27);
        return x * 2685821657736338717L;
    }

    public long nextLong() {
        return register = shift(register);
    }

    public int nextInt() {
        return (int) (register = shift(register));
    }

    private static final long MASK_31_BITS = (1L << 31) - 1L;

    public int nextInt(int n) {
        if (n > 0) {
            if ((n & -n) == n) {
                long bits = (register = shift(register));
                return (int) ((n * (bits & MASK_31_BITS)) >> 31);
            }
            long x = register;
            int bits;
            int val;
            do {
                x = shift(x);
                bits = (int) (x & MASK_31_BITS);
                val = bits % n;
            } while (bits - val + (n - 1) < 0);

            register = x;

            return val;
        }
        throw new IllegalArgumentException("Not positive");
    }

    private static final long MASK_63_BITS = (1L << 63) - 1L;

    public long nextLong(long bound) {
        long x = shift(register);
        long r = x;
        long n = bound, m = n - 1;
        if ((n & m) == 0L)  // power of two
            r = (r & m);
        else if (n > 0L) {  // reject over-represented candidates
            for (long u = r >>> 1;            // ensure nonnegative
                 u + m - (r = u % n) < 0L;    // rejection check
                 x = shift(x),
                 u = x >>> 1) // retry
                ;

        }
        else {              // range not representable as long
            while (r < 0 || r >= bound) {
                x = shift(x);
                r = x;
            }

        }

        register = x;

        return r;
    }


    private static final long DOUBLE_MASK = (1L << 53) - 1L;
    public static final double DOUBLE_STEP = 0x1.0p-53;

    public double nextDouble() {
        long bits = (register = shift(register));
        return (bits & DOUBLE_MASK) * DOUBLE_STEP;
    }

    private static final long FLOAT_MASK = (1L << 24) - 1L;
    private static final float FLOAT_STEP = 0x1.0p-24f;

    public float nextFloat() {
        long bits = (register = shift(register));
        return (bits & FLOAT_MASK - 1L) * FLOAT_STEP;
    }

    public boolean nextBoolean() {
        long bits = (register = shift(register));
        return bits > 0;
    }

    public void nextBytes(byte[] bytes) {
        for (int i = 0, len = bytes.length; i < len; )
            for (long rnd = nextLong(),
                 n = Math.min(len - i, Long.SIZE/Byte.SIZE);
                 n-- > 0; rnd >>= Byte.SIZE)
                bytes[i++] = (byte)rnd;
    }

}
