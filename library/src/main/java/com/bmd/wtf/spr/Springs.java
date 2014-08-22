/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.wtf.spr;

import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.Collector;
import com.bmd.wtf.flw.FloatingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * Utility class for creating spring instances.
 * <p/>
 * Created by davide on 8/20/14.
 */
public class Springs {

    /**
     * Avoid direct instantiation.
     */
    protected Springs() {

    }

    /**
     * Creates and returns a spring starting from the specified input stream.
     * <p/>
     * Note that the input stream will be automatically closed when no more data are available or
     * an error occurs.
     *
     * @param input the input stream.
     * @return the new spring.
     */
    public static Spring<Byte> from(final InputStream input) {

        if (input == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return new Spring<Byte>() {

            private int mByte = -2;

            @Override
            public boolean hasDrops() {

                if (mByte == -2) {

                    try {

                        mByte = input.read();

                    } catch (final IOException e) {

                        close();

                        throw new FloatingException(e);
                    }
                }

                final boolean isExhausted = (mByte == -1);

                if (isExhausted) {

                    close();
                }

                return !isExhausted;
            }

            @Override
            public Byte nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                final byte drop = (byte) (mByte & 0xff);

                try {

                    mByte = input.read();

                } catch (final IOException e) {

                    close();

                    throw new FloatingException(e);
                }

                return drop;
            }

            private void close() {

                try {

                    input.close();

                } catch (final IOException e) {

                    throw new FloatingException(e);
                }
            }
        };
    }

