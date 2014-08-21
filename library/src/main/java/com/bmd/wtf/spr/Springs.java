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

/**
 * Utility class for creating spring instances.
 * <p/>
 * Created by davide on 8/20/14.
 */
public class Springs {

    // TODO: random, etc.

    /**
     * Avoid direct instantiation.
     */
    protected Springs() {

    }

    /**
     * Creates a spring generating a sequence of bytes starting from the specified first value and
     * decreasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Byte> dec(final byte first, final byte count) {

        if ((count < 0) || ((Byte.MIN_VALUE + count) > (first + 1))) {

            throw new IllegalArgumentException();
        }

        return new Spring<Byte>() {

            private byte mNext = first;

            private final byte mLast = (byte) (first - count);

            @Override
            public boolean hasDrops() {

                return (mNext != mLast);
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

    /**
     * Creates a spring generating a sequence of chars starting from the specified first value and
     * decreasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Character> dec(final char first, final char count) {

        if ((count < 0) || ((Character.MAX_VALUE + count) > (first + 1))) {

            throw new IllegalArgumentException();
        }

        return new Spring<Character>() {

            private char mNext = first;

            private final char mLast = (char) (first - count);

            @Override
            public boolean hasDrops() {

                return (mNext != mLast);
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

    /**
     * Creates a spring generating a sequence of ints starting from the specified first value and
     * decreasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Integer> dec(final int first, final int count) {

        if ((count < 0) || ((Integer.MAX_VALUE + count) > (first + 1))) {

            throw new IllegalArgumentException();
        }

        return new Spring<Integer>() {

            private int mNext = first;

            private final int mLast = first - count;

            @Override
            public boolean hasDrops() {

                return (mNext != mLast);
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

    /**
     * Creates a spring generating a sequence of longs starting from the specified first value and
     * decreasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Long> dec(final long first, final long count) {

        if ((count < 0) || ((Long.MAX_VALUE + count) > (first + 1))) {

            throw new IllegalArgumentException();
        }

        return new Spring<Long>() {

            private long mNext = first;

            private final long mLast = first - count;

            @Override
            public boolean hasDrops() {

                return (mNext != mLast);
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

    /**
     * Creates a spring generating a sequence of shorts starting from the specified first value and
     * decreasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Short> dec(final short first, final short count) {

        if ((count < 0) || ((Short.MAX_VALUE + count) > (first + 1))) {

            throw new IllegalArgumentException();
        }

        return new Spring<Short>() {

            private short mNext = first;

            private final short mLast = (short) (first - count);

            @Override
            public boolean hasDrops() {

                return (mNext != mLast);
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

                final boolean isExhausted = (mByte != -1);

                if (isExhausted) {

                    close();
                }

                return isExhausted;
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

                final boolean isExhausted = (mLine != null);

                if (isExhausted) {

                    close();
                }

                return isExhausted;
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

                final boolean isExhausted = (mChar != -1);

                if (isExhausted) {

                    close();
                }

                return isExhausted;
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
     * Creates a spring generating a sequence of bytes starting from the specified first value and
     * increasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Byte> inc(final byte first, final byte count) {

        if ((count < 0) || ((Byte.MAX_VALUE - count) < (first - 1))) {

            throw new IllegalArgumentException();
        }

        return new Spring<Byte>() {

            private byte mNext = first;

            private final byte mLast = (byte) (first + count);

            @Override
            public boolean hasDrops() {

                return (mNext != mLast);
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
     * increasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Character> inc(final char first, final char count) {

        if ((count < 0) || ((Character.MAX_VALUE - count) < (first - 1))) {

            throw new IllegalArgumentException();
        }

        return new Spring<Character>() {

            private char mNext = first;

            private final char mLast = (char) (first + count);

            @Override
            public boolean hasDrops() {

                return (mNext != mLast);
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
     * increasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Integer> inc(final int first, final int count) {

        if ((count < 0) || ((Integer.MAX_VALUE - count) < (first - 1))) {

            throw new IllegalArgumentException();
        }

        return new Spring<Integer>() {

            private int mNext = first;

            private final int mLast = first + count;

            @Override
            public boolean hasDrops() {

                return (mNext != mLast);
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
     * increasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Long> inc(final long first, final long count) {

        if ((count < 0) || ((Long.MAX_VALUE - count) < (first - 1))) {

            throw new IllegalArgumentException();
        }

        return new Spring<Long>() {

            private long mNext = first;

            private final long mLast = first + count;

            @Override
            public boolean hasDrops() {

                return (mNext != mLast);
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
     * increasing it by the specified number of times.
     *
     * @param first the first value.
     * @param count the iteration count.
     * @return the new spring.
     */
    public static Spring<Short> inc(final short first, final short count) {

        if ((count < 0) || ((Short.MAX_VALUE - count) < (first - 1))) {

            throw new IllegalArgumentException();
        }

        return new Spring<Short>() {

            private short mNext = first;

            private final short mLast = (short) (first + count);

            @Override
            public boolean hasDrops() {

                return (mNext != mLast);
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