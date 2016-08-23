/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.android.core.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.core.AbstractRoutine;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration.OrderType;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.error.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.newInstanceOf;

/**
 * Basic implementation of a Service running routine invocations.
 * <p>
 * Created by davide-maestroni on 01/09/2015.
 */
public class InvocationService extends Service {

    /**
     * Value used as {@code what} of a message identifying an abort operation.
     */
    public static final int MSG_ABORT = -1;

    /**
     * Value used as {@code what} of a message identifying a completion operation.
     */
    public static final int MSG_COMPLETE = 2;

    /**
     * Value used as {@code what} of a message identifying a data transfer operation.
     */
    public static final int MSG_DATA = 1;

    /**
     * Value used as {@code what} of a message identifying an initialization operation.
     */
    public static final int MSG_INIT = 0;

    private static final String KEY_ABORT_EXCEPTION = "abort_exception";

    private static final String KEY_DATA_VALUE = "data_value";

    private static final String KEY_FACTORY_ARGS = "factory_args";

    private static final String KEY_INVOCATION_ID = "invocation_id";

    private static final String KEY_LOG_ARGS = "log_args";

    private static final String KEY_LOG_CLASS = "log_class";

    private static final String KEY_LOG_LEVEL = "log_level";

    private static final String KEY_OUTPUT_ORDER = "output_order";

    private static final String KEY_RUNNER_ARGS = "runner_args";

    private static final String KEY_RUNNER_CLASS = "runner_class";

    private static final String KEY_TARGET_INVOCATION = "target_invocation";

    private final Messenger mInMessenger = new Messenger(new IncomingHandler(this));

    private final HashMap<String, RoutineInvocation> mInvocations =
            new HashMap<String, RoutineInvocation>();

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private final HashMap<RoutineInfo, RoutineState> mRoutines =
            new HashMap<RoutineInfo, RoutineState>();

    /**
     * Constructor.
     */
    @SuppressWarnings("unused")
    public InvocationService() {
        this(Logger.getDefaultLog(), Logger.getDefaultLevel());
    }

    /**
     * Constructor.
     *
     * @param log      the log instance.
     * @param logLevel the log level.
     */
    public InvocationService(@Nullable final Log log, @Nullable final Level logLevel) {
        mLogger = Logger.newLogger(log, logLevel, this);
    }

    /**
     * Extracts the abort exception from the specified message.
     *
     * @param message the message.
     * @return the exception or null.
     */
    @Nullable
    public static Throwable getAbortError(@NotNull final Message message) {
        final Bundle data = message.peekData();
        if (data == null) {
            return null;
        }

        data.setClassLoader(InvocationService.class.getClassLoader());
        return (Throwable) data.getSerializable(KEY_ABORT_EXCEPTION);
    }

    /**
     * Extracts the value object from the specified message.
     *
     * @param message the message.
     * @return the value or null.
     */
    @Nullable
    public static Object getValue(@NotNull final Message message) {
        final Bundle data = message.peekData();
        if (data == null) {
            return null;
        }

        data.setClassLoader(InvocationService.class.getClassLoader());
        final ParcelableValue parcelable = data.getParcelable(KEY_DATA_VALUE);
        return (parcelable == null) ? null : parcelable.getValue();
    }

    /**
     * Puts the specified abort exception into the passed bundle.
     *
     * @param bundle       the bundle to fill.
     * @param invocationId the invocation ID.
     * @param error        the exception instance.
     */
    public static void putError(@NotNull final Bundle bundle, @NotNull final String invocationId,
            @Nullable final Throwable error) {
        putInvocationId(bundle, invocationId);
        putError(bundle, error);
    }

