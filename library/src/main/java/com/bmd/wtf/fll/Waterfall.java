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
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.FreeLeap;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.lps.LeapGenerator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/4/14.
 */
public class Waterfall<SOURCE, IN, OUT> implements River<SOURCE, IN> {

    private static final DataFall[] NO_FALL = new DataFall[0];

    private static final WeakHashMap<Leap<?, ?, ?>, Void> sLeaps =
            new WeakHashMap<Leap<?, ?, ?>, Void>();

    private static Method EQUALS_METHOD;

    static {

        try {

            EQUALS_METHOD = Object.class.getDeclaredMethod("equals", Object.class);

        } catch (final NoSuchMethodException e) {

            // Should never happen
        }
    }

    private static FreeLeap<?, ?> sFreeLeap;

    private final Current mCurrent;

    private final CurrentGenerator mCurrentGenerator;

    private final DataFall<SOURCE, IN, OUT>[] mFalls;

    private final HashMap<Class<?>, Object> mGlassMap;

    private final int mSize;

    private final Waterfall<SOURCE, SOURCE, ?> mSource;

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final HashMap<Class<?>, Object> glassMap, final int size, final Current current,
            final CurrentGenerator generator, final DataFall<SOURCE, IN, OUT>... falls) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mGlassMap = glassMap;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;
        mFalls = falls;
    }

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final HashMap<Class<?>, Object> glassMap, final int size, final Current current,
            final CurrentGenerator generator, final Leap<SOURCE, IN, OUT>... leaps) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mGlassMap = glassMap;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;

        final Object mutex;

        final Leap<SOURCE, IN, OUT>[] leapArray;

        if ((leaps.length == 1) && (size > 1)) {

            mutex = new Object();

            //noinspection unchecked
            leapArray = new Leap[size];

            Arrays.fill(leapArray, leaps[0]);

        } else {

            mutex = null;

            leapArray = leaps;
        }

        final int length = leapArray.length;

        final DataFall[] falls = new DataFall[length];

        final Barrage barrage;

        if (length == 1) {

            glassMap.remove(Barrage.class);

            barrage = null;

        } else {

            barrage = when(Barrage.class);
        }

        for (int i = 0; i < length; i++) {

            final Leap<SOURCE, IN, OUT> leap;

            if (barrage != null) {

                leap = new BarrageLeap<SOURCE, IN, OUT>(leapArray[i], barrage, i);

            } else {

                leap = leapArray[i];
            }

            if (mutex != null) {

                falls[i] = new DataFall<SOURCE, IN, OUT>(this, (current != null) ? current
                        : generator.create(i), leap, i, mutex);

            } else {

                falls[i] = new DataFall<SOURCE, IN, OUT>(this, (current != null) ? current
                        : generator.create(i), leap, i);
            }
        }

        //noinspection unchecked
        mFalls = (DataFall<SOURCE, IN, OUT>[]) falls;
    }

    public static Waterfall<Object, Object, Object> create() {

        //noinspection unchecked
        return new Waterfall<Object, Object, Object>(null, new HashMap<Class<?>, Object>(), 1,
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

    // TODO: Dam (CollectorLeap), Spring.feed(int... data), Rapids.generator(Class)

    private static void registerLeap(final Leap<?, ?, ?> leap) {

        if (sLeaps.containsKey(leap)) {

            throw new IllegalArgumentException("the waterfall already contains the leap: " + leap);
        }

        sLeaps.put(leap, null);
    }

    public Waterfall<SOURCE, IN, OUT> as() {

        final DataFall<SOURCE, IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            throw new IllegalArgumentException("there's no leap to expose");
        }

        final HashMap<Class<?>, Object> glassMap = new HashMap<Class<?>, Object>(mGlassMap);

        for (final DataFall<SOURCE, IN, OUT> fall : falls) {

            exposeGlass(glassMap, fall, fall.leap.getClass());
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, glassMap, mSize, mCurrent, mCurrentGenerator,
                                              mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> as(final Class<?> glass) {

        final DataFall<SOURCE, IN, OUT>[] falls = mFalls;

        if (falls == NO_FALL) {

            throw new IllegalArgumentException("there's no leap to expose");
        }

        final HashMap<Class<?>, Object> glassMap = new HashMap<Class<?>, Object>(mGlassMap);

        for (final DataFall<SOURCE, IN, OUT> fall : falls) {

            exposeGlass(glassMap, fall, glass);
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, glassMap, mSize, mCurrent, mCurrentGenerator,
                                              mFalls);
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

            } else if ((length != 1) && (length != size)) {

                in(1).chain().in(size).chain(waterfall);

            } else if (length == 1) {

                final DataFall<SOURCE, IN, OUT> fall = falls[0];

                for (final DataFall<?, OUT, ?> outFall : outFalls) {

                    link(fall, outFall);
                }

            } else {

                for (int i = 0; i < size; i++) {

                    link(falls[i], outFalls[i]);
                }
            }
        }
    }

    public <NOUT> Waterfall<SOURCE, OUT, NOUT> chain(
            Class<? extends Leap<SOURCE, OUT, NOUT>> glass) {

        final Leap<SOURCE, OUT, NOUT> proxy = when(glass);

        if (proxy == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not expose any glass implementing " + glass);
        }

        final FallInvocationHandler handler =
                (FallInvocationHandler) Proxy.getInvocationHandler(proxy);

        //noinspection unchecked
        final Leap<SOURCE, OUT, NOUT> leap = (Leap<SOURCE, OUT, NOUT>) handler.mLeap;

        return chain(new LockedLeap<SOURCE, OUT, NOUT>(leap, handler.mMutex));
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
                    new Waterfall<SOURCE, OUT, OUT>(mSource, mGlassMap, 1, mCurrent,
                                                    mCurrentGenerator, leap);

            final DataFall<SOURCE, OUT, OUT> outFall = waterfall.mFalls[0];

            for (final DataFall<SOURCE, IN, OUT> fall : falls) {

                link(fall, outFall);
            }

            return waterfall;
        }

        final int length = falls.length;

        if ((length != 1) && (length != size)) {

            return in(1).chain().in(size).chain();
        }

        final Leap[] leaps = new Leap[size];

        Arrays.fill(leaps, leap);

        //noinspection unchecked
        final Waterfall<SOURCE, OUT, OUT> waterfall =
                new Waterfall<SOURCE, OUT, OUT>(mSource, mGlassMap, size, mCurrent,
                                                mCurrentGenerator,
                                                (Leap<SOURCE, OUT, OUT>[]) leaps);

        final DataFall<SOURCE, OUT, OUT>[] outFalls = waterfall.mFalls;

        if (length == 1) {

            final DataFall<SOURCE, IN, OUT> fall = falls[0];

            for (final DataFall<SOURCE, OUT, OUT> outFall : outFalls) {

                link(fall, outFall);
            }

        } else {

            for (int i = 0; i < size; i++) {

                link(falls[i], outFalls[i]);
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
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mGlassMap, 1, mCurrent,
                                                     mCurrentGenerator, leap);

            final DataFall<SOURCE, OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<SOURCE, IN, OUT> fall : falls) {

                link(fall, outFall);
            }

            return waterfall;
        }

        final int length = falls.length;

        if ((length != 1) && (length != size)) {

            return in(1).chain().in(size).chain(leap);
        }

        registerLeap(leap);

        //noinspection unchecked
        final Waterfall<SOURCE, OUT, NOUT> waterfall =
                new Waterfall<SOURCE, OUT, NOUT>(mSource, mGlassMap, size, mCurrent,
                                                 mCurrentGenerator, leap);

        final DataFall<SOURCE, OUT, NOUT>[] outFalls = waterfall.mFalls;

        if (length == 1) {

            final DataFall<SOURCE, IN, OUT> fall = falls[0];

            for (final DataFall<SOURCE, OUT, NOUT> outFall : outFalls) {

                link(fall, outFall);
            }

        } else {

            for (int i = 0; i < size; i++) {

                link(falls[i], outFalls[i]);
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
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mGlassMap, 1, mCurrent,
                                                     mCurrentGenerator, leap);

            final DataFall<SOURCE, OUT, NOUT> outFall = waterfall.mFalls[0];

            for (final DataFall<SOURCE, IN, OUT> fall : falls) {

                link(fall, outFall);
            }

            return waterfall;
        }

        final int length = falls.length;

        if ((length != 1) && (length != size)) {

            return in(1).chain().in(size).chain(generator);
        }

        final Leap[] leaps = new Leap[size];

        for (int i = 0; i < size; i++) {

            final Leap<SOURCE, OUT, NOUT> leap = generator.start(i);

            registerLeap(leap);

            leaps[i] = leap;
        }

        //noinspection unchecked
        final Waterfall<SOURCE, OUT, NOUT> waterfall =
                new Waterfall<SOURCE, OUT, NOUT>(mSource, mGlassMap, size, mCurrent,
                                                 mCurrentGenerator,
                                                 (Leap<SOURCE, OUT, NOUT>[]) leaps);

        final DataFall<SOURCE, OUT, NOUT>[] outFalls = waterfall.mFalls;

        if (length == 1) {

            final DataFall<SOURCE, IN, OUT> fall = falls[0];

            for (final DataFall<SOURCE, OUT, NOUT> outFall : outFalls) {

                link(fall, outFall);
            }

        } else {

            for (int i = 0; i < size; i++) {

                link(falls[i], outFalls[i]);
            }
        }

        return waterfall;
    }

    public <GLASS> Waterfall<SOURCE, IN, OUT> cover(final GLASS glass) {

        if (glass == null) {

            return this;
        }

        boolean isChanged = false;

        final HashMap<Class<?>, Object> glassMap = new HashMap<Class<?>, Object>(mGlassMap);

        final Iterator<Object> iterator = glassMap.values().iterator();

        while (iterator.hasNext()) {

            if (glass == iterator.next()) {

                iterator.remove();

                isChanged = true;
            }
        }

        if (!isChanged) {

            return this;
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, glassMap, mSize, mCurrent, mCurrentGenerator,
                                              mFalls);
    }

    public <GLASS> Waterfall<SOURCE, IN, OUT> cover(final Class<GLASS> glass) {

        return cover(when(glass));
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

        return in(1).chain(new DataBarrage<SOURCE, OUT>(size)).as().in(size);
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
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGlassMap, mSize, null, generator, mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> in(final int fallCount) {

        if (fallCount <= 0) {

            throw new IllegalArgumentException("the fall count cannot be negative or zero");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGlassMap, fallCount, mCurrent,
                                              mCurrentGenerator, mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> in(final Current current) {

        if (current == null) {

            throw new IllegalArgumentException("the waterfalll current cannot be null");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGlassMap, mSize, current, null, mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> inBackground(final int fallCount) {

        if (fallCount <= 0) {

            throw new IllegalArgumentException("the fall count cannot be negative or zero");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGlassMap, fallCount,
                                              Currents.pool(fallCount), null, mFalls);
    }

    public Waterfall<OUT, OUT, OUT> start() {

        final int size = mSize;

        final FreeLeap<OUT, OUT> leap = freeLeap();

        final Leap[] leaps = new Leap[size];

        Arrays.fill(leaps, leap);

        //noinspection unchecked
        return new Waterfall<OUT, OUT, OUT>(null, mGlassMap, size, mCurrent, mCurrentGenerator,
                                            (Leap<OUT, OUT, OUT>[]) leaps);
    }

    public <DATA> Waterfall<DATA, DATA, DATA> start(final Class<DATA> type) {

        final int size = mSize;

        final FreeLeap<DATA, DATA> leap = freeLeap();

        final Leap[] leaps = new Leap[size];

        Arrays.fill(leaps, leap);

        //noinspection unchecked
        return new Waterfall<DATA, DATA, DATA>(null, mGlassMap, size, mCurrent, mCurrentGenerator,
                                               (Leap<DATA, DATA, DATA>[]) leaps);
    }

    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(
            final LeapGenerator<NIN, NIN, NOUT> generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfall generator cannot be null");
        }

        final int size = mSize;

        if (size == 1) {

            final Leap<NIN, NIN, NOUT> leap = generator.start(0);

            registerLeap(leap);

            //noinspection unchecked
            return new Waterfall<NIN, NIN, NOUT>(null, mGlassMap, 1, mCurrent, mCurrentGenerator,
                                                 leap);
        }

        final Leap[] leaps = new Leap[size];

        for (int i = 0; i < size; i++) {

            final Leap<NIN, NIN, NOUT> leap = generator.start(i);

            registerLeap(leap);

            leaps[i] = leap;
        }

        //noinspection unchecked
        return new Waterfall<NIN, NIN, NOUT>(null, mGlassMap, size, mCurrent, mCurrentGenerator,
                                             (Leap<NIN, NIN, NOUT>[]) leaps);
    }

    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(final Leap<NIN, NIN, NOUT> leap) {

        if (leap == null) {

            throw new IllegalArgumentException("the waterfall leap cannot be null");
        }

        registerLeap(leap);

        final int size = mSize;

        if (size == 1) {

            //noinspection unchecked
            return new Waterfall<NIN, NIN, NOUT>(null, mGlassMap, 1, mCurrent, mCurrentGenerator,
                                                 leap);
        }

        //noinspection unchecked
        return new Waterfall<NIN, NIN, NOUT>(null, mGlassMap, size, mCurrent, mCurrentGenerator,
                                             leap);
    }

    @Override
    public <GLASS> GLASS when(final Class<GLASS> glass) {

        final HashMap<Class<?>, Object> glassMap = mGlassMap;

        Object proxy = glassMap.get(glass);

        if (proxy == null) {

            Class<?> bestMatch = null;

            for (final Entry<Class<?>, Object> entry : glassMap.entrySet()) {

                final Class<?> itf = entry.getKey();

                if (glass.isAssignableFrom(itf)) {

                    if ((bestMatch == null) || itf.isAssignableFrom(bestMatch)) {

                        proxy = entry.getValue();

                        bestMatch = itf;
                    }
                }
            }
        }

        return glass.cast(proxy);
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

    private void exposeGlass(final HashMap<Class<?>, Object> glassMap, final DataFall<?, ?, ?> fall,
            final Class<?> glass) {

        final Class[] glasses = extractInterfaces(glassMap, glass);

        final Object proxy = Proxy.newProxyInstance(glass.getClassLoader(), glasses,
                                                    new FallInvocationHandler(fall));

        for (final Class<?> aClass : glasses) {

            glassMap.put(aClass, proxy);
        }
    }

    private Class[] extractInterfaces(final HashMap<Class<?>, Object> glassMap,
            final Class<?> glass) {

        if (glass.isInterface()) {

            if (Leap.class.equals(glass)) {

                throw new IllegalArgumentException("cannot expose " + Leap.class);
            }

            if (glassMap.containsKey(glass)) {

                throw new IllegalArgumentException("the same glass has been already expose");
            }

            return new Class[]{glass};
        }

        final ArrayList<Class<?>> interfaces = new ArrayList<Class<?>>();

        for (final Class<?> itf : glass.getInterfaces()) {

            if (Leap.class.equals(itf)) {

                continue;
            }

            if (glassMap.containsKey(itf)) {

                throw new IllegalArgumentException("the same glass has been already expose");
            }

            interfaces.add(itf);
        }

        return interfaces.toArray(new Class[interfaces.size()]);
    }

    private static class AnonymousLeap<SOURCE, IN, OUT> implements Leap<SOURCE, IN, OUT> {

        private final Leap<SOURCE, IN, OUT> mLeap;

        public AnonymousLeap(final Leap<SOURCE, IN, OUT> wrapped) {

            mLeap = wrapped;
        }

        @Override
        public void onFlush(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
                final int fallNumber) {

            mLeap.onFlush(upRiver, downRiver, fallNumber);
        }

        @Override
        public void onPush(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
                final int fallNumber, final IN drop) {

            mLeap.onPush(upRiver, downRiver, fallNumber, drop);
        }

        @Override
        public void onUnhandled(final River<SOURCE, IN> upRiver, final River<SOURCE, OUT> downRiver,
                final int fallNumber, final Throwable throwable) {

            mLeap.onUnhandled(upRiver, downRiver, fallNumber, throwable);
        }
    }

    private static class FallInvocationHandler implements InvocationHandler {

        private final Leap mLeap;

        private final Object mMutex;

        public FallInvocationHandler(final DataFall fall) {

            mMutex = fall.mutex;
            mLeap = fall.leap;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            synchronized (mMutex) {

                if (method.equals(EQUALS_METHOD)) {

                    return areEqual(proxy, args[0]);
                }

                return method.invoke(mLeap, args);
            }
        }

        private boolean areEqual(final Object proxy, final Object other) {

            if (proxy == other) {

                return true;
            }

            if (Proxy.isProxyClass(other.getClass())) {

                final InvocationHandler invocationHandler = Proxy.getInvocationHandler(other);

                if (invocationHandler instanceof FallInvocationHandler) {

                    return mLeap.equals(((FallInvocationHandler) invocationHandler).mLeap);
                }
            }

            return false;
        }
    }
}