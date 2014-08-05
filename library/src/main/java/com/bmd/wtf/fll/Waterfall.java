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
import com.bmd.wtf.crr.CurrentGenerator;
import com.bmd.wtf.crr.Currents;
import com.bmd.wtf.flw.Barrage;
import com.bmd.wtf.flw.Collector;
import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.lps.FreeLeap;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.lps.LeapGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Here is where everything starts.
 * <p/>
 * Each waterfall instance retains a reference to its source so to be available during the building
 * chain.
 * <p/>
 * Created by davide on 6/4/14.
 *
 * @param <SOURCE> The waterfall source data type.
 * @param <IN>     The input data type.
 * @param <OUT>    The output data type.
 */
public class Waterfall<SOURCE, IN, OUT> extends AbstractRiver<IN> {

    private static final DataFall[] NO_FALL = new DataFall[0];

    private static final Classification<Void> SELF_CLASSIFICATION = new Classification<Void>() {};

    private static final WeakHashMap<Leap<?, ?>, Void> sLeaps = new WeakHashMap<Leap<?, ?>, Void>();

    private static FreeLeap<?> sFreeLeap;

    private final Current mBackgroundCurrent;

    private final int mBackgroundPoolSize;

    private final BarrageLeap<?> mBarrage;

    private final Current mCurrent;

    private final CurrentGenerator mCurrentGenerator;

    private final DataFall<IN, OUT>[] mFalls;

    private final Classification<?> mGate;

    private final Map<Classification<?>, GateLeap<?, ?>> mGateMap;

    private final int mSize;

