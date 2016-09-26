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

package com.github.dm.jrt.core;

import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationInterruptedException;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of an invocation execution.
 * <p>
 * The class does not implement any synchronization mechanism, so, it's up to the caller to ensure
 * that the methods are never concurrently called.
 * <p>
 * Created by davide-maestroni on 09/24/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class InvocationExecution<IN, OUT> implements Execution, InvocationObserver<IN, OUT> {

  private final InputData<IN> mInputData = new InputData<IN>();

  private final InvocationManager<IN, OUT> mInvocationManager;

  private final Logger mLogger;

  private final ExecutionObserver<IN> mObserver;

  private final ResultChannel<OUT> mResultChannel;

  private volatile AbortExecution mAbortExecution;

  private int mExecutionCount = 1;

  private Invocation<IN, OUT> mInvocation;

  private boolean mIsInitialized;

  private boolean mIsTerminated;

  private boolean mIsWaitingAbortInvocation;

  private boolean mIsWaitingInvocation;

  /**
   * Constructor.
   *
   * @param manager the invocation manager.
   * @param inputs  the input iterator.
   * @param result  the result channel.
   * @param logger  the logger instance.
   */
  InvocationExecution(@NotNull final InvocationManager<IN, OUT> manager,
      @NotNull final ExecutionObserver<IN> inputs, @NotNull final ResultChannel<OUT> result,
      @NotNull final Logger logger) {
    mInvocationManager = ConstantConditions.notNull("invocation manager", manager);
    mObserver = ConstantConditions.notNull("input iterator", inputs);
    mResultChannel = ConstantConditions.notNull("result channel", result);
    mLogger = logger.subContextLogger(this);
  }

  /**
   * Returns the abort execution.
   *
   * @return the execution.
   */
  @NotNull
  public Execution abort() {
    if (mAbortExecution == null) {
      mAbortExecution = new AbortExecution();
    }

    return mAbortExecution;
  }

  public void onCreate(@NotNull final Invocation<IN, OUT> invocation) {
    mIsWaitingInvocation = false;
    final ResultChannel<OUT> resultChannel = mResultChannel;
    resultChannel.stopWaitingInvocation();
    final int count = mExecutionCount;
    mExecutionCount = 1;
    resultChannel.enterInvocation();
    try {
      for (int i = 0; i < count; ++i) {
        execute(invocation);
      }

    } finally {
      resultChannel.exitInvocation();
      final AbortExecution abortExecution = mAbortExecution;
      if (mIsWaitingAbortInvocation && (abortExecution != null)) {
        abortExecution.onCreate(invocation);
      }
    }
  }

  /**
   * Forces the recycling of the invocation.
   *
   * @param reason the reason.
   */
  void recycle(@NotNull final Throwable reason) {
    final Invocation<IN, OUT> invocation = mInvocation;
    if ((invocation != null) && !mIsTerminated) {
      mIsTerminated = true;
      final InvocationManager<IN, OUT> manager = mInvocationManager;
      if (mIsInitialized) {
        try {
          invocation.onAbort(InvocationException.wrapIfNeeded(reason));
          invocation.onRecycle(true);
          manager.recycle(invocation);

        } catch (final Throwable t) {
          manager.discard(invocation);
          InvocationInterruptedException.throwIfInterrupt(t);
        }

      } else {
        // Initialization failed, so just discard the invocation
        manager.discard(invocation);
      }
    }
  }

  private void execute(@NotNull final Invocation<IN, OUT> invocation) {
    final Logger logger = mLogger;
    final ExecutionObserver<IN> observer = mObserver;
    final InvocationManager<IN, OUT> manager = mInvocationManager;
    final ResultChannel<OUT> resultChannel = mResultChannel;
    try {
      logger.dbg("running execution");
      final boolean isComplete;
      try {
        if (mInvocation == null) {
          mInvocation = invocation;
          logger.dbg("initializing invocation: %s", invocation);
          invocation.onRestart();
          mIsInitialized = true;
        }

        final InputData<IN> inputData = mInputData;
        if (observer.onFirstInput(inputData)) {
          invocation.onInput(inputData.data, resultChannel);
          while (observer.onNextInput(inputData)) {
            invocation.onInput(inputData.data, resultChannel);
          }
        }

      } finally {
        isComplete = observer.onConsumeComplete();
      }

      if (isComplete) {
        invocation.onComplete(resultChannel);
        try {
          mIsTerminated = true;
          invocation.onRecycle(true);
          manager.recycle(invocation);

        } catch (final Throwable t) {
          logger.wrn(t, "Discarding invocation since it failed to be recycled");
          manager.discard(invocation);
          InvocationInterruptedException.throwIfInterrupt(t);

        } finally {
          resultChannel.closeImmediately();
          observer.onInvocationComplete();
        }
      }

    } catch (final Throwable t) {
      if (!resultChannel.abortImmediately(t)) {
        // Needed if the result channel is explicitly closed by the invocation
        recycle(t);
      }

      InvocationInterruptedException.throwIfInterrupt(t);
    }
  }

  /**
   * Interface defining an execution observer.
   *
   * @param <IN> the input data type.
   */
  interface ExecutionObserver<IN> {

    /**
     * Returns the exception identifying the abortion reason.
     *
     * @return the reason of the abortion.
     */
    @NotNull
    RoutineException getAbortException();

    /**
     * Notifies that the execution abortion is complete.
     */
    void onAbortComplete();

    /**
     * Checks if the input has completed, that is, all the inputs have been consumed.
     *
     * @return whether the input has completed.
     */
    boolean onConsumeComplete();

    /**
     * Asks the observer for the first input to be processed.
     *
     * @param inputData the input data to be filled.
     * @return whether any data is available.
     */
    boolean onFirstInput(@NotNull InputData<IN> inputData);

    /**
     * Notifies that the invocation execution is complete.
     */
    void onInvocationComplete();

    /**
     * Asks the observer for the next input to be processed.
     *
     * @param inputData the input data to be filled.
     * @return whether any data is available.
     */
    boolean onNextInput(@NotNull InputData<IN> inputData);
  }

  /**
   * Input data class.
   *
   * @param <IN> the input data type.
   */
  static class InputData<IN> {

    /**
     * The data object.
     */
    IN data;
  }

  /**
   * Abort execution implementation.
   */
  private class AbortExecution implements Execution, InvocationObserver<IN, OUT> {

    public void run() {
      if (mIsWaitingAbortInvocation) {
        return;
      }

      final Invocation<IN, OUT> invocation = mInvocation;
      mIsWaitingAbortInvocation = (invocation == null);
      if (mIsWaitingInvocation) {
        return;
      }

      if (invocation != null) {
        onCreate(invocation);

      } else if (!mInvocationManager.create(this)) {
        mResultChannel.startWaitingInvocation();
      }
    }

    public void onCreate(@NotNull final Invocation<IN, OUT> invocation) {
      mIsWaitingAbortInvocation = false;
      final Logger logger = mLogger;
      final ExecutionObserver<IN> observer = mObserver;
      final InvocationManager<IN, OUT> manager = mInvocationManager;
      final ResultChannel<OUT> resultChannel = mResultChannel;
      resultChannel.enterInvocation();
      try {
        final RoutineException exception = observer.getAbortException();
        logger.dbg(exception, "aborting invocation");
        try {
          if (!mIsTerminated) {
            if (mInvocation == null) {
              mInvocation = invocation;
              logger.dbg("initializing invocation: %s", invocation);
              invocation.onRestart();
              mIsInitialized = true;
            }

            if (mIsInitialized) {
              mIsTerminated = true;
              try {
                invocation.onAbort(exception);
                invocation.onRecycle(true);

              } catch (final Throwable t) {
                manager.discard(invocation);
                throw t;
              }

              manager.recycle(invocation);

            } else {
              // Initialization failed, so just discard the invocation
              mIsTerminated = true;
              manager.discard(invocation);
            }
          }

          resultChannel.close(exception);

        } catch (final Throwable t) {
          if (!mIsTerminated) {
            mIsTerminated = true;
            manager.discard(invocation);
          }

          resultChannel.close(t);
          InvocationInterruptedException.throwIfInterrupt(t);
        }

      } finally {
        resultChannel.exitInvocation();
        observer.onAbortComplete();
        if (mIsWaitingInvocation) {
          InvocationExecution.this.onCreate(invocation);
        }
      }
    }

    public void onError(@NotNull final Throwable error) {
      final ResultChannel<OUT> resultChannel = mResultChannel;
      resultChannel.stopWaitingInvocation();
      resultChannel.close(error);
    }
  }

  public void run() {
    if (mIsWaitingInvocation) {
      ++mExecutionCount;
      return;
    }

    final Invocation<IN, OUT> invocation = mInvocation;
    mIsWaitingInvocation = (invocation == null);
    if (mIsWaitingAbortInvocation) {
      return;
    }

    if (invocation != null) {
      onCreate(invocation);

    } else if (!mInvocationManager.create(this)) {
      mResultChannel.startWaitingInvocation();
    }
  }

  public void onError(@NotNull final Throwable error) {
    final ResultChannel<OUT> resultChannel = mResultChannel;
    resultChannel.stopWaitingInvocation();
    resultChannel.close(error);
  }
}
