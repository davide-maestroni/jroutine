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

import com.bmd.wtf.bdr.Stream;

/**
 * Utility class used to build {@link com.bmd.wtf.dam.Dam} objects starting from a
 * {@link com.bmd.wtf.bdr.Stream} instance.
 * <p/>
 * Created by davide on 3/10/14.
 *
 * @param <SOURCE> The spring data type.
 * @param <IN>     The input data type.
 * @param <OUT>    The output data type.
 */
public class StreamDamBuilder<SOURCE, IN, OUT> extends DamBuilder<IN, OUT> {

    private final Stream<SOURCE, ?, IN> mInStream;

    /**
     * Constructor.
     *
     * @param stream  The input stream.
     * @param handler The discharge handler.
     */
    protected StreamDamBuilder(final Stream<SOURCE, ?, IN> stream, final DischargeHandler<IN, OUT> handler) {

        super(handler);

        if (stream == null) {

            throw new IllegalArgumentException("the input stream cannot be null");
        }

        mInStream = stream;
    }

    /**
     * Constructor.
     *
     * @param other   The other instance from which to copy the parameters.
     * @param handler The discharge handler.
     */
    protected StreamDamBuilder(final StreamDamBuilder<SOURCE, IN, ?> other, final DischargeHandler<IN, OUT> handler) {

        super(other, handler);

        mInStream = other.mInStream;
    }

    /**
     * Creates a new dam builder starting from the specified stream.
     *
     * @param stream   The input stream.
     * @param <SOURCE> The spring data type.
     * @param <IN>     The input data type.
     * @param <OUT>    The transported data type.
     * @return The new dam builder.
     */
    public static <SOURCE, IN, OUT> StreamDamBuilder<SOURCE, OUT, ?> blocking(final Stream<SOURCE, IN, OUT> stream) {

        return new StreamDamBuilder<SOURCE, OUT, Object>(stream, new DischargeHandler<OUT, Object>() {

            @Override
            public Object onDischarge(final OUT drop) {

                return drop;
            }
        }
        );
    }

    @Override
    public StreamDamBuilder<SOURCE, IN, OUT> avoidDebris() {

        super.avoidDebris();

        return this;
    }

    @Override
    public StreamDamBuilder<SOURCE, IN, OUT> avoidFlush() {

        super.avoidFlush();

        return this;
    }

    @Override
    public StreamDamBuilder<SOURCE, IN, OUT> avoidNull() {

        super.avoidNull();

        return this;
    }

    @Override
    public StreamDamBuilder<SOURCE, IN, OUT> noDebris() {

        super.noDebris();

        return this;
    }

    @Override
    public <NOUT> StreamDamBuilder<SOURCE, IN, NOUT> onDischarge(final DischargeHandler<IN, NOUT> handler) {

        return new StreamDamBuilder<SOURCE, IN, NOUT>(this, handler);
    }

    @Override
    public StreamDamBuilder<SOURCE, IN, OUT> onDrop(final DropHandler handler) {

        super.onDrop(handler);

        return this;
    }

    @Override
    public StreamDamBuilder<SOURCE, IN, OUT> onFlush(final FlushHandler handler) {

        super.onFlush(handler);

        return this;
    }

    /**
     * Builds the dam and returns the stream flowing through it.
     *
     * @return The new stream flowing through the dam.
     */
    public Stream<SOURCE, IN, OUT> fallFrom() {

        return mInStream.thenFallingThrough(build());
    }
}