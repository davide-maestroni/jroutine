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
import com.bmd.wtf.flw.Stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * This class implements the connection between falls and it constitutes one of the fundamental
 * building piece which makes up the data waterfall.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <DATA> the data type.
 */
class DataStream<DATA> implements Stream<DATA> {

    private final DataFall<DATA, ?> mDownstreamFall;

    private final Current mOutCurrent;

    private final boolean mPassThrough;

    private final DataFall<?, DATA> mUpstreamFall;

    /**
     * Constructor.
     *
     * @param upstreamFall   the upstream fall.
     * @param downstreamFall the downstream fall.
     */
    public DataStream(final DataFall<?, DATA> upstreamFall,
            final DataFall<DATA, ?> downstreamFall) {

        if (upstreamFall == null) {

            throw new IllegalArgumentException("upstream fall cannot be null");
        }

        if (downstreamFall == null) {

            throw new IllegalArgumentException("downstream fall cannot be null");
        }

        mUpstreamFall = upstreamFall;
        mDownstreamFall = downstreamFall;

        mOutCurrent = downstreamFall.inputCurrent;
        mPassThrough = upstreamFall.inputCurrent.equals(mOutCurrent);
    }

    @Override
    public Stream<DATA> exception(final Throwable throwable) {

        final DataFall<DATA, ?> fall = mDownstreamFall;

        fall.raiseLevel(1);

        if (mPassThrough) {

            fall.exception(throwable);

        } else {

            final Current inputCurrent = fall.inputCurrent;

            inputCurrent.exception(fall, throwable);
        }

        return this;
    }

    @Override
    public Stream<DATA> flush() {

        final DataFall<DATA, ?> fall = mDownstreamFall;

        if (mPassThrough) {

            fall.flush(this);

        } else {

            final Current inputCurrent = fall.inputCurrent;

            inputCurrent.flush(fall, this);
        }

        return this;
    }

    @Override
    public Stream<DATA> flush(final DATA... drops) {

        return push(drops).flush();
    }

    @Override
    public Stream<DATA> flush(final Iterable<? extends DATA> drops) {

        return push(drops).flush();
    }

    @Override
    public Stream<DATA> flush(final DATA drop) {

        return push(drop).flush();
    }

