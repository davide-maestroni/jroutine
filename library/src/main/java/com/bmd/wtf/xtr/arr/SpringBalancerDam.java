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

import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;

import java.util.Collections;
import java.util.List;

/**
 * Dam wrapping an array balancer.
 * <p/>
 * Created by davide on 5/15/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
class SpringBalancerDam<IN, OUT> extends AbstractDam<IN, OUT> {

    private final ArrayBalancer<IN, OUT> mBalancer;

    private final Object mMutex;

    private final List<Spring<OUT>> mSprings;

    public SpringBalancerDam(final Object mutex, final ArrayBalancer<IN, OUT> balancer,
            final List<Spring<OUT>> springs) {

        if (mutex == null) {

            throw new IllegalArgumentException("the array balancer mutex cannot be null");
        }

        if (springs == null) {

            throw new IllegalArgumentException("the list of springs cannot be null");
        }

        if (balancer == null) {

            throw new IllegalArgumentException("the array balancer cannot be null");
        }

        mMutex = mutex;
        mSprings = Collections.unmodifiableList(springs);
        mBalancer = balancer;
    }

    @Override
    public Object onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

        synchronized (mMutex) {

            return mBalancer.onDischarge(mSprings, drop);
        }
    }

    @Override
    public Object onFlush(final Floodgate<IN, OUT> gate) {

        synchronized (mMutex) {

            return mBalancer.onFlush(mSprings);
        }
    }
}