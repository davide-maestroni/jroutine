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
import com.gh.bmd.jrt.android.runner.Runners;
import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.android.service.ServiceDisconnectedException;
import com.gh.bmd.jrt.builder.ChannelConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.OutputConsumer;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.channel.TransportChannel;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.routine.TemplateRoutine;
import com.gh.bmd.jrt.runner.TemplateExecution;
import com.gh.bmd.jrt.util.Reflection;
import com.gh.bmd.jrt.util.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.android.invocation.ContextInvocations.factoryFrom;
import static com.gh.bmd.jrt.android.invocation.ContextInvocations.factoryOf;
import static com.gh.bmd.jrt.android.service.RoutineService.getAbortError;
import static com.gh.bmd.jrt.android.service.RoutineService.getValue;
import static com.gh.bmd.jrt.android.service.RoutineService.putAsyncInvocation;
import static com.gh.bmd.jrt.android.service.RoutineService.putError;
import static com.gh.bmd.jrt.android.service.RoutineService.putInvocationId;
import static com.gh.bmd.jrt.android.service.RoutineService.putParallelInvocation;
import static com.gh.bmd.jrt.android.service.RoutineService.putValue;
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

    private final ServiceContext mContext;

    private final Object[] mFactoryArgs;

    private final Class<? extends ContextInvocation<INPUT, OUTPUT>> mInvocationClass;

    private final InvocationConfiguration mInvocationConfiguration;

    private final Logger mLogger;

    private final Routine<INPUT, OUTPUT> mRoutine;

    private final ServiceConfiguration mServiceConfiguration;

    /**
     * Constructor.
     *
     * @param context                 the service context.
     * @param invocationClass         the invocation class.
     * @param factoryArgs             the invocation factory arguments.
     * @param invocationConfiguration the invocation configuration.
     * @param serviceConfiguration    the service configuration.
     * @throws java.lang.IllegalArgumentException if at least one of the parameter is invalid.
     */
    ServiceRoutine(@Nonnull final ServiceContext context,
            @Nonnull final Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass,
            @Nullable final Object[] factoryArgs,
            @Nonnull final InvocationConfiguration invocationConfiguration,
            @Nonnull final ServiceConfiguration serviceConfiguration) {

        final Context serviceContext = context.getServiceContext();

        if (serviceContext == null) {

            throw new IllegalStateException("the service context has been destroyed");
        }

        mContext = context;
        mInvocationClass = invocationClass;
        mFactoryArgs = (factoryArgs != null) ? factoryArgs : Reflection.NO_ARGS;
        mInvocationConfiguration = invocationConfiguration;
        mServiceConfiguration = serviceConfiguration;
        mLogger = invocationConfiguration.newLogger(this);
        mRoutine = JRoutine.on(factoryFrom(serviceContext.getApplicationContext(),
                                           factoryOf(invocationClass, factoryArgs)))
                           .invocations()
                           .with(invocationConfiguration)
                           .set()
                           .buildRoutine();
        final Logger logger = mLogger;
        logger.dbg("building service routine on invocation %s with configurations: %s - %s",
                   invocationClass.getName(), invocationConfiguration, serviceConfiguration);
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> asyncInvoke() {

        return new ServiceChannel<INPUT, OUTPUT>(false, mContext, mInvocationClass, mFactoryArgs,
                                                 mInvocationConfiguration, mServiceConfiguration,
                                                 mLogger);
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> parallelInvoke() {

        return new ServiceChannel<INPUT, OUTPUT>(true, mContext, mInvocationClass, mFactoryArgs,
                                                 mInvocationConfiguration, mServiceConfiguration,
                                                 mLogger);
    }

    @Nonnull
    public InvocationChannel<INPUT, OUTPUT> syncInvoke() {

        return mRoutine.syncInvoke();
    }

    @Override
    public void purge() {

        mRoutine.purge();
    }

    /**
     * Service invocation channel implementation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class ServiceChannel<INPUT, OUTPUT> implements InvocationChannel<INPUT, OUTPUT> {

        private final ServiceContext mContext;

        private final Object[] mFactoryArgs;

        private final Messenger mInMessenger;

        private final Class<? extends ContextInvocation<INPUT, OUTPUT>> mInvocationClass;

        private final InvocationConfiguration mInvocationConfiguration;

        private final boolean mIsParallel;

        private final Logger mLogger;

        private final Object mMutex = new Object();

        private final ServiceConfiguration mServiceConfiguration;

        private final TransportChannel<INPUT> mTransportInput;

        private final TransportChannel<OUTPUT> mTransportOutput;

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
         * @param factoryArgs             the invocation factory arguments.
         * @param invocationConfiguration the invocation configuration.
         * @param serviceConfiguration    the service configuration.
         * @param logger                  the routine logger.
         */
        private ServiceChannel(boolean isParallel, @Nonnull final ServiceContext context,
                @Nonnull Class<? extends ContextInvocation<INPUT, OUTPUT>> invocationClass,
                @Nonnull final Object[] factoryArgs,
                @Nonnull final InvocationConfiguration invocationConfiguration,
                @Nonnull final ServiceConfiguration serviceConfiguration,
                @Nonnull final Logger logger) {

            mUUID = randomUUID().toString();
            mIsParallel = isParallel;
            mContext = context;
            mInMessenger = new Messenger(new IncomingHandler(
                    serviceConfiguration.getResultLooperOr(Looper.getMainLooper())));
            mInvocationClass = invocationClass;
            mFactoryArgs = factoryArgs;
            mInvocationConfiguration = invocationConfiguration;
            mServiceConfiguration = serviceConfiguration;
            mLogger = logger;
            final Log log = logger.getLog();
            final LogLevel logLevel = logger.getLogLevel();
            final OrderType inputOrderType = invocationConfiguration.getInputOrderTypeOr(null);
            final int inputMaxSize =
                    invocationConfiguration.getInputMaxSizeOr(ChannelConfiguration.DEFAULT);
            final TimeDuration inputTimeout = invocationConfiguration.getInputTimeoutOr(null);
            mTransportInput = JRoutine.transport()
                                      .channels()
                                      .withChannelOrder(inputOrderType)
                                      .withChannelMaxSize(inputMaxSize)
                                      .withChannelTimeout(inputTimeout)
                                      .withLog(log)
                                      .withLogLevel(logLevel)
                                      .set()
                                      .buildChannel();
            final int outputMaxSize =
                    invocationConfiguration.getOutputMaxSizeOr(ChannelConfiguration.DEFAULT);
            final TimeDuration outputTimeout = invocationConfiguration.getOutputTimeoutOr(null);
            final TimeDuration readTimeout = invocationConfiguration.getReadTimeoutOr(null);
            final TimeoutActionType timeoutActionType =
                    invocationConfiguration.getReadTimeoutActionOr(null);
            mTransportOutput = JRoutine.transport()
                                       .channels()
                                       .withChannelMaxSize(outputMaxSize)
                                       .withChannelTimeout(outputTimeout)
                                       .withReadTimeout(readTimeout)
                                       .withReadTimeoutAction(timeoutActionType)
                                       .withLog(log)
                                       .withLogLevel(logLevel)
                                       .set()
                                       .buildChannel();
        }

        public boolean abort() {

            bindService();
            return mTransportInput.abort();
        }

        public boolean abort(@Nullable final Throwable reason) {

            bindService();
            return mTransportInput.abort(reason);
        }

        public boolean isOpen() {

            return mTransportInput.isOpen();
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> after(@Nonnull final TimeDuration delay) {

            mTransportInput.after(delay);
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> after(final long delay,
                @Nonnull final TimeUnit timeUnit) {

            mTransportInput.after(delay, timeUnit);
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> now() {

            mTransportInput.now();
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> orderByCall() {

            mTransportInput.orderByCall();
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> orderByChance() {

            mTransportInput.orderByChance();
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> orderByDelay() {

            mTransportInput.orderByDelay();
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> pass(
                @Nullable final OutputChannel<? extends INPUT> channel) {

            bindService();
            mTransportInput.pass(channel);
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> pass(
                @Nullable final Iterable<? extends INPUT> inputs) {

            bindService();
            mTransportInput.pass(inputs);
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> pass(@Nullable final INPUT input) {

            bindService();
            mTransportInput.pass(input);
            return this;
        }

        @Nonnull
        public InvocationChannel<INPUT, OUTPUT> pass(@Nullable final INPUT... inputs) {

            bindService();
            mTransportInput.pass(inputs);
            return this;
        }

        @Nonnull
        public OutputChannel<OUTPUT> result() {

            bindService();
            mTransportInput.close();
            return mTransportOutput;
        }

        private void bindService() {

            synchronized (mMutex) {

                if (mIsBound) {

                    return;
                }

                final ServiceContext context = mContext;
                final Context serviceContext = context.getServiceContext();

                if (serviceContext == null) {

                    throw new IllegalStateException("the service context has been destroyed");
                }

                final Intent intent = context.getServiceIntent();
                mConnection = new RoutineServiceConnection();
                mIsBound =
                        serviceContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

                if (!mIsBound) {

                    throw new RoutineException("failed to bind to service: " + intent
                                                       + ", remember to add the service "
                                                       + "declaration to the Android manifest "
                                                       + "file!");
                }
            }
        }

        private void unbindService() {

            synchronized (mMutex) {

                if (mIsUnbound) {

                    return;
                }

                mIsUnbound = true;

                // unbind on main thread to avoid crashing the IPC
                Runners.mainRunner().run(new TemplateExecution() {

                    public void run() {

                        final Context serviceContext = mContext.getServiceContext();

                        if (serviceContext != null) {

                            serviceContext.unbindService(mConnection);
                        }
                    }
                }, 0, TimeUnit.MILLISECONDS);
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

                        case RoutineService.MSG_DATA: {

                            mTransportOutput.pass((OUTPUT) getValue(msg));
                        }

                        break;

                        case RoutineService.MSG_COMPLETE: {

                            mTransportOutput.close();
                            unbindService();
                        }

                        break;

                        case RoutineService.MSG_ABORT: {

                            mTransportOutput.abort(
                                    InvocationException.wrapIfNeeded(getAbortError(msg)));
                            unbindService();
                        }

                        break;

                        default: {

                            super.handleMessage(msg);
                        }
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

                    mTransportOutput.abort(t);
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
                    putParallelInvocation(message.getData(), mUUID, mInvocationClass, mFactoryArgs,
                                          mInvocationConfiguration,
                                          serviceConfiguration.getRunnerClassOr(null),
                                          serviceConfiguration.getLogClassOr(null));

                } else {

                    logger.dbg("sending async invocation message");
                    putAsyncInvocation(message.getData(), mUUID, mInvocationClass, mFactoryArgs,
                                       mInvocationConfiguration,
                                       serviceConfiguration.getRunnerClassOr(null),
                                       serviceConfiguration.getLogClassOr(null));
                }

                message.replyTo = mInMessenger;

                try {

                    mOutMessenger.send(message);
                    mConsumer = new ConnectionOutputConsumer();
                    mTransportInput.passTo(mConsumer);

                } catch (final RemoteException e) {

                    logger.err(e, "error while sending service invocation message");
                    mTransportOutput.abort(e);
                    unbindService();
                }
            }

            public void onServiceDisconnected(final ComponentName name) {

                mLogger.dbg("service disconnected: %s", name);
                mTransportInput.abort(new ServiceDisconnectedException(name));
            }
        }
    }
}
