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
package com.bmd.wtf.xtr.dam;

import com.bmd.wtf.fll.UpstreamRiver;
import com.bmd.wtf.fll.Waterfall;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/11/14.
 */
public class Dam<SOURCE, DATA> extends UpstreamRiver<SOURCE, SOURCE>
        implements CollectorBasin<SOURCE, DATA> {

    private final DamBasin<SOURCE, DATA> mBasin;

    private final Waterfall<SOURCE, DATA, DATA> mWaterfall;

    private Dam(final Waterfall<SOURCE, DATA, DATA> waterfall) {

        super(waterfall.source());

        mBasin = new DamBasin<SOURCE, DATA>(waterfall);
        mWaterfall = waterfall.chain(mBasin);
    }

    public static <SOURCE, DATA> Dam<SOURCE, DATA> build(
            final Waterfall<SOURCE, DATA, DATA> waterfall) {

        return new Dam<SOURCE, DATA>(waterfall);
    }

    @Override
    public Dam<SOURCE, DATA> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mBasin.afterMax(maxDelay, timeUnit);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> all() {

        mBasin.all();

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> collect(final List<DATA> bucket) {

        mBasin.collect(bucket);

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> collect(final int streamNumber, final List<DATA> bucket) {

        mBasin.collect(streamNumber, bucket);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> collectUnhandled(final List<Throwable> bucket) {

        mBasin.collectUnhandled(bucket);

        return this;
    }

    @Override
    public CollectorBasin<SOURCE, DATA> collectUnhandled(final int streamNumber,
            final List<Throwable> bucket) {

        mBasin.collectUnhandled(streamNumber, bucket);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> empty() {

        mBasin.empty();

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> eventuallyThrow(final RuntimeException exception) {

        mBasin.eventuallyThrow(exception);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> immediately() {

        mBasin.immediately();

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> max(final int maxCount) {

        mBasin.max(maxCount);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> onFlush() {

        mBasin.onFlush();

        return this;
    }

    @Override
    public DATA pull() {

        return mBasin.pull();
    }

    @Override
    public DATA pull(final int streamNumber) {

        return mBasin.pull(streamNumber);
    }

    @Override
    public Throwable pullUnhandled() {

        return mBasin.pullUnhandled();
    }

    @Override
    public Throwable pullUnhandled(final int streamNumber) {

        return mBasin.pullUnhandled(streamNumber);
    }

    @Override
    public Waterfall<SOURCE, DATA, DATA> release() {

        return mBasin.release();
    }

    @Override
    public Dam<SOURCE, DATA> when(final Condition<SOURCE, DATA> condition) {

        mBasin.when(condition);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> whenAvailable() {

        mBasin.whenAvailable();

        return this;
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
    public Dam<SOURCE, DATA> flush(final int streamNumber) {

        super.flush(streamNumber);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> flush() {

        super.flush();

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> forward(final Throwable throwable) {

        super.forward(throwable);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> push(final SOURCE... drops) {

        super.push(drops);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> push(final Iterable<? extends SOURCE> drops) {

        super.push(drops);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> push(final SOURCE drop) {

        super.push(drop);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends SOURCE> drops) {

        super.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final SOURCE drop) {

        super.pushAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final SOURCE... drops) {

        super.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> forward(final int streamNumber, final Throwable throwable) {

        super.forward(streamNumber, throwable);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> push(final int streamNumber, final SOURCE... drops) {

        super.push(streamNumber, drops);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> push(final int streamNumber, final Iterable<? extends SOURCE> drops) {

        super.push(streamNumber, drops);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> push(final int streamNumber, final SOURCE drop) {

        super.push(streamNumber, drop);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends SOURCE> drops) {

        super.pushAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final SOURCE drop) {

        super.pushAfter(streamNumber, delay, timeUnit, drop);

        return this;
    }

    @Override
    public Dam<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final SOURCE... drops) {

        super.pushAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }
}