    @Override
    public Stream<DATA> flushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        return pushAfter(delay, timeUnit, drops).flush();
    }

    @Override
    public Stream<DATA> flushAfter(final long delay, final TimeUnit timeUnit, final DATA drop) {

        return pushAfter(delay, timeUnit, drop).flush();
    }

    @Override
    public Stream<DATA> flushAfter(final long delay, final TimeUnit timeUnit, final DATA... drops) {

        return pushAfter(delay, timeUnit, drops).flush();
    }

    @Override
    public Stream<DATA> push(final DATA... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        final DataFall<DATA, ?> fall = mDownstreamFall;

        fall.raiseLevel(drops.length);

        if (mPassThrough) {

            for (final DATA drop : drops) {

                fall.push(drop);
            }

        } else {

            final Current inputCurrent = fall.inputCurrent;

            for (final DATA drop : drops) {

                inputCurrent.push(fall, drop);
            }
        }

        return this;
    }

    @Override
    public Stream<DATA> push(final Iterable<? extends DATA> drops) {

        if (drops == null) {

            return this;
        }

        final DataFall<DATA, ?> fall = mDownstreamFall;

        int size = 0;

        for (final DATA ignored : drops) {

            ++size;
        }

        if (size > 0) {

            fall.raiseLevel(size);

            if (mPassThrough) {

                for (final DATA drop : drops) {

                    fall.push(drop);
                }

            } else {

                final Current inputCurrent = fall.inputCurrent;

                for (final DATA drop : drops) {

                    inputCurrent.push(fall, drop);
                }
            }
        }

        return this;
    }

    @Override
    public Stream<DATA> push(final DATA drop) {

        final DataFall<DATA, ?> fall = mDownstreamFall;

        fall.raiseLevel(1);

        if (mPassThrough) {

            fall.push(drop);

        } else {

            fall.inputCurrent.push(fall, drop);
        }

        return this;
    }

    @Override
    public Stream<DATA> pushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        if (drops == null) {

            return this;
        }

        final DataFall<DATA, ?> fall = mDownstreamFall;
        final ArrayList<DATA> list = new ArrayList<DATA>();

        for (final DATA drop : drops) {

            list.add(drop);
        }

        if (!list.isEmpty()) {

            fall.raiseLevel(list.size());

            fall.inputCurrent.pushAfter(fall, delay, timeUnit, list);
        }

        return this;
    }

    @Override
    public Stream<DATA> pushAfter(final long delay, final TimeUnit timeUnit, final DATA drop) {

        final DataFall<DATA, ?> fall = mDownstreamFall;

        fall.raiseLevel(1);

        fall.inputCurrent.pushAfter(fall, delay, timeUnit, drop);

        return this;
    }

    @Override
    public Stream<DATA> pushAfter(final long delay, final TimeUnit timeUnit, final DATA... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        final DataFall<DATA, ?> fall = mDownstreamFall;

        fall.raiseLevel(drops.length);

        fall.inputCurrent.pushAfter(fall, delay, timeUnit,
                                    new ArrayList<DATA>(Arrays.asList(drops)));

        return this;
    }

    @Override
    public int hashCode() {

        int result = mDownstreamFall.hashCode();
        result = 31 * result + mOutCurrent.hashCode();
        result = 31 * result + (mPassThrough ? 1 : 0);
        result = 31 * result + mUpstreamFall.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof DataStream)) {

            return false;
        }

        final DataStream stream = (DataStream) o;

        if (mPassThrough != stream.mPassThrough) {

            return false;
        }

        if (!mDownstreamFall.equals(stream.mDownstreamFall)) {

            return false;
        }

        if (!mOutCurrent.equals(stream.mOutCurrent)) {

            return false;
        }

        //noinspection RedundantIfStatement
        if (!mUpstreamFall.equals(stream.mUpstreamFall)) {

            return false;
        }

        return true;
    }

    /**
     * Checks if this stream can reach, directly or though other streams, the specified fall.
     *
     * @param fall the target fall.
     * @return whether this stream can reach the target fall.
     */
    boolean canReach(final Collection<? extends DataFall<?, ?>> fall) {

        final ReachabilityVisitor visitor = new ReachabilityVisitor(fall);

        return !ride(Direction.DOWNSTREAM, visitor);
    }

    /**
     * Deviates the flow of this stream.
     *
     * @see com.bmd.wtf.flw.River#deviate()
     */
    void deviate() {

        mUpstreamFall.outputStreams.remove(this);
        mDownstreamFall.inputStreams.remove(this);
    }

    /**
     * Drains the stream.
     *
     * @param direction the direction of the drain.
     * @see com.bmd.wtf.flw.River#drain()
     */
    void drain(final Direction direction) {

        final DrainVisitor visitor = new DrainVisitor();

        ride(direction, visitor);

        visitor.drain();
    }

    private boolean ride(final Direction direction, final WaterfallVisitor visitor) {

        if (!visitor.visit(direction, this)) {

            return false;
        }

        final boolean isDownstream = (direction == Direction.DOWNSTREAM);

        final DataFall<?, ?> fall = isDownstream ? mDownstreamFall : mUpstreamFall;
        final CopyOnWriteArrayList<? extends DataStream<?>> poolStreams =
                (isDownstream ? fall.outputStreams : fall.inputStreams);

        if (poolStreams.isEmpty()) {

            return true;
        }

        final ArrayList<DataStream<?>> streamStack = new ArrayList<DataStream<?>>();
        final ArrayList<Integer> indexStack = new ArrayList<Integer>();

        streamStack.add(this);
        indexStack.add(0);

        do {

            final DataStream<?> currStream = streamStack.get(streamStack.size() - 1);
            final int currIndex = indexStack.get(indexStack.size() - 1);

            final CopyOnWriteArrayList<? extends DataStream<?>> streams =
                    (isDownstream ? currStream.mDownstreamFall.outputStreams
                            : currStream.mUpstreamFall.inputStreams);

            int i = 0;

            DataStream<?> nextStream = null;

            for (final DataStream<?> stream : streams) {

                if (i++ == currIndex) {

                    nextStream = stream;

                    break;
                }
            }

            if (nextStream != null) {

                final boolean goOn;

                if (!visitor.visit(direction, nextStream)) {

                    if (visitor.stopVisit()) {

                        return false;
                    }

                    goOn = false;

                } else {

                    final CopyOnWriteArrayList<? extends DataStream<?>> nextStreams =
                            (isDownstream ? fall.outputStreams : fall.inputStreams);

                    goOn = !nextStreams.isEmpty();
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

        public boolean visit(Direction direction, DataStream<?> stream);
    }

    /**
     * Implementation of a waterfall visitor used to drain the streams up or down the waterfall.
     */
    private static class DrainVisitor implements WaterfallVisitor {

        private final HashSet<DataStream<?>> mDryStreams = new HashSet<DataStream<?>>();

        public void drain() {

            for (final DataStream<?> stream : mDryStreams) {

                stream.deviate();
            }
        }

        @Override
        public boolean stopVisit() {

            return false;
        }

        @Override
        public boolean visit(final Direction direction, final DataStream<?> stream) {

            mDryStreams.add(stream);

            if (direction == Direction.DOWNSTREAM) {

                return (stream.mDownstreamFall.inputStreams.size() < 2);
            }

            return (stream.mUpstreamFall.outputStreams.size() < 2);
        }
    }

    /**
     * Implementation of a waterfall visitor used to verify whether a target stream is reachable.
     */
    private static class ReachabilityVisitor implements WaterfallVisitor {

        private final Collection<? extends DataFall<?, ?>> mFalls;

        public ReachabilityVisitor(final Collection<? extends DataFall<?, ?>> falls) {

            mFalls = falls;
        }

        @Override
        public boolean visit(final Direction direction, final DataStream<?> stream) {

            return !mFalls.contains(stream.mDownstreamFall);
        }

        @Override
        public boolean stopVisit() {

            return true;
        }
    }
}