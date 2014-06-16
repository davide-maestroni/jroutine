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

import com.bmd.wtf.flw.Glass;
import com.bmd.wtf.flw.Reflection;
import com.bmd.wtf.flw.River;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/7/14.
 */
public class UpstreamRiver<SOURCE, DATA> implements River<SOURCE, DATA> {

    private final Waterfall<SOURCE, DATA, ?> mWaterfall;

    public UpstreamRiver(final Waterfall<SOURCE, DATA, ?> waterfall) {

        mWaterfall = waterfall;
    }

    @Override
    public void drain() {

        mWaterfall.drain(false);
    }

    @Override
    public void drain(final int streamNumber) {

        mWaterfall.drain(streamNumber, false);
    }

    @Override
    public void dryUp() {

        mWaterfall.dryUp(false);
    }

    @Override
    public void dryUp(final int streamNumber) {

        mWaterfall.dryUp(streamNumber, false);
    }

    @Override
    public River<SOURCE, DATA> flush(final int streamNumber) {

        mWaterfall.flush(streamNumber);

        return this;
    }

    @Override
    public River<SOURCE, DATA> flush() {

        mWaterfall.flush();

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
    public <CLASS> Reflection<CLASS> when(final Glass<CLASS> glass) {

        return mWaterfall.when(glass);
    }
}