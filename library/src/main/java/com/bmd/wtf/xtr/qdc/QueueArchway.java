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
import java.util.Iterator;
import java.util.List;
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

    private final ArrayList<DATA> mWaitingDrops = new ArrayList<DATA>();

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

            return mWaitingDrops.contains(drop);
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
    public void onDischarge(final Floodgate<DATA, DATA> gate, final List<Spring<DATA>> springs,
            final DATA drop) {

        synchronized (mMutex) {

            final ArrayList<DATA> pendingDrops = mWaitingDrops;

            if (mDropMap.size() >= springs.size()) {

                pendingDrops.add(drop);

            } else {

                discharge(springs, drop);
            }

            if (!pendingDrops.isEmpty()) {

                gate.redropAfter(0, TimeUnit.MILLISECONDS, mMutex);
            }
        }
    }

    @Override
    public void onDrop(final Floodgate<DATA, DATA> gate, final List<Spring<DATA>> springs,
            final Object debris) {

        synchronized (mMutex) {

            if (debris == mMutex) {

                final ArrayList<DATA> pendingDrops = mWaitingDrops;

                if (mDropMap.size() < springs.size()) {

                    if (!pendingDrops.isEmpty()) {

                        discharge(springs, pendingDrops.remove(0));
                    }
                }

                if (!pendingDrops.isEmpty()) {

                    gate.redropAfter(0, TimeUnit.MILLISECONDS, mMutex);
                }

            } else {

                super.onDrop(gate, springs, debris);
            }
        }
    }

    /**
     * Releases the specified level by making the related stream available again.
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

            return false;
        }
    }

    /**
     * Removes the specified data drop from the waiting queue is there.
     *
     * @param drop The data drop to remove.
     * @return Whether the specified drop was removed from the queue.
     */
    public boolean removeIfWaiting(final DATA drop) {

        synchronized (mMutex) {

            return mWaitingDrops.remove(drop);
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
}