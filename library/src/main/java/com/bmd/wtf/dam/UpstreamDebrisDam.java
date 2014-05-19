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
 * A {@link Dam} decorator which prevents debris from flowing further downstream.
 * <p/>
 * Created by davide on 3/11/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public class UpstreamDebrisDam<IN, OUT> extends DamDecorator<IN, OUT> {

    /**
     * Constructor.
     *
     * @param wrapped The wrapped dam.
     */
    public UpstreamDebrisDam(final Dam<IN, OUT> wrapped) {

        super(wrapped);
    }

    @Override
    public Object onPushDebris(final Floodgate<IN, OUT> gate, final Object debris) {

        try {

            super.onPushDebris(gate, debris);

        } catch (final Throwable ignored) {

        }

        return null;
    }
}