    /**
     * Puts the specified asynchronous invocation info into the passed bundle.
     *
     * @param bundle                  the bundle to fill.
     * @param invocationId            the invocation ID.
     * @param targetClass             the target invocation class.
     * @param factoryArgs             the invocation factory arguments.
     * @param invocationConfiguration the invocation configuration.
     * @param runnerClass             the invocation runner class.
     * @param runnerArgs              the runner constructor args.
     * @param logClass                the invocation log class.
     * @param logArgs                 the log constructor args.
     */
    public static void putInvocation(@NotNull final Bundle bundle,
            @NotNull final String invocationId,
            @NotNull final Class<? extends ContextInvocation<?, ?>> targetClass,
            @Nullable final Object[] factoryArgs,
            @NotNull final InvocationConfiguration invocationConfiguration,
            @Nullable final Class<? extends Runner> runnerClass,
            @Nullable final Object[] runnerArgs, @Nullable final Class<? extends Log> logClass,
            @Nullable final Object[] logArgs) {
        bundle.putString(KEY_INVOCATION_ID,
                ConstantConditions.notNull("invocation ID", invocationId));
        bundle.putSerializable(KEY_TARGET_INVOCATION,
                ConstantConditions.notNull("target invocation class", targetClass));
        bundle.putParcelable(KEY_FACTORY_ARGS,
                (factoryArgs != null) ? new ParcelableValue(factoryArgs) : null);
        bundle.putSerializable(KEY_OUTPUT_ORDER,
                invocationConfiguration.getOutputOrderTypeOrElse(null));
        bundle.putSerializable(KEY_LOG_LEVEL, invocationConfiguration.getLogLevelOrElse(null));
        bundle.putSerializable(KEY_RUNNER_CLASS, runnerClass);
        bundle.putParcelable(KEY_RUNNER_ARGS,
                (runnerArgs != null) ? new ParcelableValue(runnerArgs) : null);
        bundle.putSerializable(KEY_LOG_CLASS, logClass);
        bundle.putParcelable(KEY_LOG_ARGS, (logArgs != null) ? new ParcelableValue(logArgs) : null);
    }

    /**
     * Puts the specified invocation ID into the passed bundle.
     *
     * @param bundle       the bundle to fill.
     * @param invocationId the invocation ID.
     */
    public static void putInvocationId(@NotNull final Bundle bundle,
            @NotNull final String invocationId) {
        bundle.putString(KEY_INVOCATION_ID, invocationId);
    }

    /**
     * Puts the specified value object into the passed bundle.
     *
     * @param bundle       the bundle to fill.
     * @param invocationId the invocation ID.
     * @param value        the value instance.
     */
    public static void putValue(@NotNull final Bundle bundle, @NotNull final String invocationId,
            @Nullable final Object value) {
        putInvocationId(bundle, invocationId);
        putValue(bundle, value);
    }

    private static void putError(@NotNull final Bundle bundle, @Nullable final Throwable error) {
        bundle.putSerializable(KEY_ABORT_EXCEPTION, error);
    }

    private static void putValue(@NotNull final Bundle bundle, @Nullable final Object value) {
        bundle.putParcelable(KEY_DATA_VALUE, new ParcelableValue(value));
    }

