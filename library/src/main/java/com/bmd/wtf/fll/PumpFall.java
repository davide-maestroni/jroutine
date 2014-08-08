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
import com.bmd.wtf.lps.Gate;

/**
 * Extension of a data fall used to notify a pump gate about the flowing of data drops.
 * <p/>
 * Created by davide on 7/6/14.
 *
 * @param <SOURCE> the river source data type.
 * @param <IN>     the input data type.
 * @param <OUT>    the output data type.
 */
class PumpFall<SOURCE, IN, OUT> extends DataFall<IN, OUT> {

    private final int mFallNumber;

    private final PumpGate mPump;

    /**
     * Constructor.
     *
     * @param waterfall    the containing waterfall.
     * @param inputCurrent the input current.
     * @param gate         the wrapped gate.
     * @param number       the number identifying this fall.
     * @param pumpGate     the related pump.
     */
    public PumpFall(final Waterfall<SOURCE, IN, OUT> waterfall, final Current inputCurrent,
            final Gate<IN, OUT> gate, final int number, final PumpGate pumpGate) {

        super(waterfall, inputCurrent, gate, number);

        mPump = pumpGate;
        mFallNumber = number;
    }

    @Override
    void lowerLevel() {

        super.lowerLevel();

        mPump.lowerLevel(mFallNumber);
    }

    @Override
    void raiseLevel(final int count) {

        mPump.raiseLevel(mFallNumber, count);

        super.raiseLevel(count);
    }
}