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
import com.bmd.wtf.flw.Glass;
import com.bmd.wtf.flw.Reflection;
import com.bmd.wtf.xtr.dam.Dam.BasinEvaluator;
import com.bmd.wtf.xtr.dam.Dam.DamEvaluatorBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/11/14.
 */
public class DamBasin<SOURCE, DATA> extends UpstreamRiver<SOURCE, SOURCE>
        implements CollectorBasin<SOURCE, DATA> {

    private final Dam<SOURCE, DATA> mDam;

    private final Waterfall<SOURCE, DATA, DATA> mInWaterfall;

    private final Waterfall<SOURCE, DATA, DATA> mOutWaterfall;

    private CollectorBasin<SOURCE, DATA> mBasin;

    private DamEvaluatorBuilder<SOURCE, DATA> mEvaluatorBuilder;

    private Reflection<CollectorBasin<SOURCE, DATA>> mReflection;

    DamBasin(final Waterfall<SOURCE, DATA, DATA> waterfall) {

        super(waterfall.source());

        mDam = new Dam<SOURCE, DATA>(waterfall.size());
        mInWaterfall = waterfall;
        mOutWaterfall = waterfall.asGlass().chain(mDam);
    }

    public DamBasin<SOURCE, DATA> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        getReflection().afterMax(maxDelay, timeUnit);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> all() {

        getBasin().all();

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> collect(final List<DATA> bucket) {

        getBasin().collect(bucket);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> collect(final int streamNumber, final List<DATA> bucket) {

        getBasin().collect(streamNumber, bucket);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> collectUnhandled(final List<Throwable> bucket) {

        getBasin().collectUnhandled(bucket);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> collectUnhandled(final int streamNumber,
            final List<Throwable> bucket) {

        getBasin().collectUnhandled(streamNumber, bucket);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> empty() {

        getBasin().empty();

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> max(final int maxCount) {

        getBasin().max(maxCount);

        return this;
    }

    @Override
    public DATA pull() {

        return getBasin().pull();
    }

    @Override
    public DATA pull(final int streamNumber) {

        return getBasin().pull(streamNumber);
    }

    @Override
    public Throwable pullUnhandled() {

        return getBasin().pullUnhandled();
    }

    @Override
    public Throwable pullUnhandled(final int streamNumber) {

        return getBasin().pullUnhandled(streamNumber);
    }

    @Override
    public void drain() {

        mOutWaterfall.drain(false);
    }

    @Override
    public void drain(final int streamNumber) {

        mOutWaterfall.drain(streamNumber, false);
    }

    @Override
    public void dryUp() {

        mOutWaterfall.dryUp(false);
    }

    @Override
    public void dryUp(final int streamNumber) {

        mOutWaterfall.dryUp(streamNumber, false);
    }

    @Override
    public DamBasin<SOURCE, DATA> flush(final int streamNumber) {

        super.flush(streamNumber);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> flush() {

        super.flush();

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> forward(final Throwable throwable) {

        super.forward(throwable);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> push(final SOURCE... drops) {

        super.push(drops);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> push(final Iterable<? extends SOURCE> drops) {

        super.push(drops);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> push(final SOURCE drop) {

        super.push(drop);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends SOURCE> drops) {

        super.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final SOURCE drop) {

        super.pushAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final SOURCE... drops) {

        super.pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> forward(final int streamNumber, final Throwable throwable) {

        super.forward(streamNumber, throwable);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> push(final int streamNumber, final SOURCE... drops) {

        super.push(streamNumber, drops);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> push(final int streamNumber,
            final Iterable<? extends SOURCE> drops) {

        super.push(streamNumber, drops);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> push(final int streamNumber, final SOURCE drop) {

        super.push(streamNumber, drop);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends SOURCE> drops) {

        super.pushAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final SOURCE drop) {

        super.pushAfter(streamNumber, delay, timeUnit, drop);

        return this;
    }

    @Override
    public DamBasin<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final SOURCE... drops) {

        super.pushAfter(streamNumber, delay, timeUnit, drops);

        return this;
    }

    public DamBasin<SOURCE, DATA> eventuallyThrow(final RuntimeException exception) {

        getReflection().eventuallyThrow(exception);

        return this;
    }

    public DamBasin<SOURCE, DATA> immediately() {

        getReflection().afterMax(0, TimeUnit.MILLISECONDS);

        return this;
    }

    public DamBasin<SOURCE, DATA> on(final BasinEvaluator<DATA> evaluator) {

        getEvaluatorBuilder().on(evaluator);

        return this;
    }

    public DamBasin<SOURCE, DATA> onDataAvailable() {

        getEvaluatorBuilder().onDataAvailable();

        return this;
    }

    public DamBasin<SOURCE, DATA> onFlush() {

        getEvaluatorBuilder().onFlush();

        return this;
    }

    public DamBasin<SOURCE, DATA> onThrowableAvailable() {

        getEvaluatorBuilder().onThrowableAvailable();

        return this;
    }

    public Waterfall<SOURCE, DATA, DATA> release() {

        mOutWaterfall.drain();

        return mInWaterfall;
    }

    public DamBasin<SOURCE, DATA> whenAvailable() {

        getReflection().eventually();

        return this;
    }

    private CollectorBasin<SOURCE, DATA> getBasin() {

        if (mBasin == null) {

            mBasin = mReflection.matches(mEvaluatorBuilder.match()).perform();
            mReflection = null;
        }

        return mBasin;
    }

    private DamEvaluatorBuilder<SOURCE, DATA> getEvaluatorBuilder() {

        if (mEvaluatorBuilder == null) {

            mEvaluatorBuilder = mDam.evaluator();
        }

        return mEvaluatorBuilder;
    }

    private Reflection<CollectorBasin<SOURCE, DATA>> getReflection() {

        if (mReflection == null) {

            mBasin = null;
            mEvaluatorBuilder = null;
            mReflection = mOutWaterfall.when(new Glass<CollectorBasin<SOURCE, DATA>>() {});
        }

        return mReflection;
    }
}