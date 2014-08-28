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
import com.bmd.wtf.flw.Bridge;
import com.bmd.wtf.flw.Collector;
import com.bmd.wtf.flw.Pump;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.gts.Gate;
import com.bmd.wtf.gts.GateGenerator;
import com.bmd.wtf.gts.OpenGate;
import com.bmd.wtf.spr.Spring;
import com.bmd.wtf.spr.SpringGenerator;
import com.bmd.wtf.spr.Springs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
 * @param <SOURCE> the waterfall source data type.
 * @param <IN>     the input data type.
 * @param <OUT>    the output data type.
 */
public class Waterfall<SOURCE, IN, OUT> extends AbstractRiver<IN> {

    // TODO: bridge, linq(?)
    // TODO: exception javadoc

    private static final Comparator<?> NATURAL_COMPARATOR =
            Collections.reverseOrder(Collections.reverseOrder());

    private static final DataFall[] NO_FALL = new DataFall[0];

    private static final Classification<Void> SELF_CLASSIFICATION = new Classification<Void>() {};

    private static final WeakHashMap<Gate<?, ?>, Void> sGates = new WeakHashMap<Gate<?, ?>, Void>();

    private static OpenGate<?> sOpenGate;

    private final Current mBackgroundCurrent;

    private final int mBackgroundPoolSize;

    private final Classification<?> mBridgeClassification;

    private final Map<Classification<?>, BridgeGate<?, ?>> mBridgeMap;

    private final Current mCurrent;

    private final CurrentGenerator mCurrentGenerator;

    private final DataFall<IN, OUT>[] mFalls;

    private final PumpGate<?> mPump;

    private final int mSize;

