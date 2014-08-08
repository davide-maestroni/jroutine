/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHDATA WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.wtf.fll;

import com.bmd.wtf.flw.Pump;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.AbstractGate;

/**
 * Gate implementation used to uniformly distribute the flow of data among the waterfall streams.
 * <p/>
 * The level of each stream is raised and lowered based on the number of data drop flowing through
 * it.<b/>
 * By default, the next coming data are pushed into the stream with the lower level at that moment.
 * <p/>
 * Created by davide on 6/10/14.
 *
 * @param <DATA> the data type.
 */
class PumpGate<DATA> extends AbstractGate<DATA, DATA> implements Pump<DATA> {

    private static final int REFRESH_INTERVAL = Integer.MAX_VALUE >> 1;

    private final Object mMutex = new Object();

    private final Pump<DATA> mPump;

    private final int[] mStreamLevels;

    private int mStartStream;

    private int mUpdateCount;

    /**
     * Constructor.
     * <p/>
     * When no pump is specified the drops of data will always be pushed into the default
     * stream.
     *
     * @param streamCount the total number of streams.
     */
    public PumpGate(final int streamCount) {

        mPump = this;
        mStreamLevels = new int[streamCount];
    }

    /**
     * Constructor.
     *
     * @param pump        the pump.
     * @param streamCount the total number of streams.
     */
    public PumpGate(final Pump<DATA> pump, final int streamCount) {

        if (pump == null) {

            throw new IllegalArgumentException("the pump cannot be null");
        }

        mPump = pump;
        mStreamLevels = new int[streamCount];
    }

    @Override
    public int hashCode() {

        return (mPump == this) ? super.hashCode() : mPump.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof PumpGate)) {

            return false;
        }

        final PumpGate that = (PumpGate) o;

        return (mPump == this) ? super.equals(that.mPump) : mPump.equals(that.mPump);
    }

    /**
     * Lowers the level of the stream identified by the specified number.
     *
     * @param streamNumber the stream number.
     */
    public void lowerLevel(final int streamNumber) {

        synchronized (mMutex) {

            --mStreamLevels[streamNumber];

            normalizeLevels(1);
        }
    }

    @Override
    public void onPush(final River<DATA> upRiver, final River<DATA> downRiver, final int fallNumber,
            final DATA drop) {

        final int streamNumber = mPump.onPush(drop);

        if (streamNumber == DEFAULT_STREAM) {

            downRiver.pushStream(findMinLevel(), drop);

        } else if (streamNumber == ALL_STREAMS) {

            downRiver.push(drop);

        } else if (streamNumber != NO_STREAM) {

            downRiver.pushStream(streamNumber, drop);
        }
    }

    @Override
    public int onPush(final DATA drop) {

        return DEFAULT_STREAM;
    }

    /**
     * Raises the level of the stream identified by the specified number by the specified count.
     *
     * @param streamNumber the stream number.
     * @param count        the number of level to raise.
     */
    public void raiseLevel(final int streamNumber, final int count) {

        synchronized (mMutex) {

            mStreamLevels[streamNumber] += count;

            normalizeLevels(count);
        }
    }

    private int findMinLevel() {

        synchronized (mMutex) {

            final int[] levels = mStreamLevels;
            final int length = levels.length;

            int stream = mStartStream;
            int minLevel = levels[stream];

            mStartStream = (stream + 1) % length;

            for (int i = 0; i < length; ++i) {

                final int level = levels[i];

                if (level < minLevel) {

                    minLevel = level;
                    stream = i;
                }
            }

            return stream;
        }
    }

    private void normalizeLevels(final int count) {

        if ((mUpdateCount += count) < REFRESH_INTERVAL) {

            return;
        }

        mUpdateCount = 0;

        long sum = 0;

        final int[] levels = mStreamLevels;
        final int length = levels.length;

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < length; ++i) {

            sum += levels[i];
        }

        final long mean = sum / length;

        for (int i = 0; i < length; ++i) {

            levels[i] -= mean;
        }
    }
}