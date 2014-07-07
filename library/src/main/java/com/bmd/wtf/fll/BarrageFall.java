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
package com.bmd.wtf.fll;

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.lps.Leap;

/**
 * Extension of a data fall used to notify a barrage about the flowing of data drops.
 * <p/>
 * Created by davide on 7/6/14.
 *
 * @param <SOURCE> The river source data type.
 * @param <IN>     The input data type.
 * @param <OUT>    The output data type.
 */
public class BarrageFall<SOURCE, IN, OUT> extends DataFall<SOURCE, IN, OUT> {

    private final Barrage mBarrage;

    private final int mFallNumber;

    /**
     * Constructor.
     *
     * @param waterfall    The containing waterfall.
     * @param inputCurrent The input current.
     * @param leap         The wrapped leap.
     * @param number       The number identifying this fall.
     * @param barrage      The related barrage.
     */
    public BarrageFall(final Waterfall<SOURCE, IN, OUT> waterfall, final Current inputCurrent,
            final Leap<SOURCE, IN, OUT> leap, final int number, final Barrage barrage) {

        super(waterfall, inputCurrent, leap, number);

        mBarrage = barrage;
        mFallNumber = number;
    }

    @Override
    void lowerLevel() {

        super.lowerLevel();

        mBarrage.lowerLevel(mFallNumber);
    }

    @Override
    void raiseLevel(final int count) {

        mBarrage.raiseLevel(mFallNumber, count);

        super.raiseLevel(count);
    }
}