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

/**
 * Utility class for creating {@link Dam} instances.
 * <p/>
 * Created by davide on 4/6/14.
 */
public class Dams {

    /**
     * Protected constructor to avoid direct instantiation.
     */
    protected Dams() {

    }

    /**
     * Creates a new dam preventing any data drop or object from flowing through it.
     *
     * @param <IN>  The input data type.
     * @param <OUT> The output data type.
     * @return The closed dam.
     */
    public static <IN, OUT> Dam<IN, OUT> closedDam() {

        return new ClosedDam<IN, OUT>();
    }

    /**
     * Creates a new dam preventing debris from being pulled further upstream, by wrapping the
     * specified one.
     *
     * @param dam   The dam to wrap.
     * @param <IN>  The input data type.
     * @param <OUT> The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> downstreamDebris(final Dam<IN, OUT> dam) {

        return new DownstreamDebrisDam<IN, OUT>(dam);
    }

    /**
     * Creates a new dam preventing debris from being pushed downstream or pulled upstream, by
     * wrapping the specified one.
     *
     * @param dam   The dam to wrap.
     * @param <IN>  The input data type.
     * @param <OUT> The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> noDebris(final Dam<IN, OUT> dam) {

        return new NoDebrisDam<IN, OUT>(dam);
    }

    /**
     * Creates a new dam which let any data drop and object flow through it.
     *
     * @param <DATA> The data type.
     * @return The open dam.
     */
    public static <DATA> Dam<DATA, DATA> openDam() {

        return new OpenDam<DATA>();
    }

    /**
     * Creates a new dam preventing debris from being pushed further downstream, by wrapping the
     * specified one.
     *
     * @param dam   The dam to wrap.
     * @param <IN>  The input data type.
     * @param <OUT> The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> upstreamDebris(final Dam<IN, OUT> dam) {

        return new UpstreamDebrisDam<IN, OUT>(dam);
    }

    /**
     * Creates a new dam retaining the specified one by means of a weak reference.
     * <p/>
     * Note that when the wrapped instance is GCed, the dam will behave like an open one.
     *
     * @param dam   The dam to wrap.
     * @param <IN>  The input data type.
     * @param <OUT> The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> weak(final Dam<IN, OUT> dam) {

        return new WeakDam<IN, OUT>(dam);
    }

    /**
     * Creates a new dam retaining the specified one by means of a weak reference.
     *
     * @param dam           The dam to wrap.
     * @param closeWhenNull Whether this instance must behave as a
     *                      {@link com.bmd.wtf.dam.ClosedDam} when the wrapped instance is garbage
     *                      collected, it will be open otherwise.
     * @param <IN>          The input data type.
     * @param <OUT>         The output data type.
     * @return The new dam instance.
     */
    public static <IN, OUT> Dam<IN, OUT> weak(final Dam<IN, OUT> dam, final boolean closeWhenNull) {

        return new WeakDam<IN, OUT>(dam, closeWhenNull);
    }
}