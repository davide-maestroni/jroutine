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

package com.github.dm.jrt.android.core;

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

import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.TargetInvocationFactory;
import com.github.dm.jrt.android.core.service.InvocationService;
import com.github.dm.jrt.android.core.service.ServiceDisconnectedException;
import com.github.dm.jrt.core.AbstractRoutine;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.runner.AndroidRunners.mainRunner;
import static com.github.dm.jrt.android.core.service.InvocationService.getAbortError;
import static com.github.dm.jrt.android.core.service.InvocationService.getValue;
import static com.github.dm.jrt.android.core.service.InvocationService.putError;
import static com.github.dm.jrt.android.core.service.InvocationService.putInvocation;
import static com.github.dm.jrt.android.core.service.InvocationService.putInvocationId;
import static com.github.dm.jrt.android.core.service.InvocationService.putValue;
import static com.github.dm.jrt.core.util.Reflection.NO_ARGS;
import static com.github.dm.jrt.core.util.Reflection.findBestMatchingConstructor;
import static java.util.UUID.randomUUID;

/**
 * Routine implementation employing an Android Service to run its invocations.
 * <p>
 * Created by davide-maestroni on 01/08/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class ServiceRoutine<IN, OUT> extends AbstractRoutine<IN, OUT> {

  private final ServiceContext mContext;

  private final InvocationConfiguration mInvocationConfiguration;

  private final ServiceConfiguration mServiceConfiguration;

  private final TargetInvocationFactory<IN, OUT> mTargetFactory;

  /**
   * Constructor.
   *
   * @param context                 the Service context.
   * @param target                  the invocation factory target.
   * @param invocationConfiguration the invocation configuration.
   * @param serviceConfiguration    the Service configuration.
   * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
   *                                            parameters was found for the configured log or the
   *                                            configured runner.
   * @throws java.lang.IllegalStateException    if the specified context is no more valid.
   */
  ServiceRoutine(@NotNull final ServiceContext context,
      @NotNull final TargetInvocationFactory<IN, OUT> target,
      @NotNull final InvocationConfiguration invocationConfiguration,
      @NotNull final ServiceConfiguration serviceConfiguration) {
    super(invocationConfiguration);
    final Context serviceContext = context.getServiceContext();
    if (serviceContext == null) {
      throw new IllegalStateException("the Service Context has been destroyed");
    }

    final Class<? extends Runner> runnerClass = serviceConfiguration.getRunnerClassOrElse(null);
    if (runnerClass != null) {
      findBestMatchingConstructor(runnerClass, serviceConfiguration.getRunnerArgsOrElse(NO_ARGS));
    }

    final Class<? extends Log> logClass = serviceConfiguration.getLogClassOrElse(null);
    if (logClass != null) {
      findBestMatchingConstructor(logClass, serviceConfiguration.getLogArgsOrElse(NO_ARGS));
    }

    mContext = context;
    mTargetFactory = target;
    mInvocationConfiguration = invocationConfiguration;
    mServiceConfiguration = serviceConfiguration;
    final Class<? extends ContextInvocation<IN, OUT>> invocationClass = target.getInvocationClass();
    getLogger().dbg("building Service routine on invocation %s with configurations: %s - %s",
        invocationClass.getName(), invocationConfiguration, serviceConfiguration);
  }

  @NotNull
  @Override
  protected Invocation<IN, OUT> newInvocation() throws Exception {
    return new ServiceInvocation<IN, OUT>(mContext, mTargetFactory, mInvocationConfiguration,
        mServiceConfiguration, getLogger());
  }

  /**
   * Channel consumer sending messages to the Service.
   *
   * @param <IN> the input data type.
   */
  private static class ConnectionChannelConsumer<IN> implements ChannelConsumer<IN> {

    private final Messenger mInMessenger;

    private final String mInvocationId;

    private final Messenger mOutMessenger;

    /**
     * Constructor.
     *
     * @param invocationId the invocation ID.
     * @param inMessenger  the messenger receiving data from the Service.
     * @param outMessenger the messenger sending data to the Service.
     */
    private ConnectionChannelConsumer(@NotNull final String invocationId,
        @NotNull final Messenger inMessenger, @NotNull final Messenger outMessenger) {
      mInvocationId = invocationId;
      mInMessenger = inMessenger;
      mOutMessenger = outMessenger;
    }

    @Override
    public void onComplete() throws RemoteException {
      final Message message = Message.obtain(null, InvocationService.MSG_COMPLETE);
      putInvocationId(message.getData(), mInvocationId);
      message.replyTo = mInMessenger;
      mOutMessenger.send(message);
    }

    @Override
    public void onError(@NotNull final RoutineException error) throws RemoteException {
      final Message message = Message.obtain(null, InvocationService.MSG_ABORT);
      putError(message.getData(), mInvocationId, error);
      message.replyTo = mInMessenger;
      mOutMessenger.send(message);
    }

    @Override
    public void onOutput(final IN input) throws RemoteException {
      final Message message = Message.obtain(null, InvocationService.MSG_DATA);
      putValue(message.getData(), mInvocationId, input);
      message.replyTo = mInMessenger;
      mOutMessenger.send(message);
    }
  }

  /**
   * Handler implementation managing incoming messages from the Service.
   *
   * @param <OUT> the output data type.
   */
  private static class IncomingHandler<OUT> extends Handler {

    private final ServiceContext mContext;

    private final Logger mLogger;

    private final Channel<OUT, OUT> mOutputChannel;

    private ServiceConnection mConnection;

    private boolean mIsUnbound;

    /**
     * Constructor.
     *
     * @param looper        the message Looper.
     * @param context       the Service context.
     * @param outputChannel the output channel.
     * @param logger        the logger instance.
     */
    private IncomingHandler(@NotNull final Looper looper, @NotNull final ServiceContext context,
        @NotNull final Channel<OUT, OUT> outputChannel, @NotNull final Logger logger) {
      super(looper);
      mContext = context;
      mOutputChannel = outputChannel;
      mLogger = logger;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(@NotNull final Message msg) {
      final Logger logger = mLogger;
      logger.dbg("incoming Service message: %s", msg);
      try {
        switch (msg.what) {
          case InvocationService.MSG_DATA:
            mOutputChannel.pass((OUT) getValue(msg));
            break;

          case InvocationService.MSG_COMPLETE:
            mOutputChannel.close();
            unbindService();
            break;

          case InvocationService.MSG_ABORT:
            mOutputChannel.abort(InvocationException.wrapIfNeeded(getAbortError(msg)));
            unbindService();
            break;

          default:
            super.handleMessage(msg);
        }

      } catch (final Throwable t) {
        logger.wrn(t, "error while handling Service message");
        mOutputChannel.abort(t);
        unbindService();
        InvocationInterruptedException.throwIfInterrupt(t);
      }
    }

    private void setConnection(@NotNull final ServiceConnection connection) {
      mConnection = connection;
    }

    private void unbindService() {
      if (mIsUnbound) {
        return;
      }

      mIsUnbound = true;
      final Context serviceContext = mContext.getServiceContext();
      if (serviceContext != null) {
        // Unbind on main thread to avoid crashing the IPC
        Runners.zeroDelayRunner(mainRunner()).run(new Execution() {

          @Override
          public void run() {
            // Unfortunately there is no way to know if the context is still valid
            try {
              serviceContext.unbindService(mConnection);

            } catch (final Throwable t) {
              InvocationInterruptedException.throwIfInterrupt(t);
              mLogger.wrn(t, "unbinding failed (maybe the connection was leaked...)");
            }
          }
        }, 0, TimeUnit.MILLISECONDS);
      }
    }
  }

  /**
   * Service connection implementation managing the Service communication state.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class RoutineServiceConnection<IN, OUT> implements ServiceConnection {

    private final IncomingHandler<OUT> mIncomingHandler;

    private final Channel<IN, IN> mInputChannel;

    private final InvocationConfiguration mInvocationConfiguration;

    private final String mInvocationId;

    private final Logger mLogger;

    private final Channel<OUT, OUT> mOutputChannel;

    private final ServiceConfiguration mServiceConfiguration;

    private final TargetInvocationFactory<IN, OUT> mTargetFactory;

    /**
     * Constructor.
     *
     * @param invocationId            the invocation ID.
     * @param target                  the invocation factory target.
     * @param invocationConfiguration the invocation configuration.
     * @param serviceConfiguration    the Service configuration.
     * @param handler                 the handler managing messages from the Service.
     * @param inputChannel            the input channel.
     * @param outputChannel           the output channel.
     * @param logger                  the logger instance.
     */
    private RoutineServiceConnection(@NotNull final String invocationId,
        @NotNull final TargetInvocationFactory<IN, OUT> target,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final ServiceConfiguration serviceConfiguration,
        @NotNull final IncomingHandler<OUT> handler, @NotNull final Channel<IN, IN> inputChannel,
        @NotNull final Channel<OUT, OUT> outputChannel, @NotNull final Logger logger) {
      mInvocationId = invocationId;
      mTargetFactory = target;
      mInvocationConfiguration = invocationConfiguration;
      mServiceConfiguration = serviceConfiguration;
      mIncomingHandler = handler;
      mInputChannel = inputChannel;
      mOutputChannel = outputChannel;
      mLogger = logger;
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
      final Logger logger = mLogger;
      logger.dbg("Service connected: %s", name);
      final Messenger outMessenger = new Messenger(service);
      final Message message = Message.obtain(null, InvocationService.MSG_INIT);
      logger.dbg("sending async invocation message");
      final String invocationId = mInvocationId;
      final TargetInvocationFactory<IN, OUT> targetFactory = mTargetFactory;
      final ServiceConfiguration serviceConfiguration = mServiceConfiguration;
      putInvocation(message.getData(), invocationId, targetFactory.getInvocationClass(),
          targetFactory.getFactoryArgs(), mInvocationConfiguration,
          serviceConfiguration.getRunnerClassOrElse(null),
          serviceConfiguration.getRunnerArgsOrElse((Object[]) null),
          serviceConfiguration.getLogClassOrElse(null),
          serviceConfiguration.getLogArgsOrElse((Object[]) null));
      final Messenger inMessenger = new Messenger(mIncomingHandler);
      message.replyTo = inMessenger;
      try {
        outMessenger.send(message);
        mInputChannel.bind(
            new ConnectionChannelConsumer<IN>(invocationId, inMessenger, outMessenger));

      } catch (final Throwable t) {
        logger.err(t, "error while sending Service invocation message");
        mIncomingHandler.unbindService();
        mOutputChannel.abort(InvocationException.wrapIfNeeded(t));
        InvocationInterruptedException.throwIfInterrupt(t);
      }
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
      mLogger.dbg("Service disconnected: %s", name);
      mOutputChannel.abort(new ServiceDisconnectedException(name));
    }
  }

  /**
   * Invocation implementation delegating the input processing to a dedicated Service.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class ServiceInvocation<IN, OUT> extends TemplateInvocation<IN, OUT> {

    private final ServiceContext mContext;

    private final InvocationConfiguration mInvocationConfiguration;

    private final Logger mLogger;

    private final ServiceConfiguration mServiceConfiguration;

    private final TargetInvocationFactory<IN, OUT> mTargetFactory;

    private Channel<IN, IN> mInputChannel;

    private Channel<OUT, OUT> mOutputChannel;

    /**
     * Constructor.
     *
     * @param context                 the Service context.
     * @param target                  the invocation factory target.
     * @param invocationConfiguration the invocation configuration.
     * @param serviceConfiguration    the Service configuration.
     * @param logger                  the logger instance.
     */
    private ServiceInvocation(@NotNull final ServiceContext context,
        @NotNull final TargetInvocationFactory<IN, OUT> target,
        @NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final ServiceConfiguration serviceConfiguration, @NotNull final Logger logger) {
      mContext = context;
      mTargetFactory = target;
      mInvocationConfiguration = invocationConfiguration;
      mServiceConfiguration = serviceConfiguration;
      mLogger = logger;
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
      mInputChannel.abort(reason);
    }

    @Override
    public void onComplete(@NotNull final Channel<OUT, ?> result) {
      bind(result);
      mInputChannel.close();
    }

    @Override
    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) {
      bind(result);
      mInputChannel.pass(input);
    }

    @Override
    public void onRecycle(final boolean isReused) {
      mInputChannel = null;
      mOutputChannel = null;
    }

    @Override
    public void onRestart() {
      final Logger logger = mLogger;
      mInputChannel = JRoutineCore.io()
                                  .applyChannelConfiguration()
                                  .withLog(logger.getLog())
                                  .withLogLevel(logger.getLogLevel())
                                  .configured()
                                  .buildChannel();
      mOutputChannel = JRoutineCore.io()
                                   .applyChannelConfiguration()
                                   .withLog(logger.getLog())
                                   .withLogLevel(logger.getLogLevel())
                                   .configured()
                                   .buildChannel();
      final Looper looper = mServiceConfiguration.getMessageLooperOrElse(Looper.getMainLooper());
      final IncomingHandler<OUT> handler =
          new IncomingHandler<OUT>(looper, mContext, mOutputChannel, logger);
      handler.setConnection(bindService(handler));
    }

    private void bind(@NotNull final Channel<OUT, ?> result) {
      final Channel<?, OUT> outputChannel = mOutputChannel;
      if (!outputChannel.isBound()) {
        outputChannel.bind(result);
      }
    }

    @NotNull
    private ServiceConnection bindService(@NotNull final IncomingHandler<OUT> handler) {
      final ServiceContext context = mContext;
      final Context serviceContext = context.getServiceContext();
      if (serviceContext == null) {
        throw new IllegalStateException("the Service Context has been destroyed");
      }

      final Intent intent = context.getServiceIntent();
      final RoutineServiceConnection<IN, OUT> connection =
          new RoutineServiceConnection<IN, OUT>(randomUUID().toString(), mTargetFactory,
              mInvocationConfiguration, mServiceConfiguration, handler, mInputChannel,
              mOutputChannel, mLogger);
      if (!serviceContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
        throw new RoutineException("failed to bind to Service: " + intent
            + ", remember to add the Service declaration to the Android manifest file!");
      }

      return connection;
    }
  }
}
