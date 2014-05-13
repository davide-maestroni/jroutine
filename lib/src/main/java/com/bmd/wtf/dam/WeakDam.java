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

import java.lang.ref.WeakReference;

/**
 * {@link Dam} decorator which retains a weak reference of the wrapped instance.
 * <p/>
 * Created by davide on 3/6/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public class WeakDam<IN, OUT> implements Dam<IN, OUT> {

    private final boolean mCloseWhenNull;

    private final WeakReference<Dam<IN, OUT>> mDam;

    /**
     * Default constructor.
     *
     * @param dam The wrapped dam.
     */
    public WeakDam(final Dam<IN, OUT> dam) {

        this(dam, false);
    }

    /**
     * Parametrized constructor.
     *
     * @param dam           The wrapped dam.
     * @param closeWhenNull Whether this instance must behave as a
     *                      {@link com.bmd.wtf.dam.ClosedDam} when the wrapped instance is garbage
     *                      collected.
     */
    public WeakDam(final Dam<IN, OUT> dam, final boolean closeWhenNull) {

        mDam = new WeakReference<Dam<IN, OUT>>(dam);
        mCloseWhenNull = closeWhenNull;
    }

    @Override
    public int hashCode() {

        int result = (mCloseWhenNull ? 1 : 0);

        final Dam dam = mDam.get();

        if (dam != null) {

            result = 31 * result + dam.hashCode();
        }

        return result;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof WeakDam)) {

            return false;
        }

        final WeakDam weakDam = (WeakDam) o;

        if (mCloseWhenNull != weakDam.mCloseWhenNull) {

            return false;
        }

        final Dam dam = mDam.get();

        if (dam == null) {

            return (weakDam.mDam.get() == null);
        }

        //noinspection RedundantIfStatement
        if (!dam.equals(weakDam.mDam.get())) {

            return false;
        }

        return true;
    }

    @Override
    public Object onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

        final Dam<IN, OUT> dam = mDam.get();

        if (dam != null) {

            return dam.onDischarge(gate, drop);
        }

        return null;
    }

    @Override
    public Object onFlush(final Floodgate<IN, OUT> gate) {

        final Dam<IN, OUT> dam = mDam.get();

        if (dam != null) {

            return dam.onFlush(gate);

        } else if (!mCloseWhenNull) {

            gate.flush();
        }

        return null;
    }

    @Override
    public Object onPullDebris(final Floodgate<IN, OUT> gate, final Object debris) {

        final Dam<IN, OUT> dam = mDam.get();

        if (dam != null) {

            return dam.onPullDebris(gate, debris);
        }

        return (!mCloseWhenNull) ? debris : null;
    }

    @Override
    public Object onPushDebris(final Floodgate<IN, OUT> gate, final Object debris) {

        final Dam<IN, OUT> dam = mDam.get();

        if (dam != null) {

            return dam.onPushDebris(gate, debris);

        }

        return (!mCloseWhenNull) ? debris : null;
    }
}