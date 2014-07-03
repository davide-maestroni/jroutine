/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KDATAD, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.wtf.fll;

import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.flw.River;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a river composed by a list of data stream.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <SOURCE> The source data type.
 * @param <DATA>   The data type.
 */
class StreamRiver<SOURCE, DATA> implements River<SOURCE, DATA> {

    private final List<DataStream<DATA>> mStreams;

    private final Waterfall<SOURCE, ?, ?> mWaterfall;

    /**
     * Constructor.
     *
     * @param streams   The list of streams.
     * @param waterfall The source waterfall.
     */
    public StreamRiver(final List<DataStream<DATA>> streams,
            final Waterfall<SOURCE, ?, ?> waterfall) {

        if (streams == null) {

            throw new IllegalArgumentException("the list of streams cannot be null");
        }

        if (waterfall == null) {

            throw new IllegalArgumentException("the source waterfall cannot be null");
        }

        mStreams = streams;
        mWaterfall = waterfall;
    }

    @Override
    public void deviate() {

        for (final DataStream<DATA> stream : mStreams) {

            stream.deviate();
        }
    }

    @Override
    public void deviateStream(final int streamNumber) {

        mStreams.get(streamNumber).deviate();
    }

    @Override
    public River<SOURCE, DATA> discharge() {

        for (final DataStream<DATA> stream : mStreams) {

            stream.discharge();
        }

        return this;
    }

    @Override
    public River<SOURCE, DATA> forward(final Throwable throwable) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.forward(throwable);
        }

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final DATA... drops) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.push(drops);
        }

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final Iterable<? extends DATA> drops) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.push(drops);
        }

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final DATA drop) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.push(drop);
        }

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.pushAfter(delay, timeUnit, drops);
        }

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final DATA drop) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.pushAfter(delay, timeUnit, drop);
        }

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final DATA... drops) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.pushAfter(delay, timeUnit, drops);
        }

        return this;
    }

    @Override
    public River<SOURCE, DATA> dischargeStream(final int streamNumber) {

        mStreams.get(streamNumber).discharge();

        return this;
    }

    @Override
    public void drain() {

        for (final DataStream<DATA> stream : mStreams) {

            stream.drain(true);
        }
    }

    @Override
    public void drainStream(final int streamNumber) {

        mStreams.get(streamNumber).drain(true);
    }

    @Override
    public River<SOURCE, DATA> forwardStream(final int streamNumber, final Throwable throwable) {

        mStreams.get(streamNumber).forward(throwable);

        return this;
    }

    @Override
    public <TYPE> Gate<TYPE> on(final Class<TYPE> gateType) {

        return mWaterfall.on(gateType);
    }

    @Override
    public <TYPE> Gate<TYPE> on(final TYPE leap) {

        return mWaterfall.on(leap);
    }

    @Override
    public <TYPE> Gate<TYPE> on(final Classification<TYPE> gateClassification) {

        return mWaterfall.on(gateClassification);
    }

    @Override
    public River<SOURCE, DATA> pushStream(final int streamNumber, final DATA... drops) {

        mStreams.get(streamNumber).push(drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushStream(final int streamNumber,
            final Iterable<? extends DATA> drops) {

        mStreams.get(streamNumber).push(drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushStream(final int streamNumber, final DATA drop) {

        mStreams.get(streamNumber).push(drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mStreams.get(streamNumber).pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mStreams.get(streamNumber).pushAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        mStreams.get(streamNumber).pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public int size() {

        return mStreams.size();
    }

    @Override
    public River<SOURCE, SOURCE> source() {

        return mWaterfall.source();
    }
}