    private final Waterfall<SOURCE, SOURCE, ?> mSource;

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final Map<Classification<?>, GateLeap<?, ?>> gateMap,
            final Classification<?> gateClassification, final int backgroundPoolSize,
            final Current backgroundCurrent, final BarrageLeap<?> barrageLeap, final int size,
            final Current current, final CurrentGenerator generator,
            final DataFall<IN, OUT>... falls) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mGateMap = gateMap;
        mGate = gateClassification;
        mBackgroundPoolSize = backgroundPoolSize;
        mBackgroundCurrent = backgroundCurrent;
        mBarrage = barrageLeap;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;
        mFalls = falls;
    }

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final Map<Classification<?>, GateLeap<?, ?>> gateMap,
            final Classification<?> gateClassification, final int backgroundPoolSize,
            final Current backgroundCurrent, final BarrageLeap<?> barrageLeap, final int size,
            final Current current, final CurrentGenerator generator, final Leap<IN, OUT>... leaps) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mGate = null;
        mBackgroundPoolSize = backgroundPoolSize;
        mBackgroundCurrent = backgroundCurrent;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;

        final int length = leaps.length;

        final Leap<IN, OUT> wrappedLeap;

        if (gateClassification != null) {

            final Leap<IN, OUT> leap = leaps[0];

            final HashMap<Classification<?>, GateLeap<?, ?>> fallGateMap =
                    new HashMap<Classification<?>, GateLeap<?, ?>>(gateMap);
            final GateLeap<IN, OUT> gateLeap = new GateLeap<IN, OUT>(leap);

            mapGate(fallGateMap,
                    (SELF_CLASSIFICATION == gateClassification) ? Classification.ofType(
                            leap.getClass()) : gateClassification, gateLeap);

            mGateMap = fallGateMap;

            wrappedLeap = gateLeap;

        } else {

            if (size != length) {

                wrappedLeap = new SegmentedLeap<IN, OUT>(leaps[0]);

            } else {

                wrappedLeap = null;
            }

            mGateMap = gateMap;
        }

        final DataFall[] falls = new DataFall[size];

        if (size == 1) {

            mBarrage = null;

        } else {

            mBarrage = barrageLeap;
        }

        final BarrageLeap<?> fallBarrage = mBarrage;

        for (int i = 0; i < size; ++i) {

            final Leap<IN, OUT> leap = (wrappedLeap != null) ? wrappedLeap : leaps[i];
            final Current fallCurrent;

            if (current == null) {

                fallCurrent = generator.create(i);

            } else {

                fallCurrent = current;
            }

            if (fallBarrage != null) {

                falls[i] =
                        new BarrageFall<SOURCE, IN, OUT>(this, fallCurrent, leap, i, fallBarrage);

            } else {

                falls[i] = new DataFall<IN, OUT>(this, fallCurrent, leap, i);
            }
        }

        //noinspection unchecked
        mFalls = (DataFall<IN, OUT>[]) falls;
    }

    /**
     * Creates and returns a new waterfall composed by a single synchronous stream.
     *
     * @return The newly created waterfall.
     */
    public static Waterfall<Object, Object, Object> fall() {

        final Map<Classification<?>, GateLeap<?, ?>> gateMap = Collections.emptyMap();

        //noinspection unchecked
        return new Waterfall<Object, Object, Object>(null, gateMap, null, 0, null, null, 1,
                                                     Currents.straight(), null, NO_FALL);
    }

    /**
     * Lazily creates and return a singleton free leap instance.
     *
     * @param <DATA> The data type.
     * @return The free leap instance.
     */
    private static <DATA> FreeLeap<DATA> freeLeap() {

        if (sFreeLeap == null) {

            sFreeLeap = new FreeLeap<Object>();
        }

        //noinspection unchecked
        return (FreeLeap<DATA>) sFreeLeap;
    }

    /**
     * Links an input and an output fall through a data stream.
     *
     * @param inFall  The input fall.
     * @param outFall The output fall.
     * @param <DATA>  The data type.
     * @return The data stream running between the two falls.
     */
    private static <DATA> DataStream<DATA> link(final DataFall<?, DATA> inFall,
            final DataFall<DATA, ?> outFall) {

        final DataStream<DATA> stream = new DataStream<DATA>(inFall, outFall);

        outFall.inputStreams.add(stream);
        inFall.outputStreams.add(stream);

        return stream;
    }

    /**
     * Registers the specified leap instance by making sure it is unique among all the created
     * waterfalls.
     *
     * @param leap The leap to register.
     */
    private static void registerLeap(final Leap<?, ?> leap) {

        if (sLeaps.containsKey(leap)) {

            throw new IllegalArgumentException("the waterfall already contains the leap: " + leap);
        }

        sLeaps.put(leap, null);
    }

    /**
     * Tells the waterfall to build a gate of the specified type around the next leap chained to it.
     *
     * @param gateClass The gate class.
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> as(final Class<?> gateClass) {

        return as(Classification.ofType(gateClass));
    }

    /**
     * Tells the waterfall to build a gate of the specified classification type around the next
     * leap chained to it.
     *
     * @param gateClassification The gate classification.
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> as(final Classification<?> gateClassification) {

        if (gateClassification == null) {

            throw new IllegalArgumentException("the gate classification cannot be null");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, gateClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mBarrage,
                                              mSize, mCurrent, mCurrentGenerator, mFalls);
    }

    /**
     * Tells the waterfall to build a gate around the next leap chained to it.
     * <p/>
     * The gate type will be the same as the leap raw type.
     *
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> asGate() {

        return as(SELF_CLASSIFICATION);
    }

    /**
     * Chains the specified waterfall to this one. After the call, all the data flowing through
     * this waterfall will be pushed into the target one.
     *
     * @param waterfall The waterfall to chain.
     */
    public void chain(final Waterfall<?, OUT, ?> waterfall) {

        if (this == waterfall) {

            throw new IllegalArgumentException("cannot chain a waterfall to itself");
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        if ((falls == NO_FALL) || (waterfall.mFalls == NO_FALL)) {

            throw new IllegalStateException("cannot chain a not started waterfall to another one");

        } else {

            final int size = waterfall.mSize;
            final int length = falls.length;

            final DataFall<OUT, ?>[] outFalls = waterfall.mFalls;

            for (final DataFall<OUT, ?> outFall : outFalls) {

                for (final DataStream<?> outputStream : outFall.outputStreams) {

                    //noinspection unchecked
                    if (outputStream.canReach(Arrays.asList(falls))) {

                        throw new IllegalArgumentException(
                                "a possible loop in the waterfall chain has been detected");
                    }
                }
            }

            if (size == 1) {

                final DataFall<OUT, ?> outFall = outFalls[0];

                for (final DataFall<IN, OUT> fall : falls) {

                    link(fall, outFall);
                }

            } else {

                final Waterfall<SOURCE, ?, OUT> inWaterfall;

                if ((length != 1) && (length != size)) {

                    inWaterfall = in(1).chain();

                } else {

                    inWaterfall = this;
                }

                final DataFall<?, OUT>[] inFalls = inWaterfall.mFalls;

                if (inFalls.length == 1) {

                    final DataFall<?, OUT> inFall = inFalls[0];

                    for (final DataFall<OUT, ?> outFall : outFalls) {

                        link(inFall, outFall);
                    }

                } else {

                    for (int i = 0; i < size; ++i) {

                        link(inFalls[i], outFalls[i]);
                    }
                }
            }
        }
    }

    /**
     * Chains the leap protected by the gate of the specified classification type to this
     * waterfall.
     * <p/>
     * Note that contrary to common leap, the ones protected by a gate can be added several times
     * to the same waterfall.
     *
     * @param gateClassification The gate classification.
     * @param <NOUT>             The new output data type.
     * @return The newly created waterfall.
     */
    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(
            final Classification<? extends Leap<OUT, NOUT>> gateClassification) {

        //noinspection unchecked
        final Leap<OUT, NOUT> leap = (Leap<OUT, NOUT>) findBestMatch(gateClassification);

        if (leap == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any gate of classification type "
                            + gateClassification);
        }

        final DataFall<IN, OUT>[] falls = mFalls;
        final int size = mSize;

        if (size == 1) {

            //noinspection unchecked
            final Waterfall<SOURCE, OUT, NOUT> waterfall =
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mGateMap, mGate, mBackgroundPoolSize,
                                                     mBackgroundCurrent, mBarrage, 1, mCurrent,
                                                     mCurrentGenerator, leap);

            final DataFall<OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<IN, OUT> fall : falls) {

                link(fall, outFall);
            }

            return waterfall;
        }

        final int length = falls.length;
        final Waterfall<SOURCE, ?, OUT> inWaterfall;

        if ((length != 1) && (length != size)) {

            inWaterfall = in(1).chain();

        } else {

            inWaterfall = this;
        }

        final Leap[] leaps = new Leap[size];

        Arrays.fill(leaps, leap);

        //noinspection unchecked
        final Waterfall<SOURCE, OUT, NOUT> waterfall =
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mGateMap,
                                                 inWaterfall.mGate, mBackgroundPoolSize,
                                                 mBackgroundCurrent, mBarrage, size,
                                                 inWaterfall.mCurrent,
                                                 inWaterfall.mCurrentGenerator, leaps);

        final DataFall<?, OUT>[] inFalls = inWaterfall.mFalls;
        final DataFall<OUT, NOUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<?, OUT> inFall = inFalls[0];

            for (final DataFall<OUT, NOUT> outFall : outFalls) {

                link(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; ++i) {

                link(inFalls[i], outFalls[i]);
            }
        }

        return waterfall;
    }

    /**
     * Chains a free leap to this waterfall.
     *
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> chain() {

        final DataFall<IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return (Waterfall<SOURCE, OUT, OUT>) start();
        }

        final int size = mSize;
        final FreeLeap<OUT> leap = freeLeap();

        if (size == 1) {

            //noinspection unchecked
            final Waterfall<SOURCE, OUT, OUT> waterfall =
                    new Waterfall<SOURCE, OUT, OUT>(mSource, mGateMap, mGate, mBackgroundPoolSize,
                                                    mBackgroundCurrent, mBarrage, 1, mCurrent,
                                                    mCurrentGenerator, leap);

            final DataFall<OUT, OUT> outFall = waterfall.mFalls[0];

            for (final DataFall<IN, OUT> fall : falls) {

                link(fall, outFall);
            }

            return waterfall;
        }

        final int length = falls.length;
        final Waterfall<SOURCE, ?, OUT> inWaterfall;

        if ((length != 1) && (length != size)) {

            inWaterfall = in(1).chain();

        } else {

            inWaterfall = this;
        }

        final Leap[] leaps = new Leap[size];

        Arrays.fill(leaps, leap);

        //noinspection unchecked
        final Waterfall<SOURCE, OUT, OUT> waterfall =
                new Waterfall<SOURCE, OUT, OUT>(inWaterfall.mSource, inWaterfall.mGateMap,
                                                inWaterfall.mGate, mBackgroundPoolSize,
                                                mBackgroundCurrent, mBarrage, size,
                                                inWaterfall.mCurrent, inWaterfall.mCurrentGenerator,
                                                leaps);

        final DataFall<?, OUT>[] inFalls = inWaterfall.mFalls;
        final DataFall<OUT, OUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<?, OUT> inFall = inFalls[0];

            for (final DataFall<OUT, OUT> outFall : outFalls) {

                link(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; ++i) {

                link(inFalls[i], outFalls[i]);
            }
        }

        return waterfall;
    }

    /**
     * Chains the specified leap to this waterfall.
     * <p/>
     * Note that in case this waterfall is composed by more then one data stream, all the data
     * flowing through them will be passed to the specified leap.
     *
     * @param leap   The leap instance.
     * @param <NOUT> The new output data type.
     * @return The newly created waterfall.
     */
    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(final Leap<OUT, NOUT> leap) {

        if (leap == null) {

            throw new IllegalArgumentException("the waterfall leap cannot be null");
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return (Waterfall<SOURCE, OUT, NOUT>) start(leap);
        }

        final int size = mSize;

        if (size == 1) {

            registerLeap(leap);

            //noinspection unchecked
            final Waterfall<SOURCE, OUT, NOUT> waterfall =
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mGateMap, mGate, mBackgroundPoolSize,
                                                     mBackgroundCurrent, mBarrage, 1, mCurrent,
                                                     mCurrentGenerator, leap);

            final DataFall<OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<IN, OUT> fall : falls) {

                link(fall, outFall);
            }

            return waterfall;
        }

        final int length = falls.length;
        final Waterfall<SOURCE, ?, OUT> inWaterfall;

        if ((length != 1) && (length != size)) {

            inWaterfall = in(1).chain();

        } else {

            inWaterfall = this;
        }

        registerLeap(leap);

        //noinspection unchecked
        final Waterfall<SOURCE, OUT, NOUT> waterfall =
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mGateMap,
                                                 inWaterfall.mGate, mBackgroundPoolSize,
                                                 mBackgroundCurrent, mBarrage, size,
                                                 inWaterfall.mCurrent,
                                                 inWaterfall.mCurrentGenerator, leap);

        final DataFall<?, OUT>[] inFalls = inWaterfall.mFalls;
        final DataFall<OUT, NOUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<?, OUT> inFall = inFalls[0];

            for (final DataFall<OUT, NOUT> outFall : outFalls) {

                link(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; ++i) {

                link(inFalls[i], outFalls[i]);
            }
        }

        return waterfall;
    }

    /**
     * Chains the leaps returned by the specified generator to this waterfall.
     * <p/>
     * Note that in case this waterfall is composed by more then one data stream, each leap created
     * by the generator will handle a single stream.
     *
     * @param generator The leap generator.
     * @param <NOUT>    The new output data type.
     * @return The newly created waterfall.
     */
    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(final LeapGenerator<OUT, NOUT> generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall generator cannot be null");
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return (Waterfall<SOURCE, OUT, NOUT>) start(generator);
        }

        final int size = mSize;

        if (size == 1) {

            final Leap<OUT, NOUT> leap = generator.start(0);

            registerLeap(leap);

            //noinspection unchecked
            final Waterfall<SOURCE, OUT, NOUT> waterfall =
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mGateMap, mGate, mBackgroundPoolSize,
                                                     mBackgroundCurrent, mBarrage, 1, mCurrent,
                                                     mCurrentGenerator, leap);

            final DataFall<OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<IN, OUT> fall : falls) {

                link(fall, outFall);
            }

            return waterfall;
        }

        if (mGate != null) {

            throw new IllegalStateException("cannot make a gate from more than one leap");
        }

        final int length = falls.length;
        final Waterfall<SOURCE, ?, OUT> inWaterfall;

        if ((length != 1) && (length != size)) {

            inWaterfall = in(1).chain();

        } else {

            inWaterfall = this;
        }

        final Leap[] leaps = new Leap[size];

        for (int i = 0; i < size; ++i) {

            final Leap<OUT, NOUT> leap = generator.start(i);

            registerLeap(leap);

            leaps[i] = leap;
        }

        //noinspection unchecked
        final Waterfall<SOURCE, OUT, NOUT> waterfall =
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mGateMap,
                                                 inWaterfall.mGate, mBackgroundPoolSize,
                                                 mBackgroundCurrent, mBarrage, size,
                                                 inWaterfall.mCurrent,
                                                 inWaterfall.mCurrentGenerator, leaps);

        final DataFall<?, OUT>[] inFalls = inWaterfall.mFalls;
        final DataFall<OUT, NOUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<?, OUT> inFall = inFalls[0];

            for (final DataFall<OUT, NOUT> outFall : outFalls) {

                link(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; ++i) {

                link(inFalls[i], outFalls[i]);
            }
        }

        return waterfall;
    }

    /**
     * Creates and returns a new data collector.
     *
     * @return The collector.
     */
    public Collector<OUT> collect() {

        if (mFalls == NO_FALL) {

            throw new IllegalStateException("cannot collect data from a not started waterfall");
        }

        final CollectorLeap<OUT> collectorLeap = new CollectorLeap<OUT>();
        final GateLeap<OUT, OUT> gateLeap = new GateLeap<OUT, OUT>(collectorLeap);

        final Waterfall<SOURCE, IN, OUT> waterfall;

        if (mSize != 1) {

            waterfall = in(1);

        } else {

            waterfall = this;
        }

        waterfall.chain(gateLeap);

        return new DataCollector<OUT>(gateLeap, collectorLeap);
    }

    @Override
    public void deviate() {

        for (final DataFall<IN, OUT> fall : mFalls) {

            for (final DataStream<OUT> stream : fall.outputStreams) {

                stream.deviate();
            }
        }
    }

    @Override
    public void deviateStream(final int streamNumber) {

        final DataFall<IN, OUT> fall = mFalls[streamNumber];

        for (final DataStream<OUT> stream : fall.outputStreams) {

            stream.deviate();
        }
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> discharge() {

        for (final DataFall<IN, OUT> fall : mFalls) {

            fall.inputCurrent.discharge(fall, null);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> forward(final Throwable throwable) {

        for (final DataFall<IN, OUT> fall : mFalls) {

            fall.raiseLevel(1);

            fall.inputCurrent.forward(fall, throwable);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> push(final IN... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        for (final IN drop : drops) {

            for (final DataFall<IN, OUT> fall : falls) {

                fall.raiseLevel(1);

                fall.inputCurrent.push(fall, drop);
            }
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> push(final Iterable<? extends IN> drops) {

        if (drops == null) {

            return this;
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        for (final IN drop : drops) {

            for (final DataFall<IN, OUT> fall : falls) {

                fall.raiseLevel(1);

                fall.inputCurrent.push(fall, drop);
            }
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> push(final IN drop) {

        for (final DataFall<IN, OUT> fall : mFalls) {

            fall.raiseLevel(1);

            fall.inputCurrent.push(fall, drop);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends IN> drops) {

        if (drops == null) {

            return this;
        }

        final ArrayList<IN> list = new ArrayList<IN>();

        for (final IN drop : drops) {

            list.add(drop);
        }

        if (!list.isEmpty()) {

            final int size = list.size();

            for (final DataFall<IN, OUT> fall : mFalls) {

                fall.raiseLevel(size);

                fall.inputCurrent.pushAfter(fall, delay, timeUnit, list);
            }
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushAfter(final long delay, final TimeUnit timeUnit,
            final IN drop) {

        for (final DataFall<IN, OUT> fall : mFalls) {

            fall.raiseLevel(1);

            fall.inputCurrent.pushAfter(fall, delay, timeUnit, drop);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushAfter(final long delay, final TimeUnit timeUnit,
            final IN... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        final ArrayList<IN> list = new ArrayList<IN>(Arrays.asList(drops));

        for (final DataFall<IN, OUT> fall : mFalls) {

            fall.raiseLevel(drops.length);

            fall.inputCurrent.pushAfter(fall, delay, timeUnit, list);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> dischargeStream(final int streamNumber) {

        final DataFall<IN, OUT> fall = mFalls[streamNumber];

        fall.inputCurrent.discharge(fall, null);

        return this;
    }

    @Override
    public void drain() {

        for (final DataFall<IN, OUT> fall : mFalls) {

            for (final DataStream<OUT> stream : fall.outputStreams) {

                stream.drain(Direction.DOWNSTREAM);
            }
        }
    }

    @Override
    public void drainStream(final int streamNumber) {

        final DataFall<IN, OUT> fall = mFalls[streamNumber];

        for (final DataStream<OUT> stream : fall.outputStreams) {

            stream.drain(Direction.DOWNSTREAM);
        }
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> forwardStream(final int streamNumber,
            final Throwable throwable) {

        final DataFall<IN, OUT> fall = mFalls[streamNumber];

        fall.raiseLevel(1);

        fall.inputCurrent.forward(fall, throwable);

        return this;
    }

    @Override
    public <TYPE> Gate<TYPE> on(final Class<TYPE> gateClass) {

        return on(Classification.ofType(gateClass));
    }

    @Override
    public <TYPE> Gate<TYPE> on(final TYPE leap) {

        if (leap == null) {

            throw new IllegalArgumentException("the gate leap cannot be null");
        }

        GateLeap<?, ?> gate = null;

        final Map<Classification<?>, GateLeap<?, ?>> gateMap = mGateMap;

        for (final GateLeap<?, ?> gateLeap : gateMap.values()) {

            if (gateLeap.leap == leap) {

                gate = gateLeap;

                break;
            }
        }

        if (gate == null) {

            throw new IllegalArgumentException("the waterfall does not retain the gate " + leap);
        }

        return new DataGate<TYPE>(gate, new Classification<TYPE>() {});
    }

    @Override
    public <TYPE> Gate<TYPE> on(final Classification<TYPE> gateClassification) {

        final GateLeap<?, ?> gate = findBestMatch(gateClassification);

        if (gate == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any gate of classification type "
                            + gateClassification);
        }

        return new DataGate<TYPE>(gate, gateClassification);
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushStream(final int streamNumber, final IN... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        final DataFall<IN, OUT> fall = mFalls[streamNumber];

        fall.raiseLevel(drops.length);

        for (final IN drop : drops) {

            fall.inputCurrent.push(fall, drop);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushStream(final int streamNumber,
            final Iterable<? extends IN> drops) {

        if (drops == null) {

            return this;
        }

        int size = 0;

        for (final IN ignored : drops) {

            ++size;
        }

        if (size > 0) {

            final DataFall<IN, OUT> fall = mFalls[streamNumber];

            fall.raiseLevel(size);

            for (final IN drop : drops) {

                fall.inputCurrent.push(fall, drop);
            }
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushStream(final int streamNumber, final IN drop) {

        final DataFall<IN, OUT> fall = mFalls[streamNumber];

        fall.raiseLevel(1);

        fall.inputCurrent.push(fall, drop);

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends IN> drops) {

        if (drops == null) {

            return this;
        }

        final ArrayList<IN> list = new ArrayList<IN>();

        for (final IN drop : drops) {

            list.add(drop);
        }

        if (!list.isEmpty()) {

            final DataFall<IN, OUT> fall = mFalls[streamNumber];

            fall.raiseLevel(list.size());

            fall.inputCurrent.pushAfter(fall, delay, timeUnit, list);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final IN drop) {

        final DataFall<IN, OUT> fall = mFalls[streamNumber];

        fall.raiseLevel(1);

        fall.inputCurrent.pushAfter(fall, delay, timeUnit, drop);

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final IN... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        final DataFall<IN, OUT> fall = mFalls[streamNumber];

        fall.raiseLevel(drops.length);

        fall.inputCurrent.pushAfter(fall, delay, timeUnit, new ArrayList<IN>(Arrays.asList(drops)));

        return this;
    }

    @Override
    public int size() {

        return mFalls.length;
    }

    /**
     * Deviates the flow of this waterfall, either downstream or upstream, by effectively
     * preventing any coming data to be pushed further.
     *
     * @param direction Whether the waterfall must be deviated downstream or upstream.
     * @see #deviateStream(int, com.bmd.wtf.flw.Stream.Direction)
     */
    public void deviate(final Direction direction) {

        if (direction == Direction.DOWNSTREAM) {

            deviate();

        } else {

            for (final DataFall<IN, OUT> fall : mFalls) {

                for (final DataStream<IN> stream : fall.inputStreams) {

                    stream.deviate();
                }
            }
        }
    }

    /**
     * Deviates the flow of the specified waterfall stream, either downstream or upstream, by
     * effectively preventing any coming data to be pushed further.
     *
     * @param streamNumber The number identifying the target stream.
     * @param direction    Whether the waterfall must be deviated downstream or upstream.
     * @see #deviate(com.bmd.wtf.flw.Stream.Direction)
     */
    public void deviateStream(final int streamNumber, final Direction direction) {

        if (direction == Direction.DOWNSTREAM) {

            deviateStream(streamNumber);

        } else {

            final DataFall<IN, OUT> fall = mFalls[streamNumber];

            for (final DataStream<IN> stream : fall.inputStreams) {

                stream.deviate();
            }
        }
    }

    /**
     * Uniformly distributes all the data flowing through this waterfall in the different output
     * streams.
     *
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> distribute() {

        final DataFall<IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return (Waterfall<SOURCE, OUT, OUT>) start().distribute();
        }

        final int size = mSize;

        if (size == 1) {

            return chain();
        }

        return in(1).chainBarrage(new BarrageLeap<OUT>(size)).in(size).chain();
    }

    /**
     * Distributes all the data flowing through this waterfall in the different output streams by
     * means of the specified barrage.
     *
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> distribute(final Barrage<OUT> barrage) {

        if (barrage == null) {

            throw new IllegalArgumentException("the waterfall barrage cannot be null");
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return (Waterfall<SOURCE, OUT, OUT>) start().distribute(barrage);
        }

        final int size = mSize;

        if (size == 1) {

            return chain();
        }

        return in(1).chainBarrage(new BarrageLeap<OUT>(barrage, size)).in(size).chain();
    }

    /**
     * Drains the waterfall, either downstream or upstream, by removing all the falls and rivers
     * fed only by this waterfall streams.
     *
     * @param direction Whether the waterfall must be deviated downstream or upstream.
     * @see #drainStream(int, com.bmd.wtf.flw.Stream.Direction)
     */
    public void drain(final Direction direction) {

        if (direction == Direction.DOWNSTREAM) {

            drain();

        } else {

            for (final DataFall<IN, OUT> fall : mFalls) {

                for (final DataStream<IN> stream : fall.inputStreams) {

                    stream.drain(direction);
                }
            }
        }
    }

    /**
     * Drains the specified waterfall stream, either downstream or upstream, by removing from all
     * the falls and rivers fed only by the specific stream.
     *
     * @param streamNumber The number identifying the target stream.
     * @param direction    Whether the waterfall must be deviated downstream or upstream.
     * @see #drain(com.bmd.wtf.flw.Stream.Direction)
     */
    public void drainStream(final int streamNumber, final Direction direction) {

        if (direction == Direction.DOWNSTREAM) {

            drainStream(streamNumber);

        } else {

            final DataFall<IN, OUT> fall = mFalls[streamNumber];

            for (final DataStream<IN> stream : fall.inputStreams) {

                stream.drain(direction);
            }
        }
    }

    /**
     * Makes the waterfall streams flow through the currents returned by the specified generator.
     *
     * @param generator The current generator
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> in(final CurrentGenerator generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall current generator cannot be null");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBackgroundPoolSize,
                                              mBackgroundCurrent, mBarrage, mSize, null, generator,
                                              mFalls);
    }

    /**
     * Splits the waterfall in the specified number of streams.
     *
     * @param fallCount The total fall count generating the waterfall.
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> in(final int fallCount) {

        if (fallCount <= 0) {

            throw new IllegalArgumentException("the fall count cannot be negative or zero");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBackgroundPoolSize,
                                              mBackgroundCurrent, mBarrage, fallCount, mCurrent,
                                              mCurrentGenerator, mFalls);
    }

    /**
     * Makes the waterfall streams flow through the specified current.
     *
     * @param current The current.
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> in(final Current current) {

        if (current == null) {

            throw new IllegalArgumentException("the waterfall current cannot be null");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBackgroundPoolSize,
                                              mBackgroundCurrent, mBarrage, mSize, current, null,
                                              mFalls);
    }

    /**
     * Makes the waterfall streams flow through a background current.
     * <p/>
     * The optimum thread pool size will be automatically computed based on the available resources
     * and the waterfall size.
     * <p/>
     * Note also that the same background current will be retained through the waterfall.
     *
     * @param fallCount The total fall count generating the waterfall.
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> inBackground(final int fallCount) {

        if (fallCount <= 0) {

            throw new IllegalArgumentException("the fall count cannot be negative or zero");
        }

        final int poolSize;
        final Current backgroundCurrent;

        if (mBackgroundCurrent == null) {

            poolSize = getBestPoolSize();
            backgroundCurrent = Currents.pool(poolSize);

        } else {

            poolSize = mBackgroundPoolSize;
            backgroundCurrent = mBackgroundCurrent;
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, poolSize, backgroundCurrent,
                                              mBarrage, fallCount, backgroundCurrent, null, mFalls);
    }

    /**
     * Makes the waterfall streams flow through a background current.
     * <p/>
     * The optimum thread pool size will be automatically computed based on the available resources
     * and the waterfall size, and the total fall count of the resulting waterfall will be
     * accordingly dimensioned.
     * <p/>
     * Note also that the same background current will be retained through the waterfall.
     *
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> inBackground() {

        final int poolSize;
        final Current backgroundCurrent;

        if (mBackgroundCurrent == null) {

            poolSize = getBestPoolSize();
            backgroundCurrent = Currents.pool(poolSize);

        } else {

            poolSize = mBackgroundPoolSize;
            backgroundCurrent = mBackgroundCurrent;
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, poolSize, backgroundCurrent,
                                              mBarrage, poolSize, backgroundCurrent, null, mFalls);
    }

    /**
     * Tells the waterfall to lock the gate of the specified classification type, that is, the gate
     * will not be accessible anymore to the ones requiring it.
     *
     * @param gateClassification The gate classification.
     * @param <TYPE>             The leap type.
     * @return The newly created waterfall.
     */
    public <TYPE> Waterfall<SOURCE, IN, OUT> lock(final Classification<TYPE> gateClassification) {

        final GateLeap<?, ?> gate = findBestMatch(gateClassification);

        if (gate == null) {

            return this;
        }

        final HashMap<Classification<?>, GateLeap<?, ?>> gateMap =
                new HashMap<Classification<?>, GateLeap<?, ?>>(mGateMap);

        final Iterator<GateLeap<?, ?>> iterator = gateMap.values().iterator();

        while (iterator.hasNext()) {

            if (gate == iterator.next()) {

                iterator.remove();
            }
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, gateMap, mGate, mBackgroundPoolSize,
                                              mBackgroundCurrent, mBarrage, mSize, mCurrent,
                                              mCurrentGenerator, mFalls);
    }

    /**
     * Tells the waterfall to lock the gate handling the specified leap, that is, the gate
     * will not be accessible anymore to the ones requiring it..
     *
     * @param leap   The leap instance.
     * @param <TYPE> The leap type.
     * @return The newly created waterfall.
     */
    public <TYPE extends Leap<?, ?>> Waterfall<SOURCE, IN, OUT> lock(final TYPE leap) {

        if (leap == null) {

            return this;
        }

        boolean isChanged = false;

        final HashMap<Classification<?>, GateLeap<?, ?>> gateMap =
                new HashMap<Classification<?>, GateLeap<?, ?>>(mGateMap);

        final Iterator<GateLeap<?, ?>> iterator = gateMap.values().iterator();

        while (iterator.hasNext()) {

            if (leap == iterator.next().leap) {

                iterator.remove();

                isChanged = true;
            }
        }

        if (!isChanged) {

            return this;
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, gateMap, mGate, mBackgroundPoolSize,
                                              mBackgroundCurrent, mBarrage, mSize, mCurrent,
                                              mCurrentGenerator, mFalls);
    }

    /**
     * Creates and returns a new data collector after discharging this waterfall source.
     *
     * @return The collector.
     */
    public Collector<OUT> pull() {

        final Collector<OUT> collector = collect();

        source().discharge();

        return collector;
    }

    /**
     * Creates and returns a new data collector after pushing the specified data into this
     * waterfall source and then discharging it.
     *
     * @param source The source data.
     * @return The collector.
     */
    public Collector<OUT> pull(final SOURCE source) {

        final Collector<OUT> collector = collect();

        source().push(source).discharge();

        return collector;
    }

    /**
     * Creates and returns a new data collector after pushing the specified data into this
     * waterfall source and then discharging it.
     *
     * @param sources The source data.
     * @return The collector.
     */
    public Collector<OUT> pull(final SOURCE... sources) {

        final Collector<OUT> collector = collect();

        source().push(sources).discharge();

        return collector;
    }

    /**
     * Creates and returns a new data collector after pushing the data returned by the specified
     * iterable into this waterfall source and then discharging it.
     *
     * @param sources The source data iterable.
     * @return The collector.
     */
    public Collector<OUT> pull(final Iterable<SOURCE> sources) {

        final Collector<OUT> collector = collect();

        source().push(sources).discharge();

        return collector;
    }

    /**
     * Gets the waterfall source.
     *
     * @return The source.
     */
    public Waterfall<SOURCE, SOURCE, ?> source() {

        return mSource;
    }

    /**
     * Creates and returns a new waterfall generating from this one.
     * <p/>
     * Note that the gates, the size and the currents of this waterfall will be retained.
     *
     * @return The newly created waterfall.
     */
    public Waterfall<OUT, OUT, OUT> start() {

        final int size = mSize;

        final FreeLeap<OUT> leap = freeLeap();
        final Leap[] leaps = new Leap[size];

        Arrays.fill(leaps, leap);

        final Map<Classification<?>, GateLeap<?, ?>> gateMap = Collections.emptyMap();

        //noinspection unchecked
        return new Waterfall<OUT, OUT, OUT>(null, gateMap, mGate, mBackgroundPoolSize,
                                            mBackgroundCurrent, null, size, mCurrent,
                                            mCurrentGenerator, leaps);
    }

    /**
     * Creates and returns a new waterfall generating from this one.
     * <p/>
     * Note that the gates, the size and the currents of this waterfall will be retained.
     *
     * @param dataType The data type.
     * @param <DATA>   The data type.
     * @return The newly created waterfall.
     */
    public <DATA> Waterfall<DATA, DATA, DATA> start(final Class<DATA> dataType) {

        return start(Classification.ofType(dataType));
    }

    /**
     * Creates and returns a new waterfall generating from this one.
     * <p/>
     * Note that the gates, the size and the currents of this waterfall will be retained.
     *
     * @param classification The data classification.
     * @param <DATA>         The data type.
     * @return The newly created waterfall.
     */
    public <DATA> Waterfall<DATA, DATA, DATA> start(final Classification<DATA> classification) {

        if (classification == null) {

            throw new IllegalArgumentException("the waterfall classification cannot be null");
        }

        //noinspection unchecked
        return (Waterfall<DATA, DATA, DATA>) start();
    }

    /**
     * Creates and returns a new waterfall chained to the leaps returned by the specified
     * generator.
     * <p/>
     * Note that the gates, the size and the currents of this waterfall will be retained.
     *
     * @param generator The leap generator.
     * @param <NIN>     The new input data type.
     * @param <NOUT>    The new output data type.
     * @return The newly created waterfall.
     */
    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(final LeapGenerator<NIN, NOUT> generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall generator cannot be null");
        }

        final Map<Classification<?>, GateLeap<?, ?>> gateMap = Collections.emptyMap();

        final int size = mSize;

        if (size == 1) {

            final Leap<NIN, NOUT> leap = generator.start(0);

            registerLeap(leap);

            //noinspection unchecked
            return new Waterfall<NIN, NIN, NOUT>(null, gateMap, mGate, mBackgroundPoolSize,
                                                 mBackgroundCurrent, null, 1, mCurrent,
                                                 mCurrentGenerator, leap);
        }

        if (mGate != null) {

            throw new IllegalStateException("cannot make a gate from more than one leap");
        }

        final Leap[] leaps = new Leap[size];

        for (int i = 0; i < size; ++i) {

            final Leap<NIN, NOUT> leap = generator.start(i);

            registerLeap(leap);

            leaps[i] = leap;
        }

        //noinspection unchecked
        return new Waterfall<NIN, NIN, NOUT>(null, gateMap, null, mBackgroundPoolSize,
                                             mBackgroundCurrent, null, size, mCurrent,
                                             mCurrentGenerator, leaps);
    }

    /**
     * Creates and returns a new waterfall chained to the specified leap.
     * <p/>
     * Note that the gates, the size and the currents of this waterfall will be retained.
     *
     * @param leap   The leap instance.
     * @param <NIN>  The new input data type.
     * @param <NOUT> The new output data type.
     * @return The newly created waterfall.
     */
    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(final Leap<NIN, NOUT> leap) {

        if (leap == null) {

            throw new IllegalArgumentException("the waterfall leap cannot be null");
        }

        registerLeap(leap);

        final Map<Classification<?>, GateLeap<?, ?>> gateMap = Collections.emptyMap();

        //noinspection unchecked
        return new Waterfall<NIN, NIN, NOUT>(null, gateMap, mGate, mBackgroundPoolSize,
                                             mBackgroundCurrent, null, mSize, mCurrent,
                                             mCurrentGenerator, leap);
    }

    private Waterfall<SOURCE, OUT, OUT> chainBarrage(final BarrageLeap<OUT> barrageLeap) {

        final Waterfall<SOURCE, OUT, OUT> waterfall = chain(barrageLeap);

        //noinspection unchecked
        return new Waterfall<SOURCE, OUT, OUT>(waterfall.mSource, waterfall.mGateMap,
                                               waterfall.mGate, mBackgroundPoolSize,
                                               mBackgroundCurrent, barrageLeap, waterfall.mSize,
                                               waterfall.mCurrent, waterfall.mCurrentGenerator,
                                               waterfall.mFalls);
    }

    private GateLeap<?, ?> findBestMatch(final Classification<?> gateClassification) {

        final Map<Classification<?>, GateLeap<?, ?>> gateMap = mGateMap;

        GateLeap<?, ?> leap = gateMap.get(gateClassification);

        if (leap == null) {

            Classification<?> bestMatch = null;

            for (final Entry<Classification<?>, GateLeap<?, ?>> entry : gateMap.entrySet()) {

                final Classification<?> type = entry.getKey();

                if (gateClassification.isAssignableFrom(type)) {

                    if ((bestMatch == null) || type.isAssignableFrom(bestMatch)) {

                        leap = entry.getValue();

                        bestMatch = type;
                    }
                }
            }
        }

        return leap;
    }

    private int getBestPoolSize() {

        final int processors = Runtime.getRuntime().availableProcessors();

        if (processors < 4) {

            return Math.max(1, processors - 1);
        }

        return (processors / 2);
    }

    private void mapGate(final HashMap<Classification<?>, GateLeap<?, ?>> gateMap,
            final Classification<?> gateClassification, final GateLeap<?, ?> leap) {

        if (!gateClassification.getRawType().isInstance(leap.leap)) {

            throw new IllegalArgumentException(
                    "the leap does not implement the gate classification type");
        }

        gateMap.put(gateClassification, leap);
    }
}