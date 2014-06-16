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
import com.bmd.wtf.flw.Glass;
import com.bmd.wtf.flw.Reflection;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.FreeLeap;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.lps.LeapGenerator;

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

    private static FreeLeap<?, ?> sFreeLeap;

    private final Current mCurrent;

    private final CurrentGenerator mCurrentGenerator;

    private final DataFall<SOURCE, IN, OUT>[] mFalls;

    private final Class<?> mGlass;

    private final HashMap<Class<?>, GlassLeap<?, ?, ?>> mGlassMap;

    private final int mSize;

    private final Waterfall<SOURCE, SOURCE, ?> mSource;

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final HashMap<Class<?>, GlassLeap<?, ?, ?>> glassMap, final Class<?> glass,
            final int size, final Current current, final CurrentGenerator generator,
            final DataFall<SOURCE, IN, OUT>... falls) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mGlassMap = glassMap;
        mGlass = glass;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;
        mFalls = falls;
    }

    private Waterfall(final Waterfall<SOURCE, SOURCE, ?> source,
            final HashMap<Class<?>, GlassLeap<?, ?, ?>> glassMap, final Class<?> glass,
            final int size, final Current current, final CurrentGenerator generator,
            final Leap<SOURCE, IN, OUT>... leaps) {

        //noinspection unchecked
        mSource = (source != null) ? source : (Waterfall<SOURCE, SOURCE, ?>) this;
        mGlassMap = glassMap;
        mGlass = null;
        mSize = size;
        mCurrent = current;
        mCurrentGenerator = generator;

        final int length = leaps.length;

        final DataFall[] falls = new DataFall[length];

        final Barrage barrage;

        if (length == 1) {

            glassMap.remove(Barrage.class);

            barrage = null;

        } else if (glassMap.containsKey(Barrage.class)) {

            barrage = when(new Glass<Barrage>() {}).perform();

        } else {

            barrage = null;
        }

        if (glass == null) {

            for (int i = 0; i < length; i++) {

                final Leap<SOURCE, IN, OUT> fallLeap;

                if (barrage != null) {

                    fallLeap = new BarrageLeap<SOURCE, IN, OUT>(leaps[i], barrage, i);

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

            final boolean isSelf = Reflection.class.equals(glass);

            final HashMap<Leap<?, ?, ?>, GlassLeap<?, ?, ?>> leapMap =
                    new HashMap<Leap<?, ?, ?>, GlassLeap<?, ?, ?>>();

            for (int i = 0; i < length; i++) {

                final Leap<SOURCE, IN, OUT> leap = leaps[i];

                //noinspection unchecked
                GlassLeap<SOURCE, IN, OUT> glassLeap =
                        (GlassLeap<SOURCE, IN, OUT>) leapMap.get(leap);

                if (glassLeap == null) {

                    glassLeap = new GlassLeap<SOURCE, IN, OUT>(leap);
                    leapMap.put(leap, glassLeap);

                    exposeGlass(glassMap, (isSelf) ? leap.getClass() : glass, glassLeap);
                }

                final Leap<SOURCE, IN, OUT> fallLeap;

                if (barrage != null) {

                    fallLeap = new BarrageLeap<SOURCE, IN, OUT>(glassLeap, barrage, i);

                } else {

                    fallLeap = glassLeap;
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

        //noinspection unchecked
        return new Waterfall<Object, Object, Object>(null,
                                                     new HashMap<Class<?>, GlassLeap<?, ?, ?>>(),
                                                     null, 1, Currents.straight(), null, NO_FALL);
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

    // TODO: Rapids.generator(Class)

    private static void registerLeap(final Leap<?, ?, ?> leap) {

        if (sLeaps.containsKey(leap)) {

            throw new IllegalArgumentException("the waterfall already contains the leap: " + leap);
        }

        sLeaps.put(leap, null);
    }

    public Waterfall<SOURCE, IN, OUT> as(final Class<?> glass) {

        if (glass == null) {

            throw new IllegalArgumentException("the glass class cannot be null");
        }

        if (!glass.isInterface()) {

            throw new IllegalArgumentException("the glass must be an interface");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGlassMap, glass, mSize, mCurrent,
                                              mCurrentGenerator, mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> asGlass() {

        return as(Reflection.class);
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
            Class<? extends Leap<SOURCE, OUT, NOUT>> glass) {

        if (!glass.isInterface()) {

            throw new IllegalArgumentException("the glass must be an interface");
        }

        //noinspection unchecked
        final Leap<SOURCE, OUT, NOUT> leap = (Leap<SOURCE, OUT, NOUT>) findBestMatch(glass);

        if (leap == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any reflection implementing " + glass);
        }

        final DataFall<SOURCE, IN, OUT>[] falls = mFalls;

        final int size = mSize;

        if (size == 1) {

            //noinspection unchecked
            final Waterfall<SOURCE, OUT, NOUT> waterfall =
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mGlassMap, mGlass, 1, mCurrent,
                                                     mCurrentGenerator, leap);

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
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mGlassMap,
                                                 inWaterfall.mGlass, size, inWaterfall.mCurrent,
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
                    new Waterfall<SOURCE, OUT, OUT>(mSource, mGlassMap, mGlass, 1, mCurrent,
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
                new Waterfall<SOURCE, OUT, OUT>(inWaterfall.mSource, inWaterfall.mGlassMap,
                                                inWaterfall.mGlass, size, inWaterfall.mCurrent,
                                                inWaterfall.mCurrentGenerator, leaps);

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
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mGlassMap, mGlass, 1, mCurrent,
                                                     mCurrentGenerator, leap);

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
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mGlassMap,
                                                 inWaterfall.mGlass, size, inWaterfall.mCurrent,
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
                    new Waterfall<SOURCE, OUT, NOUT>(mSource, mGlassMap, mGlass, 1, mCurrent,
                                                     mCurrentGenerator, leap);

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
                new Waterfall<SOURCE, OUT, NOUT>(inWaterfall.mSource, inWaterfall.mGlassMap,
                                                 inWaterfall.mGlass, size, inWaterfall.mCurrent,
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

        return in(1).chain(new DataBarrage<SOURCE, OUT>(size)).asGlass().in(size);
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
    public <CLASS> Reflection<CLASS> when(final Glass<CLASS> glass) {

        if (!glass.isInterface()) {

            throw new IllegalArgumentException("the glass must represent an interface");
        }

        final GlassLeap<?, ?, ?> leap = findBestMatch(glass.getRawType());

        if (leap == null) {

            throw new IllegalArgumentException(
                    "the waterfall does not retain any reflection implementing " + glass
                            .getRawType()
            );
        }

        return new DataReflection<CLASS>(leap, glass);
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

    public <CLASS> Waterfall<SOURCE, IN, OUT> forget(final CLASS glass) {

        if (glass == null) {

            return this;
        }

        boolean isChanged = false;

        final HashMap<Class<?>, GlassLeap<?, ?, ?>> glassMap =
                new HashMap<Class<?>, GlassLeap<?, ?, ?>>(mGlassMap);

        final Iterator<GlassLeap<?, ?, ?>> iterator = glassMap.values().iterator();

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
        return new Waterfall<SOURCE, IN, OUT>(mSource, glassMap, mGlass, mSize, mCurrent,
                                              mCurrentGenerator, mFalls);
    }

    public <CLASS> Waterfall<SOURCE, IN, OUT> forget(final Glass<CLASS> glass) {

        return forget(when(glass));
    }

    public Waterfall<SOURCE, IN, OUT> in(final CurrentGenerator generator) {

        if (generator == null) {

            throw new IllegalArgumentException("the waterfalll current generator cannot be null");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGlassMap, mGlass, mSize, null, generator,
                                              mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> in(final int fallCount) {

        if (fallCount <= 0) {

            throw new IllegalArgumentException("the fall count cannot be negative or zero");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGlassMap, mGlass, fallCount, mCurrent,
                                              mCurrentGenerator, mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> in(final Current current) {

        if (current == null) {

            throw new IllegalArgumentException("the waterfall current cannot be null");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGlassMap, mGlass, mSize, current, null,
                                              mFalls);
    }

    public Waterfall<SOURCE, IN, OUT> inBackground(final int fallCount) {

        if (fallCount <= 0) {

            throw new IllegalArgumentException("the fall count cannot be negative or zero");
        }

        //noinspection unchecked
        return new Waterfall<SOURCE, IN, OUT>(mSource, mGlassMap, mGlass, fallCount,
                                              Currents.pool(fallCount), null, mFalls);
    }

    public Waterfall<OUT, OUT, OUT> start() {

        final int size = mSize;

        final FreeLeap<OUT, OUT> leap = freeLeap();

        final Leap[] leaps = new Leap[size];

        Arrays.fill(leaps, leap);

        //noinspection unchecked
        return new Waterfall<OUT, OUT, OUT>(null, mGlassMap, mGlass, size, mCurrent,
                                            mCurrentGenerator, leaps);
    }

    public <DATA> Waterfall<DATA, DATA, DATA> start(final Class<DATA> type) {

        final int size = mSize;

        final FreeLeap<DATA, DATA> leap = freeLeap();

        final Leap[] leaps = new Leap[size];

        Arrays.fill(leaps, leap);

        //noinspection unchecked
        return new Waterfall<DATA, DATA, DATA>(null, mGlassMap, mGlass, size, mCurrent,
                                               mCurrentGenerator, leaps);
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
            return new Waterfall<NIN, NIN, NOUT>(null, mGlassMap, mGlass, 1, mCurrent,
                                                 mCurrentGenerator, leap);
        }

        final Leap[] leaps = new Leap[size];

        for (int i = 0; i < size; i++) {

            final Leap<NIN, NIN, NOUT> leap = generator.start(i);

            registerLeap(leap);

            leaps[i] = leap;
        }

        //noinspection unchecked
        return new Waterfall<NIN, NIN, NOUT>(null, mGlassMap, mGlass, size, mCurrent,
                                             mCurrentGenerator, leaps);
    }

    public <NIN, NOUT> Waterfall<NIN, NIN, NOUT> start(final Leap<NIN, NIN, NOUT> leap) {

        if (leap == null) {

            throw new IllegalArgumentException("the waterfall leap cannot be null");
        }

        registerLeap(leap);

        //noinspection unchecked
        return new Waterfall<NIN, NIN, NOUT>(null, mGlassMap, mGlass, mSize, mCurrent,
                                             mCurrentGenerator, leap);
    }

    private void exposeGlass(final HashMap<Class<?>, GlassLeap<?, ?, ?>> glassMap,
            final Class<?> glass, final GlassLeap<?, ?, ?> leap) {

        final Class[] glasses = extractInterfaces(glassMap, glass);

        for (final Class<?> aClass : glasses) {

            glassMap.put(aClass, leap);
        }
    }

    private Class[] extractInterfaces(final HashMap<Class<?>, GlassLeap<?, ?, ?>> glassMap,
            final Class<?> glass) {

        if (glass.isInterface()) {

            if (Leap.class.equals(glass)) {

                throw new IllegalArgumentException("the glass class cannot be " + Leap.class);
            }

            if (glassMap.containsKey(glass)) {

                throw new IllegalArgumentException("the glass class is already present");
            }

            return new Class[]{glass};
        }

        final ArrayList<Class<?>> interfaces = new ArrayList<Class<?>>();

        for (final Class<?> itf : glass.getInterfaces()) {

            if (Leap.class.equals(itf)) {

                continue;
            }

            if (glassMap.containsKey(itf)) {

                throw new IllegalArgumentException("the glass class is already present");
            }

            interfaces.add(itf);
        }

        return interfaces.toArray(new Class[interfaces.size()]);
    }

    private GlassLeap<?, ?, ?> findBestMatch(final Class<?> glass) {

        final HashMap<Class<?>, GlassLeap<?, ?, ?>> glassMap = mGlassMap;

        GlassLeap<?, ?, ?> leap = glassMap.get(glass);

        if (leap == null) {

            Class<?> bestMatch = null;

            for (final Entry<Class<?>, GlassLeap<?, ?, ?>> entry : glassMap.entrySet()) {

                final Class<?> itf = entry.getKey();

                if (glass.isAssignableFrom(itf)) {

                    if ((bestMatch == null) || itf.isAssignableFrom(bestMatch)) {

                        leap = entry.getValue();

                        bestMatch = itf;
                    }
                }
            }
        }

        return leap;
    }
}