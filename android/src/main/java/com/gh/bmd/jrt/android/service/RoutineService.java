/*
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
package com.gh.bmd.jrt.android.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;

import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.invocation.ContextInvocations;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.common.Reflection;
import com.gh.bmd.jrt.core.AbstractRoutine;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * Basic implementation of a service running routine invocations.
 * <p/>
 * Created by davide-maestroni on 1/9/15.
 */
public class RoutineService extends Service {

    public static final int MSG_ABORT = -1;

    public static final int MSG_COMPLETE = 2;

    public static final int MSG_DATA = 1;

    public static final int MSG_INIT = 0;

    private static final String KEY_ABORT_EXCEPTION = "abort_exception";

    private static final String KEY_AVAILABLE_TIMEOUT = "avail_time";

    private static final String KEY_AVAILABLE_UNIT = "avail_unit";

    private static final String KEY_CORE_INVOCATIONS = "max_retained";

    private static final String KEY_DATA_VALUE = "data_value";

    private static final String KEY_INPUT_ORDER = "input_order";

    private static final String KEY_INVOCATION_ARGS = "invocation_args";

    private static final String KEY_INVOCATION_CLASS = "invocation_class";

    private static final String KEY_INVOCATION_ID = "invocation_id";

    private static final String KEY_LOG_CLASS = "log_class";

    private static final String KEY_LOG_LEVEL = "log_level";

    private static final String KEY_MAX_INVOCATIONS = "max_running";

    private static final String KEY_OUTPUT_ORDER = "output_order";

    private static final String KEY_PARALLEL_INVOCATION = "parallel_invocation";

    private static final String KEY_RUNNER_CLASS = "runner_class";

    private final Messenger mInMessenger = new Messenger(new IncomingHandler(this));

    private final HashMap<String, RoutineInvocation> mInvocationMap =
            new HashMap<String, RoutineInvocation>();

    private final Logger mLogger;

    private final Object mMutex = new Object();

    private final HashMap<RoutineInfo, RoutineState> mRoutineMap =
            new HashMap<RoutineInfo, RoutineState>();

    /**
     * Constructor.
     */
    @SuppressWarnings("unused")
    public RoutineService() {

        this(Logger.getGlobalLog(), Logger.getGlobalLogLevel());
    }

    /**
     * Constructor.
     *
     * @param log      the log instance.
     * @param logLevel the log level.
     */
    public RoutineService(@Nullable final Log log, @Nullable final LogLevel logLevel) {

        mLogger = Logger.newLogger(log, logLevel, this);
    }

    /**
     * Extracts the abort exception from the specified message.
     *
     * @param message the message.
     * @return the exception or null.
     */
    @Nullable
    public static Throwable getAbortError(@Nonnull final Message message) {

        final Bundle data = message.peekData();

        if (data == null) {

            return null;
        }

        data.setClassLoader(RoutineService.class.getClassLoader());
        return (Throwable) data.getSerializable(KEY_ABORT_EXCEPTION);
    }

    /**
     * Extracts the value object from the specified message.
     *
     * @param message the message.
     * @return the value or null.
     */
    @Nullable
    public static Object getValue(@Nonnull final Message message) {

        final Bundle data = message.peekData();

        if (data == null) {

            return null;
        }

        data.setClassLoader(RoutineService.class.getClassLoader());
        final ParcelableValue parcelable = data.getParcelable(KEY_DATA_VALUE);
        return parcelable.getValue();
    }

    /**
     * Puts the specified asynchronous invocation info into the passed bundle.
     *
     * @param bundle                  the bundle to fill.
     * @param invocationId            the invocation ID.
     * @param invocationClass         the invocation class.
     * @param invocationConfiguration the invocation configuration.
     * @param runnerClass             the invocation runner class.
     * @param logClass                the invocation log class.
     */
    public static void putAsyncInvocation(@Nonnull final Bundle bundle,
            @Nonnull final String invocationId,
            @Nonnull final Class<? extends ContextInvocation<?, ?>> invocationClass,
            @Nonnull final InvocationConfiguration invocationConfiguration,
            @Nullable final Class<? extends Runner> runnerClass,
            @Nullable final Class<? extends Log> logClass) {

        putInvocation(bundle, false, invocationId, invocationClass, invocationConfiguration,
                      runnerClass, logClass);
    }

