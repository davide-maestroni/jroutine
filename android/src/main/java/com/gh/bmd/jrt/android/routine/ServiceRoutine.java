/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.android.routine;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.gh.bmd.jrt.android.invocation.AndroidInvocation;
import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.RoutineConfiguration.TimeoutAction;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.ParameterChannel;
import com.gh.bmd.jrt.channel.StandaloneChannel;
import com.gh.bmd.jrt.channel.StandaloneChannel.StandaloneInput;
import com.gh.bmd.jrt.channel.StandaloneChannel.StandaloneOutput;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.invocation.Invocation;
import com.gh.bmd.jrt.invocation.Invocations;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.routine.TemplateRoutine;
import com.gh.bmd.jrt.runner.Runner;
import com.gh.bmd.jrt.time.TimeDuration;

import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.android.service.RoutineService.getAbortError;
import static com.gh.bmd.jrt.android.service.RoutineService.getValue;
import static com.gh.bmd.jrt.android.service.RoutineService.putAsyncInvocation;
import static com.gh.bmd.jrt.android.service.RoutineService.putError;
import static com.gh.bmd.jrt.android.service.RoutineService.putInvocationId;
import static com.gh.bmd.jrt.android.service.RoutineService.putParallelInvocation;
import static com.gh.bmd.jrt.android.service.RoutineService.putValue;
import static com.gh.bmd.jrt.builder.RoutineConfiguration.builder;
import static com.gh.bmd.jrt.common.Reflection.findConstructor;
import static java.util.UUID.randomUUID;

