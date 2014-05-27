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
package com.bmd.wtf.xtr.bsn;

import com.bmd.wtf.bdr.Stream;
import com.bmd.wtf.src.Spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a {@link Basin} which can block waiting for the data to flow down
 * asynchronously.
 * <p/>
 * The basin will wait for a flush in order to know that data have complete.
 * <p/>
 * Created by davide on 3/7/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public class BlockingBasin<IN, OUT> extends Basin<IN, OUT> {

    private final BlockingCollectorDam<OUT> mCollector;

    /**
     * Constructor.
     *
     * @param springs   The springs from which data originate.
     * @param collector The collector dam instance.
     * @param outStream The output stream.
     */
    BlockingBasin(final Collection<Spring<IN>> springs, final BlockingCollectorDam<OUT> collector,
            final Stream<IN, OUT, OUT> outStream) {

        super(springs, collector, outStream);

        mCollector = collector;
    }

    /**
     * Creates a new basin from the specified stream.
     *
     * @param stream The stream originating the basin.
     * @param <IN>   The input data type.
     * @param <OUT>  The output data type.
     * @return The new basin.
     */
    public static <IN, OUT> BlockingBasin<IN, OUT> collect(final Stream<IN, ?, OUT> stream) {

        final BlockingCollectorDam<OUT> dam = new BlockingCollectorDam<OUT>();

        return new BlockingBasin<IN, OUT>(Collections.singleton(stream.backToSource()), dam,
                                          stream.thenFallingThrough(dam));
    }

    /**
     * Creates a new basin from the specified streams.
     *
     * @param streams The streams originating the basin.
     * @param <IN>    The input data type.
     * @param <OUT>   The output data type.
     * @return The new basin.
     */
    public static <IN, OUT> BlockingBasin<IN, OUT> collect(final Stream<IN, ?, OUT>... streams) {

        if ((streams == null) || (streams.length == 0)) {

            throw new IllegalArgumentException(
                    "the array of streams to collect cannot be null or empty");
        }

        final ArrayList<Spring<IN>> springs = new ArrayList<Spring<IN>>(streams.length);

        for (final Stream<IN, ?, OUT> stream : streams) {

            springs.add(stream.backToSource());
        }

        final BlockingCollectorDam<OUT> dam = new BlockingCollectorDam<OUT>();

        return new BlockingBasin<IN, OUT>(springs, dam,
                                          streams[0].thenMergingThrough(dam, streams));
    }

    /**
     * Creates a new basin from the streams returned by the specified iterable.
     *
     * @param streams The iterable returning the streams originating the basin.
     * @param <IN>    The input data type.
     * @param <OUT>   The output data type.
     * @return The new basin.
     */
    public static <IN, OUT> BlockingBasin<IN, OUT> collect(
            final Iterable<? extends Stream<IN, ?, OUT>> streams) {

        if (streams == null) {

            throw new IllegalArgumentException(
                    "the collection of streams to collect cannot be null or empty");
        }

        final Stream<IN, ?, OUT> firstStream = streams.iterator().next();

        final ArrayList<Spring<IN>> springs = new ArrayList<Spring<IN>>();

        for (final Stream<IN, ?, OUT> stream : streams) {

            springs.add(stream.backToSource());
        }

        final BlockingCollectorDam<OUT> dam = new BlockingCollectorDam<OUT>();

        return new BlockingBasin<IN, OUT>(springs, dam,
                                          firstStream.thenMergingThrough(dam, streams));
    }

    /**
     * Sets the timeout to wait for data to be fully collected.
     *
     * @param maxDelay The maximum delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @return This basin.
     */
    public BlockingBasin<IN, OUT> afterMax(final long maxDelay, final TimeUnit timeUnit) {

        mCollector.setTimeout(maxDelay, timeUnit);

        return this;
    }

    @Override
    public BlockingBasin<IN, OUT> collectDebrisInto(final Collection<Object> bucket) {

        super.collectDebrisInto(bucket);

        return this;
    }

    @Override
    public BlockingBasin<IN, OUT> collectOutputInto(final Collection<OUT> bucket) {

        super.collectOutputInto(bucket);

        return this;
    }

    @Override
    public BlockingBasin<IN, OUT> thenDrop(final Object debris) {

        super.thenDrop(debris);

        return this;
    }

    @Override
    public BlockingBasin<IN, OUT> thenFeedWith(final IN... drops) {

        super.thenFeedWith(drops);

        return this;
    }

    @Override
    public BlockingBasin<IN, OUT> thenFeedWith(final Iterable<? extends IN> drops) {

        super.thenFeedWith(drops);

        return this;
    }

    @Override
    public BlockingBasin<IN, OUT> thenFeedWith(final IN drop) {

        super.thenFeedWith(drop);

        return this;
    }

    @Override
    public BlockingBasin<IN, OUT> thenFlush() {

        super.thenFlush();

        return this;
    }

    @Override
    public BlockingBasin<IN, OUT> thenPartiallyFeedWith(final IN... drops) {

        super.thenPartiallyFeedWith(drops);

        return this;
    }

    @Override
    public BlockingBasin<IN, OUT> thenPartiallyFeedWith(final Iterable<? extends IN> drops) {

        super.thenPartiallyFeedWith(drops);

        return this;
    }

    @Override
    public BlockingBasin<IN, OUT> thenPartiallyFeedWith(final IN drop) {

        super.thenPartiallyFeedWith(drop);

        return this;
    }

    /**
     * Sets to 0 the timeout to wait for data to be fully collected.
     *
     * @return This basin.
     */
    public BlockingBasin<IN, OUT> immediately() {

        mCollector.setTimeout(0, TimeUnit.MILLISECONDS);

        return throwIfTimeout(null);
    }

    /**
     * Sets the exception to be thrown in case the timeout elapsed before all the data has been
     * collected. If <code>null</code> no exception will be thrown.
     *
     * @param exception The exception to be thrown.
     * @return This basin.
     */
    public BlockingBasin<IN, OUT> throwIfTimeout(final RuntimeException exception) {

        mCollector.setTimeoutException(exception);

        return this;
    }

    /**
     * Sets to an indefinite time the timeout to wait for data to be fully collected.
     *
     * @return This basin.
     */
    public BlockingBasin<IN, OUT> whenAvailable() {

        mCollector.setNoTimeout();

        return throwIfTimeout(null);
    }
}