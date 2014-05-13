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
import com.bmd.wtf.dam.DownstreamDebrisDam;
import com.bmd.wtf.dam.NoDebrisDam;
import com.bmd.wtf.dam.UpstreamDebrisDam;
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

    private static final FlushHandler CLOSED_FLUSH_HANDLER = new FlushHandler() {

        @Override
        public boolean onFlush() {

            return false;
        }
    };

    private static final PullHandler CLOSED_PULL_HANDLER = new PullHandler() {

        @Override
        public Object onPullDebris(final Object debris) {

            return null;
        }
    };

    private static final PushHandler CLOSED_PUSH_HANDLER = new PushHandler() {

        @Override
        public Object onPushDebris(final Object debris) {

            return null;
        }
    };

    private static final FlushHandler OPEN_FLUSH_HANDLER = new FlushHandler() {

        @Override
        public boolean onFlush() {

            return true;
        }
    };

    private FlushHandler mOnFlush = OPEN_FLUSH_HANDLER;

    private static final PullHandler OPEN_PULL_HANDLER = new PullHandler() {

        @Override
        public Object onPullDebris(final Object debris) {

            return debris;
        }
    };

    private PullHandler mOnPull = OPEN_PULL_HANDLER;

    private static final PushHandler OPEN_PUSH_HANDLER = new PushHandler() {

        @Override
        public Object onPushDebris(final Object debris) {

            return debris;
        }
    };

    private PushHandler mOnPush = OPEN_PUSH_HANDLER;

    private final DischargeHandler<IN, OUT> mOnDischarge;

    private boolean mAvoidNull;

    private boolean mDownstreamDebris;

    private boolean mUpstreamDebris;

    /**
     * Constructor.
     *
     * @param other   The other instance from which to copy the parameters.
     * @param handler The discharge handler.
     */
    protected DamBuilder(final DamBuilder<?, ?> other, final DischargeHandler<IN, OUT> handler) {

        this(handler);

        mOnFlush = other.mOnFlush;
        mOnPush = other.mOnPush;
        mOnPull = other.mOnPull;
        mAvoidNull = other.mAvoidNull;
        mDownstreamDebris = other.mDownstreamDebris;
        mUpstreamDebris = other.mUpstreamDebris;
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
        mDownstreamDebris = true;
        mUpstreamDebris = true;
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
     * Makes the built dam avoid debris to be pulled upstream.
     *
     * @return This builder.
     */
    public DamBuilder<IN, OUT> avoidPull() {

        mOnPull = CLOSED_PULL_HANDLER;

        return this;
    }

    /**
     * Makes the built dam avoid debris to be pushed downstream.
     *
     * @return This builder.
     */
    public DamBuilder<IN, OUT> avoidPush() {

        mOnPush = CLOSED_PUSH_HANDLER;

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

        final PushHandler push = mOnPush;

        final PullHandler pull = mOnPull;

        final Dam<IN, OUT> dam;

        if (mAvoidNull) {

            dam = new AvoidingNullDam<IN, OUT>(discharge, push, flush, pull);

        } else {

            dam = new ArtificialDam<IN, OUT>(discharge, push, flush, pull);
        }

        if (mDownstreamDebris) {

            if (!mUpstreamDebris) {

                return new DownstreamDebrisDam<IN, OUT>(dam);
            }

            return dam;

        } else {

            if (mUpstreamDebris) {

                return new UpstreamDebrisDam<IN, OUT>(dam);
            }

            return new NoDebrisDam<IN, OUT>(dam);
        }
    }

    /**
     * Makes the built dam avoid debris to be pulled upstream.
     *
     * @return This builder.
     */
    public DamBuilder<IN, OUT> downstreamDebris() {

        mDownstreamDebris = true;
        mUpstreamDebris = false;

        return this;
    }

    /**
     * Makes the built dam avoid debris to be pushed downstream or pulled upstream.
     *
     * @return This builder.
     */
    public DamBuilder<IN, OUT> noDebris() {

        mDownstreamDebris = false;
        mUpstreamDebris = false;

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
     * Set the pull handler instance.
     *
     * @param handler The pull handler.
     * @return This builder.
     */
    public DamBuilder<IN, OUT> onPullDebris(final PullHandler handler) {

        if (handler == null) {

            throw new IllegalArgumentException("the pull handler cannot be null");
        }

        mOnPull = handler;

        return this;
    }

    /**
     * Set the push handler instance.
     *
     * @param handler The push handler.
     * @return This builder.
     */
    public DamBuilder<IN, OUT> onPushDebris(final PushHandler handler) {

        if (handler == null) {

            throw new IllegalArgumentException("the push handler cannot be null");
        }

        mOnPush = handler;

        return this;
    }

    /**
     * Makes the built dam avoid debris to be pushed downstream.
     *
     * @return This builder.
     */
    public DamBuilder<IN, OUT> upstreamDebris() {

        mDownstreamDebris = false;
        mUpstreamDebris = true;

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
     * This interface defines an object handling the debris pulled through the dam.
     */
    public interface PullHandler {

        /**
         * This method is called when an debris is pulled upstream through the dam.
         *
         * @param debris The pulled debris.
         * @return The debris to pull further upstream, or <code>null</code>.
         */
        public Object onPullDebris(Object debris);
    }

    /**
     * This interface defines an object handling the debris pushed through the dam.
     */
    public interface PushHandler {

        /**
         * This method is called when an debris is pushed downstream through the dam.
         *
         * @param debris The pushed object.
         * @return The debris to push further downstream, or <code>null</code>.
         */
        public Object onPushDebris(Object debris);
    }

    /**
     * Dam built through a builder instance.
     *
     * @param <IN>  The input data type.
     * @param <OUT> The output data type.
     */
    private static class ArtificialDam<IN, OUT> implements Dam<IN, OUT> {

        private final DischargeHandler<IN, OUT> mDischarge;

        private final FlushHandler mFlush;

        private final PullHandler mPull;

        private final PushHandler mPush;

        public ArtificialDam(final DischargeHandler<IN, OUT> discharge, final PushHandler push,
                final FlushHandler flush, final PullHandler pull) {

            mDischarge = discharge;
            mPush = push;
            mFlush = flush;
            mPull = pull;
        }

        @Override
        public Object onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

            gate.discharge(mDischarge.onDischarge(drop));

            return null;
        }

        @Override
        public Object onFlush(final Floodgate<IN, OUT> gate) {

            if (mFlush.onFlush()) {

                gate.flush();
            }

            return null;
        }

        @Override
        public Object onPullDebris(final Floodgate<IN, OUT> gate, final Object debris) {

            return mPull.onPullDebris(debris);
        }

        @Override
        public Object onPushDebris(final Floodgate<IN, OUT> gate, final Object debris) {

            return mPush.onPushDebris(debris);
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

        public AvoidingNullDam(final DischargeHandler<IN, OUT> discharge, final PushHandler push,
                final FlushHandler flush, final PullHandler pull) {

            super(discharge, push, flush, pull);

            mDischarge = discharge;
        }

        @Override
        public Object onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

            final OUT out = mDischarge.onDischarge(drop);

            if (out != null) {

                gate.discharge(out);
            }

            return null;
        }
    }
}