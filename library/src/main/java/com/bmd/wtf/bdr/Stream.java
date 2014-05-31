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
package com.bmd.wtf.bdr;

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * This class implements the connection between {@link com.bmd.wtf.src.Pool}s and it
 * constitutes the fundamental building piece which makes up the data
 * {@link com.bmd.wtf.Waterfall}.
 * <p/>
 * Each instance retains a reference to its source {@link com.bmd.wtf.src.Spring} so to be
 * available during the building chain.
 * <p/>
 * Created by davide on 3/2/14.
 *
 * @param <SOURCE> The spring data type.
 * @param <IN>     The input data type of the upstream pool.
 * @param <OUT>    The transported data type, that is the output data type of the upstream pool.
 */
public class Stream<SOURCE, IN, OUT> {

    private final DataPool<OUT, ?> mDownstreamPool;

    private final Current mOutCurrent;

    private final boolean mPassThrough;

    private final Spring<SOURCE> mSpring;

    private final DataPool<IN, OUT> mUpstreamPool;

    /**
     * Avoid instantiation outside the package.
     *
     * @param spring         The associated spring.
     * @param upstreamPool   The upstream pool.
     * @param downstreamPool The downstream pool.
     */
    Stream(final Spring<SOURCE> spring, final DataPool<IN, OUT> upstreamPool, final DataPool<OUT, ?> downstreamPool) {

        mSpring = spring;
        mUpstreamPool = upstreamPool;
        mDownstreamPool = downstreamPool;

        if (downstreamPool != null) {

            mOutCurrent = downstreamPool.inputCurrent;

            mPassThrough = (upstreamPool != null) && upstreamPool.inputCurrent.equals(mOutCurrent);

        } else {

            mOutCurrent = upstreamPool.inputCurrent;

            mPassThrough = false;
        }
    }

    /**
     * Private constructor.
     *
     * @param spring       The associated spring.
     * @param upstreamPool The upstream pool.
     * @param outCurrent   The output current.
     */
    private Stream(final Spring<SOURCE> spring, final DataPool<IN, OUT> upstreamPool, final Current outCurrent) {

        mSpring = spring;
        mUpstreamPool = upstreamPool;
        mDownstreamPool = null;

        mOutCurrent = outCurrent;

        mPassThrough = false;
    }

