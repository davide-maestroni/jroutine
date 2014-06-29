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
 * @param <DATA> The data type.
 */
class DataStream<DATA> implements Stream<DATA> {

    private final DataFall<?, DATA, ?> mDownstreamFall;

    private final Current mOutCurrent;

    private final boolean mPassThrough;

    private final DataFall<?, ?, DATA> mUpstreamFall;

    public DataStream(final DataFall<?, ?, DATA> upstreamFall,
            final DataFall<?, DATA, ?> downstreamFall) {

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
    public Stream<DATA> discharge() {

        final DataFall<?, DATA, ?> fall = mDownstreamFall;

        fall.waitDry(this);

        if (mPassThrough) {

            fall.discharge(this);

        } else {

            final Current inputCurrent = fall.inputCurrent;

            inputCurrent.discharge(fall, this);
        }

        return this;
    }

    @Override
    public Stream<DATA> forward(final Throwable throwable) {

        final DataFall<?, DATA, ?> fall = mDownstreamFall;

        fall.raiseLevel(1);

        if (mPassThrough) {

            fall.forward(throwable);

        } else {

            final Current inputCurrent = fall.inputCurrent;

            inputCurrent.forward(fall, throwable);
        }

        return this;
    }

    @Override
    public Stream<DATA> push(final DATA... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        final DataFall<?, DATA, ?> fall = mDownstreamFall;

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

        final DataFall<?, DATA, ?> fall = mDownstreamFall;

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

        final DataFall<?, DATA, ?> fall = mDownstreamFall;

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

        final DataFall<?, DATA, ?> fall = mDownstreamFall;

        int size = 0;

        for (final DATA ignored : drops) {

            ++size;
        }

        if (size > 0) {

            fall.raiseLevel(size);

            fall.inputCurrent.pushAfter(fall, delay, timeUnit, drops);
        }

        return this;
    }

    @Override
    public Stream<DATA> pushAfter(final long delay, final TimeUnit timeUnit, final DATA drop) {

        final DataFall<?, DATA, ?> fall = mDownstreamFall;

        fall.raiseLevel(1);

        fall.inputCurrent.pushAfter(fall, delay, timeUnit, drop);

        return this;
    }

    @Override
    public Stream<DATA> pushAfter(final long delay, final TimeUnit timeUnit, final DATA... drops) {

        if ((drops == null) || (drops.length == 0)) {

            return this;
        }

        final DataFall<?, DATA, ?> fall = mDownstreamFall;

        fall.raiseLevel(drops.length);

        fall.inputCurrent.pushAfter(fall, delay, timeUnit, Arrays.asList(drops));

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

    boolean canReach(final Collection<? extends DataFall<?, ?, ?>> falls) {

        final ReachabilityVisitor visitor = new ReachabilityVisitor(falls);

        return !ride(true, visitor);
    }

    void deviate() {

        mUpstreamFall.outputStreams.remove(this);
        mDownstreamFall.inputStreams.remove(this);
    }

    void drain(final boolean downstream) {

        final DrainVisitor visitor = new DrainVisitor();

        ride(downstream, visitor);

        visitor.drain();
    }

    private boolean ride(final boolean downstream, final WaterfallVisitor visitor) {

        if (!visitor.visit(downstream, this)) {

            return false;
        }

        final DataFall<?, ?, ?> fall = downstream ? mDownstreamFall : mUpstreamFall;

        final CopyOnWriteArrayList<? extends DataStream<?>> poolStreams =
                (downstream ? fall.outputStreams : fall.inputStreams);

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
                    (downstream ? currStream.mDownstreamFall.outputStreams
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

                if (!visitor.visit(downstream, nextStream)) {

                    if (visitor.stopVisit()) {

                        return false;
                    }

                    goOn = false;

                } else {

                    final CopyOnWriteArrayList<? extends DataStream<?>> nextStreams =
                            (downstream ? fall.outputStreams : fall.inputStreams);

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

        public boolean visit(boolean downstream, DataStream<?> stream);
    }

    /**
     * Implementation of a {@link WaterfallVisitor} used to drain the streams up or down the
     * waterfall.
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
        public boolean visit(final boolean downstream, final DataStream<?> stream) {

            mDryStreams.add(stream);

            if (downstream) {

                return (stream.mDownstreamFall.inputStreams.size() < 2);
            }

            return (stream.mUpstreamFall.outputStreams.size() < 2);
        }
    }

    /**
     * Implementation of a {@link WaterfallVisitor} used to verify whether a target stream is
     * reachable.
     */
    private static class ReachabilityVisitor implements WaterfallVisitor {

        private final Collection<? extends DataFall<?, ?, ?>> mFalls;

        public ReachabilityVisitor(final Collection<? extends DataFall<?, ?, ?>> falls) {

            mFalls = falls;
        }

        @Override
        public boolean visit(final boolean downstream, final DataStream<?> stream) {

            return !mFalls.contains(stream.mDownstreamFall);
        }

        @Override
        public boolean stopVisit() {

            return true;
        }
    }
}