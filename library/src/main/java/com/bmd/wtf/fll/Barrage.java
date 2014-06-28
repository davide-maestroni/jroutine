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
import com.bmd.wtf.lps.Leap;

/**
 * Leap implementation used to uniformly distribute data flow among the waterfall streams.
 * <p/>
 * The level of each stream is raised and lowered based on the number of data drop entering and
 * then flowing through it.<b/>
 * The next coming data are pushed into the stream with the lower level at that moment.
 * <p/>
 * Created by davide on 6/10/14.
 *
 * @param <SOURCE> The source data type.
 * @param <DATA>   The data type.
 */
class Barrage<SOURCE, DATA> implements Leap<SOURCE, DATA, DATA> {

    private static final int REFRESH_INTERVAL = Integer.MAX_VALUE >> 1;

    private final int[] mStreamLevels;

    private int mUpdateCount;

    public Barrage(final int streamCount) {

        mStreamLevels = new int[streamCount];
    }

    public void lowerLevel(final int streamNumber) {

        --mStreamLevels[streamNumber];

        normalizeLevels();
    }

    @Override
    public void onDischarge(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber) {

        downRiver.discharge();
    }

    @Override
    public void onPush(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber, final DATA drop) {

        final int streamNumber = findMinLevel();

        ++mStreamLevels[streamNumber];

        normalizeLevels();

        downRiver.push(streamNumber, drop);
    }

    @Override
    public void onUnhandled(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber, final Throwable throwable) {

        downRiver.forward(throwable);
    }

    private int findMinLevel() {

        int min = 0;

        int minLevel = Integer.MAX_VALUE;

        final int[] levels = mStreamLevels;

        final int length = levels.length;

        for (int i = 0; i < length; i++) {

            final int level = levels[i];

            if (level < minLevel) {

                minLevel = level;
                min = i;
            }
        }

        return min;
    }

    private void normalizeLevels() {

        if (++mUpdateCount < REFRESH_INTERVAL) {

            return;
        }

        mUpdateCount = 0;

        long sum = 0;

        final int[] levels = mStreamLevels;

        final int length = levels.length;

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < length; i++) {

            sum += levels[i];
        }

        final long mean = sum / length;

        for (int i = 0; i < length; i++) {

            levels[i] -= mean;
        }
    }
}