/**
 * Routine implementation employing an Android remote service to run its invocations.
 * <p/>
 * Created by davide on 1/8/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class ServiceRoutine<INPUT, OUTPUT> extends TemplateRoutine<INPUT, OUTPUT> {

    private final RoutineConfiguration mConfiguration;

    private final Context mContext;

    private final Class<? extends AndroidInvocation<INPUT, OUTPUT>> mInvocationClass;

    private final Class<? extends Log> mLogClass;

    private final Logger mLogger;

    private final Looper mLooper;

    private final Routine<INPUT, OUTPUT> mRoutine;

    private final Class<? extends Runner> mRunnerClass;

    private final Class<? extends RoutineService> mServiceClass;

    /**
     * Constructor.
     *
     * @param context         the routine context.
     * @param serviceClass    the service class.
     * @param looper          the message looper.
     * @param invocationToken the invocation class token.
     * @param configuration   the routine configuration.
     * @param runnerClass     the asynchronous runner class.
     * @param logClass        the log class.
     * @throws java.lang.IllegalArgumentException if at least one of the parameter is invalid.
     * @throws java.lang.NullPointerException     if one of the parameters is null.
     */
    ServiceRoutine(@Nonnull final Context context,
            @Nullable final Class<? extends RoutineService> serviceClass,
            @Nullable final Looper looper,
            @Nonnull final ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> invocationToken,
            @Nonnull final RoutineConfiguration configuration,
            @Nullable final Class<? extends Runner> runnerClass,
            @Nullable final Class<? extends Log> logClass) {

        if (runnerClass != null) {

            findConstructor(runnerClass);
        }

        Log log = null;

        if (logClass != null) {

            final Constructor<? extends Log> constructor = findConstructor(logClass);

            try {

                log = constructor.newInstance();

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }

        if (log == null) {

            log = configuration.getLogOr(Logger.getGlobalLog());
        }

        final Runner runner = configuration.getRunnerOr(null);

        mContext = context.getApplicationContext();
        mLooper = looper;
        mServiceClass = (serviceClass != null) ? serviceClass : RoutineService.class;
        mInvocationClass = invocationToken.getRawClass();
        mConfiguration = configuration;
        mRunnerClass =
                (runnerClass != null) ? runnerClass : (runner != null) ? runner.getClass() : null;
        mLogClass = (logClass != null) ? logClass : log.getClass();
        mLogger = Logger.newLogger(log, configuration.getLogLevelOr(Logger.getGlobalLogLevel()),
                                   this);
        mRoutine = JRoutine.on(Invocations.factoryOf(
                (ClassToken<? extends Invocation<INPUT, OUTPUT>>) invocationToken))
                           .withConfiguration(configuration.builderFrom()
                                                           .withInputSize(Integer.MAX_VALUE)
                                                           .withInputTimeout(TimeDuration.ZERO)
                                                           .withOutputSize(Integer.MAX_VALUE)
                                                           .withOutputTimeout(TimeDuration.ZERO)
                                                           .withLog(log)
                                                           .buildConfiguration())
                           .buildRoutine();

        final Logger logger = mLogger;
        logger.dbg("building service routine with configuration: %s", configuration);
        warn(logger, configuration);
    }

    private static void warn(@Nonnull final Logger logger,
            @Nonnull final RoutineConfiguration configuration) {

        final int inputSize = configuration.getInputSizeOr(RoutineConfiguration.DEFAULT);

        if (inputSize != RoutineConfiguration.DEFAULT) {

            logger.wrn("the specified maximum input size will be ignored: %d", inputSize);
        }

        final TimeDuration inputTimeout = configuration.getInputTimeoutOr(null);

        if (inputTimeout != null) {

            logger.wrn("the specified input timeout will be ignored: %s", inputTimeout);
        }

        final int outputSize = configuration.getOutputSizeOr(RoutineConfiguration.DEFAULT);

        if (outputSize != RoutineConfiguration.DEFAULT) {

            logger.wrn("the specified maximum output size will be ignored: %d", outputSize);
        }

        final TimeDuration outputTimeout = configuration.getOutputTimeoutOr(null);

        if (outputTimeout != null) {

            logger.wrn("the specified output timeout will be ignored: %s", outputTimeout);
        }
    }

    @Nonnull
    @Override
    public ParameterChannel<INPUT, OUTPUT> invokeAsync() {

        return new ServiceChannel<INPUT, OUTPUT>(false, mContext, mServiceClass, mLooper,
                                                 mInvocationClass, mConfiguration, mRunnerClass,
                                                 mLogClass, mLogger);
    }

    @Nonnull
    @Override
    public ParameterChannel<INPUT, OUTPUT> invokeParallel() {

        return new ServiceChannel<INPUT, OUTPUT>(true, mContext, mServiceClass, mLooper,
                                                 mInvocationClass, mConfiguration, mRunnerClass,
                                                 mLogClass, mLogger);
    }

    @Nonnull
    @Override
    public ParameterChannel<INPUT, OUTPUT> invokeSync() {

        return mRoutine.invokeSync();
    }

    @Override
    public void purge() {

        mRoutine.purge();
    }

    /**
     * Service parameter channel implementation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class ServiceChannel<INPUT, OUTPUT> implements ParameterChannel<INPUT, OUTPUT> {

        private final RoutineConfiguration mConfiguration;

        private final Context mContext;

        private final Messenger mInMessenger;

        private final Class<? extends AndroidInvocation<INPUT, OUTPUT>> mInvocationClass;

        private final boolean mIsParallel;

        private final Class<? extends Log> mLogClass;

        private final Logger mLogger;

        private final Object mMutex = new Object();

        private final Class<? extends Runner> mRunnerClass;

        private final Class<? extends RoutineService> mServiceClass;

        private final StandaloneInput<INPUT> mStandaloneParamInput;

        private final StandaloneOutput<INPUT> mStandaloneParamOutput;

        private final StandaloneInput<OUTPUT> mStandaloneResultInput;

        private final StandaloneOutput<OUTPUT> mStandaloneResultOutput;

        private final String mUUID;

        private RoutineServiceConnection mConnection;

        private boolean mIsBound;

        private boolean mIsUnbound;

        private Messenger mOutMessenger;

        /**
         * Constructor.
         *
         * @param isParallel      whether the invocation is parallel.
         * @param context         the routine context.
         * @param serviceClass    the service class.
         * @param looper          the message looper.
         * @param invocationClass the invocation class.
         * @param configuration   the routine configuration.
         * @param runnerClass     the asynchronous runner class.
         * @param logClass        the log class.
         * @param logger          the routine logger.
         */
        private ServiceChannel(boolean isParallel, @Nonnull final Context context,
                @Nonnull final Class<? extends RoutineService> serviceClass,
                @Nullable final Looper looper,
                @Nonnull Class<? extends AndroidInvocation<INPUT, OUTPUT>> invocationClass,
                @Nonnull final RoutineConfiguration configuration,
                @Nullable final Class<? extends Runner> runnerClass,
                @Nullable final Class<? extends Log> logClass, @Nonnull final Logger logger) {

            Looper handlerLooper = (looper != null) ? looper : Looper.myLooper();

            if (handlerLooper == null) {

                handlerLooper = Looper.getMainLooper();
            }

            mUUID = randomUUID().toString();
            mIsParallel = isParallel;
            mContext = context;
            mInMessenger = new Messenger(new IncomingHandler(handlerLooper));
            mServiceClass = serviceClass;
            mInvocationClass = invocationClass;
            mConfiguration = configuration;
            mRunnerClass = runnerClass;
            mLogClass = logClass;
            mLogger = logger;
            final Log log = logger.getLog();
            final LogLevel logLevel = logger.getLogLevel();
            final OrderType inputOrder = configuration.getInputOrderOr(null);
            final RoutineConfiguration inputConfiguration = builder().withOutputOrder(inputOrder)
                                                                     .withOutputSize(
                                                                             Integer.MAX_VALUE)
                                                                     .withOutputTimeout(
                                                                             TimeDuration.ZERO)
                                                                     .withLog(log)
                                                                     .withLogLevel(logLevel)
                                                                     .buildConfiguration();
            final StandaloneChannel<INPUT> paramChannel =
                    JRoutine.standalone().withConfiguration(inputConfiguration).buildChannel();
            mStandaloneParamInput = paramChannel.input();
            mStandaloneParamOutput = paramChannel.output();
            final OrderType outputOrder = configuration.getOutputOrderOr(null);
            final TimeDuration readTimeout = configuration.getReadTimeoutOr(null);
            final TimeoutAction timeoutAction = configuration.getReadTimeoutActionOr(null);
            final RoutineConfiguration outputConfiguration = builder().withOutputOrder(outputOrder)
                                                                      .withOutputSize(
                                                                              Integer.MAX_VALUE)
                                                                      .withOutputTimeout(
                                                                              TimeDuration.ZERO)
                                                                      .withReadTimeout(readTimeout)
                                                                      .onReadTimeout(timeoutAction)
                                                                      .withLog(log)
                                                                      .withLogLevel(logLevel)
                                                                      .buildConfiguration();
            final StandaloneChannel<OUTPUT> resultChannel =
                    JRoutine.standalone().withConfiguration(outputConfiguration).buildChannel();
            mStandaloneResultInput = resultChannel.input();
            mStandaloneResultOutput = resultChannel.output();
        }

        @Override
        public boolean abort() {

            bindService();
            return mStandaloneParamInput.abort();
        }

        @Override
        public boolean abort(@Nullable final Throwable reason) {

            bindService();
            return mStandaloneParamInput.abort(reason);
        }

        @Override
        public boolean isOpen() {

            return mStandaloneParamInput.isOpen();
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> after(@Nonnull final TimeDuration delay) {

            mStandaloneParamInput.after(delay);
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> after(final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mStandaloneParamInput.after(delay, timeUnit);
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> now() {

            mStandaloneParamInput.now();
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final OutputChannel<INPUT> channel) {

            bindService();
            mStandaloneParamInput.pass(channel);
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> pass(
                @Nullable final Iterable<? extends INPUT> inputs) {

            bindService();
            mStandaloneParamInput.pass(inputs);
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final INPUT input) {

            bindService();
            mStandaloneParamInput.pass(input);
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final INPUT... inputs) {

            bindService();
            mStandaloneParamInput.pass(inputs);
            return this;
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> result() {

            bindService();
            mStandaloneParamInput.close();
            return mStandaloneResultOutput;
        }

        private void bindService() {

            synchronized (mMutex) {

                if (mIsBound) {

                    return;
                }

                mIsBound = true;
                final Context context = mContext;
                mConnection = new RoutineServiceConnection();
                context.bindService(new Intent(context, mServiceClass), mConnection,
                                    Context.BIND_AUTO_CREATE);
            }
        }

        private void unbindService() {

            synchronized (mMutex) {

                if (mIsUnbound) {

                    return;
                }

                mIsUnbound = true;
                mContext.unbindService(mConnection);
            }
        }

        /**
         * Output consumer sending messages to the remote service.
         */
        private class ConnectionOutputConsumer implements OutputConsumer<INPUT> {

            @Override
            public void onComplete() {

                final Message message = Message.obtain(null, RoutineService.MSG_COMPLETE);
                putInvocationId(message.getData(), mUUID);
                message.replyTo = mInMessenger;

                try {

                    mOutMessenger.send(message);

                } catch (final RemoteException e) {

                    unbindService();
                    throw new InvocationException(e);
                }
            }

            @Override
            public void onError(@Nullable final Throwable error) {

                final Message message = Message.obtain(null, RoutineService.MSG_ABORT);
                putError(message.getData(), mUUID, error);
                message.replyTo = mInMessenger;

                try {

                    mOutMessenger.send(message);

                } catch (final RemoteException e) {

                    unbindService();
                    throw new InvocationException(e);
                }
            }

            @Override
            public void onOutput(final INPUT input) {

                final Message message = Message.obtain(null, RoutineService.MSG_DATA);
                putValue(message.getData(), mUUID, input);
                message.replyTo = mInMessenger;

                try {

                    mOutMessenger.send(message);

                } catch (final RemoteException e) {

                    throw new InvocationException(e);
                }
            }
        }

        /**
         * Handler implementation managing incoming messages from the remote service.
         */
        private class IncomingHandler extends Handler {

            /**
             * Constructor.
             *
             * @param looper the message looper.
             */
            private IncomingHandler(@Nonnull final Looper looper) {

                super(looper);
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleMessage(@Nonnull final Message msg) {

                final Logger logger = mLogger;
                logger.dbg("incoming service message: %s", msg);

                try {

                    switch (msg.what) {

                        case RoutineService.MSG_DATA:
                            mStandaloneResultInput.pass((OUTPUT) getValue(msg));
                            break;

                        case RoutineService.MSG_COMPLETE:
                            mStandaloneResultInput.close();
                            unbindService();
                            break;

                        case RoutineService.MSG_ABORT:
                            mStandaloneResultInput.abort(getAbortError(msg));
                            unbindService();
                            break;

                        default:
                            super.handleMessage(msg);
                    }

                } catch (final Throwable t) {

                    logger.err(t, "error while parsing service message");

                    final Message message = Message.obtain(null, RoutineService.MSG_ABORT);
                    putError(message.getData(), mUUID, t);

                    try {

                        mOutMessenger.send(message);

                    } catch (final RemoteException e) {

                        logger.err(e, "error while sending service abort message");
                    }

                    mStandaloneResultInput.abort(t);
                    unbindService();
                }
            }
        }

        /**
         * Service connection implementation managing the remote service communication state.
         */
        private class RoutineServiceConnection implements ServiceConnection {

            private ConnectionOutputConsumer mConsumer;

            @Override
            public void onServiceConnected(final ComponentName name, final IBinder service) {

                final Logger logger = mLogger;
                logger.dbg("service connected: %s", name);

                mOutMessenger = new Messenger(service);

                final Message message = Message.obtain(null, RoutineService.MSG_INIT);

                if (mIsParallel) {

                    logger.dbg("sending parallel invocation message");
                    putParallelInvocation(message.getData(), mUUID, mInvocationClass,
                                          mConfiguration, mRunnerClass, mLogClass);

                } else {

                    logger.dbg("sending async invocation message");
                    putAsyncInvocation(message.getData(), mUUID, mInvocationClass, mConfiguration,
                                       mRunnerClass, mLogClass);
                }

                message.replyTo = mInMessenger;

                try {

                    mOutMessenger.send(message);
                    mConsumer = new ConnectionOutputConsumer();
                    mStandaloneParamOutput.bind(mConsumer);

                } catch (final RemoteException e) {

                    logger.err(e, "error while sending service invocation message");
                    mStandaloneResultInput.abort(e);
                    unbindService();
                }
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {

                mLogger.dbg("service disconnected: %s", name);
                mStandaloneParamOutput.unbind(mConsumer);
            }
        }
    }
}
