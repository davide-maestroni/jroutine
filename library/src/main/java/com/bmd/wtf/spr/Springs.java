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
     * <p/>
     * Note that data will be pulled from the waterfall when needed. If data are pushed instead
     * into the waterfall, consider calling <code>from(waterfall.collect())</code>.
     *
     * @param waterfall the input waterfall.
     * @param <DATA>    the data type.
     * @return the new spring.
     */
    public static <DATA> Spring<DATA> from(final Waterfall<?, ?, DATA> waterfall) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the spring waterfall cannot be null");
        }

        final Collector<DATA> collector = waterfall.collect();

        return new Spring<DATA>() {

            private boolean mFirst = true;

            @Override
            public boolean hasDrops() {

                if (mFirst) {

                    mFirst = false;

                    waterfall.source().flush();
                }

                return collector.hasNext();
            }

            @Override
            public DATA nextDrop() {

                if (!hasDrops()) {

                    throw new NoSuchElementException();
                }

                return collector.next();
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
    public static Spring<Boolean> randomBools(final Random random, final long count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return sequence(new SequenceIncrement<Boolean>() {

            @Override
            public Boolean first() {

                return random.nextBoolean();
            }

            @Override
            public Boolean next(final Boolean prev) {

                return first();
            }
        }, count);
    }

    /**
     * Creates and returns a spring generating random booleans.
     *
     * @param count the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextBoolean()
     */
    public static Spring<Boolean> randomBools(final long count) {

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
    public static Spring<Byte> randomBytes(final Random random, final long count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return sequence(new SequenceIncrement<Byte>() {

            private final byte[] mBytes = new byte[1];

            @Override
            public Byte first() {

                final byte[] bytes = mBytes;

                random.nextBytes(bytes);

                return bytes[0];
            }

            @Override
            public Byte next(final Byte prev) {

                return first();
            }
        }, count);
    }

    /**
     * Creates and returns a spring generating random bytes.
     *
     * @param count the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextBytes(byte[])
     */
    public static Spring<Byte> randomBytes(final long count) {

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
    public static Spring<Double> randomDoubles(final Random random, final long count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return sequence(new SequenceIncrement<Double>() {

            @Override
            public Double first() {

                return random.nextDouble();
            }

            @Override
            public Double next(final Double prev) {

                return first();
            }
        }, count);
    }

    /**
     * Creates and returns a spring generating random doubles.
     *
     * @param count the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextDouble()
     */
    public static Spring<Double> randomDoubles(final long count) {

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
    public static Spring<Float> randomFloats(final Random random, final long count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return sequence(new SequenceIncrement<Float>() {

            @Override
            public Float first() {

                return random.nextFloat();
            }

            @Override
            public Float next(final Float prev) {

                return first();
            }
        }, count);
    }

    /**
     * Creates and returns a spring generating random doubles.
     *
     * @param count the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextFloat()
     */
    public static Spring<Float> randomFloats(final long count) {

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
    public static Spring<Double> randomGaussian(final Random random, final long count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return sequence(new SequenceIncrement<Double>() {

            @Override
            public Double first() {

                return random.nextGaussian();
            }

            @Override
            public Double next(final Double prev) {

                return first();
            }
        }, count);
    }

    /**
     * Creates and returns a spring generating random gaussian values.
     *
     * @param count the iteration count.
     * @return the new spring.
     * @see java.util.Random#nextGaussian()
     */
    public static Spring<Double> randomGaussian(final long count) {

        return randomGaussian(new Random(), count);
    }

    /**
     * Creates and returns a spring generating random integers.
     *
     * @param random the random instance to be used.
     * @param count  the iteration count.
     * @return the new spring.
     */
    public static Spring<Integer> randomInts(final Random random, final long count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return sequence(new SequenceIncrement<Integer>() {

            @Override
            public Integer first() {

                return random.nextInt();
            }

            @Override
            public Integer next(final Integer prev) {

                return first();
            }
        }, count);
    }

    /**
     * Creates and returns a spring generating random integers.
     *
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Integer> randomInts(final long count) {

        return randomInts(new Random(), count);
    }

    /**
     * Creates and returns a spring generating random longs.
     *
     * @param random the random instance to be used.
     * @param count  the iteration count.
     * @return the new spring.
     */
    public static Spring<Long> randomLongs(final Random random, final long count) {

        if (random == null) {

            throw new IllegalArgumentException("the spring input cannot be null");
        }

        return sequence(new SequenceIncrement<Long>() {

            @Override
            public Long first() {

                return random.nextLong();
            }

            @Override
            public Long next(final Long prev) {

                return first();
            }
        }, count);
    }

    /**
     * Creates and returns a spring generating random longs.
     *
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Long> randomLongs(final long count) {

        return randomLongs(new Random(), count);
    }

    /**
     * Creates a spring generating a sequence of data based on the specified sequence increment.
     * <p/>
     * Note that the element count cannot be negative.
     *
     * @param inc    the sequence increment.
     * @param count  the sequence element count.
     * @param <DATA> the data type.
     * @return the new spring.
     */
    public static <DATA> Spring<DATA> sequence(final SequenceIncrement<DATA> inc,
            final long count) {

        return new SequenceSpring<DATA>(inc, count);
    }

    /**
     * Creates a spring generating a sequence of bytes starting from the specified first value and
     * ending with the last one.
     * <p/>
     * Note that the last value can be less than the first one. In such case the sequence will be
     * in decreasing order.
     *
     * @param first the first value.
     * @param last  the last value.
     * @return the new spring.
     */
    public static Spring<Byte> sequence(final byte first, final byte last) {

        if (first > last) {

            return sequence(new SequenceIncrement<Byte>() {

                @Override
                public Byte first() {

                    return first;
                }

                @Override
                public Byte next(final Byte prev) {

                    return (byte) (prev - 1);
                }
            }, (long) first - (long) last + 1);
        }

        return sequence(new SequenceIncrement<Byte>() {

            @Override
            public Byte first() {

                return first;
            }

            @Override
            public Byte next(final Byte prev) {

                return (byte) (prev + 1);
            }
        }, (long) last - (long) first + 1);
    }

    /**
     * Creates a spring generating a sequence of chars starting from the specified first value and
     * ending with the last one.
     * <p/>
     * Note that the last value can be less than the first one. In such case the sequence will be
     * in decreasing order.
     *
     * @param first the first value.
     * @param last  the last value.
     * @return the new spring.
     */
    public static Spring<Character> sequence(final char first, final char last) {

        if (first > last) {

            return sequence(new SequenceIncrement<Character>() {

                @Override
                public Character first() {

                    return first;
                }

                @Override
                public Character next(final Character prev) {

                    return (char) (prev - 1);
                }
            }, (long) first - (long) last + 1);
        }

        return sequence(new SequenceIncrement<Character>() {

            @Override
            public Character first() {

                return first;
            }

            @Override
            public Character next(final Character prev) {

                return (char) (prev + 1);
            }
        }, (long) last - (long) first + 1);
    }

    /**
     * Creates a spring generating a sequence of ints starting from the specified first value and
     * ending with the last one.
     * <p/>
     * Note that the last value can be less than the first one. In such case the sequence will be
     * in decreasing order.
     *
     * @param first the first value.
     * @param last  the last value.
     * @return the new spring.
     */
    public static Spring<Integer> sequence(final int first, final int last) {

        if (first > last) {

            return sequence(new SequenceIncrement<Integer>() {

                @Override
                public Integer first() {

                    return first;
                }

                @Override
                public Integer next(final Integer prev) {

                    return (prev - 1);
                }
            }, (long) first - (long) last + 1);
        }

        return sequence(new SequenceIncrement<Integer>() {

            @Override
            public Integer first() {

                return first;
            }

            @Override
            public Integer next(final Integer prev) {

                return (prev + 1);
            }
        }, (long) last - (long) first + 1);
    }

    /**
     * Creates a spring generating a sequence of longs starting from the specified first value and
     * ending with the last one.
     * <p/>
     * Note that the last value can be less than the first one. In such case the sequence will be
     * in decreasing order.
     *
     * @param first the first value.
     * @param last  the last value.
     * @return the new spring.
     */
    public static Spring<Long> sequence(final long first, final long last) {

        if (first > 0) {

            if ((last <= 1) && (last < (first - Long.MAX_VALUE + 1))) {

                throw new IllegalArgumentException("the count is out of range");
            }

        } else {

            if ((last >= -2) && (first < (last - Long.MAX_VALUE + 1))) {

                throw new IllegalArgumentException("the count is out of range");
            }
        }

        if (first > last) {

            return sequence(new SequenceIncrement<Long>() {

                @Override
                public Long first() {

                    return first;
                }

                @Override
                public Long next(final Long prev) {

                    return (prev - 1);
                }
            }, first - last + 1);
        }

        return sequence(new SequenceIncrement<Long>() {

            @Override
            public Long first() {

                return first;
            }

            @Override
            public Long next(final Long prev) {

                return (prev + 1);
            }
        }, last - first + 1);
    }

    /**
     * Creates a spring generating a sequence of shorts starting from the specified first value and
     * ending with the last one.
     * <p/>
     * Note that the last value can be less than the first one. In such case the sequence will be
     * in decreasing order.
     *
     * @param first the first value.
     * @param last  the last value.
     * @return the new spring.
     */
    public static Spring<Short> sequence(final short first, final short last) {

        if (first > last) {

            return sequence(new SequenceIncrement<Short>() {

                @Override
                public Short first() {

                    return first;
                }

                @Override
                public Short next(final Short prev) {

                    return (short) (prev - 1);
                }
            }, (long) first - (long) last + 1);
        }

        return sequence(new SequenceIncrement<Short>() {

            @Override
            public Short first() {

                return first;
            }

            @Override
            public Short next(final Short prev) {

                return (short) (prev + 1);
            }
        }, (long) last - (long) first + 1);
    }

    /**
     * Interface defining the increment of a sequence.
     *
     * @param <E> the element type.
     */
    public interface SequenceIncrement<E> {

        /**
         * Returns the first element in the sequence.
         *
         * @return the first element.
         */
        public E first();

        /**
         * Returns the next element in the sequence.
         *
         * @param prev the previous element.
         * @return the next element.
         */
        public E next(E prev);
    }

    /**
     * Spring implementation producing data out of the elements of a sequence.
     *
     * @param <DATA> the data type.
     */
    private static class SequenceSpring<DATA> implements Spring<DATA> {

        private final SequenceIncrement<DATA> mInc;

        private long mCount;

        private DATA mDrop;

        private boolean mFirst = true;

        /**
         * Constructor.
         *
         * @param inc   the sequence increment.
         * @param count the sequence element count.
         */
        public SequenceSpring(final SequenceIncrement<DATA> inc, final long count) {

            if (inc == null) {

                throw new IllegalArgumentException("the sequence increment cannot be null");
            }

            if (count < 0) {

                throw new IllegalArgumentException("the iteration count cannot be negative");
            }

            mInc = inc;
            mCount = count;
        }

        @Override
        public boolean hasDrops() {

            return (mCount > 0);
        }

        @Override
        public DATA nextDrop() {

            if (!hasDrops()) {

                throw new NoSuchElementException();
            }

            if (mFirst) {

                mFirst = false;

                mDrop = mInc.first();

            } else {

                mDrop = mInc.next(mDrop);
            }

            --mCount;

            return mDrop;
        }
    }
}