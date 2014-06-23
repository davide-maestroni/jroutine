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

import com.bmd.wtf.flg.Gate;
import com.bmd.wtf.flg.Gate.Action;
import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.fll.WaterfallRiver;
import com.bmd.wtf.xtr.dam.DamBasin.BasinEvaluator;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/11/14.
 */
public class Dam<SOURCE, DATA> extends WaterfallRiver<SOURCE, SOURCE> {

    private static final int AFTER_MAX = 0;

    private static final int ALL = 1;

    private static final int COLLECT_DATA = 2;

    private static final int COLLECT_UNHANDLED = 3;

    private static final int EMPTY = 4;

    private static final int EVENTUALLY_THROW = 5;

    private static final int MAX = 6;

    private static final int ON = 7;

    private static final int ON_DATA = 8;

    private static final int ON_FLUSH = 9;

    private static final int ON_THROWABLE = 10;

    private static final int PULL_DATA = 11;

    private static final int PULL_UNHANDLED = 12;

    private static final int SETUP_GATE = 13;

    private static final int WHEN_AVAILABLE = 14;

    private final Action<Object, DamBasin<SOURCE, DATA>> mAction =
            new Action<Object, DamBasin<SOURCE, DATA>>() {

                @Override
                public Object doOn(final DamBasin<SOURCE, DATA> basin, final Object... args) {

                    final int actionType = (Integer) args[0];

                    switch (actionType) {

                        case AFTER_MAX: {

                            basin.afterMax((Long) args[1], (TimeUnit) args[2]);
                            break;
                        }

                        case ALL: {

                            basin.all();
                            break;
                        }

                        case COLLECT_DATA: {

                            if (args.length == 2) {

                                //noinspection unchecked
                                basin.collectData((List<DATA>) args[1]);

                            } else {

                                //noinspection unchecked
                                basin.collectData((Integer) args[1], (List) args[2]);
                            }

                            break;
                        }

                        case COLLECT_UNHANDLED: {

                            if (args.length == 2) {

                                //noinspection unchecked
                                basin.collectUnhandled((List<Throwable>) args[1]);

                            } else {

                                //noinspection unchecked
                                basin.collectUnhandled((Integer) args[1], (List) args[2]);
                            }

                            break;
                        }

                        case EMPTY: {

                            basin.empty();
                            break;
                        }

                        case EVENTUALLY_THROW: {

                            basin.eventuallyThrow((RuntimeException) args[1]);
                            break;
                        }

                        case MAX: {

                            basin.max((Integer) args[1]);
                            break;
                        }

                        case ON: {

                            //noinspection unchecked
                            basin.on(((BasinEvaluator<DATA>) args[1]));
                            break;
                        }

                        case ON_DATA: {

                            basin.onDataAvailable();
                            break;
                        }

                        case ON_FLUSH: {

                            basin.onFlush();
                            break;
                        }

                        case ON_THROWABLE: {

                            basin.onThrowableAvailable();
                            break;
                        }

                        case PULL_DATA: {

                            if (args.length == 1) {

                                return basin.pullData();
                            }

                            return basin.pullData((Integer) args[1]);
                        }

                        case PULL_UNHANDLED: {

                            if (args.length == 1) {

                                return basin.pullUnhandled();
                            }

                            return basin.pullUnhandled((Integer) args[1]);
                        }

                        case SETUP_GATE: {

                            //noinspection unchecked
                            basin.setupGate((Gate<DamBasin<SOURCE, DATA>>) args[1]);
                            break;
                        }

                        case WHEN_AVAILABLE: {

                            basin.whenAvailable();
                            break;
                        }

                        default: {

                            break;
                        }
                    }

                    return null;
                }
            };

    private final Waterfall<SOURCE, DATA, DATA> mInWaterfall;

    private final Waterfall<SOURCE, DATA, DATA> mOutWaterfall;

    private Gate<DamBasin<SOURCE, DATA>> mGate;

