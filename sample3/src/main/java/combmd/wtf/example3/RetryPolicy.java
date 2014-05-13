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
package combmd.wtf.example3;

import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.src.Floodgate;

/**
 * This class is meant to retry the discharge of data in case an error occurred.
 *
 * @param <DATA> The data type.
 */
public class RetryPolicy<DATA> extends AbstractDam<DATA, DATA> {

    private final int mMaxCount;

    private int mCount;

    private DATA mData;

    public RetryPolicy(final int maxCount) {

        mMaxCount = maxCount;
    }

    @Override
    public Object onDischarge(final Floodgate<DATA, DATA> gate, final DATA drop) {

        // Reset the count and store the data drop for later

        mCount = 0;

        mData = drop;

        gate.discharge(drop);

        return null;
    }

    @Override
    public Object onPullDebris(final Floodgate<DATA, DATA> gate, final Object debris) {

        if (debris instanceof Throwable) {

            // This is an error, let's try again until we reach the max retry count

            if (mCount < mMaxCount) {

                ++mCount;

                gate.discharge(mData);

                return null;
            }
        }

        return super.onPullDebris(gate, debris);
    }
}