    /**
     * Puts the specified abort exception into the passed bundle.
     *
     * @param bundle       the bundle to fill.
     * @param invocationId the invocation ID.
     * @param error        the exception instance.
     */
    public static void putError(@Nonnull final Bundle bundle, @Nonnull final String invocationId,
            @Nullable final Throwable error) {

        putInvocationId(bundle, invocationId);
        putError(bundle, error);
    }

    /**
     * Puts the specified invocation ID into the passed bundle.
     *
     * @param bundle       the bundle to fill.
     * @param invocationId the invocation ID.
     */
    public static void putInvocationId(@Nonnull final Bundle bundle,
            @Nonnull final String invocationId) {

        bundle.putString(KEY_INVOCATION_ID, invocationId);
    }

    /**
     * Puts the specified parallel invocation info into the passed bundle.
     *
     * @param bundle                  the bundle to fill.
     * @param invocationId            the invocation ID.
     * @param invocationClass         the invocation class.
     * @param invocationConfiguration the invocation configuration.
     * @param runnerClass             the invocation runner class.
     * @param logClass                the invocation log class.
     */
    public static void putParallelInvocation(@Nonnull final Bundle bundle,
            @Nonnull final String invocationId,
            @Nonnull final Class<? extends ContextInvocation<?, ?>> invocationClass,
            @Nonnull final InvocationConfiguration invocationConfiguration,
            @Nullable final Class<? extends Runner> runnerClass,
            @Nullable final Class<? extends Log> logClass) {

        putInvocation(bundle, true, invocationId, invocationClass, invocationConfiguration,
                      runnerClass, logClass);
    }

    /**
     * Puts the specified value object into the passed bundle.
     *
     * @param bundle       the bundle to fill.
     * @param invocationId the invocation ID.
     * @param value        the value instance.
     */
    public static void putValue(@Nonnull final Bundle bundle, @Nonnull final String invocationId,
            @Nullable final Object value) {

        putInvocationId(bundle, invocationId);
        putValue(bundle, value);
    }

    private static void putError(@Nonnull final Bundle bundle, @Nullable final Throwable error) {

        bundle.putSerializable(RoutineService.KEY_ABORT_EXCEPTION, error);
    }

    private static void putInvocation(@Nonnull final Bundle bundle, boolean isParallel,
            @Nonnull final String invocationId,
            @Nonnull final Class<? extends ContextInvocation<?, ?>> invocationClass,
            @Nonnull final InvocationConfiguration invocationConfiguration,
            @Nullable final Class<? extends Runner> runnerClass,
            @Nullable final Class<? extends Log> logClass) {

        bundle.putBoolean(KEY_PARALLEL_INVOCATION, isParallel);
        bundle.putString(KEY_INVOCATION_ID, invocationId);
        bundle.putSerializable(KEY_INVOCATION_CLASS, invocationClass);
        final Object[] invocationArgs =
                invocationConfiguration.getFactoryArgsOr(Reflection.NO_ARGS);
        final int length = invocationArgs.length;
        final ParcelableValue[] argValues = new ParcelableValue[length];

        for (int i = 0; i < length; i++) {

            argValues[i] = new ParcelableValue(invocationArgs[i]);
        }

        bundle.putParcelableArray(KEY_INVOCATION_ARGS, argValues);
        bundle.putInt(KEY_CORE_INVOCATIONS, invocationConfiguration.getCoreInvocationsOr(
                InvocationConfiguration.DEFAULT));
        bundle.putInt(KEY_MAX_INVOCATIONS,
                      invocationConfiguration.getMaxInvocationsOr(InvocationConfiguration.DEFAULT));
        final TimeDuration availTimeout = invocationConfiguration.getAvailInstanceTimeoutOr(null);

        if (availTimeout != null) {

            bundle.putLong(KEY_AVAILABLE_TIMEOUT, availTimeout.time);
            bundle.putSerializable(KEY_AVAILABLE_UNIT, availTimeout.unit);
        }

        bundle.putSerializable(KEY_INPUT_ORDER, invocationConfiguration.getInputOrderTypeOr(null));
        bundle.putSerializable(KEY_OUTPUT_ORDER,
                               invocationConfiguration.getOutputOrderTypeOr(null));
        bundle.putSerializable(KEY_LOG_LEVEL, invocationConfiguration.getLogLevelOr(null));
        bundle.putSerializable(KEY_RUNNER_CLASS, runnerClass);
        bundle.putSerializable(KEY_LOG_CLASS, logClass);
    }

