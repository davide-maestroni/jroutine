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

import com.bmd.wtf.flw.Dam;
import com.bmd.wtf.flw.River;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a river composed by a list of data stream.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <DATA> The data type.
 */
class StreamRiver<DATA> extends AbstractRiver<DATA> {

    private final List<DataStream<DATA>> mStreams;

    private final Waterfall<?, ?, ?> mWaterfall;

    /**
     * Constructor.
     *
     * @param streams   The list of streams.
     * @param waterfall The source waterfall.
     */
    public StreamRiver(final List<DataStream<DATA>> streams, final Waterfall<?, ?, ?> waterfall) {

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
    public void drain() {

        for (final DataStream<DATA> stream : mStreams) {

            stream.drain(Direction.DOWNSTREAM);
        }
    }

    @Override
    public void drainStream(final int streamNumber) {

        mStreams.get(streamNumber).drain(Direction.DOWNSTREAM);
    }

    @Override
    public River<DATA> exception(final Throwable throwable) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.exception(throwable);
        }

        return this;
    }

    @Override
    public River<DATA> flush() {

        for (final DataStream<DATA> stream : mStreams) {

            stream.flush();
        }

        return this;
    }

    @Override
    public River<DATA> push(final DATA... drops) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.push(drops);
        }

        return this;
    }

    @Override
    public River<DATA> push(final Iterable<? extends DATA> drops) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.push(drops);
        }

        return this;
    }

    @Override
    public River<DATA> push(final DATA drop) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.push(drop);
        }

        return this;
    }

    @Override
    public River<DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.pushAfter(delay, timeUnit, drops);
        }

        return this;
    }

    @Override
    public River<DATA> pushAfter(final long delay, final TimeUnit timeUnit, final DATA drop) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.pushAfter(delay, timeUnit, drop);
        }

        return this;
    }

    @Override
    public River<DATA> pushAfter(final long delay, final TimeUnit timeUnit, final DATA... drops) {

        for (final DataStream<DATA> stream : mStreams) {

            stream.pushAfter(delay, timeUnit, drops);
        }

        return this;
    }

    @Override
    public River<DATA> flushStream(final int streamNumber) {

        mStreams.get(streamNumber).flush();

        return this;
    }

    @Override
    public <TYPE> Dam<TYPE> on(final Class<TYPE> damClass) {

        return mWaterfall.on(damClass);
    }

    @Override
    public <TYPE> Dam<TYPE> on(final TYPE gate) {

        return mWaterfall.on(gate);
    }

    @Override
    public <TYPE> Dam<TYPE> on(final Classification<TYPE> damClassification) {

        return mWaterfall.on(damClassification);
    }

    @Override
    public River<DATA> pushStream(final int streamNumber, final DATA... drops) {

        mStreams.get(streamNumber).push(drops);

        return this;
    }

    @Override
    public River<DATA> pushStream(final int streamNumber, final Iterable<? extends DATA> drops) {

        mStreams.get(streamNumber).push(drops);

        return this;
    }

    @Override
    public River<DATA> pushStream(final int streamNumber, final DATA drop) {

        mStreams.get(streamNumber).push(drop);

        return this;
    }

    @Override
    public River<DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mStreams.get(streamNumber).pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mStreams.get(streamNumber).pushAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<DATA> pushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        mStreams.get(streamNumber).pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public int size() {

        return mStreams.size();
    }

    @Override
    public River<DATA> streamException(final int streamNumber, final Throwable throwable) {

        mStreams.get(streamNumber).exception(throwable);

        return this;
    }

    @Override
    public River<DATA> flushStream(final int streamNumber, final DATA... drops) {

        mStreams.get(streamNumber).flush(drops);

        return this;
    }

    @Override
    public River<DATA> flushStream(final int streamNumber, final Iterable<? extends DATA> drops) {

        mStreams.get(streamNumber).flush(drops);

        return this;
    }

    @Override
    public River<DATA> flushStream(final int streamNumber, final DATA drop) {

        mStreams.get(streamNumber).flush(drop);

        return this;
    }

    @Override
    public River<DATA> flushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        mStreams.get(streamNumber).flushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<DATA> flushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mStreams.get(streamNumber).flushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<DATA> flushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mStreams.get(streamNumber).flushAfter(delay, timeUnit, drop);

        return this;
    }
}