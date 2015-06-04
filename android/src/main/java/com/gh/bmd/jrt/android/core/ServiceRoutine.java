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
package com.gh.bmd.jrt.android.core;

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

import com.gh.bmd.jrt.android.builder.ServiceConfiguration;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocations;
import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.android.service.ServiceDisconnectedException;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.channel.TransportChannel.TransportInput;
import com.gh.bmd.jrt.channel.TransportChannel.TransportOutput;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.routine.TemplateRoutine;
import com.gh.bmd.jrt.util.Reflection;
import com.gh.bmd.jrt.util.TimeDuration;

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
import static com.gh.bmd.jrt.util.Reflection.findConstructor;
import static java.util.UUID.randomUUID;

/**
 * Routine implementation employing an Android service to run its invocations.
 * <p/>
 * Created by davide-maestroni on 1/8/15.
 *
 * @param <INPUT>  the input data type.
 * @param <OUTPUT> the output data type.
 */
class ServiceRoutine<INPUT, OUTPUT> extends TemplateRoutine<INPUT, OUTPUT> {

    private final Context mContext;

    private final Class<? extends ContextInvocation<INPUT, OUTPUT>> mInvocationClass;

    private final InvocationConfiguration mInvocationConfiguration;

    private final Logger mLogger;

    private final Routine<INPUT, OUTPUT> mRoutine;

    private final ServiceConfiguration mServiceConfiguration;

    /**
     * Constructor.
     *
     * @param context                 the routine context.
     * @param invocationClass         the invocation class.
     * @param invocationConfiguration the invocation configuration.
     * @param serviceConfiguration    the service configuration.
     * @throws java.lang.IllegalArgumentException if at least one of the parameter is invalid.
     */
    ServiceRoutine(@Nonnull final Context context,
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass,
            @Nonnull final InvocationConfiguration invocationConfiguration,
            @Nonnull final ServiceConfiguration serviceConfiguration) {

        final Object[] invocationArgs =
                invocationConfiguration.getFactoryArgsOr(Reflection.NO_ARGS);
        findConstructor(invocationClass, invocationArgs);
        mContext = context.getApplicationContext();
        mInvocationClass = invocationClass;
        mInvocationConfiguration = invocationConfiguration;
        mServiceConfiguration = serviceConfiguration;
        mLogger = invocationConfiguration.newLogger(this);
        mRoutine = JRoutine.on(ContextInvocations.invocationFactoryOf(invocationClass, mContext))
                           .withInvocation()
                           .with(invocationConfiguration)
                           .withFactoryArgs(invocationArgs)
                           .withInputMaxSize(Integer.MAX_VALUE)
                           .withInputTimeout(TimeDuration.ZERO)
                           .withOutputMaxSize(Integer.MAX_VALUE)
                           .withOutputTimeout(TimeDuration.ZERO)
                           .set()
                           .buildRoutine();
        final Logger logger = mLogger;
        logger.dbg("building service routine on invocation %s with configurations: %s - %s",
                   invocationClass.getName(), invocationConfiguration, serviceConfiguration);
        warn(logger, invocationConfiguration);
    }

