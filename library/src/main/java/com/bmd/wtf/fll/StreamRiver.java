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

import com.bmd.wtf.flg.Gate;
import com.bmd.wtf.flw.River;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/7/14.
 */
class StreamRiver<SOURCE, DATA> implements River<SOURCE, DATA> {

    private final List<DataStream<DATA>> mStreams;

    private final Waterfall<SOURCE, ?, ?> mWaterfall;

    public StreamRiver(final List<DataStream<DATA>> streams,
            final Waterfall<SOURCE, ?, ?> waterfall) {

        mStreams = streams;
        mWaterfall = waterfall;
    }

    @Override
    public void deviate() {

        for (final DataStream<DATA> stream : mStreams) {

            stream.drain();
        }
    }

    @Override
    public void deviate(final int streamNumber) {

        mStreams.get(streamNumber).drain();
    }

    @Override
    public void drain() {


        for (final DataStream<DATA> stream : mStreams) {

            stream.dryUp(true);
        }
    }

    @Override
    public void drain(final int streamNumber) {

        mStreams.get(streamNumber).dryUp(true);
    }

    @Override
    public River<SOURCE, DATA> flush(final int streamNumber) {

        mStreams.get(streamNumber).flush();

        return this;
    }

    @Override
    public River<SOURCE, DATA> flush() {

        for (final DataStream<DATA> stream : mStreams) {

            stream.flush();
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
    public River<SOURCE, DATA> forward(final int streamNumber, final Throwable throwable) {

        mStreams.get(streamNumber).forward(throwable);

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final int streamNumber, final DATA... drops) {

        mStreams.get(streamNumber).push(drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final int streamNumber, final Iterable<? extends DATA> drops) {

        mStreams.get(streamNumber).push(drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> push(final int streamNumber, final DATA drop) {

        mStreams.get(streamNumber).push(drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mStreams.get(streamNumber).pushAfter(delay, timeUnit, drops);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mStreams.get(streamNumber).pushAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public River<SOURCE, DATA> pushAfter(final int streamNumber, final long delay,
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

    @Override
    public <TYPE> Gate<TYPE> when(final Class<TYPE> gateType) {

        return mWaterfall.when(gateType);
    }

    @Override
    public <TYPE> Gate<TYPE> when(final Classification<TYPE> gateClassification) {

        return mWaterfall.when(gateClassification);
    }
}