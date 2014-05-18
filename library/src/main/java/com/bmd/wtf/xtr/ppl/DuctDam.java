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
package com.bmd.wtf.xtr.ppl;

import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;

/**
 * This class implements a {@link com.bmd.wtf.dam.Dam} internally wrapping a {@link Duct} instance.
 * <p/>
 * Created by davide on 5/13/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
class DuctDam<IN, OUT> implements Dam<IN, OUT> {

    private final Duct<IN, OUT> mDuct;

    private final Spring<OUT> mSpring;

    /**
     * Creates a dam wrapping the specified duct feeding the specified output spring.
     *
     * @param duct   The wrapped duct.
     * @param spring The output spring.
     */
    public DuctDam(final Duct<IN, OUT> duct, final Spring<OUT> spring) {

        if (duct == null) {

            throw new IllegalArgumentException("the wrapped duct cannot be null");
        }

        if (spring == null) {

            throw new IllegalArgumentException("the output spring cannot be null");
        }

        mDuct = duct;
        mSpring = spring;
    }

    @Override
    public int hashCode() {

        return mDuct.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }
        if (!(o instanceof DuctDam)) {

            return false;
        }

        final DuctDam ductDam = (DuctDam) o;

        //noinspection RedundantIfStatement
        if (!mDuct.equals(ductDam.mDuct)) {

            return false;
        }

        return true;
    }

    @Override
    public Object onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

        return mDuct.onDischarge(mSpring, drop);
    }

    @Override
    public Object onFlush(final Floodgate<IN, OUT> gate) {

        return mDuct.onFlush(mSpring);
    }

    @Override
    public Object onPullDebris(final Floodgate<IN, OUT> gate, final Object debris) {

        return mDuct.onPullDebris(mSpring, debris);
    }

    @Override
    public Object onPushDebris(final Floodgate<IN, OUT> gate, final Object debris) {

        return mDuct.onPushDebris(mSpring, debris);
    }
}