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
 * @param <DATA> The data type.
 */
public class WaterfallRiver<DATA> implements River<DATA> {

    private final Direction mDirection;

    private final Waterfall<?, DATA, ?> mWaterfall;

    /**
     * Constructor.
     *
     * @param waterfall The wrapped waterfall.
     * @param direction Whether the river direction is downstream or upstream.
     */
    public WaterfallRiver(final Waterfall<?, DATA, ?> waterfall, final Direction direction) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the wrapped waterfall cannot be null");
        }

        mWaterfall = waterfall;
        mDirection = direction;
    }

    @Override
    public void deviate() {

        mWaterfall.deviate(mDirection);
    }

    @Override
    public void deviateStream(final int streamNumber) {

        mWaterfall.deviateStream(streamNumber, mDirection);
    }

    @Override
    public void drain() {

        mWaterfall.drain(mDirection);
    }

    @Override
    public void drainStream(final int streamNumber) {

        mWaterfall.drainStream(streamNumber, mDirection);
    }

    @Override
    public River<DATA> flush() {

        mWaterfall.flush();

        return this;
    }

    @Override
    public River<DATA> flush(final DATA... drops) {

        mWaterfall.flush(drops);

        return this;
    }

    @Override
    public River<DATA> flush(final Iterable<? extends DATA> drops) {

        mWaterfall.flush(drops);

        return this;
    }

    @Override
    public River<DATA> flush(final DATA drop) {

        mWaterfall.flush(drop);

        return this;
    }

    @Override
    public River<DATA> flushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        mWaterfall.flushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<DATA> flushAfter(final long delay, final TimeUnit timeUnit, final DATA drop) {

        mWaterfall.flushAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<DATA> flushAfter(final long delay, final TimeUnit timeUnit, final DATA... drops) {

        mWaterfall.flushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<DATA> forward(final Throwable throwable) {

        mWaterfall.forward(throwable);

        return this;
    }

    @Override
    public River<DATA> push(final DATA... drops) {

        mWaterfall.push(drops);

        return this;
    }

    @Override
    public River<DATA> push(final Iterable<? extends DATA> drops) {

        mWaterfall.push(drops);

        return this;
    }

    @Override
    public River<DATA> push(final DATA drop) {

        mWaterfall.push(drop);

        return this;
    }

    @Override
    public River<DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        mWaterfall.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<DATA> pushAfter(final long delay, final TimeUnit timeUnit, final DATA drop) {

        mWaterfall.pushAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<DATA> pushAfter(final long delay, final TimeUnit timeUnit, final DATA... drops) {

        mWaterfall.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<DATA> flushStream(final int streamNumber) {

        mWaterfall.flushStream(streamNumber);

        return this;
    }

    @Override
    public River<DATA> flushStream(final int streamNumber, final DATA... drops) {

        mWaterfall.flushStream(streamNumber, drops);

        return this;
    }

    @Override
    public River<DATA> flushStream(final int streamNumber, final Iterable<? extends DATA> drops) {

        mWaterfall.flushStream(streamNumber, drops);

        return this;
    }

    @Override
    public River<DATA> flushStream(final int streamNumber, final DATA drop) {

        mWaterfall.flushStream(streamNumber, drop);

        return this;
    }

    @Override
    public River<DATA> flushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        mWaterfall.flushStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<DATA> flushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mWaterfall.flushStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<DATA> flushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mWaterfall.flushStreamAfter(streamNumber, delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<DATA> forwardStream(final int streamNumber, final Throwable throwable) {

        mWaterfall.forwardStream(streamNumber, throwable);

        return this;
    }

    @Override
    public <TYPE> Gate<TYPE> on(final Class<TYPE> gateClass) {

        return mWaterfall.on(gateClass);
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
    public River<DATA> pushStream(final int streamNumber, final DATA... drops) {

        mWaterfall.pushStream(streamNumber, drops);

        return this;
    }

    @Override
    public River<DATA> pushStream(final int streamNumber, final Iterable<? extends DATA> drops) {

        mWaterfall.pushStream(streamNumber, drops);

        return this;
    }

    @Override
    public River<DATA> pushStream(final int streamNumber, final DATA drop) {

        mWaterfall.pushStream(streamNumber, drop);

        return this;
    }

    @Override
    public River<DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mWaterfall.pushStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mWaterfall.pushStreamAfter(streamNumber, delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        mWaterfall.pushStreamAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public int size() {

        return mWaterfall.size();
    }
}