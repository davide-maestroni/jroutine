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

import java.util.List;

/**
 * Dam wrapping an array balancer.
 * <p/>
 * Created by davide on 5/15/14.
 */
class SpringBalancerDam<DATA> extends AbstractDam<DATA, DATA> {

    private final ArrayBalancer<DATA> mBalancer;

    private final List<Spring<DATA>> mSprings;

    private final int mStreamsCount;

    public SpringBalancerDam(final ArrayBalancer<DATA> balancer, final int streamsCount,
            final List<Spring<DATA>> springs) {

        if (balancer == null) {

            throw new IllegalArgumentException("the array balancer cannot be null");
        }

        if (springs == null) {

            throw new IllegalArgumentException("the list of springs cannot be null");
        }

        if (streamsCount < 0) {

            throw new IllegalArgumentException("the total number of streams cannot be negative");
        }

        mBalancer = balancer;
        mStreamsCount = streamsCount;
        mSprings = springs;
    }

    @Override
    public Object onDischarge(final Floodgate<DATA, DATA> gate, final DATA drop) {

        final int streamNumber = mBalancer.chooseDataStream(drop, mStreamsCount);

        final List<Spring<DATA>> springs = mSprings;

        final int size = springs.size();

        if ((streamNumber >= 0) && (streamNumber < size)) {

            springs.get(streamNumber).discharge(drop);

        } else if (streamNumber == size) {

            for (final Spring<DATA> spring : springs) {

                spring.flush();
            }
        }

        return null;
    }

    @Override
    public Object onFlush(final Floodgate<DATA, DATA> gate) {

        final int streamNumber = mBalancer.chooseFlushStream(mStreamsCount);

        final List<Spring<DATA>> springs = mSprings;

        final int size = springs.size();

        if ((streamNumber >= 0) && (streamNumber < size)) {

            springs.get(streamNumber).flush();

        } else if (streamNumber == size) {

            for (final Spring<DATA> spring : springs) {

                spring.flush();
            }
        }

        return null;
    }
}