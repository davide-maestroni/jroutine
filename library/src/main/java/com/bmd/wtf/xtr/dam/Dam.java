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
import com.bmd.wtf.flg.GateControl;
import com.bmd.wtf.flg.GateControl.Action;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.fll.WaterfallRiver;
import com.bmd.wtf.xtr.dam.DamBasin.BasinEvaluator;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/11/14.
 */
public class Dam<SOURCE, DATA> extends WaterfallRiver<SOURCE, SOURCE> {

    private static final Action<Void, DamBasin<?, ?>> AFTER_MAX =
            new Action<Void, DamBasin<?, ?>>() {

                @Override
                public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

                    basin.afterMax((Long) args[0], (TimeUnit) args[1]);

                    return null;
                }
            };

    private static final Action<Void, DamBasin<?, ?>> ALL = new Action<Void, DamBasin<?, ?>>() {

        @Override
        public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

            basin.all();

            return null;
        }
    };

    private static final Action<Void, DamBasin<?, ?>> COLLECT_DATA =
            new Action<Void, DamBasin<?, ?>>() {

                @Override
                public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

                    if (args.length == 1) {

                        //noinspection unchecked
                        basin.collectData((List) args[0]);

                    } else {

                        //noinspection unchecked
                        basin.collectData((Integer) args[0], (List) args[1]);
                    }

                    return null;
                }
            };

    private static final Action<Void, DamBasin<?, ?>> COLLECT_UNHANDLED =
            new Action<Void, DamBasin<?, ?>>() {

                @Override
                public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

                    if (args.length == 1) {

                        //noinspection unchecked
                        basin.collectUnhandled((List) args[0]);

                    } else {

                        //noinspection unchecked
                        basin.collectUnhandled((Integer) args[0], (List) args[1]);
                    }

                    return null;
                }
            };

    private static final Action<Void, DamBasin<?, ?>> EMPTY = new Action<Void, DamBasin<?, ?>>() {

        @Override
        public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

            basin.empty();

            return null;
        }
    };

    private static final Action<Void, DamBasin<?, ?>> EVENTUALLY_THROW =
            new Action<Void, DamBasin<?, ?>>() {

                @Override
                public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

                    basin.eventuallyThrow((RuntimeException) args[0]);

                    return null;
                }
            };

    private static final Action<Void, DamBasin<?, ?>> MAX = new Action<Void, DamBasin<?, ?>>() {

        @Override
        public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

            basin.max((Integer) args[0]);

            return null;
        }
    };

    private static final Action<Void, DamBasin<?, ?>> ON = new Action<Void, DamBasin<?, ?>>() {

        @Override
        public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

            //noinspection unchecked
            basin.on(((BasinEvaluator) args[0]));

            return null;
        }
    };

    private static final Action<Void, DamBasin<?, ?>> ON_DATA = new Action<Void, DamBasin<?, ?>>() {

        @Override
        public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

            basin.onDataAvailable();

            return null;
        }
    };

    private static final Action<Void, DamBasin<?, ?>> ON_FLUSH =
            new Action<Void, DamBasin<?, ?>>() {

                @Override
                public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

                    basin.onFlush();

                    return null;
                }
            };

    private static final Action<Void, DamBasin<?, ?>> ON_THROWABLE =
            new Action<Void, DamBasin<?, ?>>() {

                @Override
                public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

                    basin.onThrowableAvailable();

                    return null;
                }
            };

    private static final Action<Object, DamBasin<?, ?>> PULL_DATA =
            new Action<Object, DamBasin<?, ?>>() {

                @Override
                public Object doOn(final DamBasin<?, ?> basin, final Object... args) {

                    if (args.length == 0) {

                        return basin.pullData();
                    }

                    return basin.pullData((Integer) args[0]);
                }
            };

    private static final Action<Throwable, DamBasin<?, ?>> PULL_UNHANDLED =
            new Action<Throwable, DamBasin<?, ?>>() {

                @Override
                public Throwable doOn(final DamBasin<?, ?> basin, final Object... args) {

                    if (args.length == 0) {

                        return basin.pullUnhandled();
                    }

                    return basin.pullUnhandled((Integer) args[0]);
                }
            };

    private static final Action<Void, DamBasin<?, ?>> SETUP_CONTORL =
            new Action<Void, DamBasin<?, ?>>() {

                @Override
                public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

                    //noinspection unchecked
                    basin.setUpControl((GateControl) args[0]);

                    return null;
                }
            };

    private static final Action<Void, DamBasin<?, ?>> WHEN_AVAILABLE =
            new Action<Void, DamBasin<?, ?>>() {

                @Override
                public Void doOn(final DamBasin<?, ?> basin, final Object... args) {

                    basin.whenAvailable();

                    return null;
                }
            };

    private final Waterfall<SOURCE, DATA, DATA> mInWaterfall;

    private final Waterfall<SOURCE, DATA, DATA> mOutWaterfall;

    private GateControl<DamBasin<SOURCE, DATA>> mControl;

    private Dam(final Waterfall<SOURCE, DATA, DATA> waterfall) {

        super(waterfall.source(), true);

        mInWaterfall = waterfall;
        mOutWaterfall = waterfall.asGate().chain(new DamBasin<SOURCE, DATA>(waterfall.size()));
        mControl = mOutWaterfall.when(new Gate<DamBasin<SOURCE, DATA>>() {});
    }

    public static <SOURCE, DATA> Dam<SOURCE, DATA> on(
            final Waterfall<SOURCE, DATA, DATA> waterfall) {

        return new Dam<SOURCE, DATA>(waterfall);
    }

    public Dam<SOURCE, DATA> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        //noinspection unchecked
        mControl.perform((Action) AFTER_MAX, maxDelay, timeUnit);

        return this;
    }

    public Dam<SOURCE, DATA> all() {

        //noinspection unchecked
        getControl().perform((Action) ALL);

        return this;
    }

    public Dam<SOURCE, DATA> collectData(final List<DATA> bucket) {

        //noinspection unchecked
        getControl().perform((Action) COLLECT_DATA, bucket);

        return this;
    }

    public Dam<SOURCE, DATA> collectData(final int streamNumber, final List<DATA> bucket) {

        //noinspection unchecked
        getControl().perform((Action) COLLECT_DATA, streamNumber, bucket);

        return this;
    }

    public Dam<SOURCE, DATA> collectUnhandled(final List<Throwable> bucket) {

        //noinspection unchecked
        getControl().perform((Action) COLLECT_UNHANDLED, bucket);

        return this;
    }

    public Dam<SOURCE, DATA> collectUnhandled(final int streamNumber,
            final List<Throwable> bucket) {

        //noinspection unchecked
        getControl().perform((Action) COLLECT_UNHANDLED, streamNumber, bucket);

        return this;
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

        //noinspection unchecked
        getControl().perform((Action) EMPTY);

        return this;
    }

    public Dam<SOURCE, DATA> eventuallyThrow(final RuntimeException exception) {

        //noinspection unchecked
        mControl.perform((Action) EVENTUALLY_THROW, exception);

        return this;
    }

    public Dam<SOURCE, DATA> immediately() {

        //noinspection unchecked
        mControl.perform((Action) AFTER_MAX, 0, TimeUnit.MILLISECONDS);

        return this;
    }

    public Dam<SOURCE, DATA> max(final int maxCount) {

        //noinspection unchecked
        getControl().perform((Action) MAX, maxCount);

        return this;
    }

    public Dam<SOURCE, DATA> on(final BasinEvaluator<DATA> evaluator) {

        //noinspection unchecked
        mControl.perform((Action) ON, evaluator);

        return this;
    }

    public Dam<SOURCE, DATA> onDataAvailable() {

        //noinspection unchecked
        mControl.perform((Action) ON_DATA);

        return this;
    }

    public Dam<SOURCE, DATA> onFlush() {

        //noinspection unchecked
        mControl.perform((Action) ON_FLUSH);

        return this;
    }

    public Dam<SOURCE, DATA> onThrowableAvailable() {

        //noinspection unchecked
        mControl.perform((Action) ON_THROWABLE);

        return this;
    }

    public DATA pullData() {

        //noinspection unchecked
        return (DATA) getControl().perform((Action) PULL_DATA);
    }

    public DATA pullData(final int streamNumber) {

        //noinspection unchecked
        return (DATA) getControl().perform((Action) PULL_DATA, streamNumber);
    }

    public Throwable pullUnhandled() {

        //noinspection unchecked
        return (Throwable) getControl().perform((Action) PULL_UNHANDLED);
    }

    public Throwable pullUnhandled(final int streamNumber) {

        //noinspection unchecked
        return (Throwable) getControl().perform((Action) PULL_UNHANDLED, streamNumber);
    }

    public Waterfall<SOURCE, DATA, DATA> release() {

        drain();

        return mInWaterfall;
    }

    public Dam<SOURCE, DATA> whenAvailable() {

        //noinspection unchecked
        mControl.perform((Action) WHEN_AVAILABLE);

        return this;
    }

    private GateControl<DamBasin<SOURCE, DATA>> getControl() {

        //noinspection unchecked
        mControl.perform((Action) SETUP_CONTORL, mControl);

        return mControl;
    }
}