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
package com.bmd.wtf.example3;

import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.xtr.qdc.QueueArchway;

/**
 * This class is meant to retry the discharge of data in case an error occurred.
 *
 * @param <DATA> The data type.
 */
public class RetryPolicy<DATA> extends OpenDam<DATA> {

    private final QueueArchway<DATA> mArchway;

    private final int mMaxCount;

    private final int mStreamNumber;

    private int mCount;

    public RetryPolicy(final QueueArchway<DATA> archway, final int streamNumber,
            final int maxCount) {

        mArchway = archway;
        mStreamNumber = streamNumber;
        mMaxCount = maxCount;
    }

    @Override
    public void onDrop(final Floodgate<DATA, DATA> gate, final Object debris) {

        if (debris instanceof String) {

            // Reset the count and store the data drop for later

            mCount = 0;

        } else if (debris instanceof Throwable) {

            if (mCount++ < mMaxCount) {

                mArchway.refillLevel(mStreamNumber);

                return;
            }
        }

        super.onDrop(gate, debris);
    }
}