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
 * Created by davide on 6/7/14.
 */
public class WaterfallRiver<SOURCE, DATA> implements River<SOURCE, DATA> {

    private final boolean mIsDownstream;

    private final Waterfall<SOURCE, DATA, ?> mWaterfall;

    public WaterfallRiver(final Waterfall<SOURCE, DATA, ?> waterfall, final boolean isDownstream) {

        mWaterfall = waterfall;
        mIsDownstream = isDownstream;
    }

    @Override
    public void deviate() {

        mWaterfall.deviate(mIsDownstream);
    }

    @Override
    public void deviate(final int streamNumber) {

        mWaterfall.deviate(streamNumber, mIsDownstream);
    }

    @Override
    public River<SOURCE, DATA> discharge() {

        mWaterfall.discharge();

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
    public River<SOURCE, DATA> discharge(final int streamNumber) {

        mWaterfall.discharge(streamNumber);

        return this;
    }

    @Override
    public void drain() {

        mWaterfall.drain(mIsDownstream);
    }

    @Override
    public void drain(final int streamNumber) {

        mWaterfall.drain(streamNumber, mIsDownstream);
    }

    @Override
    public River<SOURCE, DATA> forward(final int streamNumber, final Throwable throwable) {

        mWaterfall.forward(streamNumber, throwable);

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final int streamNumber, final DATA... drops) {

        mWaterfall.push(streamNumber, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final int streamNumber, final Iterable<? extends DATA> drops) {

        mWaterfall.push(streamNumber, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final int streamNumber, final DATA drop) {

        mWaterfall.push(streamNumber, drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mWaterfall.pushAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mWaterfall.pushAfter(streamNumber, delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        mWaterfall.pushAfter(streamNumber, delay, timeUnit, drops);

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

    @Override
    public <TYPE> Gate<TYPE> when(final Class<TYPE> gateType) {

        return mWaterfall.when(gateType);
    }

    @Override
    public <TYPE> Gate<TYPE> when(final Classification<TYPE> gateClassification) {

        return mWaterfall.when(gateClassification);
    }
}