    private final Waterfall<SOURCE, SOURCE, ?> mSource;

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final Map<Classification<?>, BridgeGate<?, ?>> bridgeMap,
            final Classification<?> bridgeClassification, final int backgroundPoolSize,
            final Current backgroundCurrent, final PumpGate<?> pumpGate, final int size,
            final Current current, final CurrentGenerator generator,
            final DataFall<IN, OUT>[] falls) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mBridgeMap = bridgeMap;
        mBridgeClassification = bridgeClassification;
        mBackgroundPoolSize = backgroundPoolSize;
        mBackgroundCurrent = backgroundCurrent;
        mPump = pumpGate;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;
        mFalls = falls;
    }

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final Map<Classification<?>, BridgeGate<?, ?>> bridgeMap,
            final Classification<?> bridgeClassification, final int backgroundPoolSize,
            final Current backgroundCurrent, final PumpGate<?> pumpGate, final int size,
            final Current current, final CurrentGenerator generator, final Gate<IN, OUT>[] gates) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mBridgeClassification = null;
        mBackgroundPoolSize = backgroundPoolSize;
        mBackgroundCurrent = backgroundCurrent;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;

        final int length = gates.length;

        final Gate<IN, OUT> wrappedGate;

        if (bridgeClassification != null) {

            final Gate<IN, OUT> gate = gates[0];

            final HashMap<Classification<?>, BridgeGate<?, ?>> fallBridgeMap =
                    new HashMap<Classification<?>, BridgeGate<?, ?>>(bridgeMap);
            final BridgeGate<IN, OUT> bridgeGate = new BridgeGate<IN, OUT>(gate);

            mapBridge(fallBridgeMap,
                      (SELF_CLASSIFICATION == bridgeClassification) ? Classification.ofType(
                              gate.getClass()) : bridgeClassification, bridgeGate);

            mBridgeMap = fallBridgeMap;

            wrappedGate = bridgeGate;

        } else {

            if (size != length) {

                wrappedGate = new BarrageGate<IN, OUT>(gates[0]);

            } else {

                wrappedGate = null;
            }

            mBridgeMap = bridgeMap;
        }

        final DataFall[] falls = new DataFall[size];

        if (size == 1) {

            mPump = null;

        } else {

            mPump = pumpGate;
        }

        final PumpGate<?> fallPump = mPump;

        for (int i = 0; i < size; ++i) {

            final Gate<IN, OUT> gate = (wrappedGate != null) ? wrappedGate : gates[i];
            final Current fallCurrent;

            if (current == null) {

                fallCurrent = generator.create(i);

            } else {

                fallCurrent = current;
            }

            if (fallPump != null) {

                falls[i] = new PumpFall<SOURCE, IN, OUT>(this, fallCurrent, gate, i, fallPump);

            } else {

                falls[i] = new DataFall<IN, OUT>(this, fallCurrent, gate, i);
            }
        }

        //noinspection unchecked
        mFalls = (DataFall<IN, OUT>[]) falls;
    }

    /**
     * Creates and returns a new waterfall composed by a single synchronous stream.
     *
     * @return the newly created waterfall.
     */
    public static Waterfall<Object, Object, Object> fall() {

        final Map<Classification<?>, BridgeGate<?, ?>> bridgeMap = Collections.emptyMap();

        //noinspection unchecked
        return new Waterfall<Object, Object, Object>(null, bridgeMap, null, 0, null, null, 1,
                                                     Currents.passThrough(), null, NO_FALL);
    }

    /**
     * Connects an input and an output fall through a data stream.
     *
     * @param inFall  the input fall.
     * @param outFall the output fall.
     * @param <DATA>  the data type.
     * @return the data stream running between the two falls.
     * @throws IllegalArgumentException if the input or the output fall are null.
     */
    private static <DATA> DataStream<DATA> connect(final DataFall<?, DATA> inFall,
            final DataFall<DATA, ?> outFall) {

        final DataStream<DATA> stream = new DataStream<DATA>(inFall, outFall);

        outFall.inputStreams.add(stream);
        inFall.outputStreams.add(stream);

        return stream;
    }

    /**
     * Lazily creates and return a singleton open gate instance.
     *
     * @param <DATA> the data type.
     * @return the open gate instance.
     */
    private static <DATA> OpenGate<DATA> openGate() {

        if (sOpenGate == null) {

            sOpenGate = new OpenGate<Object>();
        }

        //noinspection unchecked
        return (OpenGate<DATA>) sOpenGate;
    }

    /**
     * Registers the specified gate instance by making sure it is unique among all the created
     * waterfalls.
     *
     * @param gate the gate to register.
     * @throws IllegalArgumentException if the gate is null or is already registered.
     */
    private static void registerGate(final Gate<?, ?> gate) {

        if (gate == null) {

            throw new IllegalArgumentException("the waterfall gate cannot be null");
        }

        if (sGates.containsKey(gate)) {

            throw new IllegalArgumentException("the waterfall already contains the gate: " + gate);
        }

        sGates.put(gate, null);
    }

    /**
     * Tells the waterfall to build a bridge above the next gate chained to it.
     * <p/>
     * The bridge type will be the same as the gate raw type.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> bridge() {

        return bridge(SELF_CLASSIFICATION);
    }

    /**
     * Tells the waterfall to build a bridge of the specified type above the next gate chained to it.
     *
     * @param bridgeClass the bridge class.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the bridge class is null.
     */
    public Waterfall<SOURCE, IN, OUT> bridge(final Class<?> bridgeClass) {

        return bridge(Classification.ofType(bridgeClass));
    }

    /**
     * Tells the waterfall to build a bridge of the specified classification type above the next
     * gate chained to it.
     *
     * @param bridgeClassification the bridge classification.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the bridge classification is null.
     */
    public Waterfall<SOURCE, IN, OUT> bridge(final Classification<?> bridgeClassification) {

        if (bridgeClassification == null) {

            throw new IllegalArgumentException("the bridge classification cannot be null");
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, mBridgeMap, bridgeClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mPump, mSize,
                                              mCurrent, mCurrentGenerator, mFalls);
    }

    /**
     * Chains the gate protected by the bridge of the specified classification type to this
     * waterfall.
     * <p/>
     * Note that contrary to common gate, the ones protected by a bridge can be added several times
     * to the same waterfall.
     *
     * @param bridgeClassification the bridge classification.
     * @param <NOUT>               the new output data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if no protected gate is found.
     */
    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(
            final Classification<? extends Gate<OUT, NOUT>> bridgeClassification) {

        //noinspection unchecked
        final Gate<OUT, NOUT> gate = (Gate<OUT, NOUT>) findBestMatch(bridgeClassification);

        if (gate == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any bridge of classification type "
                            + bridgeClassification);
        }

        final DataFall<IN, OUT>[] falls = mFalls;
        final int size = mSize;

        //noinspection unchecked
        final Gate<OUT, NOUT>[] gates = new Gate[size];

        if (size == 1) {

            gates[0] = gate;

            final Waterfall<SOURCE, OUT, NOUT> waterfall =
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mBridgeMap, mBridgeClassification,
                                                     mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                                     1, mCurrent, mCurrentGenerator, gates);

            final DataFall<OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<IN, OUT> fall : falls) {

                connect(fall, outFall);
            }

            return waterfall;
        }

        final int length = falls.length;
        final Waterfall<SOURCE, ?, OUT> inWaterfall;

        if ((length != 1) && (length != size)) {

            inWaterfall = merge();

        } else {

            inWaterfall = this;
        }

        Arrays.fill(gates, gate);

        final Waterfall<SOURCE, OUT, NOUT> waterfall =
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mBridgeMap,
                                                 inWaterfall.mBridgeClassification,
                                                 mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                                 size, inWaterfall.mCurrent,
                                                 inWaterfall.mCurrentGenerator, gates);

        final DataFall<?, OUT>[] inFalls = inWaterfall.mFalls;
        final DataFall<OUT, NOUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<?, OUT> inFall = inFalls[0];

            for (final DataFall<OUT, NOUT> outFall : outFalls) {

                connect(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; ++i) {

                connect(inFalls[i], outFalls[i]);
            }
        }

        return waterfall;
    }

    /**
     * Chains an open gate to this waterfall.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> chain() {

        final DataFall<IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return (Waterfall<SOURCE, OUT, OUT>) start();
        }

        final int size = mSize;
        final OpenGate<OUT> gate = openGate();

        //noinspection unchecked
        final Gate<OUT, OUT>[] gates = new Gate[size];

        if (size == 1) {

            gates[0] = gate;

            final Waterfall<SOURCE, OUT, OUT> waterfall =
                    new Waterfall<SOURCE, OUT, OUT>(mSource, mBridgeMap, mBridgeClassification,
                                                    mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                                    1, mCurrent, mCurrentGenerator, gates);

            final DataFall<OUT, OUT> outFall = waterfall.mFalls[0];

            for (final DataFall<IN, OUT> fall : falls) {

                connect(fall, outFall);
            }

            return waterfall;
        }

        final int length = falls.length;
        final Waterfall<SOURCE, ?, OUT> inWaterfall;

        if ((length != 1) && (length != size)) {

            inWaterfall = merge();

        } else {

            inWaterfall = this;
        }

        Arrays.fill(gates, gate);

        final Waterfall<SOURCE, OUT, OUT> waterfall =
                new Waterfall<SOURCE, OUT, OUT>(inWaterfall.mSource, inWaterfall.mBridgeMap,
                                                inWaterfall.mBridgeClassification,
                                                mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                                size, inWaterfall.mCurrent,
                                                inWaterfall.mCurrentGenerator, gates);

        final DataFall<?, OUT>[] inFalls = inWaterfall.mFalls;
        final DataFall<OUT, OUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<?, OUT> inFall = inFalls[0];

            for (final DataFall<OUT, OUT> outFall : outFalls) {

                connect(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; ++i) {

                connect(inFalls[i], outFalls[i]);
            }
        }

        return waterfall;
    }

    /**
     * Chains the specified gates to this waterfall.
     * <p/>
     * Note that calling this method will have the same effect as calling first
     * <code>in(gates.length)</code>.
     *
     * @param gates  the gate instances.
     * @param <NOUT> the new output data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the gate collection is null, empty or contains null
     *                                  instances or instances already chained to this or another
     *                                  waterfall.
     */
    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(
            final Collection<? extends Gate<OUT, NOUT>> gates) {

        if (gates == null) {

            throw new IllegalArgumentException("the waterfall gate collection cannot be null");
        }

        final int length = gates.size();

        if (length == 0) {

            throw new IllegalArgumentException("the waterfall gate collection cannot be empty");
        }

        final Waterfall<SOURCE, IN, OUT> waterfall;

        if (mSize == length) {

            waterfall = this;

        } else {

            waterfall = in(length);
        }

        final Iterator<? extends Gate<OUT, NOUT>> iterator = gates.iterator();

        return waterfall.chain(new GateGenerator<OUT, NOUT>() {

            @Override
            public Gate<OUT, NOUT> create(final int fallNumber) {

                return iterator.next();
            }
        });
    }

    /**
     * Chains the specified gate to this waterfall.
     * <p/>
     * Note that in case this waterfall is composed by more then one data stream, all the data
     * flowing through them will be passed to the specified gate.
     *
     * @param gate   the gate instance.
     * @param <NOUT> the new output data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the gate is null or already chained to this or another
     *                                  waterfall.
     */
    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(final Gate<OUT, NOUT> gate) {

        final DataFall<IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return (Waterfall<SOURCE, OUT, NOUT>) start(gate);
        }

        final int size = mSize;

        //noinspection unchecked
        final Gate<OUT, NOUT>[] gates = new Gate[]{gate};

        if (size == 1) {

            registerGate(gate);

            final Waterfall<SOURCE, OUT, NOUT> waterfall =
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mBridgeMap, mBridgeClassification,
                                                     mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                                     1, mCurrent, mCurrentGenerator, gates);

            final DataFall<OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<IN, OUT> fall : falls) {

                connect(fall, outFall);
            }

            return waterfall;
        }

        final int length = falls.length;
        final Waterfall<SOURCE, ?, OUT> inWaterfall;

        if ((length != 1) && (length != size)) {

            inWaterfall = merge();

        } else {

            inWaterfall = this;
        }

        registerGate(gate);

        final Waterfall<SOURCE, OUT, NOUT> waterfall =
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mBridgeMap,
                                                 inWaterfall.mBridgeClassification,
                                                 mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                                 size, inWaterfall.mCurrent,
                                                 inWaterfall.mCurrentGenerator, gates);

        final DataFall<?, OUT>[] inFalls = inWaterfall.mFalls;
        final DataFall<OUT, NOUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<?, OUT> inFall = inFalls[0];

            for (final DataFall<OUT, NOUT> outFall : outFalls) {

                connect(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; ++i) {

                connect(inFalls[i], outFalls[i]);
            }
        }

        return waterfall;
    }

    /**
     * Chains the gates returned by the specified generator to this waterfall.
     * <p/>
     * Note that in case this waterfall is composed by more then one data stream, each gate created
     * by the generator will handle a single stream.
     *
     * @param generator the gate generator.
     * @param <NOUT>    the new output data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the generator is null or returns a null gate or one
     *                                  already chained to this or another waterfall.
     */
    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(final GateGenerator<OUT, NOUT> generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall generator cannot be null");
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return (Waterfall<SOURCE, OUT, NOUT>) start(generator);
        }

        final int size = mSize;

        //noinspection unchecked
        final Gate<OUT, NOUT>[] gates = new Gate[size];

        if (size == 1) {

            final Gate<OUT, NOUT> gate = generator.create(0);

            registerGate(gate);

            gates[0] = gate;

            final Waterfall<SOURCE, OUT, NOUT> waterfall =
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mBridgeMap, mBridgeClassification,
                                                     mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                                     1, mCurrent, mCurrentGenerator, gates);

            final DataFall<OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<IN, OUT> fall : falls) {

                connect(fall, outFall);
            }

            return waterfall;
        }

        if (mBridgeClassification != null) {

            throw new IllegalStateException("cannot make a bridge from more than one gate");
        }

        final int length = falls.length;
        final Waterfall<SOURCE, ?, OUT> inWaterfall;

        if ((length != 1) && (length != size)) {

            inWaterfall = merge();

        } else {

            inWaterfall = this;
        }

        for (int i = 0; i < size; ++i) {

            final Gate<OUT, NOUT> gate = generator.create(i);

            registerGate(gate);

            gates[i] = gate;
        }

        final Waterfall<SOURCE, OUT, NOUT> waterfall =
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mBridgeMap,
                                                 inWaterfall.mBridgeClassification,
                                                 mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                                 size, inWaterfall.mCurrent,
                                                 inWaterfall.mCurrentGenerator, gates);

        final DataFall<?, OUT>[] inFalls = inWaterfall.mFalls;
        final DataFall<OUT, NOUT>[] outFalls = waterfall.mFalls;

        if (inFalls.length == 1) {

            final DataFall<?, OUT> inFall = inFalls[0];

            for (final DataFall<OUT, NOUT> outFall : outFalls) {

                connect(inFall, outFall);
            }

        } else {

            for (int i = 0; i < size; ++i) {

                connect(inFalls[i], outFalls[i]);
            }
        }

        return waterfall;
    }

    /**
     * Tells the waterfall to close the bridge handling the specified gate, that is, the bridge
     * will not be accessible anymore to the ones requiring it.
     *
     * @param gate   the gate instance.
     * @param <TYPE> the gate type.
     * @return the newly created waterfall.
     */
    public <TYPE extends Gate<?, ?>> Waterfall<SOURCE, IN, OUT> close(final TYPE gate) {

        if (gate == null) {

            return this;
        }

        boolean isChanged = false;

        final HashMap<Classification<?>, BridgeGate<?, ?>> bridgeMap =
                new HashMap<Classification<?>, BridgeGate<?, ?>>(mBridgeMap);

        final Iterator<BridgeGate<?, ?>> iterator = bridgeMap.values().iterator();

        while (iterator.hasNext()) {

            if (gate == iterator.next().gate) {

                iterator.remove();

                isChanged = true;
            }
        }

        if (!isChanged) {

            return this;
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, bridgeMap, mBridgeClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mPump, mSize,
                                              mCurrent, mCurrentGenerator, mFalls);
    }

    /**
     * Tells the waterfall to close the bridge of the specified classification type, that is, the bridge
     * will not be accessible anymore to the ones requiring it.
     *
     * @param bridgeClassification the bridge classification.
     * @param <TYPE>               the gate type.
     * @return the newly created waterfall.
     */
    public <TYPE> Waterfall<SOURCE, IN, OUT> close(
            final Classification<TYPE> bridgeClassification) {

        final BridgeGate<?, ?> bridge = findBestMatch(bridgeClassification);

        if (bridge == null) {

            return this;
        }

        final HashMap<Classification<?>, BridgeGate<?, ?>> bridgeMap =
                new HashMap<Classification<?>, BridgeGate<?, ?>>(mBridgeMap);

        final Iterator<BridgeGate<?, ?>> iterator = bridgeMap.values().iterator();

        while (iterator.hasNext()) {

            if (bridge == iterator.next()) {

                iterator.remove();
            }
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, bridgeMap, mBridgeClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mPump, mSize,
                                              mCurrent, mCurrentGenerator, mFalls);
    }

    /**
     * Creates and returns a new data collector.
     *
     * @return the collector.
     * @throws IllegalStateException if this waterfall was not started.
     */
    public Collector<OUT> collect() {

        if (mFalls == NO_FALL) {

            throw new IllegalStateException("cannot collect data from a not started waterfall");
        }

        final CollectorGate<OUT> collectorGate = new CollectorGate<OUT>();
        final BridgeGate<OUT, OUT> bridgeGate = new BridgeGate<OUT, OUT>(collectorGate);

        final Waterfall<SOURCE, IN, OUT> waterfall;

        if (mSize != 1) {

            waterfall = in(1);

        } else {

            waterfall = this;
        }

        waterfall.chain(bridgeGate);

        return new DataCollector<OUT>(bridgeGate, collectorGate);
    }

    /**
     * Causes the data drops flowing through this waterfall streams to be concatenated, so that all
     * the data coming from the stream number 0 will come before the ones coming from the stream
     * number 1, and so on.<br/>
     * The resulting waterfall will have size equal to 1.
     * <p/>
     * Note that the returned waterfall internally cache the data pushed through it and then
     * discharge them on the first flush.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> concat() {

        return chain(new CumulativeCacheGate<OUT, OUT>(size()) {

            @Override
            protected void onClear(final List<List<OUT>> cache, final River<OUT> upRiver,
                    final River<OUT> downRiver) {

                for (final List<OUT> data : cache) {

                    downRiver.push(data);
                }

                super.onClear(cache, upRiver, downRiver);
            }
        }).merge();
    }

    /**
     * Causes the data drops flowing through this waterfall to be delayed of the specified time.
     * <p/>
     * Note that the returned waterfall internally cache the data pushed through it and then
     * discharge them on the first flush.
     *
     * @param delay    the delay in <code>timeUnit</code> time units.
     * @param timeUnit the delay time unit.
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> delay(final long delay, final TimeUnit timeUnit) {

        return chain(new CacheGate<OUT, OUT>(size()) {

            @Override
            protected void onClear(final List<OUT> cache, final int streamNumber,
                    final River<OUT> upRiver, final River<OUT> downRiver) {

                downRiver.pushAfter(delay, timeUnit, cache);

                super.onClear(cache, streamNumber, upRiver, downRiver);
            }
        });
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
    public Waterfall<SOURCE, IN, OUT> exception(final Throwable throwable) {

        for (final DataFall<IN, OUT> fall : mFalls) {

            fall.raiseLevel(1);

            fall.inputCurrent.exception(fall, throwable);
        }

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> flush() {

        for (final DataFall<IN, OUT> fall : mFalls) {

            fall.inputCurrent.flush(fall, null);
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
    public Waterfall<SOURCE, IN, OUT> flushStream(final int streamNumber) {

        final DataFall<IN, OUT> fall = mFalls[streamNumber];

        fall.inputCurrent.flush(fall, null);

        return this;
    }

    @Override
    public <TYPE> Bridge<TYPE> on(final Class<TYPE> bridgeClass) {

        return on(Classification.ofType(bridgeClass));
    }

    @Override
    public <TYPE> Bridge<TYPE> on(final TYPE gate) {

        if (gate == null) {

            throw new IllegalArgumentException("the bridge gate cannot be null");
        }

        BridgeGate<?, ?> bridge = null;

        final Map<Classification<?>, BridgeGate<?, ?>> bridgeMap = mBridgeMap;

        for (final BridgeGate<?, ?> bridgeGate : bridgeMap.values()) {

            if (bridgeGate.gate == gate) {

                bridge = bridgeGate;

                break;
            }
        }

        if (bridge == null) {

            throw new IllegalArgumentException("the waterfall does not retain the bridge " + gate);
        }

        return new DataBridge<TYPE>(bridge, new Classification<TYPE>() {});
    }

    @Override
    public <TYPE> Bridge<TYPE> on(final Classification<TYPE> bridgeClassification) {

        final BridgeGate<?, ?> bridge = findBestMatch(bridgeClassification);

        if (bridge == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any bridge of classification type "
                            + bridgeClassification);
        }

        return new DataBridge<TYPE>(bridge, bridgeClassification);
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

    @Override
    public Waterfall<SOURCE, IN, OUT> streamException(final int streamNumber,
            final Throwable throwable) {

        final DataFall<IN, OUT> fall = mFalls[streamNumber];

        fall.raiseLevel(1);

        fall.inputCurrent.exception(fall, throwable);

        return this;
    }

    /**
     * Deviates the flow of this waterfall, either downstream or upstream, by effectively
     * preventing any coming data to be pushed further.
     *
     * @param direction whether the waterfall must be deviated downstream or upstream.
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
     * @param streamNumber the number identifying the target stream.
     * @param direction    whether the waterfall must be deviated downstream or upstream.
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
     * @return the newly created waterfall.
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

        return in(1).chainPump(new PumpGate<OUT>(size)).in(size).chain();
    }

    /**
     * Distributes all the data flowing through this waterfall in the different output streams by
     * means of the specified pump.
     *
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the pump is null.
     */
    public Waterfall<SOURCE, OUT, OUT> distribute(final Pump<OUT> pump) {

        if (pump == null) {

            throw new IllegalArgumentException("the waterfall pump cannot be null");
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            //noinspection unchecked
            return (Waterfall<SOURCE, OUT, OUT>) start().distribute(pump);
        }

        final int size = mSize;

        if (size == 1) {

            return chain();
        }

        return in(1).chainPump(new PumpGate<OUT>(pump, size)).in(size).chain();
    }

    /**
     * Drains the waterfall, either downstream or upstream, by removing all the falls and rivers
     * fed only by this waterfall streams.
     *
     * @param direction whether the waterfall must be deviated downstream or upstream.
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
     * @param streamNumber the number identifying the target stream.
     * @param direction    whether the waterfall must be deviated downstream or upstream.
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
     * Filters the data flowing through this waterfall by retaining only the specified count of
     * drops, starting from the last minus the specified offset and discarding the other ones.
     * <p/>
     * Note that the returned waterfall internally cache the data pushed through it and then
     * discharge them on the first flush.
     *
     * @param endOffset the offset from the last data drop before a flush.
     * @param count     the count of the element in the range.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the offset is negative, the count is negative or equal
     *                                  to zero or is greater than the offset + 1.
     */
    public Waterfall<SOURCE, OUT, OUT> endRange(final int endOffset, final int count) {

        if ((endOffset < 0) || (count <= 0) || (count > (endOffset + 1))) {

            throw new IllegalArgumentException("invalid range indexes");
        }

        final long maxSize = (long) endOffset + 1;

        final int length = Math.max(size(), 1);
        final long[] counts = new long[length];

        //noinspection unchecked
        final ArrayList<OUT>[] lists = new ArrayList[length];

        for (int i = 0; i < lists.length; i++) {

            lists[i] = new ArrayList<OUT>();
        }

        return chain(new OpenGate<OUT>() {

            @Override
            public void onPush(final River<OUT> upRiver, final River<OUT> downRiver,
                    final int fallNumber, final OUT drop) {

                ++counts[fallNumber];

                final ArrayList<OUT> list = lists[fallNumber];

                list.add(drop);

                if (list.size() > maxSize) {

                    list.remove(0);
                }
            }

            @Override
            public void onFlush(final River<OUT> upRiver, final River<OUT> downRiver,
                    final int fallNumber) {

                final long index = counts[fallNumber];
                final ArrayList<OUT> list = lists[fallNumber];

                if (index > (maxSize - count)) {

                    downRiver.push(list.subList(0, count + (int) Math.min(0, index - maxSize)));
                }

                list.clear();

                super.onFlush(upRiver, downRiver, fallNumber);
            }
        });
    }

    /**
     * Makes this waterfall feed the specified one. After the call, all the data flowing through
     * this waterfall will be pushed into the target one.
     *
     * @param waterfall the waterfall to chain.
     * @throws IllegalArgumentException if the waterfall is null or equal to this one, or this or
     *                                  the target one was not started, or if a possible closed
     *                                  loop in the waterfall chain is detected.
     */
    public void feed(final Waterfall<?, OUT, ?> waterfall) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the waterfall cannot be null");
        }

        if (this == waterfall) {

            throw new IllegalArgumentException("cannot feed a waterfall with itself");
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        if ((falls == NO_FALL) || (waterfall.mFalls == NO_FALL)) {

            throw new IllegalStateException("cannot feed a not started waterfall");
        }

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

                connect(fall, outFall);
            }

        } else {

            final Waterfall<SOURCE, ?, OUT> inWaterfall;

            if ((length != 1) && (length != size)) {

                inWaterfall = merge();

            } else {

                inWaterfall = this;
            }

            final DataFall<?, OUT>[] inFalls = inWaterfall.mFalls;

            if (inFalls.length == 1) {

                final DataFall<?, OUT> inFall = inFalls[0];

                for (final DataFall<OUT, ?> outFall : outFalls) {

                    connect(inFall, outFall);
                }

            } else {

                for (int i = 0; i < size; ++i) {

                    connect(inFalls[i], outFalls[i]);
                }
            }
        }
    }

    /**
     * Makes this waterfall feed the stream identified by the specified number. After the call, all
     * the data flowing through this waterfall will be pushed into the target stream.
     *
     * @param streamNumber the number identifying the target stream.
     * @param waterfall    the target waterfall.
     * @throws IllegalArgumentException if the waterfall is null or equal to this one, or this or
     *                                  the target one was not started, or if a possible closed
     *                                  loop in the waterfall chain is detected.
     */
    public void feedStream(final int streamNumber, final Waterfall<?, OUT, ?> waterfall) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the waterfall cannot be null");
        }

        if (this == waterfall) {

            throw new IllegalArgumentException("cannot feed a waterfall with itself");
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        if ((falls == NO_FALL) || (waterfall.mFalls == NO_FALL)) {

            throw new IllegalStateException("cannot feed a waterfall with a not started one");
        }

        final DataFall<OUT, ?> outFall = waterfall.mFalls[streamNumber];

        for (final DataStream<?> outputStream : outFall.outputStreams) {

            //noinspection unchecked
            if (outputStream.canReach(Arrays.asList(falls))) {

                throw new IllegalArgumentException(
                        "a possible loop in the waterfall chain has been detected");
            }
        }

        for (final DataFall<IN, OUT> fall : falls) {

            connect(fall, outFall);
        }
    }

    /**
     * Causes the data drops flowing through this waterfall streams to be merged into a single
     * collection.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, List<OUT>> flat() {

        return chain(new CacheGate<OUT, List<OUT>>(size()) {

            @Override
            protected void onClear(final List<OUT> cache, final int streamNumber,
                    final River<OUT> upRiver, final River<List<OUT>> downRiver) {

                downRiver.push(new ArrayList<OUT>(cache));

                super.onClear(cache, streamNumber, upRiver, downRiver);
            }
        });
    }

    /**
     * Makes the waterfall streams flow through the currents returned by the specified generator.
     *
     * @param generator the current generator
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the generator is null or returns a null current.
     */
    public Waterfall<SOURCE, IN, OUT> in(final CurrentGenerator generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall current generator cannot be null");
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, mBridgeMap, mBridgeClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mPump, mSize,
                                              null, generator, mFalls);
    }

    /**
     * Splits the waterfall in the specified number of streams.
     *
     * @param fallCount the total fall count generating the waterfall.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the fall count is negative or 0.
     */
    public Waterfall<SOURCE, IN, OUT> in(final int fallCount) {

        if (fallCount <= 0) {

            throw new IllegalArgumentException("the fall count cannot be negative or zero");
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, mBridgeMap, mBridgeClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                              fallCount, mCurrent, mCurrentGenerator, mFalls);
    }

    /**
     * Makes the waterfall streams flow through the specified current.
     *
     * @param current the current.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the current is null.
     */
    public Waterfall<SOURCE, IN, OUT> in(final Current current) {

        if (current == null) {

            throw new IllegalArgumentException("the waterfall current cannot be null");
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, mBridgeMap, mBridgeClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mPump, mSize,
                                              current, null, mFalls);
    }

    /**
     * Makes the waterfall streams flow through a background current.
     * <p/>
     * The optimum thread pool size will be automatically computed based on the available resources
     * and the waterfall size.
     * <p/>
     * Note also that the same background current will be retained through the waterfall.
     *
     * @param fallCount the total fall count generating the waterfall.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the fall count is negative or 0.
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

        return new Waterfall<SOURCE, IN, OUT>(mSource, mBridgeMap, mBridgeClassification, poolSize,
                                              backgroundCurrent, mPump, fallCount,
                                              backgroundCurrent, null, mFalls);
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
     * @return the newly created waterfall.
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

        return new Waterfall<SOURCE, IN, OUT>(mSource, mBridgeMap, mBridgeClassification, poolSize,
                                              backgroundCurrent, mPump, poolSize, backgroundCurrent,
                                              null, mFalls);
    }

    /**
     * Causes the data drops flowing through this waterfall streams to be interleaved, so that the
     * first drop coming from the stream number 0 will come before the first one coming from the
     * stream number 1, and so on.<br/>
     * The resulting waterfall will have size equal to 1.
     * <p/>
     * Note that the returned waterfall internally cache the data pushed through it and then
     * discharge them on the first flush.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> interleave() {

        return chain(new CumulativeCacheGate<OUT, OUT>(size()) {

            @Override
            protected void onClear(final List<List<OUT>> cache, final River<OUT> upRiver,
                    final River<OUT> downRiver) {

                boolean empty = false;

                while (!empty) {

                    empty = true;

                    for (final List<OUT> data : cache) {

                        if (!data.isEmpty()) {

                            empty = false;

                            downRiver.push(data.remove(0));
                        }
                    }
                }

                super.onClear(cache, upRiver, downRiver);
            }
        }).merge();
    }

    /**
     * Makes this waterfall join the specified one, so that all the data coming from this
     * waterfall N streams will flow through the first N streams of the resulting waterfall, and
     * the ones coming from the specified waterfall streams will flow through the streams N + 1, N
     * + 2, ..., etc.
     * <p/>
     * TODO how to handle the bridges?
     * <p/>
     * Note that the bridges, the size and the currents of this waterfall will be retained.
     *
     * @param waterfall the waterfall to join.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the waterfall is null.
     */
    public Waterfall<OUT, OUT, OUT> join(final Waterfall<?, ?, OUT> waterfall) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the waterfall cannot be null");
        }

        return join(Collections.singleton(waterfall));
    }

    /**
     * Makes this waterfall join the specified ones, so that all the data coming from this
     * waterfall N streams will flow through the first N streams of the resulting waterfall, the
     * ones coming from the streams of the first waterfall in the specified collection will flow
     * through the streams N + 1, N + 2, ..., etc., and so on.
     * <p/>
     * TODO how to handle the bridges?
     * <p/>
     * Note that the bridges, the size and the currents of this waterfall will be retained.
     *
     * @param waterfalls the collection of the waterfall to join.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the waterfall collection is null or contains null
     *                                  waterfalls.
     */
    public Waterfall<OUT, OUT, OUT> join(
            final Collection<? extends Waterfall<?, ?, OUT>> waterfalls) {

        if (waterfalls == null) {

            throw new IllegalArgumentException("the waterfall collection cannot be null");
        }

        final ArrayList<Waterfall<?, ?, OUT>> inWaterfalls =
                new ArrayList<Waterfall<?, ?, OUT>>(waterfalls.size() + 1);

        int totSize = 0;

        if (mFalls == NO_FALL) {

            totSize += 1;

            inWaterfalls.add(start());

        } else {

            totSize += size();

            inWaterfalls.add(this);
        }

        for (final Waterfall<?, ?, OUT> waterfall : waterfalls) {

            if (waterfall.mFalls == NO_FALL) {

                totSize += 1;

                inWaterfalls.add(waterfall.start());

            } else {

                totSize += waterfall.size();

                inWaterfalls.add(waterfall);
            }
        }

        final OpenGate<OUT> gate = openGate();

        //noinspection unchecked
        final Gate<OUT, OUT>[] gates = new Gate[totSize];

        Arrays.fill(gates, gate);

        final Waterfall<OUT, OUT, OUT> waterfall =
                new Waterfall<OUT, OUT, OUT>(null, mBridgeMap, mBridgeClassification,
                                             mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                             totSize, mCurrent, mCurrentGenerator, gates);

        int number = 0;

        final DataFall<OUT, OUT>[] outFalls = waterfall.mFalls;

        for (final Waterfall<?, ?, OUT> inWaterfall : inWaterfalls) {

            for (final DataFall<?, OUT> inFall : inWaterfall.mFalls) {

                connect(inFall, outFalls[number++]);
            }
        }

        return waterfall;
    }

    /**
     * Causes the data drops flowing through this waterfall streams to be merged into a single
     * stream.<br/>
     * The resulting waterfall will have size equal to 1.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> merge() {

        if (mFalls.length > 1) {

            return in(1).chain();
        }

        return chain();
    }

    /**
     * Creates and returns a new data collector after flushing this waterfall source.
     *
     * @return the collector.
     * @throws IllegalStateException if this waterfall was not started.
     */
    public Collector<OUT> pull() {

        final Collector<OUT> collector = collect();

        source().flush();

        return collector;
    }

    /**
     * Creates and returns a new data collector after pushing the specified data into this
     * waterfall source and then flushing it.
     *
     * @param source the source data.
     * @return the collector.
     * @throws IllegalStateException if this waterfall was not started.
     */
    public Collector<OUT> pull(final SOURCE source) {

        final Collector<OUT> collector = collect();

        source().push(source).flush();

        return collector;
    }

    /**
     * Creates and returns a new data collector after pushing the specified data into this
     * waterfall source and then flushing it.
     *
     * @param sources the source data.
     * @return the collector.
     * @throws IllegalStateException if this waterfall was not started.
     */
    public Collector<OUT> pull(final SOURCE... sources) {

        final Collector<OUT> collector = collect();

        source().push(sources).flush();

        return collector;
    }

    /**
     * Creates and returns a new data collector after pushing the data returned by the specified
     * iterable into this waterfall source and then flushing it.
     *
     * @param sources the source data iterable.
     * @return the collector.
     * @throws IllegalStateException if this waterfall was not started.
     */
    public Collector<OUT> pull(final Iterable<SOURCE> sources) {

        final Collector<OUT> collector = collect();

        source().push(sources).flush();

        return collector;
    }

    /**
     * Filters the data flowing through this waterfall by retaining only the specified count of
     * drops, starting from the specified offset and discarding the other ones.
     * <p/>
     * Note that the returned waterfall internally cache the data pushed through it and then
     * discharge them on the first flush.
     *
     * @param offset the offset from the first data drop.
     * @param count  the count of the element in the range.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the offset is negative, the count is negative or equal
     *                                  to zero.
     */
    public Waterfall<SOURCE, OUT, OUT> range(final int offset, final int count) {

        if ((offset < 0) || (count <= 0)) {

            throw new IllegalArgumentException("invalid range indexes");
        }

        final long last = (long) offset + (long) count;

        final int length = Math.max(size(), 1);
        final long[] counts = new long[length];

        //noinspection unchecked
        final ArrayList<OUT>[] lists = new ArrayList[length];

        for (int i = 0; i < lists.length; i++) {

            lists[i] = new ArrayList<OUT>();
        }

        return chain(new OpenGate<OUT>() {

            @Override
            public void onPush(final River<OUT> upRiver, final River<OUT> downRiver,
                    final int fallNumber, final OUT drop) {

                final long index = counts[fallNumber];

                if (index < last) {

                    counts[fallNumber] = index + 1;

                    if (index >= offset) {

                        lists[fallNumber].add(drop);
                    }
                }
            }

            @Override
            public void onFlush(final River<OUT> upRiver, final River<OUT> downRiver,
                    final int fallNumber) {

                final ArrayList<OUT> list = lists[fallNumber];

                downRiver.push(list);
                list.clear();

                super.onFlush(upRiver, downRiver, fallNumber);
            }
        });
    }

    /**
     * Causes the data drops flowing through this waterfall streams to flow down in the reverse
     * order.
     * <p/>
     * Note that the returned waterfall internally cache the data pushed through it and then
     * discharge them on the first flush.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> revert() {

        return chain(new CacheGate<OUT, OUT>(size()) {

            @Override
            protected void onClear(final List<OUT> cache, final int streamNumber,
                    final River<OUT> upRiver, final River<OUT> downRiver) {

                Collections.reverse(cache);
                downRiver.push(cache);

                super.onClear(cache, streamNumber, upRiver, downRiver);
            }
        });
    }

    /**
     * Causes the data drops flowing through this waterfall streams to flow down sorted in the
     * natural order.
     * <p/>
     * Note that the returned waterfall internally cache the data pushed through it and then
     * discharge them on the first flush.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> sort() {

        //noinspection unchecked
        return sort((Comparator<? super OUT>) NATURAL_COMPARATOR);
    }

    /**
     * Causes the data drops flowing through this waterfall streams to flow down sorted in the
     * order decided by the specified comparator.
     * <p/>
     * Note that the returned waterfall internally cache the data pushed through it and then
     * discharge them on the first flush.
     *
     * @param comparator the comparator.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the comparator is null.
     */
    public Waterfall<SOURCE, OUT, OUT> sort(final Comparator<? super OUT> comparator) {

        if (comparator == null) {

            throw new IllegalArgumentException("the data comparator cannot be null");
        }

        return chain(new CacheGate<OUT, OUT>(size()) {

            @Override
            protected void onClear(final List<OUT> cache, final int streamNumber,
                    final River<OUT> upRiver, final River<OUT> downRiver) {

                Collections.sort(cache, comparator);
                downRiver.push(cache);

                super.onClear(cache, streamNumber, upRiver, downRiver);
            }
        });
    }

    /**
     * Gets the waterfall source.
     *
     * @return the source.
     */
    public Waterfall<SOURCE, SOURCE, ?> source() {

        return mSource;
    }

    /**
     * Creates and returns a new waterfall fed by to the springs returned by the specified
     * generator.
     * <p/>
     * Note that the bridges, the size and the currents of this waterfall will be retained.
     *
     * @param generator the spring generator.
     * @param <DATA>    the spring data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the generator is null or returns a null spring.
     */
    public <DATA> Waterfall<Void, Void, DATA> spring(final SpringGenerator<DATA> generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall spring generator cannot be null");
        }

        return start(new GateGenerator<Void, DATA>() {

            @Override
            public Gate<Void, DATA> create(final int fallNumber) {

                return new SpringGate<DATA>(generator.create(fallNumber));
            }
        });
    }

    /**
     * Creates and returns a new waterfall fed by the specified spring.
     * <p/>
     * Note that the bridges and the currents of this waterfall will be retained, while the size will
     * be equal to 1.
     *
     * @param spring the spring instance.
     * @param <DATA> the spring data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the spring is null.
     */
    public <DATA> Waterfall<Void, Void, DATA> spring(final Spring<DATA> spring) {

        return spring(Collections.singleton(spring));
    }

    /**
     * Creates and returns a new waterfall fed by the specified springs.
     * <p/>
     * Note that the bridges and the currents of this waterfall will be retained, while the size will
     * be equal to the one of the specified collection.
     *
     * @param springs the spring instances.
     * @param <DATA>  the spring data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the spring collection is null or contains a null spring.
     */
    public <DATA> Waterfall<Void, Void, DATA> spring(
            final Collection<? extends Spring<DATA>> springs) {

        if (springs == null) {

            throw new IllegalArgumentException("the waterfall spring collection cannot be null");
        }

        final ArrayList<SpringGate<DATA>> gates = new ArrayList<SpringGate<DATA>>(springs.size());

        for (final Spring<DATA> spring : springs) {

            gates.add(new SpringGate<DATA>(spring));
        }

        return start(gates);
    }

    /**
     * Creates and returns a new waterfall fed by both this waterfall as a spring and the specified
     * one.
     * <p/>
     * Note that the bridges and the currents of this waterfall will be retained, while the size will
     * be equal to the one of the specified collection.
     *
     * @param spring the spring instance.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the spring is null.
     */
    public Waterfall<Void, Void, OUT> springWith(final Spring<OUT> spring) {

        if (spring == null) {

            throw new IllegalArgumentException("the spring cannot be null");
        }

        return springWith(Collections.singleton(spring));
    }

    /**
     * Creates and returns a new waterfall fed by this waterfall as a spring and all the specified
     * ones.
     * <p/>
     * Note that the bridges and the currents of this waterfall will be retained, while the size will
     * be equal to the one of the specified collection.
     *
     * @param springs the spring collection.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the spring collection is null or contains a null spring.
     */
    public Waterfall<Void, Void, OUT> springWith(final Collection<? extends Spring<OUT>> springs) {

        if (springs == null) {

            throw new IllegalArgumentException("the spring collection cannot be null");
        }

        final ArrayList<Spring<OUT>> list = new ArrayList<Spring<OUT>>(springs.size() + 1);

        list.add(Springs.from(this));
        list.addAll(springs);

        return spring(list);
    }

    /**
     * Creates and returns a new waterfall generating from this one.
     * <p/>
     * Note that the bridges, the size and the currents of this waterfall will be retained.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<OUT, OUT, OUT> start() {

        final int size = mSize;

        //noinspection unchecked
        final Gate<OUT, OUT>[] gates = new Gate[size];
        Arrays.fill(gates, openGate());

        final Map<Classification<?>, BridgeGate<?, ?>> bridgeMap = Collections.emptyMap();

        return new Waterfall<OUT, OUT, OUT>(null, bridgeMap, mBridgeClassification,
                                            mBackgroundPoolSize, mBackgroundCurrent, null, size,
                                            mCurrent, mCurrentGenerator, gates);
    }

    /**
     * Creates and returns a new waterfall generating from this one.
     * <p/>
     * Note that the bridges, the size and the currents of this waterfall will be retained.
     *
     * @param dataType the data type.
     * @param <DATA>   the data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the data type is null.
     */
    public <DATA> Waterfall<DATA, DATA, DATA> start(final Class<DATA> dataType) {

        return start(Classification.ofType(dataType));
    }

    /**
     * Creates and returns a new waterfall generating from this one.
     * <p/>
     * Note that the bridges, the size and the currents of this waterfall will be retained.
     *
     * @param classification the data classification.
     * @param <DATA>         the data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the data classification is null.
     */
    public <DATA> Waterfall<DATA, DATA, DATA> start(final Classification<DATA> classification) {

        if (classification == null) {

            throw new IllegalArgumentException("the waterfall classification cannot be null");
        }

        //noinspection unchecked
        return (Waterfall<DATA, DATA, DATA>) start();
    }

    /**
     * Creates and returns a new waterfall chained to the gates returned by the specified
     * generator.
     * <p/>
     * Note that the bridges, the size and the currents of this waterfall will be retained.
     *
     * @param generator the gate generator.
     * @param <NIN>     the new input data type.
     * @param <NOUT>    the new output data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the generator is null or returns a null gate.
     */
    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(final GateGenerator<NIN, NOUT> generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall gate generator cannot be null");
        }

        final Map<Classification<?>, BridgeGate<?, ?>> bridgeMap = Collections.emptyMap();

        final int size = mSize;

        //noinspection unchecked
        final Gate<NIN, NOUT>[] gates = new Gate[size];

        if (size <= 1) {

            final Gate<NIN, NOUT> gate = generator.create(0);

            registerGate(gate);

            gates[0] = gate;

            return new Waterfall<NIN, NIN, NOUT>(null, bridgeMap, mBridgeClassification,
                                                 mBackgroundPoolSize, mBackgroundCurrent, null, 1,
                                                 mCurrent, mCurrentGenerator, gates);
        }

        if (mBridgeClassification != null) {

            throw new IllegalStateException("cannot make a bridge from more than one gate");
        }

        for (int i = 0; i < size; ++i) {

            final Gate<NIN, NOUT> gate = generator.create(i);

            registerGate(gate);

            gates[i] = gate;
        }

        return new Waterfall<NIN, NIN, NOUT>(null, bridgeMap, null, mBackgroundPoolSize,
                                             mBackgroundCurrent, null, size, mCurrent,
                                             mCurrentGenerator, gates);
    }

    /**
     * Creates and returns a new waterfall chained to the specified gates.
     * <p/>
     * Note that the bridges and the currents of this waterfall will be retained, while the size will
     * be equal to the one of the specified array.
     *
     * @param gates  the gate instances.
     * @param <NIN>  the new input data type.
     * @param <NOUT> the new output data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the gate collection is null or contains a null gate.
     */
    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(
            final Collection<? extends Gate<NIN, NOUT>> gates) {

        if (gates == null) {

            throw new IllegalArgumentException("the waterfall gate collection cannot be null");
        }

        final int length = gates.size();

        if (length == 0) {

            throw new IllegalArgumentException("the waterfall gate collection cannot be empty");
        }

        final Waterfall<SOURCE, IN, OUT> waterfall;

        if (mSize == length) {

            waterfall = this;

        } else {

            waterfall = in(length);
        }

        final Iterator<? extends Gate<NIN, NOUT>> iterator = gates.iterator();

        return waterfall.start(new GateGenerator<NIN, NOUT>() {

            @Override
            public Gate<NIN, NOUT> create(final int fallNumber) {

                return iterator.next();
            }
        });
    }

    /**
     * Creates and returns a new waterfall chained to the specified gate.
     * <p/>
     * Note that the bridges, the size and the currents of this waterfall will be retained.
     *
     * @param gate   the gate instance.
     * @param <NIN>  the new input data type.
     * @param <NOUT> the new output data type.
     * @return the newly created waterfall.
     * @throws IllegalArgumentException if the gate is null.
     */
    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(final Gate<NIN, NOUT> gate) {

        registerGate(gate);

        final Map<Classification<?>, BridgeGate<?, ?>> bridgeMap = Collections.emptyMap();

        //noinspection unchecked
        final Gate<NIN, NOUT>[] gates = new Gate[]{gate};

        return new Waterfall<NIN, NIN, NOUT>(null, bridgeMap, mBridgeClassification,
                                             mBackgroundPoolSize, mBackgroundCurrent, null, mSize,
                                             mCurrent, mCurrentGenerator, gates);
    }

    /**
     * Makes this waterfall forward an exception in case the specified timeout elapses before at
     * least one data drop is pushed through it.
     * <p/>
     * TODO: starts immediately
     *
     * @param timeout   the timeout in <code>timeUnit</code> time units.
     * @param timeUnit  the delay time unit.
     * @param exception the exception to forward.
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> throwOnTimeout(final long timeout, final TimeUnit timeUnit,
            final RuntimeException exception) {

        return chain(new TimeoutGate<OUT>(this, timeout, timeUnit, exception));
    }

    private Waterfall<SOURCE, OUT, OUT> chainPump(final PumpGate<OUT> pumpGate) {

        final Waterfall<SOURCE, OUT, OUT> waterfall = chain(pumpGate);

        return new Waterfall<SOURCE, OUT, OUT>(waterfall.mSource, waterfall.mBridgeMap,
                                               waterfall.mBridgeClassification, mBackgroundPoolSize,
                                               mBackgroundCurrent, pumpGate, waterfall.mSize,
                                               waterfall.mCurrent, waterfall.mCurrentGenerator,
                                               waterfall.mFalls);
    }

    private BridgeGate<?, ?> findBestMatch(final Classification<?> bridgeClassification) {

        final Map<Classification<?>, BridgeGate<?, ?>> bridgeMap = mBridgeMap;

        BridgeGate<?, ?> gate = bridgeMap.get(bridgeClassification);

        if (gate == null) {

            Classification<?> bestMatch = null;

            for (final Entry<Classification<?>, BridgeGate<?, ?>> entry : bridgeMap.entrySet()) {

                final Classification<?> type = entry.getKey();

                if (bridgeClassification.isAssignableFrom(type)) {

                    if ((bestMatch == null) || type.isAssignableFrom(bestMatch)) {

                        gate = entry.getValue();

                        bestMatch = type;
                    }
                }
            }
        }

        return gate;
    }

    private int getBestPoolSize() {

        final int processors = Runtime.getRuntime().availableProcessors();

        if (processors < 4) {

            return Math.max(1, processors - 1);
        }

        return (processors / 2);
    }

    private void mapBridge(final HashMap<Classification<?>, BridgeGate<?, ?>> bridgeMap,
            final Classification<?> bridgeClassification, final BridgeGate<?, ?> gate) {

        if (!bridgeClassification.getRawType().isInstance(gate.gate)) {

            throw new IllegalArgumentException(
                    "the gate does not implement the bridge classification type");
        }

        bridgeMap.put(bridgeClassification, gate);
    }
}