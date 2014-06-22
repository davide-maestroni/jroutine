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
import com.bmd.wtf.flg.GateControl;
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
 * Created by davide on 6/4/14.
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
            final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap, final Classification<?> gate,
            final Barrage<SOURCE, ?> barrage, final int size, final Current current,
            final CurrentGenerator generator, final DataFall<SOURCE, IN, OUT>... falls) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mGateMap = gateMap;
        mGate = gate;
        mBarrage = barrage;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;
        mFalls = falls;
    }

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap, final Classification<?> gate,
            final Barrage<SOURCE, ?> barrage, final int size, final Current current,
            final CurrentGenerator generator, final Leap<SOURCE, IN, OUT>... leaps) {

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

        if (gate == null) {

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

            final boolean isSelf = (SELF_CLASSIFICATION == gate);

            final HashMap<Leap<?, ?, ?>, GateLeap<?, ?, ?>> leapMap =
                    new HashMap<Leap<?, ?, ?>, GateLeap<?, ?, ?>>();

            for (int i = 0; i < length; i++) {

                final Leap<SOURCE, IN, OUT> leap = leaps[i];

                //noinspection unchecked
                GateLeap<SOURCE, IN, OUT> gateLeap = (GateLeap<SOURCE, IN, OUT>) leapMap.get(leap);

                if (gateLeap == null) {

                    gateLeap = new GateLeap<SOURCE, IN, OUT>(leap);
                    leapMap.put(leap, gateLeap);

                    mapGate(fallGateMap, (isSelf) ? Classification.from(leap.getClass()) : gate,
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

    public static Waterfall<Object, Object, Object> create() {

        final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap = Collections.emptyMap();

        //noinspection unchecked
        return new Waterfall<Object, Object, Object>(null, gateMap, null, null, 1,
                                                     Currents.straight(), null, NO_FALL);
    }

    private static <SOURCE, DATA> FreeLeap<SOURCE, DATA> freeLeap() {

        if (sFreeLeap == null) {

            sFreeLeap = new FreeLeap<Object, Object>();
        }

        //noinspection unchecked
        return (FreeLeap<SOURCE, DATA>) sFreeLeap;
    }

    private static <DATA> DataStream<DATA> link(final DataFall<?, ?, DATA> inFall,
            final DataFall<?, DATA, ?> outFall) {

        final DataStream<DATA> stream = new DataStream<DATA>(inFall, outFall);

        outFall.inputStreams.add(stream);
        inFall.outputStreams.add(stream);

        return stream;
    }

    private static void registerLeap(final Leap<?, ?, ?> leap) {

        if (sLeaps.containsKey(leap)) {

            throw new IllegalArgumentException("the waterfall already contains the leap: " + leap);
        }

        sLeaps.put(leap, null);
    }

    public Waterfall<SOURCE, IN, OUT> as(final Class<?> gateType) {

        return as(Classification.from(gateType));
    }

    public Waterfall<SOURCE, IN, OUT> as(final Classification<?> gate) {

        if (gate == null) {

            throw new IllegalArgumentException("the gate cannot be null");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, gate, mBarrage, mSize, mCurrent,
                                              mCurrentGenerator, mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> asGate() {

        return as(SELF_CLASSIFICATION);
    }

    public <TYPE> Waterfall<SOURCE, IN, OUT> breakDown(final Classification<TYPE> gate) {

        return breakDown(when(gate));
    }

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

    public void chain(final Waterfall<?, OUT, ?> waterfall) {

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

    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(
            final Classification<? extends Leap<SOURCE, OUT, NOUT>> gate) {

        //noinspection unchecked
        final Leap<SOURCE, OUT, NOUT> leap = (Leap<SOURCE, OUT, NOUT>) findBestMatch(gate);

        if (leap == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any gate of type " + gate);
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

    public void drain(final boolean downStream) {

        if (downStream) {

            drain();

        } else {

            for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

                for (final DataStream<IN> stream : fall.inputStreams) {

                    stream.drain();
                }
            }
        }
    }

    public void drain(final int streamNumber, final boolean downStream) {

        if (downStream) {

            drain(streamNumber);

        } else {

            final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

            for (final DataStream<IN> stream : fall.inputStreams) {

                stream.drain();
            }
        }
    }

    @Override
    public void drain() {

        for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

            for (final DataStream<OUT> stream : fall.outputStreams) {

                stream.drain();
            }
        }
    }

    @Override
    public void drain(final int streamNumber) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        for (final DataStream<OUT> stream : fall.outputStreams) {

            stream.drain();
        }
    }

    @Override
    public void dryUp() {

        for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

            for (final DataStream<OUT> stream : fall.outputStreams) {

                stream.dryUp(true);
            }
        }
    }

    @Override
    public void dryUp(final int streamNumber) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        for (final DataStream<OUT> stream : fall.outputStreams) {

            stream.dryUp(true);
        }
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> flush(final int streamNumber) {

        final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

        fall.inputCurrent.flush(fall);

        return this;
    }

    @Override
    public Waterfall<SOURCE, IN, OUT> flush() {

        for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

            fall.inputCurrent.flush(fall);
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
    public <TYPE> GateControl<TYPE> when(final Class<TYPE> type) {

        return when(Classification.from(type));
    }

    @Override
    public <TYPE> GateControl<TYPE> when(final Classification<TYPE> gate) {

        final GateLeap<?, ?, ?> leap = findBestMatch(gate);

        if (leap == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any gate of type " + gate);
        }

        return new DataGateControl<TYPE>(leap, gate);
    }

    public void dryUp(final boolean downStream) {

        if (downStream) {

            dryUp();

        } else {

            for (final DataFall<SOURCE, IN, OUT> fall : mFalls) {

                for (final DataStream<IN> stream : fall.inputStreams) {

                    stream.dryUp(false);
                }
            }
        }
    }

    public void dryUp(final int streamNumber, final boolean downStream) {

        if (downStream) {

            dryUp(streamNumber);

        } else {

            final DataFall<SOURCE, IN, OUT> fall = mFalls[streamNumber];

            for (final DataStream<IN> stream : fall.inputStreams) {

                stream.dryUp(false);
            }
        }
    }

    public Waterfall<SOURCE, IN, OUT> in(final CurrentGenerator generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfalll current generator cannot be null");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBarrage, mSize, null,
                                              generator, mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> in(final int fallCount) {

        if (fallCount <= 0) {

            throw new IllegalArgumentException("the fall count cannot be negative or zero");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBarrage, fallCount,
                                              mCurrent, mCurrentGenerator, mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> in(final Current current) {

        if (current == null) {

            throw new IllegalArgumentException("the waterfall current cannot be null");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBarrage, mSize, current,
                                              null, mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> inBackground(final int poolSize) {

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGateMap, mGate, mBarrage, mSize,
                                              Currents.pool(poolSize), null, mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> inBackground() {

        final int processors = Runtime.getRuntime().availableProcessors();

        final int poolSize;

        if (processors < 4) {

            poolSize = Math.max(1, Math.min(mSize, processors - 1));

        } else {

            poolSize = Math.min(mSize, processors / 2);
        }

        return inBackground(poolSize);
    }

    public Waterfall<OUT, OUT, OUT> start() {

        final int size = mSize;

        final FreeLeap<OUT, OUT> leap = freeLeap();

        final Leap[] leaps = new Leap[size];

        Arrays.fill(leaps, leap);

        final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap = Collections.emptyMap();

        //noinspection unchecked
        return new Waterfall<OUT, OUT, OUT>(null, gateMap, null, null, size, mCurrent,
                                            mCurrentGenerator, leaps);
    }

    public <DATA> Waterfall<DATA, DATA, DATA> start(final Class<DATA> dataType) {

        return start(Classification.from(dataType));
    }

    public <DATA> Waterfall<DATA, DATA, DATA> start(final Classification<DATA> classification) {

        //noinspection unchecked
        return (Waterfall<DATA, DATA, DATA>) start();
    }

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

    private GateLeap<?, ?, ?> findBestMatch(final Classification<?> gate) {

        final Map<Classification<?>, GateLeap<?, ?, ?>> gateMap = mGateMap;

        GateLeap<?, ?, ?> leap = gateMap.get(gate);

        if (leap == null) {

            Classification<?> bestMatch = null;

            for (final Entry<Classification<?>, GateLeap<?, ?, ?>> entry : gateMap.entrySet()) {

                final Classification<?> type = entry.getKey();

                if (gate.isAssignableFrom(type)) {

                    if ((bestMatch == null) || type.isAssignableFrom(bestMatch)) {

                        leap = entry.getValue();

                        bestMatch = type;
                    }
                }
            }
        }

        return leap;
    }

    private void mapGate(final HashMap<Classification<?>, GateLeap<?, ?, ?>> gateMap,
            final Classification<?> gate, final GateLeap<?, ?, ?> leap) {

        if (gateMap.containsKey(gate)) {

            throw new IllegalArgumentException("the gate type is already present");
        }

        gateMap.put(gate, leap);
    }
}