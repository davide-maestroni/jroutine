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
 * A {@link Dam} decorator which prevents debris from being pushed upstream and downstream.
 * <p/>
 * Created by davide on 3/11/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public class NoDebrisDam<IN, OUT> extends DamDecorator<IN, OUT> {

    /**
     * Default constructor.
     *
     * @param wrapped The wrapped dam.
     */
    public NoDebrisDam(final Dam<IN, OUT> wrapped) {

        super(wrapped);
    }

    @Override
    public Object onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

        try {

            super.onDischarge(gate, drop);

        } catch (final Throwable t) {

            // Ignore it
        }

        return null;
    }

    @Override
    public Object onFlush(final Floodgate<IN, OUT> gate) {

        try {

            super.onFlush(gate);

        } catch (final Throwable t) {

            // Ignore it
        }

        return null;
    }

    @Override
    public Object onPullDebris(final Floodgate<IN, OUT> gate, final Object debris) {

        try {

            super.onPullDebris(gate, debris);

        } catch (final Throwable t) {

            // Ignore it
        }

        return null;
    }

    @Override
    public Object onPushDebris(final Floodgate<IN, OUT> gate, final Object debris) {

        try {

            super.onPushDebris(gate, debris);

        } catch (final Throwable t) {

            // Ignore it
        }

        return null;
    }
}