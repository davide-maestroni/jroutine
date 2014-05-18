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
 */
class DebrisBalancerDam<DATA> extends OpenDam<DATA> {

    private final ArrayBalancer<DATA> mBalancer;

    private final int mStreamNumber;

    public DebrisBalancerDam(final ArrayBalancer<DATA> balancer, final int streamNumber) {

        if (balancer == null) {

            throw new IllegalArgumentException("the array balancer cannot be null");
        }

        mBalancer = balancer;
        mStreamNumber = streamNumber;
    }

    @Override
    public Object onPullDebris(final Floodgate<DATA, DATA> gate, final Object debris) {

        if (mBalancer.choosePulledDebrisStream(debris, mStreamNumber)) {

            return super.onPullDebris(gate, debris);
        }

        return null;
    }

    @Override
    public Object onPushDebris(final Floodgate<DATA, DATA> gate, final Object debris) {

        if (mBalancer.choosePushedDebrisStream(debris, mStreamNumber)) {

            return super.onPushDebris(gate, debris);
        }

        return null;
    }
}