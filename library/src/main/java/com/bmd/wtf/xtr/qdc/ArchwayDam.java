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
package com.bmd.wtf.xtr.qdc;

import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.src.Spring;

import java.util.Collections;
import java.util.List;

/**
 * This class implements a {@link com.bmd.wtf.dam.Dam} internally wrapping an {@link Archway} instance.
 * <p/>
 * Created by davide on 5/13/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
class ArchwayDam<IN, OUT> implements Dam<IN, OUT> {

    private final Archway<IN, OUT> mArchway;

    private final List<Spring<OUT>> mSprings;

    /**
     * Creates a dam wrapping the specified archway feeding the specified output springs.
     *
     * @param archway The wrapped archway.
     * @param springs The list output springs.
     */
    public ArchwayDam(final Archway<IN, OUT> archway, final List<Spring<OUT>> springs) {

        if (archway == null) {

            throw new IllegalArgumentException("the wrapped archway cannot be null");
        }

        mArchway = archway;
        mSprings = Collections.unmodifiableList(springs);
    }

    @Override
    public int hashCode() {

        return mArchway.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof ArchwayDam)) {

            return false;
        }

        final ArchwayDam archwayDam = (ArchwayDam) o;

        //noinspection RedundantIfStatement
        if (!mArchway.equals(archwayDam.mArchway)) {

            return false;
        }

        return true;
    }

    @Override
    public void onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

        final List<Spring<OUT>> springs = mSprings;

        try {

            mArchway.onDischarge(gate, springs, drop);

        } catch (final Throwable t) {

            for (final Spring<OUT> spring : springs) {

                spring.drop(t);
            }
        }
    }

    @Override
    public void onDrop(final Floodgate<IN, OUT> gate, final Object debris) {

        final List<Spring<OUT>> springs = mSprings;

        try {

            mArchway.onDrop(gate, mSprings, debris);

        } catch (final Throwable t) {

            for (final Spring<OUT> spring : springs) {

                spring.drop(t);
            }
        }
    }

    @Override
    public void onFlush(final Floodgate<IN, OUT> gate) {

        final List<Spring<OUT>> springs = mSprings;

        try {

            mArchway.onFlush(gate, mSprings);

        } catch (final Throwable t) {

            for (final Spring<OUT> spring : springs) {

                spring.drop(t);
            }
        }
    }
}