    private static void putValue(@Nonnull final Bundle bundle, @Nullable final Object value) {

        bundle.putParcelable(KEY_DATA_VALUE, new ParcelableValue(value));
    }

    /**
     * Returns a context invocation factory instance creating invocations of the specified type.
     *
     * @param invocationClass the invocation class.
     * @return the context invocation factory.
     */
    @Nonnull
    public ContextInvocationFactory<Object, Object> getInvocationFactory(
            @Nonnull Class<? extends ContextInvocation<Object, Object>> invocationClass) {

        return ContextInvocations.factoryOf(invocationClass);
    }

    @Override
    public void onDestroy() {

        synchronized (mMutex) {

            for (final RoutineState routineState : mRoutineMap.values()) {

                routineState.purge();
            }
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(@Nonnull final Intent intent) {

        return mInMessenger.getBinder();
    }

    @Nonnull
    private RoutineInvocation getInvocation(@Nonnull final Message message) {

        final Bundle data = message.peekData();

        if (data == null) {

            mLogger.err("the service message has no data");
            throw new IllegalArgumentException(
                    "[" + getClass().getName() + "] the service message has no data");
        }

        data.setClassLoader(getClassLoader());
        final String invocationId = data.getString(KEY_INVOCATION_ID);

        if (invocationId == null) {

            mLogger.err("the service message has no invocation ID");
            throw new IllegalArgumentException(
                    "[" + getClass().getName() + "] the service message has no invocation ID");
        }

        synchronized (mMutex) {

            final RoutineInvocation invocation = mInvocationMap.get(invocationId);

            if (invocation == null) {

                mLogger.err("the service message has no invalid invocation ID: %d", invocationId);
                throw new IllegalArgumentException(
                        "[" + getClass().getName() + "] the service message has invalid "
                                + "invocation ID: " + invocationId);
            }

            return invocation;
        }
    }

    @SuppressWarnings("unchecked")
    private void initRoutine(final Message message) {

        final Bundle data = message.peekData();

        if (data == null) {

            mLogger.err("the service message has no data");
            throw new IllegalArgumentException(
                    "[" + getClass().getName() + "] the service message has no data");
        }

        data.setClassLoader(getClassLoader());
        final String invocationId = data.getString(KEY_INVOCATION_ID);

        if (invocationId == null) {

            mLogger.err("the service message has no invocation ID");
            throw new IllegalArgumentException(
                    "[" + getClass().getName() + "] the service message has no invocation ID");
        }

        final Class<? extends ContextInvocation<Object, Object>> invocationClass =
                (Class<? extends ContextInvocation<Object, Object>>) data.getSerializable(
                        KEY_INVOCATION_CLASS);

        if (invocationClass == null) {

            mLogger.err("the service message has no invocation class");
            throw new IllegalArgumentException(
                    "[" + getClass().getName() + "] the service message has no invocation class");
        }

        final Parcelable[] parcelableArgs = data.getParcelableArray(KEY_INVOCATION_ARGS);

        if (parcelableArgs == null) {

            mLogger.err("the service message has no invocation arguments");
            throw new IllegalArgumentException(
                    "[" + getClass().getName() + "] the service message has no invocation"
                            + " arguments");
        }

        final int length = parcelableArgs.length;
        final Object[] invocationArgs = new Object[length];

        for (int i = 0; i < length; i++) {

            invocationArgs[i] = ((ParcelableValue) parcelableArgs[i]).getValue();
        }

        synchronized (mMutex) {

            final HashMap<String, RoutineInvocation> invocationMap = mInvocationMap;

            if (invocationMap.containsKey(invocationId)) {

                mLogger.err("an invocation with the same ID is already running: %d", invocationId);
                throw new IllegalArgumentException(
                        "[" + getClass().getName() + "] an invocation with the same ID is"
                                + " already running: " + invocationId);
            }

            final int coreInvocations = data.getInt(KEY_CORE_INVOCATIONS);
            final int maxInvocations = data.getInt(KEY_MAX_INVOCATIONS);
            final long timeout = data.getLong(KEY_AVAILABLE_TIMEOUT);
            final TimeUnit timeUnit = (TimeUnit) data.getSerializable(KEY_AVAILABLE_UNIT);
            final TimeDuration availTimeout =
                    (timeUnit != null) ? TimeDuration.fromUnit(timeout, timeUnit) : null;
            final OrderType inputOrderType = (OrderType) data.getSerializable(KEY_INPUT_ORDER);
            final OrderType outputOrderType = (OrderType) data.getSerializable(KEY_OUTPUT_ORDER);
            final LogLevel logLevel = (LogLevel) data.getSerializable(KEY_LOG_LEVEL);
            final Class<? extends Runner> runnerClass =
                    (Class<? extends Runner>) data.getSerializable(KEY_RUNNER_CLASS);
            final Class<? extends Log> logClass =
                    (Class<? extends Log>) data.getSerializable(KEY_LOG_CLASS);
            final RoutineInfo routineInfo =
                    new RoutineInfo(invocationClass, invocationArgs, inputOrderType,
                                    outputOrderType, runnerClass, logClass, logLevel);
            final HashMap<RoutineInfo, RoutineState> routineMap = mRoutineMap;
            RoutineState routineState = routineMap.get(routineInfo);

            if (routineState == null) {

                final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                        InvocationConfiguration.builder();

                if (runnerClass != null) {

                    try {

                        builder.withAsyncRunner(findConstructor(runnerClass).newInstance());

                    } catch (final Throwable t) {

                        mLogger.err(t, "error creating the runner instance");
                        throw new IllegalArgumentException(t);
                    }
                }

                if (logClass != null) {

                    try {

                        builder.withLog(findConstructor(logClass).newInstance());

                    } catch (final Throwable t) {

                        mLogger.err(t, "error creating the log instance");
                        throw new IllegalArgumentException(t);
                    }
                }

                builder.withCoreInvocations(coreInvocations)
                       .withMaxInvocations(maxInvocations)
                       .withAvailInstanceTimeout(availTimeout)
                       .withInputOrder(inputOrderType)
                       .withOutputOrder(outputOrderType)
                       .withLogLevel(logLevel);
                final ContextInvocationFactory<Object, Object> factory =
                        getInvocationFactory(invocationClass);
                final AndroidRoutine androidRoutine =
                        new AndroidRoutine(this, builder.set(), factory, invocationArgs);
                routineState = new RoutineState(androidRoutine);
                routineMap.put(routineInfo, routineState);
            }

            final boolean isParallel = data.getBoolean(KEY_PARALLEL_INVOCATION);
            final InvocationChannel<Object, Object> channel =
                    (isParallel) ? routineState.invokeParallel() : routineState.invokeAsync();
            final RoutineInvocation routineInvocation =
                    new RoutineInvocation(invocationId, channel, routineInfo, routineState);
            invocationMap.put(invocationId, routineInvocation);
        }
    }

    /**
     * Synchronous Android routine implementation.
     */
    private static class AndroidRoutine extends AbstractRoutine<Object, Object> {

        private final Object[] mArgs;

        private final Context mContext;

        private final ContextInvocationFactory<Object, Object> mFactory;

        /**
         * Constructor.
         *
         * @param context        the routine context.
         * @param configuration  the invocation configuration.
         * @param factory        the invocation factory.
         * @param invocationArgs the invocation constructor arguments.
         */
        private AndroidRoutine(@Nonnull final Context context,
                @Nonnull final InvocationConfiguration configuration,
                @Nonnull final ContextInvocationFactory<Object, Object> factory,
                @Nonnull final Object[] invocationArgs) {

            super(configuration);
            mContext = context;
            mFactory = factory;
            mArgs = invocationArgs;
        }

        @Nonnull
        @Override
        protected Invocation<Object, Object> newInvocation(final boolean async) {

            final Logger logger = getLogger();

            try {

                final ContextInvocationFactory<Object, Object> factory = mFactory;
                logger.dbg("creating a new instance of type: %s", factory.getInvocationType());
                final ContextInvocation<Object, Object> invocation = factory.newInvocation(mArgs);
                invocation.onContext(mContext);
                return invocation;

            } catch (final RoutineException e) {

                logger.err(e, "error creating the invocation instance");
                throw e;

            } catch (final Throwable t) {

                logger.err(t, "error creating the invocation instance");
                throw new InvocationException(t);
            }
        }
    }

    /**
     * Handler implementation managing incoming messages from the routine invocation.
     */
    private static class IncomingHandler extends Handler {

        private final WeakReference<RoutineService> mService;

        /**
         * Constructor.
         *
         * @param service the service.
         */
        private IncomingHandler(final RoutineService service) {

            mService = new WeakReference<RoutineService>(service);
        }

        @Override
        public void handleMessage(@Nonnull final Message msg) {

            final RoutineService service = mService.get();

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
                        final RoutineInvocation invocation = service.getInvocation(msg);
                        invocation.result(new ServiceOutputConsumer(invocation, msg.replyTo));
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
                                        service.mInvocationMap.get(invocationId);

                                if (invocation != null) {

                                    invocation.recycle();
                                }
                            }
                        }
                    }

                } catch (final Throwable ignored) {

                    logger.err(ignored, "error while destroying invocation");
                }

