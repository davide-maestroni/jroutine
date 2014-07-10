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

import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.AbstractLeap;

/**
 * Leap implementation used to uniformly distribute the flow of data among the waterfall streams.
 * <p/>
 * The level of each stream is raised and lowered based on the number of data drop flowing through
 * it.<b/>
 * The next coming data are pushed into the stream with the lower level at that moment.
 * <p/>
 * Created by davide on 6/10/14.
 *
 * @param <SOURCE> The source data type.
 * @param <DATA>   The data type.
 */
class Barrage<SOURCE, DATA> extends AbstractLeap<SOURCE, DATA, DATA> {

    private static final int REFRESH_INTERVAL = Integer.MAX_VALUE >> 1;

    private final Object mMutex = new Object();

    private final int[] mStreamLevels;

    private int mStartStream;

    private int mUpdateCount;

    /**
     * Constructor.
     *
     * @param streamCount The total number of streams.
     */
    public Barrage(final int streamCount) {

        mStreamLevels = new int[streamCount];
    }

    /**
     * Lowers the level of the stream identified by the specified number.
     *
     * @param streamNumber The stream number.
     */
    public void lowerLevel(final int streamNumber) {

        synchronized (mMutex) {

            --mStreamLevels[streamNumber];

            normalizeLevels(1);
        }
    }

    @Override
    public void onPush(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber, final DATA drop) {

        downRiver.pushStream(findMinLevel(), drop);
    }

    /**
     * Raises the level of the stream identified by the specified number by the specified count.
     *
     * @param streamNumber The stream number.
     * @param count        The number of level to raise.
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