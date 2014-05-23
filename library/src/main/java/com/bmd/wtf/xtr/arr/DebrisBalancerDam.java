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

import com.bmd.wtf.dam.OpenDam;
import com.bmd.wtf.src.Floodgate;

/**
 * Dam handling debris flowing through an array balancer.
 * <p/>
 * Created by davide on 5/15/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
class DebrisBalancerDam<IN, OUT> extends OpenDam<OUT> {

    private final ArrayBalancer<IN, OUT> mBalancer;

    private final Object mMutex;

    private final int mStreamNumber;

    public DebrisBalancerDam(final Object mutex, final int streamNumber,
            final ArrayBalancer<IN, OUT> balancer) {

        if (mutex == null) {

            throw new IllegalArgumentException("the array balancer mutex cannot be null");
        }

        if (balancer == null) {

            throw new IllegalArgumentException("the array balancer cannot be null");
        }

        mMutex = mutex;
        mStreamNumber = streamNumber;
        mBalancer = balancer;
    }

    @Override
    public Object onPullDebris(final Floodgate<OUT, OUT> gate, final Object debris) {

        synchronized (mMutex) {

            return mBalancer.onPullDebris(mStreamNumber, gate, debris);
        }
    }

    @Override
    public Object onPushDebris(final Floodgate<OUT, OUT> gate, final Object debris) {

        synchronized (mMutex) {

            return mBalancer.onPushDebris(mStreamNumber, gate, debris);
        }
    }
}