                try {

                    final Messenger outMessenger = msg.replyTo;

                    if (outMessenger == null) {

                        logger.wrn("avoid aborting since reply messenger is null");
                        return;
                    }

                    final Message message = Message.obtain(null, RoutineService.MSG_ABORT);
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
    private static class RoutineInfo {

        private final OrderType mInputOrder;

        private final Object[] mInvocationArgs;

        private final Class<? extends ContextInvocation<?, ?>> mInvocationClass;

        private final Class<? extends Log> mLogClass;

        private final LogLevel mLogLevel;

        private final OrderType mOutputOrder;

        private final Class<? extends Runner> mRunnerClass;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         * @param invocationArgs  the invocation constructor arguments.
         * @param inputOrder      the input data order.
         * @param outputOrder     the output data order.
         * @param runnerClass     the runner class.
         * @param logClass        the log class.
         * @param logLevel        the log level.
         */
        private RoutineInfo(@Nonnull final Class<? extends ContextInvocation<?, ?>> invocationClass,
                @Nonnull final Object[] invocationArgs, @Nullable final OrderType inputOrder,
                @Nullable final OrderType outputOrder,
                @Nullable final Class<? extends Runner> runnerClass,
                @Nullable final Class<? extends Log> logClass, @Nullable final LogLevel logLevel) {

            mInvocationClass = invocationClass;
            mInvocationArgs = invocationArgs;
            mInputOrder = inputOrder;
            mOutputOrder = outputOrder;
            mRunnerClass = runnerClass;
            mLogClass = logClass;
            mLogLevel = logLevel;
        }

        @Override
        public boolean equals(final Object o) {

            // auto-generated code
            if (this == o) {

                return true;
            }

            if (!(o instanceof RoutineInfo)) {

                return false;
            }

            final RoutineInfo that = (RoutineInfo) o;
            return mInputOrder == that.mInputOrder && Arrays.equals(mInvocationArgs,
                                                                    that.mInvocationArgs)
                    && mInvocationClass.equals(that.mInvocationClass) && !(mLogClass != null
                    ? !mLogClass.equals(that.mLogClass) : that.mLogClass != null)
                    && mLogLevel == that.mLogLevel && mOutputOrder == that.mOutputOrder && !(
                    mRunnerClass != null ? !mRunnerClass.equals(that.mRunnerClass)
                            : that.mRunnerClass != null);
        }

        @Override
        public int hashCode() {

            // auto-generated code
            int result = mInputOrder != null ? mInputOrder.hashCode() : 0;
            result = 31 * result + Arrays.hashCode(mInvocationArgs);
            result = 31 * result + mInvocationClass.hashCode();
            result = 31 * result + (mLogClass != null ? mLogClass.hashCode() : 0);
            result = 31 * result + (mLogLevel != null ? mLogLevel.hashCode() : 0);
            result = 31 * result + (mOutputOrder != null ? mOutputOrder.hashCode() : 0);
            result = 31 * result + (mRunnerClass != null ? mRunnerClass.hashCode() : 0);
            return result;
        }
    }

    /**
     * Class storing the last routine state.
     */
    private static class RoutineState {

        private final AndroidRoutine mRoutine;

        private int mInvocationCount;

        /**
         * Constructor.
         *
         * @param routine the routine instance.
         */
        private RoutineState(@Nonnull final AndroidRoutine routine) {

            mRoutine = routine;
        }

        /**
         * Increments count of the running routines and starts an asynchronous invocation.
         *
         * @return the invocation parameter channel.
         */
        @Nonnull
        public InvocationChannel<Object, Object> invokeAsync() {

            ++mInvocationCount;
            return mRoutine.invokeAsync();
        }

        /**
         * Increments count of the running routines and starts a parallel invocation.
         *
         * @return the invocation parameter channel.
         */
        @Nonnull
        public InvocationChannel<Object, Object> invokeParallel() {

            ++mInvocationCount;
            return mRoutine.invokeParallel();
        }

        /**
         * Makes the routine purge all the cached invocation instances.
         */
        public void purge() {

            mRoutine.purge();
        }

        /**
         * Decrements count of the running routines and returns the updated value.
         *
         * @return the running routines count.
         */
        public int releaseInvocation() {

            return --mInvocationCount;
        }
    }

    /**
     * Output consumer sending messages to the routine.
     */
    private static class ServiceOutputConsumer implements OutputConsumer<Object> {

        private final RoutineInvocation mInvocation;

        private final Messenger mOutMessenger;

        /**
         * Constructor.
         *
         * @param invocation the routine invocation.
         * @param messenger  the output messenger.
         */
        @SuppressWarnings("ConstantConditions")
        private ServiceOutputConsumer(@Nonnull final RoutineInvocation invocation,
                @Nonnull final Messenger messenger) {

            if (messenger == null) {

                throw new NullPointerException("the output messenger must not be null");
            }

            mInvocation = invocation;
            mOutMessenger = messenger;
        }

        public void onComplete() {

            mInvocation.recycle();

            try {

                mOutMessenger.send(Message.obtain(null, RoutineService.MSG_COMPLETE));

            } catch (final RemoteException e) {

                throw new InvocationException(e);
            }
        }

        public void onError(@Nullable final Throwable error) {

            mInvocation.recycle();
            final Message message = Message.obtain(null, RoutineService.MSG_ABORT);
            putError(message.getData(), error);

            try {

                mOutMessenger.send(message);

            } catch (final RemoteException e) {

                throw new InvocationException(e);
            }
        }

        public void onOutput(final Object o) {

            final Message message = Message.obtain(null, RoutineService.MSG_DATA);
            putValue(message.getData(), o);

            try {

                mOutMessenger.send(message);

            } catch (final RemoteException e) {

                throw new InvocationException(e);
            }
        }
    }

    /**
     * Class storing the routine invocation information.
     */
    private class RoutineInvocation {

        private final InvocationChannel<Object, Object> mChannel;

        private final String mId;

        private final RoutineInfo mRoutineInfo;

        private final RoutineState mRoutineState;

        /**
         * Constructor.
         *
         * @param id      the invocation ID.
         * @param channel the invocation parameter channel.
         * @param info    the routine info.
         * @param state   the routine state.
         */
        private RoutineInvocation(@Nonnull final String id,
                @Nonnull final InvocationChannel<Object, Object> channel,
                @Nonnull final RoutineInfo info, @Nonnull final RoutineState state) {

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
        public void abort(@Nullable final Throwable reason) {

            mChannel.abort(reason);
        }

        /**
         * Passes the specified input to the invocation parameter channel.
         *
         * @param input the input.
         * @throws com.gh.bmd.jrt.channel.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException         if the channel is already closed.
         */
        public void pass(@Nullable final Object input) {

            mChannel.pass(input);
        }

        /**
         * Recycles this invocation, that is, removes it from the service cache.
         */
        public void recycle() {

            synchronized (mMutex) {

                mInvocationMap.remove(mId);

                if (mRoutineState.releaseInvocation() <= 0) {

                    mRoutineMap.remove(mRoutineInfo);
                }
            }
        }

        /**
         * Closes the input channel and binds the specified consumer to the output one.
         *
         * @throws com.gh.bmd.jrt.channel.RoutineException if the execution has been aborted.
         * @throws java.lang.IllegalStateException         if the channel is already closed or
         *                                                 already bound to a consumer.
         */
        public void result(@Nonnull final OutputConsumer<Object> consumer) {

            mChannel.result().passTo(consumer);
        }
    }
}
