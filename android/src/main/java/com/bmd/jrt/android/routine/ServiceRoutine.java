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
package com.bmd.jrt.android.routine;

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

import com.bmd.jrt.android.invocation.AndroidInvocation;
import com.bmd.jrt.android.service.RoutineService;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.channel.IOChannel;
import com.bmd.jrt.channel.IOChannel.IOChannelInput;
import com.bmd.jrt.channel.IOChannel.IOChannelOutput;
import com.bmd.jrt.channel.OutputChannel;
import com.bmd.jrt.channel.OutputConsumer;
import com.bmd.jrt.channel.ParameterChannel;
import com.bmd.jrt.common.ClassToken;
import com.bmd.jrt.common.RoutineException;
import com.bmd.jrt.invocation.Invocation;
import com.bmd.jrt.log.Log;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.log.Logger;
import com.bmd.jrt.routine.Routine;
import com.bmd.jrt.routine.TemplateRoutine;
import com.bmd.jrt.runner.Runner;
import com.bmd.jrt.time.TimeDuration;

import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.bmd.jrt.android.service.RoutineService.getAbortError;
import static com.bmd.jrt.android.service.RoutineService.getValue;
import static com.bmd.jrt.android.service.RoutineService.putAsyncInvocation;
import static com.bmd.jrt.android.service.RoutineService.putError;
import static com.bmd.jrt.android.service.RoutineService.putInvocationId;
import static com.bmd.jrt.android.service.RoutineService.putParallelInvocation;
import static com.bmd.jrt.android.service.RoutineService.putValue;
import static com.bmd.jrt.common.Reflection.findConstructor;
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
     * @throws NullPointerException     if one of the parameters is null.
     * @throws IllegalArgumentException if at least one of the parameter is invalid.
     */
    ServiceRoutine(@Nonnull final Context context,
            @Nullable final Class<? extends RoutineService> serviceClass,
            @Nullable final Looper looper,
            @Nonnull final ClassToken<? extends AndroidInvocation<INPUT, OUTPUT>> invocationToken,
            @Nonnull final RoutineConfiguration configuration,
            @Nullable final Class<? extends Runner> runnerClass,
            @Nullable final Class<? extends Log> logClass) {

        final Log log;

        if (logClass != null) {

            final Constructor<? extends Log> constructor = findConstructor(logClass);

            try {

                log = constructor.newInstance();

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }

        } else {

            log = Logger.getDefaultLog();
        }

        mContext = context.getApplicationContext();
        mLooper = looper;
        mServiceClass = (serviceClass != null) ? serviceClass : RoutineService.class;
        mInvocationClass = invocationToken.getRawClass();
        mConfiguration = configuration;
        mRunnerClass = runnerClass;
        mLogClass = logClass;
        final LogLevel logLevel = configuration.getLogLevel(LogLevel.DEFAULT);
        mLogger = Logger.create(log, logLevel, this);
        mRoutine = JRoutine.on((ClassToken<? extends Invocation<INPUT, OUTPUT>>) invocationToken)
                           .apply(configuration).loggedWith(log)
                           .logLevel(logLevel)
                           .buildRoutine();
    }

    @Override
    public void flush() {

        mRoutine.flush();
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

        private final IOChannelInput<INPUT> mParamChannelInput;

        private final IOChannelOutput<INPUT> mParamChannelOutput;

        private final IOChannelInput<OUTPUT> mResultChannelInput;

        private final IOChannelOutput<OUTPUT> mResultChannelOutput;

        private final Class<? extends Runner> mRunnerClass;

        private final Class<? extends RoutineService> mServiceClass;

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

            mUUID = randomUUID().toString();
            mIsParallel = isParallel;
            mContext = context;
            mInMessenger = new Messenger(
                    new IncomingHandler((looper != null) ? looper : Looper.myLooper()));
            mServiceClass = serviceClass;
            mInvocationClass = invocationClass;
            mConfiguration = configuration;
            mRunnerClass = runnerClass;
            mLogClass = logClass;
            mLogger = logger;
            final Log log = logger.getLog();
            final LogLevel logLevel = logger.getLogLevel();
            final IOChannel<INPUT> paramChannel = JRoutine.io()
                                                          .dataOrder(configuration.getInputOrder(
                                                                  DataOrder.DEFAULT))
                                                          .maxSize(Integer.MAX_VALUE)
                                                          .bufferTimeout(TimeDuration.ZERO)
                                                          .loggedWith(log)
                                                          .logLevel(logLevel)
                                                          .buildChannel();
            mParamChannelInput = paramChannel.input();
            mParamChannelOutput = paramChannel.output();
            final IOChannel<OUTPUT> resultChannel = JRoutine.io()
                                                            .dataOrder(configuration.getOutputOrder(
                                                                    DataOrder.DEFAULT))
                                                            .maxSize(Integer.MAX_VALUE)
                                                            .bufferTimeout(TimeDuration.ZERO)
                                                            .loggedWith(log)
                                                            .logLevel(logLevel)
                                                            .buildChannel();
            mResultChannelInput = resultChannel.input();
            mResultChannelOutput = resultChannel.output();
        }

        @Override
        public boolean abort() {

            bindService();
            return mParamChannelInput.abort();
        }

        @Override
        public boolean abort(@Nullable final Throwable reason) {

            bindService();
            return mParamChannelInput.abort(reason);
        }

        @Override
        public boolean isOpen() {

            return mParamChannelInput.isOpen();
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> after(@Nonnull final TimeDuration delay) {

            mParamChannelInput.after(delay);
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> after(final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mParamChannelInput.after(delay, timeUnit);
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> now() {

            mParamChannelInput.now();
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final OutputChannel<INPUT> channel) {

            bindService();
            mParamChannelInput.pass(channel);
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> pass(
                @Nullable final Iterable<? extends INPUT> inputs) {

            bindService();
            mParamChannelInput.pass(inputs);
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final INPUT input) {

            bindService();
            mParamChannelInput.pass(input);
            return this;
        }

        @Nonnull
        @Override
        public ParameterChannel<INPUT, OUTPUT> pass(@Nullable final INPUT... inputs) {

            bindService();
            mParamChannelInput.pass(inputs);
            return this;
        }

        @Nonnull
        @Override
        public OutputChannel<OUTPUT> result() {

            bindService();
            mParamChannelInput.close();
            return mResultChannelOutput;
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
                    throw new RoutineException(e);
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
                    throw new RoutineException(e);
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

                    throw new RoutineException(e);
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
            public void handleMessage(final Message msg) {

                final Logger logger = mLogger;
                logger.dbg("incoming service message: %s", msg);

                try {

                    switch (msg.what) {

                        case RoutineService.MSG_DATA:
                            mResultChannelInput.pass((OUTPUT) getValue(msg));
                            break;

                        case RoutineService.MSG_COMPLETE:
                            unbindService();
                            mResultChannelInput.close();
                            break;

                        case RoutineService.MSG_ABORT:
                            unbindService();
                            mResultChannelInput.abort(getAbortError(msg));
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

                    unbindService();
                    mResultChannelInput.abort(t);
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
                    mParamChannelOutput.bind(mConsumer);

                } catch (final RemoteException e) {

                    logger.err(e, "error while sending service invocation message");
                    unbindService();
                    mResultChannelInput.abort(e);
                }
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {

                mLogger.dbg("service disconnected: %s", name);
                mParamChannelOutput.unbind(mConsumer);
            }
        }
    }
}
