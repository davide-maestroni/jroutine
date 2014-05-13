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
package com.bmd.wtf.example2;

import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.xtr.arr.AbstractBarrage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * This class is meant to balance data flow by serving data drops to available streams.
 *
 * @param <DATA> The data type.
 */
public class Balancer<DATA> extends AbstractBarrage<DATA, DATA> {

    private final int mMaxStreams;

    private final ArrayList<DATA> mPending = new ArrayList<DATA>();

    private final HashMap<Integer, DATA> mServed = new HashMap<Integer, DATA>();

    public Balancer(final int maxStreams) {

        mMaxStreams = maxStreams;
    }

    @Override
    public Object onDischarge(final int streamNumber, final Floodgate<DATA, DATA> gate,
            final DATA drop) {

        if (mServed.containsValue(drop)) {

            // We are already serving it

            return null;
        }

        if (mServed.size() >= mMaxStreams) {

            // All streams are busy, let's store it for later

            if (!mPending.contains(drop)) {

                mPending.add(drop);
            }

        } else if (!mServed.containsKey(streamNumber)) {

            // The stream is available, let's discharge the data drop

            mServed.put(streamNumber, drop);

            gate.discharge(drop);
        }

        return null;
    }

    @Override
    public Object onPullDebris(final int streamNumber, final Floodgate<DATA, DATA> gate,
            final Object debris) {

        // The processing either failed or completed, let's make the stream available again

        mServed.remove(streamNumber);

        if (!mPending.isEmpty()) {

            // Let's discharge a pending data drop, if any

            onDischarge(streamNumber, gate, mPending.remove(0));
        }

        return super.onPullDebris(streamNumber, gate, debris);
    }

    protected Collection<DATA> getPending() {

        return mPending;
    }
}