    /**
     * Logs any warning related to ignored options in the specified configuration.
     *
     * @param logger        the logger instance.
     * @param configuration the invocation configuration.
     */
    private static void warn(@Nonnull final Logger logger,
            @Nonnull final InvocationConfiguration configuration) {

        final int inputSize = configuration.getInputMaxSizeOr(InvocationConfiguration.DEFAULT);

        if (inputSize != InvocationConfiguration.DEFAULT) {

            logger.wrn("the specified maximum input size will be ignored: %d", inputSize);
        }

        final TimeDuration inputTimeout = configuration.getInputTimeoutOr(null);

        if (inputTimeout != null) {

            logger.wrn("the specified input timeout will be ignored: %s", inputTimeout);
        }

        final int outputSize = configuration.getOutputMaxSizeOr(InvocationConfiguration.DEFAULT);

        if (outputSize != InvocationConfiguration.DEFAULT) {

            logger.wrn("the specified maximum output size will be ignored: %d", outputSize);
        }

        final TimeDuration outputTimeout = configuration.getOutputTimeoutOr(null);

        if (outputTimeout != null) {

            logger.wrn("the specified output timeout will be ignored: %s", outputTimeout);
        }
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> invokeAsync() {

        return new ServiceChannel<INPUT, OUTPUT>(false, mContext, mInvocationClass,
                                                 mInvocationConfiguration, mServiceConfiguration,
                                                 mLogger);
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> invokeParallel() {

        return new ServiceChannel<INPUT, OUTPUT>(true, mContext, mInvocationClass,
                                                 mInvocationConfiguration, mServiceConfiguration,
                                                 mLogger);
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> invokeSync() {

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
    private static class ServiceChannel<INPUT, OUTPUT> implements InvocationChannel<INPUT, OUTPUT> {

        private final Context mContext;

        private final Messenger mInMessenger;

        private final Class<? extends ContextInvocation<INPUT, OUTPUT>> mInvocationClass;

        private final InvocationConfiguration mInvocationConfiguration;

        private final boolean mIsParallel;

        private final Logger mLogger;

        private final Object mMutex = new Object();

        private final Class<? extends RoutineService> mServiceClass;

        private final ServiceConfiguration mServiceConfiguration;

        private final TransportInput<INPUT> mTransportParamInput;

        private final TransportOutput<INPUT> mTransportParamOutput;

        private final TransportInput<OUTPUT> mTransportResultInput;

        private final TransportOutput<OUTPUT> mTransportResultOutput;

        private final String mUUID;

        private RoutineServiceConnection mConnection;

        private boolean mIsBound;

        private boolean mIsUnbound;

        private Messenger mOutMessenger;

        /**
         * Constructor.
         *
         * @param isParallel              whether the invocation is parallel.
         * @param context                 the routine context.
         * @param invocationClass         the invocation class.
         * @param invocationConfiguration the invocation configuration.
         * @param serviceConfiguration    the service configuration.
         * @param logger                  the routine logger.
         */
        private ServiceChannel(boolean isParallel, @Nonnull final Context context,
                @Nonnull Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass,
                @Nonnull final InvocationConfiguration invocationConfiguration,
                @Nonnull final ServiceConfiguration serviceConfiguration,
                @Nonnull final Logger logger) {

            mUUID = randomUUID().toString();
            mIsParallel = isParallel;
            mContext = context;
            mServiceClass = serviceConfiguration.getServiceClassOr(RoutineService.class);
            mInMessenger = new Messenger(new IncomingHandler(
                    serviceConfiguration.getResultLooperOr(Looper.getMainLooper())));
            mInvocationClass = invocationClass;
            mInvocationConfiguration = invocationConfiguration;
            mServiceConfiguration = serviceConfiguration;
            mLogger = logger;
            final Log log = logger.getLog();
            final LogLevel logLevel = logger.getLogLevel();
            final OrderType inputOrderType = invocationConfiguration.getInputOrderTypeOr(null);
            final TransportChannel<INPUT> paramChannel = JRoutine.transport()
                                                                 .withInvocation()
                                                                 .withOutputOrder(inputOrderType)
                                                                 .withOutputMaxSize(
                                                                         Integer.MAX_VALUE)
                                                                 .withOutputTimeout(
                                                                         TimeDuration.ZERO)
                                                                 .withLog(log)
                                                                 .withLogLevel(logLevel)
                                                                 .set()
                                                                 .buildChannel();
            mTransportParamInput = paramChannel.input();
            mTransportParamOutput = paramChannel.output();
            final OrderType outputOrderType = invocationConfiguration.getOutputOrderTypeOr(null);
            final TimeDuration readTimeout = invocationConfiguration.getReadTimeoutOr(null);
            final TimeoutActionType timeoutActionType =
                    invocationConfiguration.getReadTimeoutActionOr(null);
            final TransportChannel<OUTPUT> resultChannel = JRoutine.transport()
                                                                   .withInvocation()
                                                                   .withOutputOrder(outputOrderType)
                                                                   .withOutputMaxSize(
                                                                           Integer.MAX_VALUE)
                                                                   .withOutputTimeout(
                                                                           TimeDuration.ZERO)
                                                                   .withReadTimeout(readTimeout)
                                                                   .withReadTimeoutAction(
                                                                           timeoutActionType)
                                                                   .withLog(log)
                                                                   .withLogLevel(logLevel)
                                                                   .set()
                                                                   .buildChannel();
            mTransportResultInput = resultChannel.input();
            mTransportResultOutput = resultChannel.output();
        }

        public boolean abort() {

            bindService();
            return mTransportParamInput.abort();
        }

        public boolean abort(@Nullable final Throwable reason) {

            bindService();
            return mTransportParamInput.abort(reason);
        }

        public boolean isOpen() {

            return mTransportParamInput.isOpen();
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> after(@Nonnull final TimeDuration delay) {

            mTransportParamInput.after(delay);
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> after(final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mTransportParamInput.after(delay, timeUnit);
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> now() {

            mTransportParamInput.now();
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> pass(
                @Nullable final OutputChannel<? extends INPUT> channel) {

            bindService();
            mTransportParamInput.pass(channel);
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> pass(
                @Nullable final Iterable<? extends INPUT> inputs) {

            bindService();
            mTransportParamInput.pass(inputs);
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> pass(@Nullable final INPUT input) {

            bindService();
            mTransportParamInput.pass(input);
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> pass(@Nullable final INPUT... inputs) {

            bindService();
            mTransportParamInput.pass(inputs);
            return this;
        }

        @Nonnull
        public OutputChannel<OUTPUT> result() {

            bindService();
            mTransportParamInput.close();
            return mTransportResultOutput;
        }

        private void bindService() {

            synchronized (mMutex) {

                if (mIsBound) {

                    return;
                }

                final Context context = mContext;
                mConnection = new RoutineServiceConnection();
                mIsBound = context.bindService(new Intent(context, mServiceClass), mConnection,
                                               Context.BIND_AUTO_CREATE);

                if (!mIsBound) {

                    throw new RoutineException(
                            "failed to bind to service: " + mServiceClass.getName()
                                    + ", remember to add it to the Android manifest file!");
                }
            }
        }

        private void unbindService() {

            synchronized (mMutex) {

                if (mIsUnbound) {

                    return;
                }

                mIsUnbound = true;

                // postpone unbind to avoid crashing the IPC
                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    public void run() {

                        mContext.unbindService(mConnection);
                    }
                });
            }
        }

        /**
         * Output consumer sending messages to the service.
         */
        private class ConnectionOutputConsumer implements OutputConsumer<INPUT> {

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
         * Handler implementation managing incoming messages from the service.
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
                            mTransportResultInput.pass((OUTPUT) getValue(msg));
                            break;

                        case RoutineService.MSG_COMPLETE:
                            mTransportResultInput.close();
                            unbindService();
                            break;

                        case RoutineService.MSG_ABORT:
                            mTransportResultInput.abort(getAbortError(msg));
                            unbindService();
                            break;

                        default:
                            super.handleMessage(msg);
                    }

                } catch (final Throwable t) {

                    logger.err(t, "error while parsing service message");

                    try {

                        final Message message = Message.obtain(null, RoutineService.MSG_ABORT);
                        putError(message.getData(), mUUID, t);
                        mOutMessenger.send(message);

                    } catch (final Throwable ignored) {

                        logger.err(ignored, "error while sending service abort message");
                    }

                    mTransportResultInput.abort(t);
                    unbindService();
                }
            }
        }

        /**
         * Service connection implementation managing the service communication state.
         */
        private class RoutineServiceConnection implements ServiceConnection {

            private ConnectionOutputConsumer mConsumer;

            public void onServiceConnected(final ComponentName name, final IBinder service) {

                final Logger logger = mLogger;
                logger.dbg("service connected: %s", name);
                mOutMessenger = new Messenger(service);
                final ServiceConfiguration serviceConfiguration = mServiceConfiguration;
                final Message message = Message.obtain(null, RoutineService.MSG_INIT);

                if (mIsParallel) {

                    logger.dbg("sending parallel invocation message");
                    putParallelInvocation(message.getData(), mUUID, mInvocationClass,
                                          mInvocationConfiguration,
                                          serviceConfiguration.getRunnerClassOr(null),
                                          serviceConfiguration.getLogClassOr(null));

                } else {

                    logger.dbg("sending async invocation message");
                    putAsyncInvocation(message.getData(), mUUID, mInvocationClass,
                                       mInvocationConfiguration,
                                       serviceConfiguration.getRunnerClassOr(null),
                                       serviceConfiguration.getLogClassOr(null));
                }

                message.replyTo = mInMessenger;

                try {

                    mOutMessenger.send(message);
                    mConsumer = new ConnectionOutputConsumer();
                    mTransportParamOutput.passTo(mConsumer);

                } catch (final RemoteException e) {

                    logger.err(e, "error while sending service invocation message");
                    mTransportResultInput.abort(e);
                    unbindService();
                }
            }

            public void onServiceDisconnected(final ComponentName name) {

                mLogger.dbg("service disconnected: %s", name);
                mTransportParamOutput.abort(new ServiceDisconnectedException(name));
            }
        }
    }
}
