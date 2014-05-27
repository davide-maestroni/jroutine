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
 * {@link Dam} decorator class.
 * <p/>
 * Created by davide on 3/7/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public class DamDecorator<IN, OUT> implements Dam<IN, OUT> {

    private final Dam<IN, OUT> mDam;

    /**
     * Creates a new dam wrapping the specified one.
     *
     * @param wrapped The wrapped dam.
     */
    public DamDecorator(final Dam<IN, OUT> wrapped) {

        if (wrapped == null) {

            throw new IllegalArgumentException("wrapped dam cannot be null");
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
    public void onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

        mDam.onDischarge(gate, drop);
    }

    @Override
    public void onDrop(final Floodgate<IN, OUT> gate, final Object debris) {

        mDam.onDrop(gate, debris);
    }

    @Override
    public void onFlush(final Floodgate<IN, OUT> gate) {

        mDam.onFlush(gate);
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