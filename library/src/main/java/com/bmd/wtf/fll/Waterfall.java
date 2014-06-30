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
import com.bmd.wtf.flw.Collector;
import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.FreeLeap;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.lps.LeapGenerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
public class Waterfall<SOURCE, IN, OUT> implements River<SOURCE, IN> {

    private static final DataFall[] NO_FALL = new DataFall[0];

    private static final Classification<Void> SELF_CLASSIFICATION = new Classification<Void>() {};

    private static final WeakHashMap<Leap<?, ?, ?>, Void> sLeaps =
            new WeakHashMap<Leap<?, ?, ?>, Void>();

    private static FreeLeap<?, ?> sFreeLeap;

    private final Barrage<SOURCE, ?> mBarrage;

    private final Current mCurrent;

    private final CurrentGenerator mCurrentGenerator;

    private final DataFall<SOURCE, IN, OUT>[] mFalls;

    private final Classification<?> mGate;

    private final Map<Classification<?>, GateLeap<?, ?, ?>> mGateMap;

    private final int mSize;

    private final Waterfall<SOURCE, SOURCE, ?> mSource;

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap,
            final Classification<?> gateClassification, final Barrage<SOURCE, ?> barrage,
            final int size, final Current current, final CurrentGenerator generator,
            final DataFall<SOURCE, IN, OUT>... falls) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mGateMap = gateMap;
        mGate = gateClassification;
        mBarrage = barrage;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;
        mFalls = falls;
    }

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap,
            final Classification<?> gateClassification, final Barrage<SOURCE, ?> barrage,
            final int size, final Current current, final CurrentGenerator generator,
            final Leap<SOURCE, IN, OUT>... leaps) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mGate = null;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;

        final int length = leaps.length;

        final DataFall[] falls = new DataFall[length];

        if (length == 1) {

            mBarrage = null;

        } else {

            mBarrage = barrage;
        }

        if (gateClassification == null) {

            mGateMap = gateMap;

            final Barrage<SOURCE, ?> fallBarrage = mBarrage;

            for (int i = 0; i < length; i++) {

                final Leap<SOURCE, IN, OUT> fallLeap;

                if (fallBarrage != null) {

                    fallLeap = new BarrageLeap<SOURCE, IN, OUT>(leaps[i], fallBarrage, i);

                } else {

                    fallLeap = leaps[i];
                }

                final Current fallCurrent;

                if (current == null) {

                    fallCurrent = generator.create(i);

                } else {

                    fallCurrent = current;
                }

                falls[i] = new DataFall<SOURCE, IN, OUT>(this, fallCurrent, fallLeap, i);
            }

        } else {

            final HashMap<Classification<?>, GateLeap<?, ?, ?>> fallGateMap =
                    new HashMap<Classification<?>, GateLeap<?, ?, ?>>(gateMap);

            mGateMap = fallGateMap;

            final Barrage<SOURCE, ?> fallBarrage = mBarrage;

            final boolean isSelf = (SELF_CLASSIFICATION == gateClassification);

            final HashMap<Leap<?, ?, ?>, GateLeap<?, ?, ?>> leapMap =
                    new HashMap<Leap<?, ?, ?>, GateLeap<?, ?, ?>>();

            for (int i = 0; i < length; i++) {

                final Leap<SOURCE, IN, OUT> leap = leaps[i];

                //noinspection unchecked
                GateLeap<SOURCE, IN, OUT> gateLeap = (GateLeap<SOURCE, IN, OUT>) leapMap.get(leap);

                if (gateLeap == null) {

                    gateLeap = new GateLeap<SOURCE, IN, OUT>(leap);
                    leapMap.put(leap, gateLeap);

                    mapGate(fallGateMap,
                            (isSelf) ? Classification.ofType(leap.getClass()) : gateClassification,
                            gateLeap);
                }

                final Leap<SOURCE, IN, OUT> fallLeap;

                if (fallBarrage != null) {

                    fallLeap = new BarrageLeap<SOURCE, IN, OUT>(gateLeap, fallBarrage, i);

                } else {

                    fallLeap = gateLeap;
                }

                final Current fallCurrent;

                if (current == null) {

                    fallCurrent = generator.create(i);

                } else {

                    fallCurrent = current;
                }

                falls[i] = new DataFall<SOURCE, IN, OUT>(this, fallCurrent, fallLeap, i);
            }
        }

        //noinspection unchecked
        mFalls = (DataFall<SOURCE, IN, OUT>[]) falls;
    }

    /**
     * Creates and returns a new waterfall composed by a single synchronous stream.
     *
     * @return The newly created waterfall.
     */
    public static Waterfall<Object, Object, Object> create() {

        final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap = Collections.emptyMap();

        //noinspection unchecked
        return new Waterfall<Object, Object, Object>(null, gateMap, null, null, 1,
                                                     Currents.straight(), null, NO_FALL);
    }

    /**
     * Lazily creates and return a singleton free leap instance.
     *
     * @param <SOURCE> The source data type.
     * @param <DATA>   The data type.
     * @return The free leap instance.
     */
    private static <SOURCE, DATA> FreeLeap<SOURCE, DATA> freeLeap() {

        if (sFreeLeap == null) {

            sFreeLeap = new FreeLeap<Object, Object>();
        }

        //noinspection unchecked
        return (FreeLeap<SOURCE, DATA>) sFreeLeap;
    }

    /**
     * Links an input and an output fall through a data stream.
     *
     * @param inFall  The input fall.
     * @param outFall The output fall.
     * @param <DATA>  The data type.
     * @return The data stream running between the two falls.
     */
    private static <DATA> DataStream<DATA> link(final DataFall<?, ?, DATA> inFall,
            final DataFall<?, DATA, ?> outFall) {

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
    private static void registerLeap(final Leap<?, ?, ?> leap) {

        if (sLeaps.containsKey(leap)) {

            throw new IllegalArgumentException("the waterfall already contains the leap: " + leap);
        }

        sLeaps.put(leap, null);
    }

    /**
     * Tells the waterfall to build a gate of the specified type around the next leap chained to it.
     *
     * @param gateType The gate type.
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> as(final Class<?> gateType) {

        return as(Classification.ofType(gateType));
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
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, gateClassification, mBarrage,
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
     * Tells the waterfall to break the gate of the specified classification type.
     *
     * @param gateClassification The gate classification.
     * @param <TYPE>             The gate type.
     * @return The newly created waterfall.
     */
    public <TYPE> Waterfall<SOURCE, IN, OUT> breakDown(
            final Classification<TYPE> gateClassification) {

        return breakDown(when(gateClassification));
    }

    /**
     * Tells the waterfall to break the specified gate.
     *
     * @param gate   The gate.
     * @param <TYPE> The gate type.
     * @return The newly created waterfall.
     */
    public <TYPE> Waterfall<SOURCE, IN, OUT> breakDown(final TYPE gate) {

        if (gate == null) {

            return this;
        }

        boolean isChanged = false;

        final HashMap<Classification<?>, GateLeap<?, ?, ?>> gateMap =
                new HashMap<Classification<?>, GateLeap<?, ?, ?>>(mGateMap);

        final Iterator<GateLeap<?, ?, ?>> iterator = gateMap.values().iterator();

        while (iterator.hasNext()) {

            if (gate == iterator.next()) {

                iterator.remove();

                isChanged = true;
            }
        }

        if (!isChanged) {

            return this;
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, gateMap, mGate, mBarrage, mSize, mCurrent,
                                              mCurrentGenerator, mFalls);
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

        final int size = mSize;

        final DataFall<SOURCE, IN, OUT>[] falls = mFalls;

        final int length = falls.length;

        if (falls == NO_FALL) {

            start().chain(waterfall);

        } else {

            final DataFall<?, OUT, ?>[] outFalls = waterfall.mFalls;

            for (final DataFall<?, OUT, ?> outFall : outFalls) {

                for (final DataStream<?> outputStream : outFall.outputStreams) {

                    //noinspection unchecked
                    if (outputStream.canReach(Arrays.asList(falls))) {

                        throw new IllegalArgumentException(
                                "a possible loop in the waterfall chain has been detected");
                    }
                }
            }

            if (size == 1) {

                final DataFall<?, OUT, ?> outFall = outFalls[0];

                for (final DataFall<SOURCE, IN, OUT> fall : falls) {

                    link(fall, outFall);
                }

            } else {

                final Waterfall<SOURCE, ?, OUT> inWaterfall;

                if ((length != 1) && (length != size)) {

                    inWaterfall = in(1).chain();

                } else {

                    inWaterfall = this;
                }

                final DataFall<SOURCE, ?, OUT>[] inFalls = inWaterfall.mFalls;

                if (inFalls.length == 1) {

                    final DataFall<SOURCE, ?, OUT> inFall = inFalls[0];

                    for (final DataFall<?, OUT, ?> outFall : outFalls) {

                        link(inFall, outFall);
                    }

                } else {

                    for (int i = 0; i < size; i++) {

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
            final Classification<? extends Leap<SOURCE, OUT, NOUT>> gateClassification) {

        //noinspection unchecked
        final Leap<SOURCE, OUT, NOUT> leap =
                (Leap<SOURCE, OUT, NOUT>) findBestMatch(gateClassification);

        if (leap == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any gate of classification type "
                            + gateClassification
            );
        }

        final DataFall<SOURCE, IN, OUT>[] falls = mFalls;

        final int size = mSize;

        if (size == 1) {

            //noinspection unchecked
            final Waterfall<SOURCE, OUT, NOUT> waterfall =
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mGateMap, mGate, mBarrage, 1,
                                                     mCurrent, mCurrentGenerator, leap);

            final DataFall<SOURCE, OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<SOURCE, IN, OUT> fall : falls) {

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
                                                 inWaterfall.mGate, mBarrage, size,
                                                 inWaterfall.mCurrent,
                                                 inWaterfall.mCurrentGenerator, leaps);

        final DataFall<SOURCE, ?, OUT>[] inFalls = inWaterfall.mFalls;

        final DataFall<SOURCE, OUT, NOUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<SOURCE, ?, OUT> inFall = inFalls[0];

            for (final DataFall<SOURCE, OUT, NOUT> outFall : outFalls) {

                link(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; i++) {

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

        final DataFall<SOURCE, IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return (Waterfall<SOURCE, OUT, OUT>) start();
        }

        final int size = mSize;

        final FreeLeap<SOURCE, OUT> leap = freeLeap();

        if (size == 1) {

            //noinspection unchecked
            final Waterfall<SOURCE, OUT, OUT> waterfall =
                    new Waterfall<SOURCE, OUT, OUT>(mSource, mGateMap, mGate, mBarrage, 1, mCurrent,
                                                    mCurrentGenerator, leap);

            final DataFall<SOURCE, OUT, OUT> outFall = waterfall.mFalls[0];

            for (final DataFall<SOURCE, IN, OUT> fall : falls) {

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
                                                inWaterfall.mGate, mBarrage, size,
                                                inWaterfall.mCurrent, inWaterfall.mCurrentGenerator,
                                                leaps);

        final DataFall<SOURCE, ?, OUT>[] inFalls = inWaterfall.mFalls;

        final DataFall<SOURCE, OUT, OUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<SOURCE, ?, OUT> inFall = inFalls[0];

            for (final DataFall<SOURCE, OUT, OUT> outFall : outFalls) {

                link(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; i++) {

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
    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(final Leap<SOURCE, OUT, NOUT> leap) {

        if (leap == null) {

            throw new IllegalArgumentException("the waterfall leap cannot be null");
        }

        final DataFall<SOURCE, IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return ((Waterfall<SOURCE, OUT, OUT>) start()).chain(leap);
        }

        final int size = mSize;

        if (size == 1) {

            registerLeap(leap);

            //noinspection unchecked
            final Waterfall<SOURCE, OUT, NOUT> waterfall =
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mGateMap, mGate, mBarrage, 1,
                                                     mCurrent, mCurrentGenerator, leap);

            final DataFall<SOURCE, OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<SOURCE, IN, OUT> fall : falls) {

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

        final Leap[] leaps = new Leap[size];

        Arrays.fill(leaps, new SegmentedLeap<SOURCE, OUT, NOUT>(leap));

        //noinspection unchecked
        final Waterfall<SOURCE, OUT, NOUT> waterfall =
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mGateMap,
                                                 inWaterfall.mGate, mBarrage, size,
                                                 inWaterfall.mCurrent,
                                                 inWaterfall.mCurrentGenerator, leaps);

        final DataFall<SOURCE, ?, OUT>[] inFalls = inWaterfall.mFalls;

        final DataFall<SOURCE, OUT, NOUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<SOURCE, ?, OUT> inFall = inFalls[0];

            for (final DataFall<SOURCE, OUT, NOUT> outFall : outFalls) {

                link(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; i++) {

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
    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(
            final LeapGenerator<SOURCE, OUT, NOUT> generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall generator cannot be null");
        }

        final DataFall<SOURCE, IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return ((Waterfall<SOURCE, OUT, OUT>) start()).chain(generator);
        }

        final int size = mSize;

        if (size == 1) {

            final Leap<SOURCE, OUT, NOUT> leap = generator.start(0);

            registerLeap(leap);

            //noinspection unchecked
            final Waterfall<SOURCE, OUT, NOUT> waterfall =
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mGateMap, mGate, mBarrage, 1,
                                                     mCurrent, mCurrentGenerator, leap);

            final DataFall<SOURCE, OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<SOURCE, IN, OUT> fall : falls) {

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

        for (int i = 0; i < size; i++) {

            final Leap<SOURCE, OUT, NOUT> leap = generator.start(i);

            registerLeap(leap);

            leaps[i] = leap;
        }

        //noinspection unchecked
        final Waterfall<SOURCE, OUT, NOUT> waterfall =
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mGateMap,
                                                 inWaterfall.mGate, mBarrage, size,
                                                 inWaterfall.mCurrent,
                                                 inWaterfall.mCurrentGenerator, leaps);

        final DataFall<SOURCE, ?, OUT>[] inFalls = inWaterfall.mFalls;

        final DataFall<SOURCE, OUT, NOUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<SOURCE, ?, OUT> inFall = inFalls[0];

            for (final DataFall<SOURCE, OUT, NOUT> outFall : outFalls) {

                link(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; i++) {

                link(inFalls[i], outFalls[i]);
            }
        }

        return waterfall;
    }

    //TODO
    public Collector<OUT> collect() {

        final CollectorLeap<SOURCE, OUT> collectorLeap = new CollectorLeap<SOURCE, OUT>();
        final GateLeap<SOURCE, OUT, OUT> gateLeap = new GateLeap<SOURCE, OUT, OUT>(collectorLeap);

        final Waterfall<SOURCE, IN, OUT> waterfall;

        if (mSize != 1) {

            waterfall = in(1);

        } else {

            waterfall = this;
        }

        waterfall.chain(gateLeap);

        return new DataCollector<SOURCE, OUT>(gateLeap, collectorLeap);
    }

    @Override
    public void deviate() {

        for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

            for (final DataStream<OUT> stream : fall.outputStreams) {

                stream.deviate();
            }
        }
    }

    @Override
    public void deviate(final int streamNumber) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        for (final DataStream<OUT> stream : fall.outputStreams) {

            stream.deviate();
        }
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> discharge() {

        for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

            fall.inputCurrent.discharge(fall, null);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> forward(final Throwable throwable) {

        for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

            fall.inputCurrent.forward(fall, throwable);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> push(final IN... drops) {

        final DataFall<SOURCE, IN, OUT>[] falls = mFalls;

        for (final IN drop : drops) {

            for (final DataFall<SOURCE, IN, OUT> fall : falls) {

                fall.inputCurrent.push(fall, drop);
            }
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> push(final Iterable<? extends IN> drops) {

        final DataFall<SOURCE, IN, OUT>[] falls = mFalls;

        for (final IN drop : drops) {

            for (final DataFall<SOURCE, IN, OUT> fall : falls) {

                fall.inputCurrent.push(fall, drop);
            }
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> push(final IN drop) {

        for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

            fall.inputCurrent.push(fall, drop);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends IN> drops) {

        for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

            fall.inputCurrent.pushAfter(fall, delay, timeUnit, drops);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushAfter(final long delay, final TimeUnit timeUnit,
            final IN drop) {

        for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

            fall.inputCurrent.pushAfter(fall, delay, timeUnit, drop);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushAfter(final long delay, final TimeUnit timeUnit,
            final IN... drops) {

        final List<IN> list = Arrays.asList(drops);

        for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

            fall.inputCurrent.pushAfter(fall, delay, timeUnit, list);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> discharge(final int streamNumber) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        fall.inputCurrent.discharge(fall, null);

        return this;
    }

    @Override
    public void drain() {

        for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

            for (final DataStream<OUT> stream : fall.outputStreams) {

                stream.drain(true);
            }
        }
    }

    @Override
    public void drain(final int streamNumber) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        for (final DataStream<OUT> stream : fall.outputStreams) {

            stream.drain(true);
        }
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> forward(final int streamNumber, final Throwable throwable) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        fall.inputCurrent.forward(fall, throwable);

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> push(final int streamNumber, final IN... drops) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        for (final IN drop : drops) {

            fall.inputCurrent.push(fall, drop);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> push(final int streamNumber,
            final Iterable<? extends IN> drops) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        for (final IN drop : drops) {

            fall.inputCurrent.push(fall, drop);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> push(final int streamNumber, final IN drop) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        fall.inputCurrent.push(fall, drop);

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends IN> drops) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        fall.inputCurrent.pushAfter(fall, delay, timeUnit, drops);

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final IN drop) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        fall.inputCurrent.pushAfter(fall, delay, timeUnit, drop);

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final IN... drops) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        fall.inputCurrent.pushAfter(fall, delay, timeUnit, Arrays.asList(drops));

        return this;
    }

    @Override
    public int size() {

        return mFalls.length;
    }

    @Override
    public Waterfall<SOURCE, SOURCE, ?> source() {

        return mSource;
    }

    @Override
    public <TYPE> Gate<TYPE> when(final Class<TYPE> gateType) {

        return when(Classification.ofType(gateType));
    }

    @Override
    public <TYPE> Gate<TYPE> when(final Classification<TYPE> gateClassification) {

        final GateLeap<?, ?, ?> leap = findBestMatch(gateClassification);

        if (leap == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any gate of classification type "
                            + gateClassification
            );
        }

        return new DataGate<TYPE>(leap, gateClassification);
    }

    /**
     * Deviates the flow of this waterfall, either downstream or upstream, by effectively
     * preventing any coming data to be pushed further.
     *
     * @param downStream Whether the waterfall must be deviated downstream.
     * @see #deviate(int, boolean)
     */
    public void deviate(final boolean downStream) {

        if (downStream) {

            deviate();

        } else {

            for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

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
     * @param downStream   Whether the waterfall must be deviated downstream.
     * @see #deviate(boolean)
     */
    public void deviate(final int streamNumber, final boolean downStream) {

        if (downStream) {

            deviate(streamNumber);

        } else {

            final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

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

        final DataFall<SOURCE, IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return (Waterfall<SOURCE, OUT, OUT>) start().distribute();
        }

        final int size = mSize;

        if (size == 1) {

            return chain();
        }

        return in(1).chainBarrage(new Barrage<SOURCE, OUT>(size)).in(size);
    }

    /**
     * Drains the waterfall, either downstream or upstream, by removing all the falls and rivers
     * fed only by this waterfall streams.
     *
     * @param downStream Whether the waterfall must be deviated downstream.
     * @see #drain(int, boolean)
     */
    public void drain(final boolean downStream) {

        if (downStream) {

            drain();

        } else {

            for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

                for (final DataStream<IN> stream : fall.inputStreams) {

                    stream.drain(false);
                }
            }
        }
    }

    /**
     * Drains the specified waterfall stream, either downstream or upstream, by removing from all
     * the falls and rivers fed only by the specific stream.
     *
     * @param streamNumber The number identifying the target stream.
     * @param downStream   Whether the waterfall must be deviated downstream.
     * @see #drain(boolean)
     */
    public void drain(final int streamNumber, final boolean downStream) {

        if (downStream) {

            drain(streamNumber);

        } else {

            final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

            for (final DataStream<IN> stream : fall.inputStreams) {

                stream.drain(false);
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
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBarrage, mSize, null,
                                              generator, mFalls);
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
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBarrage, fallCount,
                                              mCurrent, mCurrentGenerator, mFalls);
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
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBarrage, mSize, current,
                                              null, mFalls);
    }

    /**
     * Makes the waterfall streams flow through a background current with the specified thread pool
     * size.
     *
     * @param poolSize The pool size.
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> inBackground(final int poolSize) {

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBarrage, poolSize,
                                              Currents.pool(Math.min(poolSize, getBestPoolSize())),
                                              null, mFalls);
    }

    /**
     * Makes the waterfall streams flow through a background current.
     * <p/>
     * The optimum thead pool size will be automatically computed based on the available resources
     * and the waterfall size.
     *
     * @return The newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> inBackground() {

        final int poolSize = getBestPoolSize();

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBarrage, poolSize,
                                              Currents.pool(poolSize), null, mFalls);
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
     * Creates and returns a new waterfall with the same size of this one.
     *
     * @return The newly created waterfall.
     */
    public Waterfall<OUT, OUT, OUT> start() {

        final int size = mSize;

        final FreeLeap<OUT, OUT> leap = freeLeap();

        final Leap[] leaps = new Leap[size];

        Arrays.fill(leaps, leap);

        final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap = Collections.emptyMap();

        //noinspection unchecked
        return new Waterfall<OUT, OUT, OUT>(null, gateMap, mGate, null, size, mCurrent,
                                            mCurrentGenerator, leaps);
    }

    /**
     * Creates and returns a new waterfall with the same size of this one.
     *
     * @param dataType The data type.
     * @param <DATA>   The data type.
     * @return The newly created waterfall.
     */
    public <DATA> Waterfall<DATA, DATA, DATA> start(final Class<DATA> dataType) {

        return start(Classification.ofType(dataType));
    }

    /**
     * Creates and returns a new waterfall with the same size of this one.
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
     * Creates and returns a new waterfall with the same size of this one and chained to the leaps
     * returned by the specified generator.
     *
     * @param generator The leap generator.
     * @param <NIN>     The new input data type.
     * @param <NOUT>    The new output data type.
     * @return The newly created waterfall.
     */
    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(
            final LeapGenerator<NIN, NIN, NOUT> generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall generator cannot be null");
        }

        final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap = Collections.emptyMap();

        final int size = mSize;

        if (size == 1) {

            final Leap<NIN, NIN, NOUT> leap = generator.start(0);

            registerLeap(leap);

            //noinspection unchecked
            return new Waterfall<NIN, NIN, NOUT>(null, gateMap, mGate, null, 1, mCurrent,
                                                 mCurrentGenerator, leap);
        }

        final Leap[] leaps = new Leap[size];

        for (int i = 0; i < size; i++) {

            final Leap<NIN, NIN, NOUT> leap = generator.start(i);

            registerLeap(leap);

            leaps[i] = leap;
        }

        //noinspection unchecked
        return new Waterfall<NIN, NIN, NOUT>(null, gateMap, mGate, null, size, mCurrent,
                                             mCurrentGenerator, leaps);
    }

    /**
     * Creates and returns a new waterfall with the same size of this one and chained to the
     * specified leap.
     *
     * @param leap   The leap instance.
     * @param <NIN>  The new input data type.
     * @param <NOUT> The new output data type.
     * @return The newly created waterfall.
     */
    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(final Leap<NIN, NIN, NOUT> leap) {

        if (leap == null) {

            throw new IllegalArgumentException("the waterfall leap cannot be null");
        }

        registerLeap(leap);

        final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap = Collections.emptyMap();

        //noinspection unchecked
        return new Waterfall<NIN, NIN, NOUT>(null, gateMap, mGate, null, mSize, mCurrent,
                                             mCurrentGenerator, leap);
    }

    private Waterfall<SOURCE, OUT, OUT> chainBarrage(final Barrage<SOURCE, OUT> barrage) {

        final Waterfall<SOURCE, OUT, OUT> waterfall = chain(barrage);

        //noinspection unchecked
        return new Waterfall<SOURCE, OUT, OUT>(waterfall.mSource, waterfall.mGateMap,
                                               waterfall.mGate, barrage, waterfall.mSize,
                                               waterfall.mCurrent, waterfall.mCurrentGenerator,
                                               waterfall.mFalls);
    }

    private GateLeap<?, ?, ?> findBestMatch(final Classification<?> gateClassification) {

        final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap = mGateMap;

        GateLeap<?, ?, ?> leap = gateMap.get(gateClassification);

        if (leap == null) {

            Classification<?> bestMatch = null;

            for (final Entry<Classification<?>, GateLeap<?, ?, ?>> entry : gateMap.entrySet()) {

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

    private void mapGate(final HashMap<Classification<?>, GateLeap<?, ?, ?>> gateMap,
            final Classification<?> gateClassification, final GateLeap<?, ?, ?> leap) {

        if (!gateClassification.getRawType().isInstance(leap)) {

            throw new IllegalArgumentException(
                    "the leap does not implement the gate classification type");
        }

        if (gateMap.containsKey(gateClassification)) {

            throw new IllegalArgumentException("the gate classification type is already present");
        }

        gateMap.put(gateClassification, leap);
    }
}