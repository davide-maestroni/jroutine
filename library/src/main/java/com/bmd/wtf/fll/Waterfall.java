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
import com.bmd.wtf.flw.Dam;
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
 * @param <SOURCE> the waterfall source data type.
 * @param <IN>     the input data type.
 * @param <OUT>    the output data type.
 */
public class Waterfall<SOURCE, IN, OUT> extends AbstractRiver<IN> {

    // TODO: springs, bridge, linq(?)

    private static final DataFall[] NO_FALL = new DataFall[0];

    private static final Classification<Void> SELF_CLASSIFICATION = new Classification<Void>() {};

    private static final WeakHashMap<Gate<?, ?>, Void> sGates = new WeakHashMap<Gate<?, ?>, Void>();

    private static OpenGate<?> sOpenGate;

    private final Current mBackgroundCurrent;

    private final int mBackgroundPoolSize;

    private final Current mCurrent;

    private final CurrentGenerator mCurrentGenerator;

    private final Classification<?> mDamClassification;

    private final Map<Classification<?>, DamGate<?, ?>> mDamMap;

    private final DataFall<IN, OUT>[] mFalls;

    private final PumpGate<?> mPump;

    private final int mSize;

    private final Waterfall<SOURCE, SOURCE, ?> mSource;

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final Map<Classification<?>, DamGate<?, ?>> damMap,
            final Classification<?> damClassification, final int backgroundPoolSize,
            final Current backgroundCurrent, final PumpGate<?> pumpGate, final int size,
            final Current current, final CurrentGenerator generator,
            final DataFall<IN, OUT>[] falls) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mDamMap = damMap;
        mDamClassification = damClassification;
        mBackgroundPoolSize = backgroundPoolSize;
        mBackgroundCurrent = backgroundCurrent;
        mPump = pumpGate;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;
        mFalls = falls;
    }

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final Map<Classification<?>, DamGate<?, ?>> damMap,
            final Classification<?> damClassification, final int backgroundPoolSize,
            final Current backgroundCurrent, final PumpGate<?> pumpGate, final int size,
            final Current current, final CurrentGenerator generator, final Gate<IN, OUT>[] gates) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mDamClassification = null;
        mBackgroundPoolSize = backgroundPoolSize;
        mBackgroundCurrent = backgroundCurrent;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;

        final int length = gates.length;

        final Gate<IN, OUT> wrappedGate;

        if (damClassification != null) {

            final Gate<IN, OUT> gate = gates[0];

            final HashMap<Classification<?>, DamGate<?, ?>> fallDamMap =
                    new HashMap<Classification<?>, DamGate<?, ?>>(damMap);
            final DamGate<IN, OUT> damGate = new DamGate<IN, OUT>(gate);

            mapDam(fallDamMap, (SELF_CLASSIFICATION == damClassification) ? Classification.ofType(
                    gate.getClass()) : damClassification, damGate);

            mDamMap = fallDamMap;

            wrappedGate = damGate;

        } else {

            if (size != length) {

                wrappedGate = new BarrageGate<IN, OUT>(gates[0]);

            } else {

                wrappedGate = null;
            }

            mDamMap = damMap;
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

        final Map<Classification<?>, DamGate<?, ?>> damMap = Collections.emptyMap();

        //noinspection unchecked
        return new Waterfall<Object, Object, Object>(null, damMap, null, 0, null, null, 1,
                                                     Currents.straight(), null, NO_FALL);
    }

    /**
     * Connects an input and an output fall through a data stream.
     *
     * @param inFall  the input fall.
     * @param outFall the output fall.
     * @param <DATA>  the data type.
     * @return the data stream running between the two falls.
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
     * Chains the stream identified by the specified number to this waterfall. After the call, all
     * the data flowing through this waterfall will be pushed into the target stream.
     *
     * @param streamNumber the number identifying the target stream.
     * @param waterfall    the target waterfall.
     */
    public void chain(final int streamNumber, final Waterfall<?, OUT, ?> waterfall) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the waterfall cannot be null");
        }

        if (this == waterfall) {

            throw new IllegalArgumentException("cannot chain a waterfall to itself");
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        if ((falls == NO_FALL) || (waterfall.mFalls == NO_FALL)) {

            throw new IllegalStateException("cannot chain a not started waterfall to another one");
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
     * Chains the specified waterfall to this one. After the call, all the data flowing through
     * this waterfall will be pushed into the target one.
     *
     * @param waterfall the waterfall to chain.
     */
    public void chain(final Waterfall<?, OUT, ?> waterfall) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the waterfall cannot be null");
        }

        if (this == waterfall) {

            throw new IllegalArgumentException("cannot chain a waterfall to itself");
        }

        final DataFall<IN, OUT>[] falls = mFalls;

        if ((falls == NO_FALL) || (waterfall.mFalls == NO_FALL)) {

            throw new IllegalStateException("cannot chain a not started waterfall to another one");
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
     * Chains the gate protected by the dam of the specified classification type to this
     * waterfall.
     * <p/>
     * Note that contrary to common gate, the ones protected by a dam can be added several times
     * to the same waterfall.
     *
     * @param damClassification the dam classification.
     * @param <NOUT>            the new output data type.
     * @return the newly created waterfall.
     */
    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(
            final Classification<? extends Gate<OUT, NOUT>> damClassification) {

        //noinspection unchecked
        final Gate<OUT, NOUT> gate = (Gate<OUT, NOUT>) findBestMatch(damClassification);

        if (gate == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any dam of classification type "
                            + damClassification);
        }

        final DataFall<IN, OUT>[] falls = mFalls;
        final int size = mSize;

        //noinspection unchecked
        final Gate<OUT, NOUT>[] gates = new Gate[size];

        if (size == 1) {

            gates[0] = gate;

            final Waterfall<SOURCE, OUT, NOUT> waterfall =
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mDamMap, mDamClassification,
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
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mDamMap,
                                                 inWaterfall.mDamClassification,
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
                    new Waterfall<SOURCE, OUT, OUT>(mSource, mDamMap, mDamClassification,
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
                new Waterfall<SOURCE, OUT, OUT>(inWaterfall.mSource, inWaterfall.mDamMap,
                                                inWaterfall.mDamClassification, mBackgroundPoolSize,
                                                mBackgroundCurrent, mPump, size,
                                                inWaterfall.mCurrent, inWaterfall.mCurrentGenerator,
                                                gates);

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
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mDamMap, mDamClassification,
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
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mDamMap,
                                                 inWaterfall.mDamClassification,
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
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mDamMap, mDamClassification,
                                                     mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                                     1, mCurrent, mCurrentGenerator, gates);

            final DataFall<OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<IN, OUT> fall : falls) {

                connect(fall, outFall);
            }

            return waterfall;
        }

        if (mDamClassification != null) {

            throw new IllegalStateException("cannot make a dam from more than one gate");
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
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mDamMap,
                                                 inWaterfall.mDamClassification,
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
     * Tells the waterfall to close the dam handling the specified gate, that is, the dam
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

        final HashMap<Classification<?>, DamGate<?, ?>> damMap =
                new HashMap<Classification<?>, DamGate<?, ?>>(mDamMap);

        final Iterator<DamGate<?, ?>> iterator = damMap.values().iterator();

        while (iterator.hasNext()) {

            if (gate == iterator.next().gate) {

                iterator.remove();

                isChanged = true;
            }
        }

        if (!isChanged) {

            return this;
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, damMap, mDamClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mPump, mSize,
                                              mCurrent, mCurrentGenerator, mFalls);
    }

    /**
     * Tells the waterfall to close the dam of the specified classification type, that is, the dam
     * will not be accessible anymore to the ones requiring it.
     *
     * @param damClassification the dam classification.
     * @param <TYPE>            the gate type.
     * @return the newly created waterfall.
     */
    public <TYPE> Waterfall<SOURCE, IN, OUT> close(final Classification<TYPE> damClassification) {

        final DamGate<?, ?> dam = findBestMatch(damClassification);

        if (dam == null) {

            return this;
        }

        final HashMap<Classification<?>, DamGate<?, ?>> damMap =
                new HashMap<Classification<?>, DamGate<?, ?>>(mDamMap);

        final Iterator<DamGate<?, ?>> iterator = damMap.values().iterator();

        while (iterator.hasNext()) {

            if (dam == iterator.next()) {

                iterator.remove();
            }
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, damMap, mDamClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mPump, mSize,
                                              mCurrent, mCurrentGenerator, mFalls);
    }

    /**
     * Creates and returns a new data collector.
     *
     * @return the collector.
     */
    public Collector<OUT> collect() {

        if (mFalls == NO_FALL) {

            throw new IllegalStateException("cannot collect data from a not started waterfall");
        }

        final CollectorGate<OUT> collectorGate = new CollectorGate<OUT>();
        final DamGate<OUT, OUT> damGate = new DamGate<OUT, OUT>(collectorGate);

        final Waterfall<SOURCE, IN, OUT> waterfall;

        if (mSize != 1) {

            waterfall = in(1);

        } else {

            waterfall = this;
        }

        waterfall.chain(damGate);

        return new DataCollector<OUT>(damGate, collectorGate);
    }

    /**
     * Causes the data drops flowing through this waterfall streams to be concatenated, so that all
     * the data coming from the stream number 0 will come before the ones coming from the stream
     * number 1, and so on.
     * <p/>
     * Note that the resulting waterfall will have size equal to 1.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> concat() {

        final int length = Math.max(size(), 1);

        //noinspection unchecked
        final ArrayList<OUT>[] lists = new ArrayList[length];

        for (int i = 0; i < lists.length; i++) {

            lists[i] = new ArrayList<OUT>();
        }

        return chain(new OpenGate<OUT>() {

            private int mFlushes;

            @Override
            public void onPush(final River<OUT> upRiver, final River<OUT> downRiver,
                    final int fallNumber, final OUT drop) {

                lists[fallNumber].add(drop);
            }

            @Override
            public void onFlush(final River<OUT> upRiver, final River<OUT> downRiver,
                    final int fallNumber) {

                if (++mFlushes >= length) {

                    mFlushes = 0;

                    for (final ArrayList<OUT> list : lists) {

                        downRiver.push(list);

                        list.clear();
                    }
                }

                super.onFlush(upRiver, downRiver, fallNumber);
            }
        }).merge();
    }

    /**
     * Tells the waterfall to build a dam around the next gate chained to it.
     * <p/>
     * The dam type will be the same as the gate raw type.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> dam() {

        return dam(SELF_CLASSIFICATION);
    }

    /**
     * Tells the waterfall to build a dam of the specified type around the next gate chained to it.
     *
     * @param damClass the dam class.
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> dam(final Class<?> damClass) {

        return dam(Classification.ofType(damClass));
    }

    /**
     * Tells the waterfall to build a dam of the specified classification type around the next
     * gate chained to it.
     *
     * @param damClassification the dam classification.
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> dam(final Classification<?> damClassification) {

        if (damClassification == null) {

            throw new IllegalArgumentException("the dam classification cannot be null");
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, mDamMap, damClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mPump, mSize,
                                              mCurrent, mCurrentGenerator, mFalls);
    }

    /**
     * Causes the data drops flowing through this waterfall to be delayed of the specified time.
     *
     * @param delay    the delay in <code>timeUnit</code> time units.
     * @param timeUnit the delay time unit.
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> delay(final long delay, final TimeUnit timeUnit) {

        return merge().chain(new OpenGate<OUT>() {

            private final ArrayList<OUT> mData = new ArrayList<OUT>();

            @Override
            public void onPush(final River<OUT> upRiver, final River<OUT> downRiver,
                    final int fallNumber, final OUT drop) {

                mData.add(drop);
            }

            @Override
            public void onFlush(final River<OUT> upRiver, final River<OUT> downRiver,
                    final int fallNumber) {

                downRiver.pushAfter(delay, timeUnit, mData);

                mData.clear();

                super.onFlush(upRiver, downRiver, fallNumber);
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
    public Waterfall<SOURCE, IN, OUT> flush() {

        for (final DataFall<IN, OUT> fall : mFalls) {

            fall.inputCurrent.flush(fall, null);
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
    public Waterfall<SOURCE, IN, OUT> flushStream(final int streamNumber) {

        final DataFall<IN, OUT> fall = mFalls[streamNumber];

        fall.inputCurrent.flush(fall, null);

        return this;
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
    public <TYPE> Dam<TYPE> on(final Class<TYPE> damClass) {

        return on(Classification.ofType(damClass));
    }

    @Override
    public <TYPE> Dam<TYPE> on(final TYPE gate) {

        if (gate == null) {

            throw new IllegalArgumentException("the dam gate cannot be null");
        }

        DamGate<?, ?> dam = null;

        final Map<Classification<?>, DamGate<?, ?>> damMap = mDamMap;

        for (final DamGate<?, ?> damGate : damMap.values()) {

            if (damGate.gate == gate) {

                dam = damGate;

                break;
            }
        }

        if (dam == null) {

            throw new IllegalArgumentException("the waterfall does not retain the dam " + gate);
        }

        return new DataDam<TYPE>(dam, new Classification<TYPE>() {});
    }

    @Override
    public <TYPE> Dam<TYPE> on(final Classification<TYPE> damClassification) {

        final DamGate<?, ?> dam = findBestMatch(damClassification);

        if (dam == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any dam of classification type "
                            + damClassification);
        }

        return new DataDam<TYPE>(dam, damClassification);
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
     * Makes the waterfall streams flow through the currents returned by the specified generator.
     *
     * @param generator the current generator
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> in(final CurrentGenerator generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall current generator cannot be null");
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, mDamMap, mDamClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mPump, mSize,
                                              null, generator, mFalls);
    }

    /**
     * Splits the waterfall in the specified number of streams.
     *
     * @param fallCount the total fall count generating the waterfall.
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> in(final int fallCount) {

        if (fallCount <= 0) {

            throw new IllegalArgumentException("the fall count cannot be negative or zero");
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, mDamMap, mDamClassification,
                                              mBackgroundPoolSize, mBackgroundCurrent, mPump,
                                              fallCount, mCurrent, mCurrentGenerator, mFalls);
    }

    /**
     * Makes the waterfall streams flow through the specified current.
     *
     * @param current the current.
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, IN, OUT> in(final Current current) {

        if (current == null) {

            throw new IllegalArgumentException("the waterfall current cannot be null");
        }

        return new Waterfall<SOURCE, IN, OUT>(mSource, mDamMap, mDamClassification,
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

        return new Waterfall<SOURCE, IN, OUT>(mSource, mDamMap, mDamClassification, poolSize,
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

        return new Waterfall<SOURCE, IN, OUT>(mSource, mDamMap, mDamClassification, poolSize,
                                              backgroundCurrent, mPump, poolSize, backgroundCurrent,
                                              null, mFalls);
    }

    /**
     * Causes the data drops flowing through this waterfall streams to be interleaved, so that the
     * first drop coming from the stream number 0 will come before the first one coming from the
     * stream number 1, and so on.
     * <p/>
     * Note that the resulting waterfall will have size equal to 1.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<SOURCE, OUT, OUT> interleave() {

        final int length = Math.max(size(), 1);

        //noinspection unchecked
        final ArrayList<OUT>[] lists = new ArrayList[length];

        for (int i = 0; i < lists.length; i++) {

            lists[i] = new ArrayList<OUT>();
        }

        return chain(new OpenGate<OUT>() {

            private int mFlushes;

            @Override
            public void onPush(final River<OUT> upRiver, final River<OUT> downRiver,
                    final int fallNumber, final OUT drop) {

                lists[fallNumber].add(drop);
            }

            @Override
            public void onFlush(final River<OUT> upRiver, final River<OUT> downRiver,
                    final int fallNumber) {

                if (++mFlushes >= length) {

                    mFlushes = 0;

                    boolean empty = false;

                    while (!empty) {

                        empty = true;

                        for (final ArrayList<OUT> list : lists) {

                            if (!list.isEmpty()) {

                                empty = false;

                                downRiver.push(list.remove(0));
                            }
                        }
                    }
                }

                super.onFlush(upRiver, downRiver, fallNumber);
            }
        }).merge();
    }

    /**
     * Makes this waterfall to join the specified one, so that all the data coming from this
     * waterfall streams will flow through the stream number 0 of the resulting waterfall, and the
     * ones coming from the specified waterfall streams will flow through the stream number 1.
     *
     * @param waterfall the waterfall to join.
     * @return the newly created waterfall.
     */
    public Waterfall<OUT, OUT, OUT> join(final Waterfall<?, ?, OUT> waterfall) {

        if (waterfall == null) {

            throw new IllegalArgumentException("the waterfall cannot be null");
        }

        return join(Collections.singleton(waterfall));
    }

    /**
     * Makes this waterfall to join the specified ones, so that all the data coming from this
     * waterfall streams will flow through the stream number 0 of the resulting waterfall, the
     * ones coming from the streams of the first waterfall in the specified collection will flow
     * through the stream number 1, and so on.
     *
     * @param waterfalls the collection of the waterfall to join.
     * @return the newly created waterfall.
     */
    public Waterfall<OUT, OUT, OUT> join(
            final Collection<? extends Waterfall<?, ?, OUT>> waterfalls) {

        if (waterfalls == null) {

            throw new IllegalArgumentException("the waterfall collection cannot be null");
        }

        final int size = waterfalls.size() + 1;

        if (size == 1) {

            return start();
        }

        final OpenGate<OUT> gate = openGate();

        //noinspection unchecked
        final Gate<OUT, OUT>[] gates = new Gate[size];

        Arrays.fill(gates, gate);

        final Waterfall<OUT, OUT, OUT> waterfall =
                new Waterfall<OUT, OUT, OUT>(null, mDamMap, mDamClassification, mBackgroundPoolSize,
                                             mBackgroundCurrent, mPump, size, mCurrent,
                                             mCurrentGenerator, gates);

        final DataFall<OUT, OUT>[] outFalls = waterfall.mFalls;

        for (final DataFall<IN, OUT> fall : mFalls) {

            connect(fall, outFalls[0]);
        }

        int number = 0;

        for (final Waterfall<?, ?, OUT> inWaterfall : waterfalls) {

            ++number;

            for (final DataFall<?, OUT> fall : inWaterfall.mFalls) {

                connect(fall, outFalls[number]);
            }
        }

        return waterfall;
    }

    /**
     * Causes the data drops flowing through this waterfall streams to be merged into a single
     * stream.
     * <p/>
     * Note that the resulting waterfall will have size equal to 1.
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
     * Creates and returns a new data collector after discharging this waterfall source.
     *
     * @return the collector.
     */
    public Collector<OUT> pull() {

        final Collector<OUT> collector = collect();

        source().flush();

        return collector;
    }

    /**
     * Creates and returns a new data collector after pushing the specified data into this
     * waterfall source and then discharging it.
     *
     * @param source the source data.
     * @return the collector.
     */
    public Collector<OUT> pull(final SOURCE source) {

        final Collector<OUT> collector = collect();

        source().push(source).flush();

        return collector;
    }

    /**
     * Creates and returns a new data collector after pushing the specified data into this
     * waterfall source and then discharging it.
     *
     * @param sources the source data.
     * @return the collector.
     */
    public Collector<OUT> pull(final SOURCE... sources) {

        final Collector<OUT> collector = collect();

        source().push(sources).flush();

        return collector;
    }

    /**
     * Creates and returns a new data collector after pushing the data returned by the specified
     * iterable into this waterfall source and then discharging it.
     *
     * @param sources the source data iterable.
     * @return the collector.
     */
    public Collector<OUT> pull(final Iterable<SOURCE> sources) {

        final Collector<OUT> collector = collect();

        source().push(sources).flush();

        return collector;
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
     * Note that the dams, the size and the currents of this waterfall will be retained.
     *
     * @param generator the spring generator.
     * @param <DATA>    the spring data type.
     * @return the newly created waterfall.
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
     * Note that the dams and the currents of this waterfall will be retained, while the size will
     * be equal to 1.
     *
     * @param spring the spring instance.
     * @param <DATA> the spring data type.
     * @return the newly created waterfall.
     */
    public <DATA> Waterfall<Void, Void, DATA> spring(final Spring<DATA> spring) {

        return spring(Collections.singleton(spring));
    }

    /**
     * Creates and returns a new waterfall fed by the specified springs.
     * <p/>
     * Note that the dams and the currents of this waterfall will be retained, while the size will
     * be equal to the one of the specified collection.
     *
     * @param springs the spring instances.
     * @param <DATA>  the spring data type.
     * @return the newly created waterfall.
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

    // TODO
    public Waterfall<Void, Void, OUT> sprout(final Spring<OUT> spring) {

        return sprout(Collections.singleton(spring));
    }

    // TODO
    public Waterfall<Void, Void, OUT> sprout(final Collection<? extends Spring<OUT>> springs) {

        final ArrayList<Spring<OUT>> list = new ArrayList<Spring<OUT>>(springs.size() + 1);

        list.add(Springs.from(this));
        list.addAll(springs);

        return spring(list);
    }

    /**
     * Creates and returns a new waterfall generating from this one.
     * <p/>
     * Note that the dams, the size and the currents of this waterfall will be retained.
     *
     * @return the newly created waterfall.
     */
    public Waterfall<OUT, OUT, OUT> start() {

        final int size = mSize;

        //noinspection unchecked
        final Gate<OUT, OUT>[] gates = new Gate[size];
        Arrays.fill(gates, openGate());

        final Map<Classification<?>, DamGate<?, ?>> damMap = Collections.emptyMap();

        return new Waterfall<OUT, OUT, OUT>(null, damMap, mDamClassification, mBackgroundPoolSize,
                                            mBackgroundCurrent, null, size, mCurrent,
                                            mCurrentGenerator, gates);
    }

    /**
     * Creates and returns a new waterfall generating from this one.
     * <p/>
     * Note that the dams, the size and the currents of this waterfall will be retained.
     *
     * @param dataType the data type.
     * @param <DATA>   the data type.
     * @return the newly created waterfall.
     */
    public <DATA> Waterfall<DATA, DATA, DATA> start(final Class<DATA> dataType) {

        return start(Classification.ofType(dataType));
    }

    /**
     * Creates and returns a new waterfall generating from this one.
     * <p/>
     * Note that the dams, the size and the currents of this waterfall will be retained.
     *
     * @param classification the data classification.
     * @param <DATA>         the data type.
     * @return the newly created waterfall.
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
     * Note that the dams, the size and the currents of this waterfall will be retained.
     *
     * @param generator the gate generator.
     * @param <NIN>     the new input data type.
     * @param <NOUT>    the new output data type.
     * @return the newly created waterfall.
     */
    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(final GateGenerator<NIN, NOUT> generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall gate generator cannot be null");
        }

        final Map<Classification<?>, DamGate<?, ?>> damMap = Collections.emptyMap();

        final int size = mSize;

        //noinspection unchecked
        final Gate<NIN, NOUT>[] gates = new Gate[size];

        if (size <= 1) {

            final Gate<NIN, NOUT> gate = generator.create(0);

            registerGate(gate);

            gates[0] = gate;

            return new Waterfall<NIN, NIN, NOUT>(null, damMap, mDamClassification,
                                                 mBackgroundPoolSize, mBackgroundCurrent, null, 1,
                                                 mCurrent, mCurrentGenerator, gates);
        }

        if (mDamClassification != null) {

            throw new IllegalStateException("cannot make a dam from more than one gate");
        }

        for (int i = 0; i < size; ++i) {

            final Gate<NIN, NOUT> gate = generator.create(i);

            registerGate(gate);

            gates[i] = gate;
        }

        return new Waterfall<NIN, NIN, NOUT>(null, damMap, null, mBackgroundPoolSize,
                                             mBackgroundCurrent, null, size, mCurrent,
                                             mCurrentGenerator, gates);
    }

    /**
     * Creates and returns a new waterfall chained to the specified gates.
     * <p/>
     * Note that the dams and the currents of this waterfall will be retained, while the size will
     * be equal to the one of the specified array.
     *
     * @param gates  the gate instances.
     * @param <NIN>  the new input data type.
     * @param <NOUT> the new output data type.
     * @return the newly created waterfall.
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
     * Note that the dams, the size and the currents of this waterfall will be retained.
     *
     * @param gate   the gate instance.
     * @param <NIN>  the new input data type.
     * @param <NOUT> the new output data type.
     * @return the newly created waterfall.
     */
    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(final Gate<NIN, NOUT> gate) {

        registerGate(gate);

        final Map<Classification<?>, DamGate<?, ?>> damMap = Collections.emptyMap();

        //noinspection unchecked
        final Gate<NIN, NOUT>[] gates = new Gate[]{gate};

        return new Waterfall<NIN, NIN, NOUT>(null, damMap, mDamClassification, mBackgroundPoolSize,
                                             mBackgroundCurrent, null, mSize, mCurrent,
                                             mCurrentGenerator, gates);
    }

    // TODO: timeout?
    public Waterfall<SOURCE, OUT, OUT> throwOnTimeout(final long delay, final TimeUnit timeUnit,
            final RuntimeException exception) {

        return chain(new TimeoutGate<OUT>(this, delay, timeUnit, exception));
    }

    private Waterfall<SOURCE, OUT, OUT> chainPump(final PumpGate<OUT> pumpGate) {

        final Waterfall<SOURCE, OUT, OUT> waterfall = chain(pumpGate);

        return new Waterfall<SOURCE, OUT, OUT>(waterfall.mSource, waterfall.mDamMap,
                                               waterfall.mDamClassification, mBackgroundPoolSize,
                                               mBackgroundCurrent, pumpGate, waterfall.mSize,
                                               waterfall.mCurrent, waterfall.mCurrentGenerator,
                                               waterfall.mFalls);
    }

    private DamGate<?, ?> findBestMatch(final Classification<?> damClassification) {

        final Map<Classification<?>, DamGate<?, ?>> damMap = mDamMap;

        DamGate<?, ?> gate = damMap.get(damClassification);

        if (gate == null) {

            Classification<?> bestMatch = null;

            for (final Entry<Classification<?>, DamGate<?, ?>> entry : damMap.entrySet()) {

                final Classification<?> type = entry.getKey();

                if (damClassification.isAssignableFrom(type)) {

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

    private void mapDam(final HashMap<Classification<?>, DamGate<?, ?>> damMap,
            final Classification<?> damClassification, final DamGate<?, ?> gate) {

        if (!damClassification.getRawType().isInstance(gate.gate)) {

            throw new IllegalArgumentException(
                    "the gate does not implement the dam classification type");
        }

        damMap.put(damClassification, gate);
    }
}