    /**
     * Returns a Context invocation factory instance creating invocations of the specified type.
     *
     * @param targetClass the target invocation class.
     * @param args        the factory arguments.
     * @return the Context invocation factory.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public ContextInvocationFactory<?, ?> getInvocationFactory(
            @NotNull final Class<? extends ContextInvocation<?, ?>> targetClass,
            @Nullable final Object... args) throws Exception {
        return factoryOf((Class<? extends ContextInvocation<Object, Object>>) targetClass, args);
    }

    @Override
    public void onDestroy() {
        final ArrayList<RoutineState> routineStates;
        synchronized (mMutex) {
            routineStates = new ArrayList<RoutineState>(mRoutines.values());
        }

        for (final RoutineState routineState : routineStates) {
            routineState.mRoutine.clear();
        }

        super.onDestroy();
    }

    @Override
    public final IBinder onBind(@NotNull final Intent intent) {
        return mInMessenger.getBinder();
    }

    @NotNull
    private RoutineInvocation getInvocation(@NotNull final Message message) {
        final Bundle data = message.peekData();
        if (data == null) {
            mLogger.err("the Service message has no data");
            throw new IllegalArgumentException(
                    "[" + getClass().getName() + "] the Service message has no data");
        }

        data.setClassLoader(getClassLoader());
        final String invocationId = data.getString(KEY_INVOCATION_ID);
        if (invocationId == null) {
            mLogger.err("the Service message has no invocation ID");
            throw new IllegalArgumentException(
                    "[" + getClass().getName() + "] the Service message has no invocation ID");
        }

        synchronized (mMutex) {
            final RoutineInvocation invocation = mInvocations.get(invocationId);
            if (invocation == null) {
                mLogger.err("the Service message has invalid invocation ID: %s", invocationId);
                throw new IllegalArgumentException(
                        "[" + getClass().getName() + "] the Service message has invalid "
                                + "invocation ID: " + invocationId);
            }

            return invocation;
        }
    }

    private void initRoutine(@NotNull final Message message) throws Exception {
        final Bundle data = message.peekData();
        if (data == null) {
            mLogger.err("the Service message has no data");
            throw new IllegalArgumentException(
                    "[" + getClass().getName() + "] the Service message has no data");
        }

        data.setClassLoader(getClassLoader());
        final String invocationId = data.getString(KEY_INVOCATION_ID);
        if (invocationId == null) {
            mLogger.err("the Service message has no invocation ID");
            throw new IllegalArgumentException(
                    "[" + getClass().getName() + "] the Service message has no invocation ID");
        }

        @SuppressWarnings("unchecked") final Class<? extends ContextInvocation<?, ?>> targetClass =
                (Class<? extends ContextInvocation<?, ?>>) data.getSerializable(
                        KEY_TARGET_INVOCATION);
        if (targetClass == null) {
            mLogger.err("the Service message has no target invocation class");
            throw new IllegalArgumentException(
                    "[" + getClass().getName() + "] the Service message has no target "
                            + "invocation class");
        }

        final ParcelableValue value = data.getParcelable(KEY_FACTORY_ARGS);
        final Object[] args =
                ((value != null) && (value.getValue() != null)) ? (Object[]) value.getValue()
                        : Reflection.NO_ARGS;
        synchronized (mMutex) {
            final HashMap<String, RoutineInvocation> invocations = mInvocations;
            if (invocations.containsKey(invocationId)) {
                mLogger.err("an invocation with the same ID is already running: %s", invocationId);
                throw new IllegalArgumentException(
                        "[" + getClass().getName() + "] an invocation with the same ID is"
                                + " already running: " + invocationId);
            }

            final OrderType outputOrderType = (OrderType) data.getSerializable(KEY_OUTPUT_ORDER);
            final Level logLevel = (Level) data.getSerializable(KEY_LOG_LEVEL);
            @SuppressWarnings("unchecked") final Class<? extends Runner> runnerClass =
                    (Class<? extends Runner>) data.getSerializable(KEY_RUNNER_CLASS);
            @SuppressWarnings("unchecked") final Class<? extends Log> logClass =
                    (Class<? extends Log>) data.getSerializable(KEY_LOG_CLASS);
            final RoutineInfo routineInfo =
                    new RoutineInfo(targetClass, args, outputOrderType, runnerClass, logClass,
                            logLevel);
            final HashMap<RoutineInfo, RoutineState> routines = mRoutines;
            RoutineState routineState = routines.get(routineInfo);
            if (routineState == null) {
                final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                        InvocationConfiguration.builder();
                if (runnerClass != null) {
                    final ParcelableValue runnerValue = data.getParcelable(KEY_RUNNER_ARGS);
                    final Object[] runnerArgs =
                            ((runnerValue != null) && (runnerValue.getValue() != null))
                                    ? (Object[]) runnerValue.getValue() : Reflection.NO_ARGS;
                    try {
                        builder.withRunner(newInstanceOf(runnerClass, runnerArgs));

                    } catch (final Exception e) {
                        mLogger.err(e, "error creating the runner instance");
                        throw e;
                    }
                }

                if (logClass != null) {
                    final ParcelableValue logValue = data.getParcelable(KEY_LOG_ARGS);
                    final Object[] logArgs = ((logValue != null) && (logValue.getValue() != null))
                            ? (Object[]) logValue.getValue() : Reflection.NO_ARGS;
                    try {
                        builder.withLog(newInstanceOf(logClass, logArgs));

                    } catch (final Exception e) {
                        mLogger.err(e, "error creating the log instance");
                        throw e;
                    }
                }

                builder.withOutputOrder(outputOrderType).withLogLevel(logLevel);
                final ContextInvocationFactory<?, ?> factory =
                        getInvocationFactory(targetClass, args);
                final ContextRoutine contextRoutine =
                        new ContextRoutine(this, builder.configured(), factory);
                routineState = new RoutineState(contextRoutine);
                routines.put(routineInfo, routineState);
            }

            final Channel<Object, Object> channel = routineState.invoke();
            final RoutineInvocation routineInvocation =
                    new RoutineInvocation(invocationId, channel, routineInfo, routineState);
            routineInvocation.bind(new ServiceChannelConsumer(routineInvocation, message.replyTo));
            invocations.put(invocationId, routineInvocation);
        }
    }

    /**
     * Context routine implementation.
     */
    private static class ContextRoutine extends AbstractRoutine<Object, Object> {