    private static boolean canReachFrom(final DataPool<?, ?> pool, final Stream<?, ?, ?> dst) {

        if (dst.mUpstreamPool == null) {

            return false;
        }

        for (final Stream<?, ?, ?> stream : pool.outputStreams) {

            if (stream.canReach(dst)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Returns the source spring of this stream.
     *
     * @return The spring.
     */
    public Spring<SOURCE> backToSource() {

        return mSpring;
    }

    @Override
    public int hashCode() {

        int result = mDownstreamPool != null ? mDownstreamPool.hashCode() : 0;
        result = 31 * result + mOutCurrent.hashCode();
        result = 31 * result + (mPassThrough ? 1 : 0);
        result = 31 * result + mSpring.hashCode();
        result = 31 * result + (mUpstreamPool != null ? mUpstreamPool.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof Stream)) {

            return false;
        }

        final Stream stream = (Stream) o;

        if (mPassThrough != stream.mPassThrough) {

            return false;
        }

        if ((mDownstreamPool != null) ? !mDownstreamPool.equals(stream.mDownstreamPool)
                : stream.mDownstreamPool != null) {

            return false;
        }

        if (!mOutCurrent.equals(stream.mOutCurrent)) {

            return false;
        }

        if (!mSpring.equals(stream.mSpring)) {

            return false;
        }

        //noinspection RedundantIfStatement
        if ((mUpstreamPool != null) ? !mUpstreamPool.equals(stream.mUpstreamPool) : stream.mUpstreamPool != null) {

            return false;
        }

        return true;
    }

    /**
     * Makes this stream fall through the specified dam.
     * <p/>
     * Before (where 'a' is this stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *
     *     </code>
     * </pre>
     * <p/>
     * After (where 'r' is the returned stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *           \
     *           (DAM)-r1-
     *
     *     </code>
     * </pre>
     *
     * @param dam    The dam.
     * @param <NOUT> The output data type.
     * @return A new stream flowing from the dam.
     */
    public <NOUT> Stream<SOURCE, OUT, NOUT> thenFallingThrough(final Dam<OUT, NOUT> dam) {

        if (dam == null) {

            throw new IllegalArgumentException("the dam cannot be null");
        }

        final DataPool<OUT, NOUT> outPool = new DataPool<OUT, NOUT>(mOutCurrent, dam);
        final Stream<SOURCE, IN, OUT> stream = new Stream<SOURCE, IN, OUT>(mSpring, mUpstreamPool, outPool);

        if (mUpstreamPool != null) {

            mUpstreamPool.outputStreams.add(stream);
        }

        outPool.inputStreams.add(stream);

        return new Stream<SOURCE, OUT, NOUT>(mSpring, outPool, (DataPool<NOUT, ?>) null);
    }

    /**
     * Feeds the specified stream with the data flowing into this one, and returns a new stream
     * originating from the same pool.
     * <p/>
     * Before (where 'a' is this stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *
     *         -()-b2-
     *
     *     </code>
     * </pre>
     * <p/>
     * After (where 'r' is the returned stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *           \
     *         --()-b2-
     *            \
     *              r1-
     *
     *     </code>
     * </pre>
     *
     * @param stream The stream to feed.
     * @param <NOUT> The transported data type of the target stream.
     * @return The new stream.
     */
    public <NOUT> Stream<SOURCE, OUT, NOUT> thenFeeding(final Stream<?, OUT, NOUT> stream) {

        thenFlowingInto(stream);

        return new Stream<SOURCE, OUT, NOUT>(mSpring, stream.mUpstreamPool, (DataPool<NOUT, ?>) null);
    }

    /**
     * Feeds the specified stream with the data flowing into this one, and returns it.
     * <p/>
     * Before (where 'a' is this stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *
     *         -()-b2-
     *
     *     </code>
     * </pre>
     * <p/>
     * After (where the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *           \
     *         --()-b2-
     *
     *     </code>
     * </pre>
     *
     * @param stream    The stream to feed.
     * @param <NSOURCE> The data type of the target stream spring.
     * @param <NOUT>    The transported data type of the target stream.
     * @return The target stream.
     */
    public <NSOURCE, NOUT> Stream<NSOURCE, OUT, NOUT> thenFlowingInto(final Stream<NSOURCE, OUT, NOUT> stream) {

        if (this == stream) {

            return stream;
        }

        final DataPool<IN, OUT> upPool = mUpstreamPool;

        final DataPool<OUT, ?> downPool = mDownstreamPool;

        if ((upPool != null) && (downPool == null) && !mOutCurrent.equals(upPool.inputCurrent)) {

            return thenFallingThrough(new OpenDam<OUT>()).thenFlowingInto(stream);
        }

        final DataPool<OUT, NOUT> pool = stream.mUpstreamPool;

        if (pool == null) {

            throw new DryStreamException("the target stream has no upstream pool and cannot be fed");
        }

        if (canReachFrom(pool, this)) {

            throw new ClosedLoopException("a stream closed loop has been detected during feeding");
        }

        final Stream<SOURCE, IN, OUT> feedingStream = new Stream<SOURCE, IN, OUT>(mSpring, upPool, pool);

        if (upPool != null) {

            upPool.outputStreams.add(feedingStream);
        }

        pool.inputStreams.add(feedingStream);

        return stream;
    }

    /**
     * Makes this stream run into the specified current.
     *
     * @param current The current instance.
     * @return A new stream running into the passed current.
     */
    public Stream<SOURCE, IN, OUT> thenFlowingInto(final Current current) {

        if (current == null) {

            throw new IllegalArgumentException("the current cannot be null");
        }

        return new Stream<SOURCE, IN, OUT>(mSpring, mUpstreamPool, current);
    }

    /**
     * Joins the specified stream with this one, and returns this stream.
     * <p/>
     * After the joining this stream pool will feed all the streams fed by the target stream pool.
     * <p/>
     * Before (where 'a' is this stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *
     *         -()-b2-()-
     *           \
     *             c2-()-
     *
     *     </code>
     * </pre>
     * <p/>
     * After (where the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         ------(=====)-a1-
     *                \    \
     *         -()-b2-()-   \
     *           \           \
     *             -------c2-()-
     *
     *     </code>
     * </pre>
     *
     * @param stream    The stream to join.
     * @param <NSOURCE> The data type of the target stream spring.
     * @param <NIN>     The data type of the target stream pool.
     * @return This stream.
     */
    public <NSOURCE, NIN> Stream<SOURCE, IN, OUT> thenJoining(final Stream<NSOURCE, NIN, OUT> stream) {

        thenJoiningInto(stream);

        return this;
    }

    /**
     * Joins the specified stream with this one, and returns it.
     * <p/>
     * After the joining this stream pool will feed all the streams fed by the target stream pool.
     * <p/>
     * Before (where 'a' is this stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *
     *         -()-b2-()-
     *           \
     *             c2-()-
     *
     *     </code>
     * </pre>
     * <p/>
     * After (where the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         ------(=====)-a1-
     *                \    \
     *         -()-b2-()-   \
     *           \           \
     *             -------c2-()-
     *
     *     </code>
     * </pre>
     *
     * @param stream    The stream to join.
     * @param <NSOURCE> The data type of the target stream spring.
     * @param <NIN>     The data type of the target stream pool.
     * @return The target stream.
     */
    public <NSOURCE, NIN> Stream<NSOURCE, NIN, OUT> thenJoiningInto(final Stream<NSOURCE, NIN, OUT> stream) {

        if (this == stream) {

            return stream;
        }

        final DataPool<IN, OUT> upPool = mUpstreamPool;

        final DataPool<OUT, ?> downPool = mDownstreamPool;

        if ((upPool != null) && (downPool == null) && !mOutCurrent.equals(upPool.inputCurrent)) {

            return thenFallingThrough(new OpenDam<OUT>()).thenJoiningInto(stream);
        }

        final Collection<? extends Stream<?, NIN, OUT>> outStreams;

        final DataPool<NIN, OUT> joiningUpPool = stream.mUpstreamPool;

        final DataPool<OUT, ?> joiningDownPool = stream.mDownstreamPool;

        if (joiningUpPool != null) {

            outStreams = joiningUpPool.outputStreams;

        } else {

            if (joiningDownPool == null) {

                throw new DryStreamException("cannot join the target stream since it has no downstream pool");
            }

            outStreams = Collections.singleton(stream);
        }

        for (final Stream<?, NIN, OUT> outStream : outStreams) {

            if (outStream.canReach(this)) {

                throw new ClosedLoopException("a stream closed loop has been detected during join");
            }
        }

        for (final Stream<?, ?, OUT> outStream : outStreams) {

            final DataPool<OUT, ?> outPool = outStream.mDownstreamPool;

            final Stream<SOURCE, IN, OUT> joiningStream = new Stream<SOURCE, IN, OUT>(mSpring, upPool, outPool);

            if (upPool != null) {

                upPool.outputStreams.add(joiningStream);
            }

            outPool.inputStreams.add(joiningStream);
        }

        return stream;
    }

    /**
     * Merges the specified stream with this one.
     * <p/>
     * Note that a new pool collecting all the data flowing through the merging streams is created
     * in the process.
     * <p/>
     * Before (where 'a' is this stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *
     *         -()-b2-
     *
     *     </code>
     * </pre>
     * <p/>
     * After (where 'r' is the returned stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-()-r1-
     *                /
     *         ------()-b2-
     *
     *     </code>
     * </pre>
     *
     * @param stream    The stream to merge.
     * @param <NSOURCE> The data type of the target stream spring.
     * @param <NIN>     The data type of the target stream pool.
     * @return A new stream fed by all the merging stream.
     */
    public <NSOURCE, NIN> Stream<SOURCE, OUT, OUT> thenMerging(final Stream<NSOURCE, NIN, OUT> stream) {

        return thenMergingThrough(new OpenDam<OUT>(), stream);
    }

    /**
     * Merges the specified streams with this one.
     * <p/>
     * Note that a new pool collecting all the data flowing through the merging streams is created
     * in the process.
     * <p/>
     * Before (where 'a' is this stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *
     *         -()-b2-
     *
     *         -()-*2-
     *
     *     </code>
     * </pre>
     * <p/>
     * After (where 'r' is the returned stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *           \
     *           (=========)-r1-
     *           /        /
     *         -()-b2-   /
     *                  /
     *         --------()-*2-
     *
     *     </code>
     * </pre>
     *
     * @param streams   The streams to merge.
     * @param <NSOURCE> The data type of the target stream spring.
     * @return A new stream fed by all the merging stream.
     */
    public <NSOURCE> Stream<SOURCE, OUT, OUT> thenMerging(final Stream<NSOURCE, ?, OUT>... streams) {

        return thenMergingThrough(new OpenDam<OUT>(), streams);
    }

    /**
     * Merges the streams returned by the specified iterable with this one.
     * <p/>
     * Note that a new pool collecting all the data flowing through the merging streams is created
     * in the process.
     * <p/>
     * Before (where 'a' is this stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *
     *         -()-b2-
     *
     *         -()-*2-
     *
     *     </code>
     * </pre>
     * <p/>
     * After (where 'r' is the returned stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *           \
     *           (=========)-r1-
     *           /        /
     *         -()-b2-   /
     *                  /
     *         --------()-*2-
     *
     *     </code>
     * </pre>
     *
     * @param streams   The iterable returning the streams to merge.
     * @param <NSOURCE> The data type of the target stream spring.
     * @return A new stream fed by all the merging streams.
     */
    public <NSOURCE> Stream<SOURCE, OUT, OUT> thenMerging(final Iterable<? extends Stream<NSOURCE, ?, OUT>> streams) {

        return thenMergingThrough(new OpenDam<OUT>(), streams);
    }

    /**
     * Merges the streams returned by the specified iterable with this one, through the specified
     * dam.
     * <p/>
     * Before (where 'a' is this stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *
     *         -()-b2-
     *
     *         -()-*2-
     *
     *     </code>
     * </pre>
     * <p/>
     * After (where 'r' is the returned stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *           \
     *           (===DAM===)-r1-
     *           /        /
     *         -()-b2-   /
     *                  /
     *         --------()-*2-
     *
     *     </code>
     * </pre>
     *
     * @param dam       The merging dam.
     * @param streams   The iterable returning the streams to merge.
     * @param <NSOURCE> The data type of the target stream spring.
     * @param <NOUT>    The data type of the returned stream.
     * @return A new stream fed by all the merging stream.
     */
    public <NSOURCE, NOUT> Stream<SOURCE, OUT, NOUT> thenMergingThrough(final Dam<OUT, NOUT> dam,
            final Iterable<? extends Stream<NSOURCE, ?, OUT>> streams) {

        final Stream<SOURCE, OUT, NOUT> resultStream = thenFallingThrough(dam);

        final DataPool<OUT, NOUT> downPool = resultStream.mUpstreamPool;

        for (final Stream<NSOURCE, ?, OUT> stream : streams) {

            if (this == stream) {

                continue;
            }

            //noinspection unchecked
            final DataPool<Object, OUT> upPool = (DataPool<Object, OUT>) stream.mUpstreamPool;

            final Stream<NSOURCE, Object, OUT> mergeStream =
                    new Stream<NSOURCE, Object, OUT>(stream.mSpring, upPool, downPool);

            if (upPool != null) {

                upPool.outputStreams.add(mergeStream);
            }

            downPool.inputStreams.add(mergeStream);
        }

        return resultStream;
    }

    /**
     * Merges the specified streams with this one, through the specified dam.
     * <p/>
     * Before (where 'a' is this stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *
     *         -()-b2-
     *
     *         -()-*2-
     *
     *     </code>
     * </pre>
     * <p/>
     * After (where 'r' is the returned stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *           \
     *           (===DAM===)-r1-
     *           /        /
     *         -()-b2-   /
     *                  /
     *         --------()-*2-
     *
     *     </code>
     * </pre>
     *
     * @param dam       The merging dam.
     * @param streams   The streams to merge.
     * @param <NSOURCE> The data type of the target stream spring.
     * @param <NOUT>    The data type of the returned stream.
     * @return A new stream fed by all the merging stream.
     */
    public <NSOURCE, NOUT> Stream<SOURCE, OUT, NOUT> thenMergingThrough(final Dam<OUT, NOUT> dam,
            final Stream<NSOURCE, ?, OUT>... streams) {

        final Stream<SOURCE, OUT, NOUT> resultStream = thenFallingThrough(dam);

        final DataPool<OUT, NOUT> downPool = resultStream.mUpstreamPool;

        for (final Stream<NSOURCE, ?, OUT> stream : streams) {

            if (this == stream) {

                continue;
            }

            //noinspection unchecked
            final DataPool<Object, OUT> upPool = (DataPool<Object, OUT>) stream.mUpstreamPool;

            final Stream<NSOURCE, Object, OUT> mergeStream =
                    new Stream<NSOURCE, Object, OUT>(stream.mSpring, upPool, downPool);

            if (upPool != null) {

                upPool.outputStreams.add(mergeStream);
            }

            downPool.inputStreams.add(mergeStream);
        }

        return resultStream;
    }

    /**
     * Merges the specified stream with this one, through the specified dam.
     * <p/>
     * Before (where 'a' is this stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *
     *         -()-b2-
     *
     *     </code>
     * </pre>
     * <p/>
     * After (where 'r' is the returned stream, and the number represents the type of source):
     * <pre>
     *     <code>
     *
     *         -()-a1-
     *           \
     *           (DAM)-r1-
     *           /
     *         -()-b2-
     *
     *     </code>
     * </pre>
     *
     * @param dam       The merging dam.
     * @param stream    The stream to merge.
     * @param <NSOURCE> The data type of the target stream spring.
     * @param <NOUT>    The data type of the returned stream.
     * @return A new stream fed by all the merging stream.
     */
    public <NSOURCE, NIN, NOUT> Stream<SOURCE, OUT, NOUT> thenMergingThrough(final Dam<OUT, NOUT> dam,
            final Stream<NSOURCE, NIN, OUT> stream) {

        final Stream<SOURCE, OUT, NOUT> resultStream = thenFallingThrough(dam);

        if (this == stream) {

            return resultStream;
        }

        final DataPool<OUT, NOUT> downPool = resultStream.mUpstreamPool;

        final DataPool<NIN, OUT> upPool = stream.mUpstreamPool;

        final Stream<NSOURCE, NIN, OUT> mergeStream = new Stream<NSOURCE, NIN, OUT>(stream.mSpring, upPool, downPool);

        if (upPool != null) {

            upPool.outputStreams.add(mergeStream);
        }

        downPool.inputStreams.add(mergeStream);

        return resultStream;
    }

    void discharge(final OUT drop) {

        final DataPool<OUT, ?> pool = mDownstreamPool;

        pool.incrementIdleCountdown(1);

        if (mPassThrough) {

            pool.discharge(drop);

        } else {

            final Current inputCurrent = pool.inputCurrent;

            inputCurrent.discharge(pool, drop);
        }
    }

    void dischargeAfter(final long delay, final TimeUnit timeUnit, final OUT drop) {

        final DataPool<OUT, ?> pool = mDownstreamPool;

        pool.incrementIdleCountdown(1);

        pool.inputCurrent.dischargeAfter(pool, delay, timeUnit, drop);
    }

    void dischargeAfter(final long delay, final TimeUnit timeUnit, final Iterable<? extends OUT> drops) {

        final DataPool<OUT, ?> pool = mDownstreamPool;

        int size = 0;

        for (final OUT ignored : drops) {

            ++size;
        }

        pool.incrementIdleCountdown(size);

        pool.inputCurrent.dischargeAfter(pool, delay, timeUnit, drops);
    }

    void drain(final boolean downstream) {

        final DryUpVisitor visitor = new DryUpVisitor(this);

        ride(downstream, visitor);

        visitor.dryUp();
    }

    void drop(final Object debris) {

        final DataPool<OUT, ?> pool = mDownstreamPool;

        pool.incrementIdleCountdown(1);

        if (mPassThrough) {

            pool.drop(debris);

        } else {

            final Current inputCurrent = pool.inputCurrent;

            inputCurrent.drop(pool, debris);
        }
    }

    void dropAfter(final long delay, final TimeUnit timeUnit, final Object debris) {

        final DataPool<OUT, ?> pool = mDownstreamPool;

        pool.incrementIdleCountdown(1);

        pool.inputCurrent.dropAfter(pool, delay, timeUnit, debris);
    }

    void flush() {

        final DataPool<OUT, ?> pool = mDownstreamPool;

        pool.waitIdle();

        if (mPassThrough) {

            pool.flush();

        } else {

            final Current inputCurrent = pool.inputCurrent;

            inputCurrent.flush(pool);
        }
    }

    private boolean canReach(final Stream<?, ?, ?> targetStream) {

        final ReachabilityVisitor visitor = new ReachabilityVisitor(targetStream);

        return !ride(true, visitor);
    }

    private void dryUp() {

        final DataPool<IN, OUT> upPool = mUpstreamPool;
        final DataPool<OUT, ?> downPool = mDownstreamPool;

        if (upPool != null) {

            upPool.outputStreams.remove(this);
        }

        downPool.inputStreams.remove(this);
    }

    private boolean ride(final boolean downstream, final WaterfallVisitor visitor) {

        if (!downstream && (mUpstreamPool == null)) {

            return true;
        }

        if (!visitor.visit(downstream, this)) {

            if (visitor.stopVisit()) {

                return false;
            }
        }

        final DataPool<?, ?> pool = downstream ? mDownstreamPool : mUpstreamPool;

        if (pool == null) {

            return true;
        }

        final CopyOnWriteArraySet<? extends Stream<?, ?, ?>> poolStreams =
                (downstream ? pool.outputStreams : pool.inputStreams);

        if (poolStreams.isEmpty()) {

            return true;
        }

        final ArrayList<Stream<?, ?, ?>> streamStack = new ArrayList<Stream<?, ?, ?>>();
        final ArrayList<Integer> indexStack = new ArrayList<Integer>();

        streamStack.add(this);
        indexStack.add(0);

        do {

            final Stream<?, ?, ?> currStream = streamStack.get(streamStack.size() - 1);
            final int currIndex = indexStack.get(indexStack.size() - 1);

            final CopyOnWriteArraySet<? extends Stream<?, ?, ?>> streams =
                    (downstream ? currStream.mDownstreamPool.outputStreams : currStream.mUpstreamPool.inputStreams);

            if (currIndex < streams.size()) {

                final boolean goOn;

                int i = 0;

                Stream<?, ?, ?> nextStream = null;

                for (final Stream<?, ?, ?> stream : streams) {

                    if (i++ == currIndex) {

                        nextStream = stream;

                        break;
                    }
                }

                if (nextStream == null) {

                    throw new ConcurrentModificationException();
                }

                if (!visitor.visit(downstream, nextStream)) {

                    if (visitor.stopVisit()) {

                        return false;
                    }

                    goOn = false;

                } else {

                    final DataPool<?, ?> nextPool = downstream ? nextStream.mDownstreamPool : nextStream.mUpstreamPool;

                    if (nextPool == null) {

                        goOn = false;

                    } else {

                        final CopyOnWriteArraySet<? extends Stream<?, ?, ?>> nextStreams =
                                (downstream ? pool.outputStreams : pool.inputStreams);

                        goOn = !nextStreams.isEmpty();
                    }
                }

                indexStack.set(indexStack.size() - 1, currIndex + 1);

                if (goOn) {

                    streamStack.add(nextStream);
                    indexStack.add(0);
                }

            } else {

                streamStack.remove(streamStack.size() - 1);
                indexStack.remove(indexStack.size() - 1);
            }

        } while (!streamStack.isEmpty());

        return true;
    }

    /**
     * Definition of a visitor of the waterfall streams.
     */
    private interface WaterfallVisitor {

        public boolean stopVisit();

        public boolean visit(boolean downstream, Stream<?, ?, ?> stream);
    }

    /**
     * Implementation of a {@link WaterfallVisitor} used to drain the streams up or down the
     * waterfall.
     */
    private static class DryUpVisitor implements WaterfallVisitor {

        private final HashSet<Stream<?, ?, ?>> mDryStreams = new HashSet<Stream<?, ?, ?>>();

        private final Stream<?, ?, ?> mOriginStream;

        public DryUpVisitor(final Stream<?, ?, ?> originStream) {

            mOriginStream = originStream;

            mDryStreams.add(originStream);
        }

        public void dryUp() {

            for (final Stream<?, ?, ?> stream : mDryStreams) {

                stream.dryUp();
            }
        }

        @Override
        public boolean stopVisit() {

            return false;
        }

        @Override
        public boolean visit(final boolean downstream, final Stream<?, ?, ?> stream) {

            if (stream == mOriginStream) {

                return true;
            }

            mDryStreams.add(stream);

            if (downstream) {

                return (stream.mDownstreamPool.inputStreams.size() < 2);
            }

            return (stream.mUpstreamPool == null) || (stream.mUpstreamPool.outputStreams.size() < 2);
        }
    }

    /**
     * Implementation of a {@link WaterfallVisitor} used to verify whether a target stream is
     * reachable.
     */
    private static class ReachabilityVisitor implements WaterfallVisitor {

        private final DataPool<?, ?> mUpstreamPool;

        public ReachabilityVisitor(final Stream<?, ?, ?> targetStream) {

            mUpstreamPool = targetStream.mUpstreamPool;
        }

        @Override
        public boolean stopVisit() {

            return true;
        }

        @Override
        public boolean visit(final boolean downstream, final Stream<?, ?, ?> stream) {

            return mUpstreamPool != stream.mDownstreamPool;
        }
    }
}