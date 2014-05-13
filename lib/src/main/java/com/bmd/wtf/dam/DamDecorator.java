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
package com.bmd.wtf.dam;

import com.bmd.wtf.src.Floodgate;

/**
 * {@link com.bmd.wtf.dam.Dam} decorator class.
 * <p/>
 * Created by davide on 3/7/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public class DamDecorator<IN, OUT> implements Dam<IN, OUT> {

    private final Dam<IN, OUT> mDam;

    /**
     * Default constructor.
     *
     * @param wrapped The wrapped dam.
     */
    public DamDecorator(final Dam<IN, OUT> wrapped) {

        if (wrapped == null) {

            throw new IllegalArgumentException();
        }

        mDam = wrapped;
    }

    @Override
    public int hashCode() {

        return mDam.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {

            return true;
        }

        //noinspection SimplifiableIfStatement
        if (!(obj instanceof DamDecorator)) {

            return false;
        }

        return mDam.equals(((DamDecorator) obj).mDam);
    }

    @Override
    public Object onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

        return mDam.onDischarge(gate, drop);
    }

    @Override
    public Object onFlush(final Floodgate<IN, OUT> gate) {

        return mDam.onFlush(gate);
    }

    @Override
    public Object onPullDebris(final Floodgate<IN, OUT> gate, final Object debris) {

        return mDam.onPullDebris(gate, debris);
    }

    @Override
    public Object onPushDebris(final Floodgate<IN, OUT> gate, final Object debris) {

        return mDam.onPushDebris(gate, debris);
    }

    /**
     * Returns the wrapped dam.
     *
     * @return The wrapped instance.
     */
    protected Dam<IN, OUT> wrapped() {

        return mDam;
    }
}