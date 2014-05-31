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
import com.bmd.wtf.src.Spring;

import java.util.HashMap;

/**
 * This class is meant to retry the discharge of data in case an error occurred.
 */
public class RetryPolicy extends OpenDam<String> {

    private final int mMaxCount;

    private final HashMap<String, Integer> mRetryCounts = new HashMap<String, Integer>();

    private final Spring<String> mSpring;

    public RetryPolicy(final Spring<String> spring, final int maxCount) {

        mSpring = spring;
        mMaxCount = maxCount;
    }

    @Override
    public void onDischarge(final Floodgate<String, String> gate, final String drop) {

        // Reset the count

        onReset(drop);

        super.onDischarge(gate, drop);
    }

    @Override
    public void onDrop(final Floodgate<String, String> gate, final Object debris) {

        if (debris instanceof Throwable) {

            final String url = ((Throwable) debris).getMessage();

            final Integer count = mRetryCounts.get(url);

            final int currentCount = (count != null) ? count : 0;

            if (currentCount < mMaxCount) {

                mRetryCounts.put(url, currentCount + 1);
                mSpring.discharge(url);

                return;
            }

            mRetryCounts.remove(url);
        }

        super.onDrop(gate, debris);
    }

    protected void onReset(final String url) {

        mRetryCounts.remove(url);
    }
}