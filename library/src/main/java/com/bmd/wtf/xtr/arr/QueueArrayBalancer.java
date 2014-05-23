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
package com.bmd.wtf.xtr.arr;

import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Implementation of an {@link com.bmd.wtf.xtr.arr.ArrayBalancer} discharging data only into the
 * first available stream. Exceeding drops are retained in a queue that is consumed only when one
 * stream becomes again available. Busy streams are made available when an object, which
 * <code>equals</code> the discharged drop, is pulled upstream.
 * <p/>
 * Created by davide on 5/15/14.
 *
 * @param <DATA> The data type.
 */
public class QueueArrayBalancer<DATA> extends AbstractArrayBalancer<DATA, DATA> {

    private final HashMap<DATA, Integer> mDropMap = new HashMap<DATA, Integer>();

    private final ArrayList<DATA> mPendingDrops = new ArrayList<DATA>();

    @Override
    public Object onDischarge(final List<Spring<DATA>> springs, final DATA drop) {

        final int size = springs.size();

        if (mDropMap.size() >= size) {

            mPendingDrops.add(drop);
        }

        final Collection<Integer> busyStreams = mDropMap.values();

        for (int i = 0; i < size; i++) {

            if (!busyStreams.contains(i)) {

                mDropMap.put(drop, i);

                springs.get(i).discharge(drop);

                return null;
            }
        }

        return null;
    }

    @Override
    public Object onPullDebris(final int streamNumber, final Floodgate<DATA, DATA> gate,
            final Object debris) {

        final HashMap<DATA, Integer> dropMap = mDropMap;

        final ArrayList<DATA> pendingDrops = mPendingDrops;

        //noinspection SuspiciousMethodCalls
        if ((dropMap.remove(debris) != null) && !pendingDrops.isEmpty()) {

            final DATA drop = pendingDrops.remove(0);

            dropMap.put(drop, streamNumber);

            gate.discharge(drop);
        }

        return super.onPullDebris(streamNumber, gate, debris);
    }
}