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

import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.src.Floodgate;

/**
 * This class implements a {@link com.bmd.wtf.dam.Dam} internally wrapping a {@link Barrage}
 * instance.
 * <p/>
 * Created by davide on 3/3/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
class BarrageDam<IN, OUT> implements Dam<IN, OUT> {

    private final Barrage<IN, OUT> mBarrage;

    private final int mStreamNumber;

    /**
     * Creates a dam associated to the specified stream number, wrapping the specified barrage.
     *
     * @param streamNumber The number of the stream associated with the barrage.
     * @param barrage      The wrapped barrage.
     */
    public BarrageDam(final int streamNumber, final Barrage<IN, OUT> barrage) {

        if (barrage == null) {

            throw new IllegalArgumentException("the wrapped barrage cannot be null");
        }

        mStreamNumber = streamNumber;
        mBarrage = barrage;
    }

    @Override
    public Object onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

        synchronized (mBarrage) {

            return mBarrage.onDischarge(mStreamNumber, gate, drop);
        }
    }

    @Override
    public Object onFlush(final Floodgate<IN, OUT> gate) {

        synchronized (mBarrage) {

            return mBarrage.onFlush(mStreamNumber, gate);
        }
    }

    @Override
    public Object onPullDebris(final Floodgate<IN, OUT> gate, final Object debris) {

        synchronized (mBarrage) {

            return mBarrage.onPullDebris(mStreamNumber, gate, debris);
        }
    }

    @Override
    public Object onPushDebris(final Floodgate<IN, OUT> gate, final Object debris) {

        synchronized (mBarrage) {

            return mBarrage.onPushDebris(mStreamNumber, gate, debris);
        }
    }
}