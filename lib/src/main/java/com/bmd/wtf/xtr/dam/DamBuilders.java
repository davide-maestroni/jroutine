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
package com.bmd.wtf.xtr.dam;

import com.bmd.wtf.dam.AbstractDam;
import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.dam.Dams;
import com.bmd.wtf.src.Floodgate;
import com.bmd.wtf.xtr.dam.DamBuilder.DischargeHandler;

/**
 * Utility class for {@link com.bmd.wtf.dam.Dam} instances extending the
 * {@link com.bmd.wtf.dam.Dams} one.
 * <p/>
 * Created by davide on 4/6/14.
 */
public class DamBuilders extends Dams {

    /**
     * Protected constructor to avoid direct instantiation.
     */
    protected DamBuilders() {

    }

    /**
     * Creates a new dam preventing debris from being pulled upstream, from the specified handler.
     *
     * @param handler The discharge handler.
     * @param <IN>    The input data type.
     * @param <OUT>   The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> downstreamDebris(final DischargeHandler<IN, OUT> handler) {

        return downstreamDebris(simpleDam(handler));
    }

    /**
     * Creates a new dam preventing debris from being pulled upstream and <code>null</code> data
     * to be discharged, from the specified handler.
     *
     * @param handler The discharge handler.
     * @param <IN>    The input data type.
     * @param <OUT>   The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> downstreamDebrisAvoidingNull(
            final DischargeHandler<IN, OUT> handler) {

        return downstreamDebris(simpleDamAvoidingNull(handler));
    }

    /**
     * Creates a new dam preventing debris from being pushed downstream or pulled upstream, from
     * the specified handler.
     *
     * @param handler The discharge handler.
     * @param <IN>    The input data type.
     * @param <OUT>   The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> noDebris(final DischargeHandler<IN, OUT> handler) {

        return noDebris(simpleDam(handler));
    }

    /**
     * Creates a new dam preventing debris from being pushed downstream or pulled upstream and
     * <code>null</code> data to be discharged, from the specified handler.
     *
     * @param handler The discharge handler.
     * @param <IN>    The input data type.e
     * @param <OUT>   The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> noDebrisAvoidingNull(
            final DischargeHandler<IN, OUT> handler) {

        return noDebris(simpleDamAvoidingNull(handler));
    }

    /**
     * Creates a new dam from the specified handler.
     *
     * @param handler The discharge handler.
     * @param <IN>    The input data type.
     * @param <OUT>   The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> simpleDam(final DischargeHandler<IN, OUT> handler) {

        if (handler == null) {

            throw new IllegalArgumentException();
        }

        return new AbstractDam<IN, OUT>() {

            @Override
            public Object onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

                gate.discharge(handler.onDischarge(drop));

                return null;
            }
        };
    }

    /**
     * Creates a new dam avoiding <code>null</code> data to be discharged, from the specified
     * handler.
     *
     * @param handler The discharge handler.
     * @param <IN>    The input data type.
     * @param <OUT>   The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> simpleDamAvoidingNull(
            final DischargeHandler<IN, OUT> handler) {

        if (handler == null) {

            throw new IllegalArgumentException();
        }

        return new AbstractDam<IN, OUT>() {

            @Override
            public Object onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

                final OUT out = handler.onDischarge(drop);

                if (out != null) {

                    gate.discharge(out);
                }

                return null;
            }
        };
    }

    /**
     * Creates a new dam preventing debris from being pushed downstream, by wrapping the specified
     * one.
     *
     * @param handler The discharge handler.
     * @param <IN>    The input data type.
     * @param <OUT>   The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> upstreamDebris(final DischargeHandler<IN, OUT> handler) {

        return upstreamDebris(simpleDam(handler));
    }

    /**
     * Creates a new dam preventing debris from being pushed downstream and <code>null</code> data
     * to be discharged, from the specified handler.
     *
     * @param handler The discharge handler.
     * @param <IN>    The input data type.
     * @param <OUT>   The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> upstreamDebrisAvoidingNull(
            final DischargeHandler<IN, OUT> handler) {

        return upstreamDebris(simpleDamAvoidingNull(handler));
    }
}