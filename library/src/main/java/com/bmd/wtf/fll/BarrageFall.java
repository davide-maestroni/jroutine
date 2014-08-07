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
 * Extension of a data fall used to notify a barrage leap about the flowing of data drops.
 * <p/>
 * Created by davide on 7/6/14.
 *
 * @param <SOURCE> the river source data type.
 * @param <IN>     the input data type.
 * @param <OUT>    the output data type.
 */
class BarrageFall<SOURCE, IN, OUT> extends DataFall<IN, OUT> {

    private final BarrageLeap mBarrage;

    private final int mFallNumber;

    /**
     * Constructor.
     *
     * @param waterfall    the containing waterfall.
     * @param inputCurrent the input current.
     * @param leap         the wrapped leap.
     * @param number       the number identifying this fall.
     * @param barrageLeap  the related barrage.
     */
    public BarrageFall(final Waterfall<SOURCE, IN, OUT> waterfall, final Current inputCurrent,
            final Leap<IN, OUT> leap, final int number, final BarrageLeap barrageLeap) {

        super(waterfall, inputCurrent, leap, number);

        mBarrage = barrageLeap;
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