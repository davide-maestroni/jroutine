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

import com.bmd.wtf.dam.Dam;
import com.bmd.wtf.dam.NoDebrisDam;
import com.bmd.wtf.src.Floodgate;

/**
 * Utility class used to build {@link com.bmd.wtf.dam.Dam} objects with the ability to specify
 * only part of the Dam behavior.
 * <p/>
 * Created by davide on 3/10/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public class DamBuilder<IN, OUT> {

    private static final DropHandler CLOSED_DROP_HANDLER = new DropHandler() {

        @Override
        public Object onDrop(final Object debris) {

            return null;
        }
    };

    private static final FlushHandler CLOSED_FLUSH_HANDLER = new FlushHandler() {

        @Override
        public boolean onFlush() {

            return false;
        }
    };

    private static final DropHandler OPEN_DROP_HANDLER = new DropHandler() {

        @Override
        public Object onDrop(final Object debris) {

            return debris;
        }
    };

    private DropHandler mOnDrop = OPEN_DROP_HANDLER;

    private static final FlushHandler OPEN_FLUSH_HANDLER = new FlushHandler() {

        @Override
        public boolean onFlush() {

            return true;
        }
    };

    private FlushHandler mOnFlush = OPEN_FLUSH_HANDLER;

    private final DischargeHandler<IN, OUT> mOnDischarge;

    private boolean mAvoidNull;

    private boolean mNoDebris;

    /**
     * Constructor.
     *
     * @param other   The other instance from which to copy the parameters.
     * @param handler The discharge handler.
     */
    protected DamBuilder(final DamBuilder<?, ?> other, final DischargeHandler<IN, OUT> handler) {

        this(handler);

        mOnFlush = other.mOnFlush;
        mOnDrop = other.mOnDrop;
        mAvoidNull = other.mAvoidNull;
        mNoDebris = other.mNoDebris;
    }

    /**
     * Constructor.
     *
     * @param handler The discharge handler.
     */
    protected DamBuilder(final DischargeHandler<IN, OUT> handler) {

        if (handler == null) {

            throw new IllegalArgumentException("the discharge handler cannot be null");
        }

        mOnDischarge = handler;
    }

    /**
     * Creates a new builder based on the specified handler.
     *
     * @param handler The discharge handler.
     * @param <IN>    The input data type.
     * @param <OUT>   The output data type.
     * @return The new builder.
     */
    public static <IN, OUT> DamBuilder<IN, OUT> basedOn(final DischargeHandler<IN, OUT> handler) {

        return new DamBuilder<IN, OUT>(handler);
    }

    /**
     * Makes the built dam avoid debris to be dropped downstream.
     *
     * @return This builder.
     */
    public DamBuilder<IN, OUT> avoidDebris() {

        mOnDrop = CLOSED_DROP_HANDLER;

        return this;
    }

    /**
     * Makes the built dam avoid flush to be propagated.
     *
     * @return This builder.
     */
    public DamBuilder<IN, OUT> avoidFlush() {

        mOnFlush = CLOSED_FLUSH_HANDLER;

        return this;
    }

    /**
     * Makes the built dam avoid <code>null</code> data to be discharged.
     *
     * @return This builder.
     */
    public DamBuilder<IN, OUT> avoidNull() {

        mAvoidNull = true;

        return this;
    }

    /**
     * Builds the dam.
     *
     * @return The newly created dam.
     */
    public Dam<IN, OUT> build() {

        final DischargeHandler<IN, OUT> discharge = mOnDischarge;

        final FlushHandler flush = mOnFlush;

        final DropHandler drop = mOnDrop;

        final Dam<IN, OUT> dam;

        if (mAvoidNull) {

            dam = new AvoidingNullDam<IN, OUT>(discharge, drop, flush);

        } else {

            dam = new ArtificialDam<IN, OUT>(discharge, drop, flush);
        }

        if (mNoDebris) {

            return new NoDebrisDam<IN, OUT>(dam);
        }

        return dam;
    }

    /**
     * Makes the built dam avoid debris to be pushed downstream or pulled upstream.
     *
     * @return This builder.
     */
    public DamBuilder<IN, OUT> noDebris() {

        mNoDebris = true;

        return this;
    }

    /**
     * Creates a new builder based on the specified handler.
     * <p/>
     * Note that all the remaining configuration from this builder will be retained.
     *
     * @param handler The discharge handler.
     * @param <NOUT>  The output data type.
     * @return The new builder.
     */
    public <NOUT> DamBuilder<IN, NOUT> onDischarge(final DischargeHandler<IN, NOUT> handler) {

        return new DamBuilder<IN, NOUT>(this, handler);
    }

    /**
     * Set the drop handler instance.
     *
     * @param handler The drop handler.
     * @return This builder.
     */
    public DamBuilder<IN, OUT> onDrop(final DropHandler handler) {

        if (handler == null) {

            throw new IllegalArgumentException("the drop handler cannot be null");
        }

        mOnDrop = handler;

        return this;
    }

    /**
     * Set the flush handler instance.
     *
     * @param handler The flush handler.
     * @return This builder.
     */
    public DamBuilder<IN, OUT> onFlush(final FlushHandler handler) {

        if (handler == null) {

            throw new IllegalArgumentException("the flush handler cannot be null");
        }

        mOnFlush = handler;

        return this;
    }

    /**
     * This interface defines an object handling the data discharged through the dam.
     *
     * @param <IN>  The input data type.
     * @param <OUT> The output data type.
     */
    public interface DischargeHandler<IN, OUT> {

        /**
         * This method is called when a data drop is discharged through the dam.
         *
         * @param drop The drop of data discharged.
         * @return The output data.
         */
        public OUT onDischarge(IN drop);
    }

    /**
     * This interface defines an object handling the debris dropped through the dam.
     */
    public interface DropHandler {

        /**
         * This method is called when a debris is dropped downstream through the dam.
         *
         * @param debris The dropped object.
         * @return The debris to drop further downstream, or <code>null</code>.
         */
        public Object onDrop(Object debris);
    }

    /**
     * This interface defines an object handling the data flushed through the dam.
     */
    public interface FlushHandler {

        /**
         * This method is called when data are flushed through the dam.
         *
         * @return Whether to propagate further the flush.
         */
        public boolean onFlush();
    }

    /**
     * Dam built through a builder instance.
     *
     * @param <IN>  The input data type.
     * @param <OUT> The output data type.
     */
    private static class ArtificialDam<IN, OUT> implements Dam<IN, OUT> {

        private final DischargeHandler<IN, OUT> mDischarge;

        private final DropHandler mDrop;

        private final FlushHandler mFlush;

        public ArtificialDam(final DischargeHandler<IN, OUT> discharge, final DropHandler drop,
                final FlushHandler flush) {

            mDischarge = discharge;
            mDrop = drop;
            mFlush = flush;
        }

        @Override
        public void onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

            gate.discharge(mDischarge.onDischarge(drop));
        }

        @Override
        public void onDrop(final Floodgate<IN, OUT> gate, final Object debris) {

            final Object next = mDrop.onDrop(debris);

            if (next != null) {

                gate.drop(next);
            }
        }

        @Override
        public void onFlush(final Floodgate<IN, OUT> gate) {

            if (mFlush.onFlush()) {

                gate.flush();
            }
        }
    }

    /**
     * Dam built through a builder instance, preventing <code>null</code> data to be discharged.
     *
     * @param <IN>  The input data type.
     * @param <OUT> The output data type.
     */
    private static class AvoidingNullDam<IN, OUT> extends ArtificialDam<IN, OUT> {

        private final DischargeHandler<IN, OUT> mDischarge;

        public AvoidingNullDam(final DischargeHandler<IN, OUT> discharge, final DropHandler drop,
                final FlushHandler flush) {

            super(discharge, drop, flush);

            mDischarge = discharge;
        }

        @Override
        public void onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

            final OUT out = mDischarge.onDischarge(drop);

            if (out != null) {

                gate.discharge(out);
            }
        }
    }
}