        private final Context mContext;

        private final ContextInvocationFactory<?, ?> mFactory;

        /**
         * Constructor.
         *
         * @param context       the routine Context.
         * @param configuration the invocation configuration.
         * @param factory       the invocation factory.
         */
        private ContextRoutine(@NotNull final Context context,
                @NotNull final InvocationConfiguration configuration,
                @NotNull final ContextInvocationFactory<?, ?> factory) {
            super(configuration);
            mContext = context;
            mFactory = factory;
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        protected Invocation<Object, Object> newInvocation() throws Exception {
            final ContextInvocationFactory<?, ?> factory = mFactory;
            getLogger().dbg("creating a new instance");
            final ContextInvocation<?, ?> invocation = factory.newInvocation();
            invocation.onContext(mContext);
            return (Invocation<Object, Object>) invocation;
        }
    }

    /**
     * Handler implementation managing incoming messages from the routine invocation.
     */
    private static class IncomingHandler extends Handler {

        private final WeakReference<InvocationService> mService;

        /**
         * Constructor.
         *
         * @param service the Service.
         */
        private IncomingHandler(@NotNull final InvocationService service) {
            mService = new WeakReference<InvocationService>(service);
        }

        @Override
        public void handleMessage(@NotNull final Message msg) {
            final InvocationService service = mService.get();
            if (service == null) {
                super.handleMessage(msg);
                return;
            }

            final Logger logger = service.mLogger;
            logger.dbg("incoming routine message: %s", msg);
            try {
                switch (msg.what) {
                    case MSG_DATA:
                        service.getInvocation(msg).pass(getValue(msg));
                        break;

                    case MSG_COMPLETE:
                        service.getInvocation(msg).close();
                        break;

                    case MSG_ABORT:
                        service.getInvocation(msg).abort(getAbortError(msg));
                        break;

                    case MSG_INIT:
                        service.initRoutine(msg);
                        break;

                    default:
                        super.handleMessage(msg);
                }

            } catch (final Throwable t) {
                logger.err(t, "error while parsing routine message");
                try {
                    final Bundle data = msg.peekData();
                    if (data != null) {
                        data.setClassLoader(service.getClassLoader());
                        final String invocationId = data.getString(KEY_INVOCATION_ID);
                        if (invocationId != null) {
                            synchronized (service.mMutex) {
                                final RoutineInvocation invocation =
                                        service.mInvocations.get(invocationId);
                                if (invocation != null) {
                                    invocation.recycle();
                                }
                            }
                        }
                    }

                } catch (final Throwable ignored) {
                    logger.err(ignored, "error while discarding invocation");
                }

                try {
                    final Messenger outMessenger = msg.replyTo;
                    if (outMessenger == null) {
                        logger.err("avoid aborting since reply messenger is null");
                        return;
                    }

                    final Message message = Message.obtain(null, MSG_ABORT);
                    putError(message.getData(), t);
                    outMessenger.send(message);

                } catch (final Throwable ignored) {
                    logger.err(ignored, "error while sending routine abort message");
                }
            }
        }
    }

    /**
     * Class storing the routine information.
     */
    private static class RoutineInfo extends DeepEqualObject {

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         * @param factoryArgs     the invocation constructor arguments.
         * @param outputOrder     the output data order.
         * @param runnerClass     the runner class.
         * @param logClass        the log class.
         * @param logLevel        the log level.
         */
        private RoutineInfo(@NotNull final Class<? extends ContextInvocation<?, ?>> invocationClass,
                @NotNull final Object[] factoryArgs, @Nullable final OrderType outputOrder,
                @Nullable final Class<? extends Runner> runnerClass,
                @Nullable final Class<? extends Log> logClass, @Nullable final Level logLevel) {
            super(asArgs(invocationClass, factoryArgs, outputOrder, runnerClass, logClass,
                    logLevel));
        }
    }

