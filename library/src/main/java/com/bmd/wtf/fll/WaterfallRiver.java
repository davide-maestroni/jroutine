/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KDATAD, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.wtf.fll;

import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.flw.River;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of a river wrapping a waterfall instance.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <SOURCE> The source data type.
 * @param <DATA>   The data type.
 */
public class WaterfallRiver<SOURCE, DATA> implements River<SOURCE, DATA> {

    private final boolean mIsDownstream;

    private final Waterfall<SOURCE, DATA, ?> mWaterfall;

    /**
     * Constructor.
     *
     * @param waterfall    The wrapped waterfall.
     * @param isDownstream Whether the river direction is downstream.
     */
    public WaterfallRiver(final Waterfall<SOURCE, DATA, ?> waterfall, final boolean isDownstream) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the wrapped waterfall cannot be null");
        }

        mWaterfall = waterfall;
        mIsDownstream = isDownstream;
    }

    @Override
    public void deviate() {

        mWaterfall.deviate(mIsDownstream);
    }

    @Override
    public void deviateStream(final int streamNumber) {

        mWaterfall.deviate(streamNumber, mIsDownstream);
    }

    @Override
    public River<SOURCE, DATA> discharge() {

        mWaterfall.discharge();

        return this;
    }

    @Override
    public River<SOURCE, DATA> discharge(final DATA... drops) {

        mWaterfall.discharge(drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> discharge(final Iterable<? extends DATA> drops) {

        mWaterfall.discharge(drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> discharge(final DATA drop) {

        mWaterfall.discharge(drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> dischargeAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        mWaterfall.dischargeAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> dischargeAfter(final long delay, final TimeUnit timeUnit,
            final DATA drop) {

        mWaterfall.dischargeAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> dischargeAfter(final long delay, final TimeUnit timeUnit,
            final DATA... drops) {

        mWaterfall.dischargeAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> forward(final Throwable throwable) {

        mWaterfall.forward(throwable);

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final DATA... drops) {

        mWaterfall.push(drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final Iterable<? extends DATA> drops) {

        mWaterfall.push(drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final DATA drop) {

        mWaterfall.push(drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        mWaterfall.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final DATA drop) {

        mWaterfall.pushAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final DATA... drops) {

        mWaterfall.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> dischargeStream(final int streamNumber) {

        mWaterfall.dischargeStream(streamNumber);

        return this;
    }

    @Override
    public River<SOURCE, DATA> dischargeStream(final int streamNumber, final DATA... drops) {

        mWaterfall.dischargeStream(streamNumber, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> dischargeStream(final int streamNumber,
            final Iterable<? extends DATA> drops) {

        mWaterfall.dischargeStream(streamNumber, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> dischargeStream(final int streamNumber, final DATA drop) {

        mWaterfall.dischargeStream(streamNumber, drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> dischargeStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mWaterfall.dischargeStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> dischargeStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mWaterfall.dischargeStreamAfter(streamNumber, delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> dischargeStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        mWaterfall.dischargeStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public void drain() {

        mWaterfall.drain(mIsDownstream);
    }

    @Override
    public void drainStream(final int streamNumber) {

        mWaterfall.drain(streamNumber, mIsDownstream);
    }

    @Override
    public River<SOURCE, DATA> forwardStream(final int streamNumber, final Throwable throwable) {

        mWaterfall.forwardStream(streamNumber, throwable);

        return this;
    }

    @Override
    public <TYPE> Gate<TYPE> on(final Class<TYPE> gateType) {

        return mWaterfall.on(gateType);
    }

    @Override
    public <TYPE> Gate<TYPE> on(final TYPE leap) {

        return mWaterfall.on(leap);
    }

    @Override
    public <TYPE> Gate<TYPE> on(final Classification<TYPE> gateClassification) {

        return mWaterfall.on(gateClassification);
    }

    @Override
    public River<SOURCE, DATA> pushStream(final int streamNumber, final DATA... drops) {

        mWaterfall.pushStream(streamNumber, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushStream(final int streamNumber,
            final Iterable<? extends DATA> drops) {

        mWaterfall.pushStream(streamNumber, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushStream(final int streamNumber, final DATA drop) {

        mWaterfall.pushStream(streamNumber, drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mWaterfall.pushStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mWaterfall.pushStreamAfter(streamNumber, delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        mWaterfall.pushStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public int size() {

        return mWaterfall.size();
    }

    @Override
    public River<SOURCE, SOURCE> source() {

        return mWaterfall.source();
    }
}