    /**
     * Creates and returns a spring starting from the specified waterfall.
     *
     * @param waterfall the input waterfall.
     * @param <DATA>    the data type.
     * @return the new spring.
     */
    public static <DATA> Spring<DATA> from(final Waterfall<?, ?, DATA> waterfall) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the spring waterfall cannot be null");
        }

        return new Spring<DATA>() {

            private Collector<DATA> mCollector;

            @Override
            public boolean hasDrops() {

                if (mCollector == null) {

                    mCollector = waterfall.pull();
                }

                return mCollector.hasNext();
            }

            @Override
            public DATA nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                return mCollector.next();
            }
        };
    }

    /**
     * Creates and returns a spring starting from the specified reader.
     * <p/>
     * Note that the input reader will be automatically closed when no more data are available or
     * an error occurs.
     *
     * @param input the input reader.
     * @return the new spring.
     */
    public static Spring<String> from(final BufferedReader input) {

        if (input == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return new Spring<String>() {

            private boolean mFirst = true;

            private String mLine;

            @Override
            public boolean hasDrops() {

                if (mFirst) {

                    mFirst = false;

                    try {

                        mLine = input.readLine();

                    } catch (final IOException e) {

                        close();

                        throw new FloatingException(e);
                    }
                }

                final boolean isExhausted = (mLine == null);

                if (isExhausted) {

                    close();
                }

                return !isExhausted;
            }

            @Override
            public String nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                final String drop = mLine;

                try {

                    mLine = input.readLine();

                } catch (final IOException e) {

                    close();

                    throw new FloatingException(e);
                }

                return drop;
            }

            private void close() {

                try {

                    input.close();

                } catch (final IOException e) {

                    throw new FloatingException(e);
                }
            }
        };
    }

    /**
     * Creates and returns a spring starting from the specified reader.
     * <p/>
     * Note that the input reader will be automatically closed when no more data are available or
     * an error occurs.
     *
     * @param input the input reader.
     * @return the new spring.
     */
    public static Spring<Character> from(final Reader input) {

        if (input == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return new Spring<Character>() {

            private boolean mFirst = true;

            private int mChar;

            @Override
            public boolean hasDrops() {

                if (mFirst) {

                    mFirst = false;

                    try {

                        mChar = input.read();

                    } catch (final IOException e) {

                        close();

                        throw new FloatingException(e);
                    }
                }

                final boolean isExhausted = (mChar == -1);

                if (isExhausted) {

                    close();
                }

                return !isExhausted;
            }

            @Override
            public Character nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                final char drop = (char) (mChar & 0xffff);

                try {

                    mChar = input.read();

                } catch (final IOException e) {

                    close();

                    throw new FloatingException(e);
                }

                return drop;
            }

            private void close() {

                try {

                    input.close();

                } catch (final IOException e) {

                    throw new FloatingException(e);
                }
            }
        };
    }

    /**
     * Creates and returns a spring starting from the specified char sequence.
     *
     * @param input the input sequence.
     * @return the new spring.
     */
    public static Spring<Character> from(final CharSequence input) {

        if (input == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return new Spring<Character>() {

            private int mCount;

            @Override
            public boolean hasDrops() {

                return (mCount < input.length());
            }

            @Override
            public Character nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                return input.charAt(mCount++);
            }
        };
    }

    /**
     * Creates and returns a spring starting from the specified data drops.
     *
     * @param drops  the drops of data.
     * @param <DATA> the data type.
     * @return the new spring.
     */
    public static <DATA> Spring<DATA> from(final DATA... drops) {

        if (drops == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        final int length = drops.length;

        return new Spring<DATA>() {

            private int mCount;

            @Override
            public boolean hasDrops() {

                return (mCount < length);
            }

            @Override
            public DATA nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                return drops[mCount++];
            }
        };
    }

    /**
     * Creates and returns a spring starting from the specified data drops iterable.
     *
     * @param drops  the iterable returning the drops of data.
     * @param <DATA> the data type.
     * @return the new spring.
     */
    public static <DATA> Spring<DATA> from(final Iterable<DATA> drops) {

        if (drops == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return from(drops.iterator());
    }

    /**
     * Creates and returns a spring starting from the specified data drops iterator.
     *
     * @param drops  the iterator returning the drops of data.
     * @param <DATA> the data type.
     * @return the new spring.
     */
    public static <DATA> Spring<DATA> from(final Iterator<DATA> drops) {

        if (drops == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return new Spring<DATA>() {

            @Override
            public boolean hasDrops() {

                return drops.hasNext();
            }

            @Override
            public DATA nextDrop() {

                return drops.next();
            }
        };
    }

    /**
     * Creates and returns a spring generating random booleans.
     *
     * @param random the random instance to be used.
     * @param count  the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextBoolean()
     */
    public static Spring<Boolean> randomBools(final Random random, final int count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        if (count < 0) {

            throw new IllegalArgumentException("the iteration count cannot be negative");
        }

        return new Spring<Boolean>() {

            private int mCount;

            @Override
            public boolean hasDrops() {

                return (mCount < count);
            }

            @Override
            public Boolean nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                final boolean next = random.nextBoolean();

                ++mCount;

                return next;
            }
        };
    }

    /**
     * Creates and returns a spring generating random booleans.
     *
     * @param count the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextBoolean()
     */
    public static Spring<Boolean> randomBools(final int count) {

        return randomBools(new Random(), count);
    }

    /**
     * Creates and returns a spring generating random bytes.
     *
     * @param random the random instance to be used.
     * @param count  the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextBytes(byte[])
     */
    public static Spring<Byte> randomBytes(final Random random, final int count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        if (count < 0) {

            throw new IllegalArgumentException("the iteration count cannot be negative");
        }

        return new Spring<Byte>() {

            private final byte[] mBytes = new byte[1];

            private int mCount;

            @Override
            public boolean hasDrops() {

                return (mCount < count);
            }

            @Override
            public Byte nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                final byte[] bytes = mBytes;

                random.nextBytes(bytes);

                ++mCount;

                return bytes[0];
            }
        };
    }

    /**
     * Creates and returns a spring generating random bytes.
     *
     * @param count the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextBytes(byte[])
     */
    public static Spring<Byte> randomBytes(final int count) {

        return randomBytes(new Random(), count);
    }

    /**
     * Creates and returns a spring generating random doubles.
     *
     * @param random the random instance to be used.
     * @param count  the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextDouble()
     */
    public static Spring<Double> randomDoubles(final Random random, final int count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        if (count < 0) {

            throw new IllegalArgumentException("the iteration count cannot be negative");
        }

        return new Spring<Double>() {

            private int mCount;

            @Override
            public boolean hasDrops() {

                return (mCount < count);
            }

            @Override
            public Double nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                final double next = random.nextDouble();

                ++mCount;

                return next;
            }
        };
    }

    /**
     * Creates and returns a spring generating random doubles.
     *
     * @param count the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextDouble()
     */
    public static Spring<Double> randomDoubles(final int count) {

        return randomDoubles(new Random(), count);
    }

    /**
     * Creates and returns a spring generating random floats.
     *
     * @param random the random instance to be used.
     * @param count  the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextFloat()
     */
    public static Spring<Float> randomFloats(final Random random, final int count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        if (count < 0) {

            throw new IllegalArgumentException("the iteration count cannot be negative");
        }

        return new Spring<Float>() {

            private int mCount;

            @Override
            public boolean hasDrops() {

                return (mCount < count);
            }

            @Override
            public Float nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                final float next = random.nextFloat();

                ++mCount;

                return next;
            }
        };
    }

    /**
     * Creates and returns a spring generating random doubles.
     *
     * @param count the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextFloat()
     */
    public static Spring<Float> randomFloats(final int count) {

        return randomFloats(new Random(), count);
    }

    /**
     * Creates and returns a spring generating random gaussian values.
     *
     * @param random the random instance to be used.
     * @param count  the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextGaussian()
     */
    public static Spring<Double> randomGaussian(final Random random, final int count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        if (count < 0) {

            throw new IllegalArgumentException("the iteration count cannot be negative");
        }

        return new Spring<Double>() {

            private int mCount;

            @Override
            public boolean hasDrops() {

                return (mCount < count);
            }

            @Override
            public Double nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                final double next = random.nextGaussian();

                ++mCount;

                return next;
            }
        };
    }

    /**
     * Creates and returns a spring generating random gaussian values.
     *
     * @param count the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextGaussian()
     */
    public static Spring<Double> randomGaussian(final int count) {

        return randomGaussian(new Random(), count);
    }

    /**
     * Creates and returns a spring generating random integers.
     *
     * @param random the random instance to be used.
     * @param count  the iteration count.
     * @return the new spring.
     */
    public static Spring<Integer> randomInts(final Random random, final int count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        if (count < 0) {

            throw new IllegalArgumentException("the iteration count cannot be negative");
        }

        return new Spring<Integer>() {

            private int mCount;

            @Override
            public boolean hasDrops() {

                return (mCount < count);
            }

            @Override
            public Integer nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                final int next = random.nextInt();

                ++mCount;

                return next;
            }
        };
    }

    /**
     * Creates and returns a spring generating random integers.
     *
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Integer> randomInts(final int count) {

        return randomInts(new Random(), count);
    }

    /**
     * Creates and returns a spring generating random longs.
     *
     * @param random the random instance to be used.
     * @param count  the iteration count.
     * @return the new spring.
     */
    public static Spring<Long> randomLongs(final Random random, final int count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        if (count < 0) {

            throw new IllegalArgumentException("the iteration count cannot be negative");
        }

        return new Spring<Long>() {

            private int mCount;

            @Override
            public boolean hasDrops() {

                return (mCount < count);
            }

            @Override
            public Long nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                final long next = random.nextLong();

                ++mCount;

                return next;
            }
        };
    }

    /**
     * Creates and returns a spring generating random longs.
     *
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Long> randomLongs(final int count) {

        return randomLongs(new Random(), count);
    }

    /**
     * Creates a spring generating a sequence of bytes starting from the specified first value and
     * increasing or decreasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count: increasing if positive and decreasing if negative.
     * @return the new spring.
     */
    public static Spring<Byte> sequence(final byte first, final long count) {

        if (count < 0) {

            if ((Byte.MIN_VALUE - count) > first) {

                throw new IllegalArgumentException();
            }

            return new Spring<Byte>() {

                private boolean mIsLast = false;

                private byte mNext = first;

                private final byte mLast = (byte) (first + count);

                @Override
                public boolean hasDrops() {

                    if (mIsLast) {

                        return (mNext == mLast);
                    }

                    mIsLast = (mNext == mLast);

                    return true;
                }

                @Override
                public Byte nextDrop() {

                    if (!hasDrops()) {

                        throw new NoSuchElementException();
                    }

                    return mNext--;
                }
            };
        }

        if ((count != 0) && ((Byte.MAX_VALUE - count) < first)) {

            throw new IllegalArgumentException();
        }

        return new Spring<Byte>() {

            private boolean mIsLast = false;

            private byte mNext = first;

            private final byte mLast = (byte) (first + count);

            @Override
            public boolean hasDrops() {

                if (mIsLast) {

                    return (mNext == mLast);
                }

                mIsLast = (mNext == mLast);

                return true;
            }

            @Override
            public Byte nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                return mNext++;
            }
        };
    }

    /**
     * Creates a spring generating a sequence of chars starting from the specified first value and
     * increasing or decreasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count: increasing if positive and decreasing if negative.
     * @return the new spring.
     */
    public static Spring<Character> sequence(final char first, final long count) {

        if (count < 0) {

            if ((Character.MIN_VALUE - count) > first) {

                throw new IllegalArgumentException();
            }

            return new Spring<Character>() {

                private boolean mIsLast = false;

                private char mNext = first;

                private final char mLast = (char) (first + count);

                @Override
                public boolean hasDrops() {

                    if (mIsLast) {

                        return (mNext == mLast);
                    }

                    mIsLast = (mNext == mLast);

                    return true;
                }

                @Override
                public Character nextDrop() {

                    if (!hasDrops()) {

                        throw new NoSuchElementException();
                    }

                    return mNext--;
                }
            };
        }

        if ((count != 0) && ((Character.MAX_VALUE - count) < first)) {

            throw new IllegalArgumentException();
        }

        return new Spring<Character>() {

            private boolean mIsLast = false;

            private char mNext = first;

            private final char mLast = (char) (first + count);

            @Override
            public boolean hasDrops() {

                if (mIsLast) {

                    return (mNext == mLast);
                }

                mIsLast = (mNext == mLast);

                return true;
            }

            @Override
            public Character nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                return mNext++;
            }
        };
    }

    /**
     * Creates a spring generating a sequence of ints starting from the specified first value and
     * increasing or decreasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count: increasing if positive and decreasing if negative.
     * @return the new spring.
     */
    public static Spring<Integer> sequence(final int first, final long count) {

        if (count < 0) {

            if ((Integer.MIN_VALUE - count) > first) {

                throw new IllegalArgumentException();
            }

            return new Spring<Integer>() {

                private boolean mIsLast = false;

                private int mNext = first;

                private final int mLast = (int) (first + count);

                @Override
                public boolean hasDrops() {

                    if (mIsLast) {

                        return (mNext == mLast);
                    }

                    mIsLast = (mNext == mLast);

                    return true;
                }

                @Override
                public Integer nextDrop() {

                    if (!hasDrops()) {

                        throw new NoSuchElementException();
                    }

                    return mNext--;
                }
            };
        }

        if ((count != 0) && ((Integer.MAX_VALUE - count) < first)) {

            throw new IllegalArgumentException();
        }

        return new Spring<Integer>() {

            private boolean mIsLast = false;

            private int mNext = first;

            private final int mLast = (int) (first + count);

            @Override
            public boolean hasDrops() {

                if (mIsLast) {

                    return (mNext == mLast);
                }

                mIsLast = (mNext == mLast);

                return true;
            }

            @Override
            public Integer nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                return mNext++;
            }
        };
    }

    /**
     * Creates a spring generating a sequence of longs starting from the specified first value and
     * increasing or decreasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count: increasing if positive and decreasing if negative.
     * @return the new spring.
     */
    public static Spring<Long> sequence(final long first, final long count) {

        if (count < 0) {

            if ((Long.MIN_VALUE - count) > first) {

                throw new IllegalArgumentException();
            }

            return new Spring<Long>() {

                private boolean mIsLast = false;

                private long mNext = first;

                private final long mLast = (long) (first + count);

                @Override
                public boolean hasDrops() {

                    if (mIsLast) {

                        return (mNext == mLast);
                    }

                    mIsLast = (mNext == mLast);

                    return true;
                }

                @Override
                public Long nextDrop() {

                    if (!hasDrops()) {

                        throw new NoSuchElementException();
                    }

                    return mNext--;
                }
            };
        }

        if ((count != 0) && ((Long.MAX_VALUE - count) < first)) {

            throw new IllegalArgumentException();
        }

        return new Spring<Long>() {

            private boolean mIsLast = false;

            private long mNext = first;

            private final long mLast = (long) (first + count);

            @Override
            public boolean hasDrops() {

                if (mIsLast) {

                    return (mNext == mLast);
                }

                mIsLast = (mNext == mLast);

                return true;
            }

            @Override
            public Long nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                return mNext++;
            }
        };
    }

    /**
     * Creates a spring generating a sequence of shorts starting from the specified first value and
     * increasing or decreasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count: increasing if positive and decreasing if negative.
     * @return the new spring.
     */
    public static Spring<Short> sequence(final short first, final long count) {

        if (count < 0) {

            if ((Short.MIN_VALUE - count) > first) {

                throw new IllegalArgumentException();
            }

            return new Spring<Short>() {

                private boolean mIsLast = false;

                private short mNext = first;

                private final short mLast = (short) (first + count);

                @Override
                public boolean hasDrops() {

                    if (mIsLast) {

                        return (mNext == mLast);
                    }

                    mIsLast = (mNext == mLast);

                    return true;
                }

                @Override
                public Short nextDrop() {

                    if (!hasDrops()) {

                        throw new NoSuchElementException();
                    }

                    return mNext--;
                }
            };
        }

        if ((count != 0) && ((Short.MAX_VALUE - count) < first)) {

            throw new IllegalArgumentException();
        }

        return new Spring<Short>() {

            private boolean mIsLast = false;

            private short mNext = first;

            private final short mLast = (short) (first + count);

            @Override
            public boolean hasDrops() {

                if (mIsLast) {

                    return (mNext == mLast);
                }

                mIsLast = (mNext == mLast);

                return true;
            }

            @Override
            public Short nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                return mNext++;
            }
        };
    }
}