    /**
     * Class storing the last routine state.
     */
    private static class RoutineState {

        private final ContextRoutine mRoutine;

        private int mInvocationCount;

        /**
         * Constructor.
         *
         * @param routine the routine instance.
         */
        private RoutineState(@NotNull final ContextRoutine routine) {
            mRoutine = routine;
        }

        /**
         * Clear the routine invocation cache.
         */
        void clear() {
            mRoutine.clear();
        }

        /**
         * Increments the count of the running routines and starts an asynchronous invocation.
         *
         * @return the invocation channel.
         */
        @NotNull
        Channel<Object, Object> invoke() {
            ++mInvocationCount;
            return mRoutine.call();
        }

        /**
         * Decrements the count of the running routines and returns the updated value.
         *
         * @return the running routines count.
         */
        int releaseInvocation() {
            return --mInvocationCount;
        }
    }

    /**
     * Channel consumer sending messages to the routine.
     */
    private static class ServiceChannelConsumer implements ChannelConsumer<Object> {

        private final RoutineInvocation mInvocation;

        private final Messenger mOutMessenger;

        /**
         * Constructor.
         *
         * @param invocation the routine invocation.
         * @param messenger  the output messenger.
         */
        private ServiceChannelConsumer(@NotNull final RoutineInvocation invocation,
                @NotNull final Messenger messenger) {
            mInvocation = invocation;
            mOutMessenger = ConstantConditions.notNull("output messenger", messenger);
        }

        @Override
        public void onComplete() throws RemoteException {
            mInvocation.recycle();
            mOutMessenger.send(Message.obtain(null, MSG_COMPLETE));
        }

        @Override
        public void onError(@NotNull final RoutineException error) throws RemoteException {
            mInvocation.recycle();
            final Message message = Message.obtain(null, MSG_ABORT);
            putError(message.getData(), error);
            mOutMessenger.send(message);
        }

        @Override
        public void onOutput(final Object o) throws RemoteException {
            final Message message = Message.obtain(null, MSG_DATA);
            putValue(message.getData(), o);
            mOutMessenger.send(message);
        }
    }

    /**
     * Class storing the routine invocation information.
     */
    private class RoutineInvocation {

        private final Channel<Object, Object> mChannel;

        private final String mId;

        private final RoutineInfo mRoutineInfo;

        private final RoutineState mRoutineState;

        /**
         * Constructor.
         *
         * @param id      the invocation ID.
         * @param channel the invocation channel.
         * @param info    the routine info.
         * @param state   the routine state.
         */
        private RoutineInvocation(@NotNull final String id,
                @NotNull final Channel<Object, Object> channel, @NotNull final RoutineInfo info,
                @NotNull final RoutineState state) {
            mId = id;
            mChannel = channel;
            mRoutineInfo = info;
            mRoutineState = state;
        }

        /**
         * Closes the channel and abort the transfer of data.
         *
         * @param reason the throwable object identifying the reason of the routine abortion.
         */
        void abort(@Nullable final Throwable reason) {
            mChannel.abort(reason);
        }

        /**
         * Binds the specified consumer to the channel.
         *
         * @throws com.github.dm.jrt.core.error.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException               if the channel is already closed
         *                                                       or already bound to a consumer.
         */
        void bind(@NotNull final ChannelConsumer<Object> consumer) {
            mChannel.bind(consumer);
        }

        /**
         * Closes the channel.
         */
        void close() {
            mChannel.close();
        }

        /**
         * Passes the specified input to the invocation channel.
         *
         * @param input the input.
         * @throws com.github.dm.jrt.core.error.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException               if the channel is already closed.
         */
        void pass(@Nullable final Object input) {
            mChannel.pass(input);
        }

        /**
         * Recycles this invocation, that is, removes it from the Service cache.
         */
        void recycle() {
            synchronized (mMutex) {
                mInvocations.remove(mId);
                final RoutineState routineState = mRoutineState;
                if (routineState.releaseInvocation() <= 0) {
                    routineState.clear();
                    mRoutines.remove(mRoutineInfo);
                }
            }
        }
    }
}
