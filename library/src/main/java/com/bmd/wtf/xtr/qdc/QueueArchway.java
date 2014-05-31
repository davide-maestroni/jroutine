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
package com.bmd.wtf.xtr.qdc;

import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of an {@link Archway} discharging data into the first available stream.
 * Exceeding drops are retained in an internal queue that is consumed only when one stream becomes
 * again available. Busy streams are made available by  explicitly calling the
 * {@link #consume(Object)} method.
 * <p/>
 * Created by davide on 5/15/14.
 *
 * @param <DATA> The data type.
 */
public class QueueArchway<DATA> extends AbstractArchway<DATA, DATA> {

    private final HashMap<DATA, Integer> mDropMap = new HashMap<DATA, Integer>();

    private final Object mMutex = new Object();

    private final ArrayList<Fluid<DATA>> mQueue = new ArrayList<Fluid<DATA>>();

    private final HashSet<DATA> mRchargeDrop = new HashSet<DATA>();

    private boolean mIsWaitingRedrop;

    /**
     * Signals to this archway that the specified drop has been consumed, so the related stream
     * can be made available again.
     *
     * @param drop The consumed data drop.
     * @return Whether the consuming stream was made available.
     */
    public boolean consume(final DATA drop) {

        synchronized (mMutex) {

            return (mDropMap.remove(drop) != null);
        }
    }

    /**
     * Checks if the specified data drop is in the queue waiting to be consumed.
     *
     * @param drop The data drop.
     * @return Whether the data drop is in the waiting queue.
     */
    public boolean isWaiting(final DATA drop) {

        synchronized (mMutex) {

            for (final Fluid<DATA> fluid : mQueue) {

                if (fluid.is(drop)) {

                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Gets the level of the stream consuming the specified drop.
     *
     * @param drop The data drop.
     * @return The stream level or <code>-1</code> if the drop is not being consumed.
     */
    public int levelOf(final DATA drop) {

        synchronized (mMutex) {

            final Integer level = mDropMap.get(drop);

            return (level != null) ? level : -1;
        }
    }

    @Override
    public void onDischarge(final Floodgate<DATA, DATA> gate, final List<Spring<DATA>> springs, final DATA drop) {

        synchronized (mMutex) {

            if (mDropMap.size() >= springs.size()) {

                mQueue.add(new Fluid<DATA>() {

                    @Override
                    public void flow(final Floodgate<DATA, DATA> gate, final List<Spring<DATA>> springs) {

                        onDischarge(gate, springs, drop);
                    }

                    @Override
                    public boolean is(final DATA other) {

                        return (drop == null) ? other == null : drop.equals(other);
                    }
                });

            } else {

                discharge(springs, drop);
            }
        }

        keepAlive(gate);
    }

    @Override
    public void onDrop(final Floodgate<DATA, DATA> gate, final List<Spring<DATA>> springs, final Object debris) {

        synchronized (mMutex) {

            if (debris == mMutex) {

                mIsWaitingRedrop = false;

                final HashMap<DATA, Integer> dropMap = mDropMap;

                for (final DATA drop : mRchargeDrop) {

                    final Integer level = dropMap.remove(drop);

                    if (level != null) {

                        springs.get(level).discharge(drop);
                    }
                }

                if (dropMap.size() < springs.size()) {

                    final ArrayList<Fluid<DATA>> queue = mQueue;

                    if (!queue.isEmpty()) {

                        queue.remove(0).flow(gate, springs);
                    }
                }

            } else if (!mDropMap.isEmpty()) {

                mQueue.add(new Fluid<DATA>() {

                    @Override
                    public void flow(final Floodgate<DATA, DATA> gate, final List<Spring<DATA>> springs) {

                        onDrop(gate, springs, debris);
                    }

                    @Override
                    public boolean is(final DATA drop) {

                        return false;
                    }
                });

            } else {

                super.onDrop(gate, springs, debris);
            }

            keepAlive(gate);
        }
    }

    @Override
    public void onFlush(final Floodgate<DATA, DATA> gate, final List<Spring<DATA>> springs) {

        if (!mDropMap.isEmpty()) {

            mQueue.add(new Fluid<DATA>() {

                @Override
                public void flow(final Floodgate<DATA, DATA> gate, final List<Spring<DATA>> springs) {

                    onFlush(gate, springs);
                }

                @Override
                public boolean is(final DATA drop) {

                    return false;
                }
            });

        } else {

            super.onFlush(gate, springs);
        }

        keepAlive(gate);
    }

    /**
     * Recharges the specified data drop into the same stream consuming it. If the drop is not
     * currently consumed nothing will happen.
     *
     * @param drop The data drop to recharge.
     * @return Whether the drop was currently being served.
     */

    public boolean recharge(final DATA drop) {

        synchronized (mMutex) {

            if (mDropMap.containsKey(drop)) {

                mRchargeDrop.add(drop);

                return true;
            }
        }

        return false;
    }

    /**
     * Refills the stream at the specified level by dropping again the same data drop being
     * consumed. If the stream is not currently busy nothing will happen.
     *
     * @param levelNumber The number of the level.
     * @return Whether the stream was currently busy.
     */
    public boolean refillLevel(final int levelNumber) {

        synchronized (mMutex) {

            for (final Entry<DATA, Integer> entry : mDropMap.entrySet()) {

                if (entry.getValue() == levelNumber) {

                    mRchargeDrop.add(entry.getKey());

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Releases the stream at the specified level by making the related stream available again.
     *
     * @param levelNumber The number of the level.
     * @return Whether the consuming stream was made available.
     */
    public boolean releaseLevel(final int levelNumber) {

        synchronized (mMutex) {

            final Iterator<Integer> iterator = mDropMap.values().iterator();

            while (iterator.hasNext()) {

                if (iterator.next() == levelNumber) {

                    iterator.remove();

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Removes the specified data drop from the waiting queue is there.
     *
     * @param drop The data drop to remove.
     * @return Whether the specified drop was removed from the queue.
     */
    public boolean removeIfWaiting(final DATA drop) {

        synchronized (mMutex) {

            final Iterator<Fluid<DATA>> iterator = mQueue.iterator();

            while (iterator.hasNext()) {

                if (iterator.next().is(drop)) {

                    iterator.remove();

                    return true;
                }
            }

            return false;
        }
    }

    private void discharge(final List<Spring<DATA>> springs, final DATA drop) {

        final HashMap<DATA, Integer> dropMap = mDropMap;

        final Collection<Integer> busyStreams = dropMap.values();

        final int size = springs.size();

        for (int i = 0; i < size; i++) {

            if (!busyStreams.contains(i)) {

                dropMap.put(drop, i);

                springs.get(i).discharge(drop);

                break;
            }
        }
    }

    private void keepAlive(final Floodgate<DATA, DATA> gate) {

        if (!mIsWaitingRedrop && !mDropMap.isEmpty() || !mQueue.isEmpty()) {

            mIsWaitingRedrop = true;

            gate.redropAfter(0, TimeUnit.MILLISECONDS, mMutex);
        }
    }

    private interface Fluid<DATA> {

        public void flow(Floodgate<DATA, DATA> gate, List<Spring<DATA>> springs);

        public boolean is(DATA drop);
    }
}