    private Dam(final Waterfall<SOURCE, DATA, DATA> waterfall) {

        super(waterfall.source(), true);

        mInWaterfall = waterfall;
        mOutWaterfall = waterfall.asGate().chain(new DamBasin<SOURCE, DATA>(waterfall.size()));
        mGate = mOutWaterfall.when(new Classification<DamBasin<SOURCE, DATA>>() {});
    }

    public static <SOURCE, DATA> Dam<SOURCE, DATA> on(
            final Waterfall<SOURCE, DATA, DATA> waterfall) {

        return new Dam<SOURCE, DATA>(waterfall);
    }

    public Dam<SOURCE, DATA> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mGate.perform(mAction, AFTER_MAX, maxDelay, timeUnit);

        return this;
    }

    public Dam<SOURCE, DATA> all() {

        setupGate().perform(mAction, ALL);

        return this;
    }

    public Dam<SOURCE, DATA> collectData(final List<DATA> bucket) {

        setupGate().perform(mAction, COLLECT_DATA, bucket);

        return this;
    }

    public Dam<SOURCE, DATA> collectData(final int streamNumber, final List<DATA> bucket) {

        setupGate().perform(mAction, COLLECT_DATA, streamNumber, bucket);

        return this;
    }

    public Dam<SOURCE, DATA> collectUnhandled(final List<Throwable> bucket) {

        setupGate().perform(mAction, COLLECT_UNHANDLED, bucket);

        return this;
    }

    public Dam<SOURCE, DATA> collectUnhandled(final int streamNumber,
            final List<Throwable> bucket) {

        setupGate().perform(mAction, COLLECT_UNHANDLED, streamNumber, bucket);

        return this;
    }

    @Override
    public void deviate() {

        mOutWaterfall.deviate(false);
    }

    @Override
    public void deviate(final int streamNumber) {

        mOutWaterfall.deviate(streamNumber, false);
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

    public Dam<SOURCE, DATA> empty() {

        setupGate().perform(mAction, EMPTY);

        return this;
    }

    public Dam<SOURCE, DATA> eventuallyThrow(final RuntimeException exception) {

        mGate.perform(mAction, EVENTUALLY_THROW, exception);

        return this;
    }

    public Dam<SOURCE, DATA> immediately() {

        mGate.perform(mAction, AFTER_MAX, 0, TimeUnit.MILLISECONDS);

        return this;
    }

    public Dam<SOURCE, DATA> max(final int maxCount) {

        setupGate().perform(mAction, MAX, maxCount);

        return this;
    }

    public Dam<SOURCE, DATA> on(final BasinEvaluator<DATA> evaluator) {

        mGate.perform(mAction, ON, evaluator);

        return this;
    }

    public Dam<SOURCE, DATA> onDataAvailable() {

        mGate.perform(mAction, ON_DATA);

        return this;
    }

    public Dam<SOURCE, DATA> onFlush() {

        mGate.perform(mAction, ON_FLUSH);

        return this;
    }

    public Dam<SOURCE, DATA> onThrowableAvailable() {

        mGate.perform(mAction, ON_THROWABLE);

        return this;
    }

    public DATA pullData() {

        //noinspection unchecked
        return (DATA) setupGate().perform(mAction, PULL_DATA);
    }

    public DATA pullData(final int streamNumber) {

        //noinspection unchecked
        return (DATA) setupGate().perform(mAction, PULL_DATA, streamNumber);
    }

    public Throwable pullUnhandled() {

        return (Throwable) setupGate().perform(mAction, PULL_UNHANDLED);
    }

    public Throwable pullUnhandled(final int streamNumber) {

        return (Throwable) setupGate().perform(mAction, PULL_UNHANDLED, streamNumber);
    }

    public Waterfall<SOURCE, DATA, DATA> release() {

        deviate();

        return mInWaterfall;
    }

    public Dam<SOURCE, DATA> whenAvailable() {

        mGate.perform(mAction, WHEN_AVAILABLE);

        return this;
    }

    private Gate<DamBasin<SOURCE, DATA>> setupGate() {

        mGate.perform(mAction, SETUP_GATE, mGate);

        return mGate;
    }
}