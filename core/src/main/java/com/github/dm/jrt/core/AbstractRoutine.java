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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration.InvocationModeType;
import com.github.dm.jrt.core.invocation.InterruptedInvocationException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.log.Logger;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Execution;
import com.github.dm.jrt.core.runner.Runner;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.SimpleQueue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Basic abstract implementation of a routine.
 * <p>
 * This class provides a default implementation of all the routine features, like invocation modes
 * and recycling of invocation objects.
 * <br>
 * The inheriting class just needs to create invocation objects when required.
 * <p>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class AbstractRoutine<IN, OUT> implements Routine<IN, OUT> {

  private static final int DEFAULT_CORE_INVOCATIONS = 10;

  private static final int DEFAULT_MAX_INVOCATIONS = Integer.MAX_VALUE;

  private final InvocationConfiguration mConfiguration;

  private final int mCoreInvocations;

  private final InvocationModeType mInvocationMode;

  private final SimpleQueue<Invocation<IN, OUT>> mInvocations =
      new SimpleQueue<Invocation<IN, OUT>>();

  private final Logger mLogger;

  private final int mMaxInvocations;

  private final Object mMutex = new Object();

  private final SimpleQueue<InvocationObserver<IN, OUT>> mObservers =
      new SimpleQueue<InvocationObserver<IN, OUT>>();

  private final Runner mRunner;

  private volatile AbstractRoutine<IN, OUT> mElementRoutine;

  private int mRunningCount;

  /**
   * Constructor.
   *
   * @param configuration the invocation configuration.
   */
  protected AbstractRoutine(@NotNull final InvocationConfiguration configuration) {
    mConfiguration = configuration;
    final int priority = configuration.getPriorityOrElse(InvocationConfiguration.DEFAULT);
    final Runner runner = configuration.getRunnerOrElse(Runners.sharedRunner());
    if (priority != InvocationConfiguration.DEFAULT) {
      mRunner = Runners.priorityRunner(runner).getRunner(priority);

    } else {
      mRunner = runner;
    }

    mInvocationMode = configuration.getModeOrElse(InvocationModeType.SIMPLE);
    mMaxInvocations = configuration.getMaxInvocationsOrElse(DEFAULT_MAX_INVOCATIONS);
    mCoreInvocations = configuration.getCoreInvocationsOrElse(DEFAULT_CORE_INVOCATIONS);
    mLogger = configuration.newLogger(this);
    mLogger.dbg("building routine with configuration: %s", configuration);
  }

  /**
   * Constructor.
   *
   * @param configuration the invocation configuration.
   * @param runner        the runner instance.
   * @param logger        the logger instance.
   */
  private AbstractRoutine(@NotNull final InvocationConfiguration configuration,
      @NotNull final Runner runner, @NotNull final Logger logger) {
    // parallel routine
    mConfiguration = configuration;
    mRunner = runner;
    mInvocationMode = InvocationModeType.SIMPLE;
    mMaxInvocations = configuration.getMaxInvocationsOrElse(DEFAULT_MAX_INVOCATIONS);
    mCoreInvocations = configuration.getCoreInvocationsOrElse(DEFAULT_CORE_INVOCATIONS);
    mLogger = logger.subContextLogger(this);
  }

  public void clear() {
    synchronized (mMutex) {
      final SimpleQueue<Invocation<IN, OUT>> asyncInvocations = mInvocations;
      for (final Invocation<IN, OUT> invocation : asyncInvocations) {
        discard(invocation);
      }

      asyncInvocations.clear();
    }
  }

  @NotNull
  public Channel<IN, OUT> invoke() {
    final InvocationModeType invocationMode = mInvocationMode;
    mLogger.dbg("invoking routine: %s", invocationMode);
    if (invocationMode == InvocationModeType.SIMPLE) {
      return invokeInternal();
    }

    return getElementRoutine().invokeInternal();
  }

  /**
   * Returns the routine invocation configuration.
   *
   * @return the invocation configuration.
   */
  @NotNull
  protected InvocationConfiguration getConfiguration() {
    return mConfiguration;
  }

  /**
   * Returns the routine logger.
   *
   * @return the logger instance.
   */
  @NotNull
  protected Logger getLogger() {
    return mLogger;
  }

  /**
   * Creates a new invocation instance.
   *
   * @return the invocation instance.
   * @throws java.lang.Exception if an unexpected error occurs.
   */
  @NotNull
  protected abstract Invocation<IN, OUT> newInvocation() throws Exception;

  private void discard(final @NotNull Invocation<IN, OUT> invocation) {
    try {
      invocation.onDestroy();

    } catch (final Throwable t) {
      InterruptedInvocationException.throwIfInterrupt(t);
      mLogger.wrn(t, "ignoring exception while discarding invocation instance");
    }
  }

  @NotNull
  private AbstractRoutine<IN, OUT> getElementRoutine() {
    if (mElementRoutine == null) {
      mElementRoutine =
          new AbstractRoutine<IN, OUT>(mConfiguration.builderFrom().withInputBackoff(null).apply(),
              mRunner, mLogger) {

            @NotNull
            @Override
            protected Invocation<IN, OUT> newInvocation() {
              return new ParallelInvocation<IN, OUT>(AbstractRoutine.this);
            }
          };
    }

    return mElementRoutine;
  }

  @NotNull
  private Channel<IN, OUT> invokeInternal() {
    final ConcurrentRunner runner = new ConcurrentRunner(mRunner);
    return new InvocationChannel<IN, OUT>(mConfiguration, new DefaultInvocationManager(runner),
        runner, mLogger);
  }

  /**
   * Invocation observer notifying events through a specific runner.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class DelayedObserver<IN, OUT> implements InvocationObserver<IN, OUT> {

    private final InvocationObserver<IN, OUT> mObserver;

    private final Runner mRunner;

    /**
     * Constructor.
     *
     * @param observer the wrapped observer.
     * @param runner   the runner instance.
     */
    private DelayedObserver(@NotNull final InvocationObserver<IN, OUT> observer,
        @NotNull final Runner runner) {
      mObserver = observer;
      mRunner = runner;
    }

    public void onCreate(@NotNull final Invocation<IN, OUT> invocation) {
      mRunner.run(new Execution() {

        public void run() {
          mObserver.onCreate(invocation);
        }
      }, 0, TimeUnit.MILLISECONDS);
    }

    public void onError(@NotNull final Throwable error) {
      mRunner.run(new Execution() {

        public void run() {
          mObserver.onError(error);
        }
      }, 0, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Implementation of an invocation handling parallel mode.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class ParallelInvocation<IN, OUT> extends TemplateInvocation<IN, OUT> {

    private final AbstractRoutine<IN, OUT> mRoutine;

    private boolean mForceInvocation;

    /**
     * Constructor.
     *
     * @param routine the routine to invoke in parallel mode.
     */
    private ParallelInvocation(@NotNull final AbstractRoutine<IN, OUT> routine) {
      mRoutine = routine;
    }

    @Override
    public void onComplete(@NotNull final Channel<OUT, ?> result) {
      if (mForceInvocation) {
        final Channel<IN, OUT> channel = mRoutine.invokeInternal();
        result.pass(channel);
        channel.close();
      }
    }

    @Override
    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) {
      mForceInvocation = false;
      final Channel<IN, OUT> channel = mRoutine.invokeInternal();
      result.pass(channel);
      channel.pass(input).close();
    }

    @Override
    public boolean onRecycle() {
      return true;
    }

    @Override
    public void onRestart() {
      mForceInvocation = true;
    }
  }

  /**
   * Default implementation of an invocation manager supporting recycling of invocation instances.
   */
  private class DefaultInvocationManager implements InvocationManager<IN, OUT> {

    private final ConcurrentRunner mManagerRunner;

    /**
     * Constructor.
     *
     * @param runner the runner used for asynchronous invocation.
     */
    private DefaultInvocationManager(@NotNull final ConcurrentRunner runner) {
      mManagerRunner = runner;
    }

    public boolean create(@NotNull final InvocationObserver<IN, OUT> observer) {
      return createInternal(ConstantConditions.notNull("invocation observer", observer));
    }

    public void discard(@NotNull final Invocation<IN, OUT> invocation) {
      mLogger.wrn("discarding invocation instance after error: %s", invocation);
      try {
        invocation.onRecycle();

      } catch (final Throwable t) {
        discardInternal(invocation);
        InterruptedInvocationException.throwIfInterrupt(t);
        return;
      }

      discardInternal(invocation);
    }

    public void recycle(@NotNull final Invocation<IN, OUT> invocation) {
      final Logger logger = mLogger;
      final boolean canRecycle;
      try {
        canRecycle = invocation.onRecycle();

      } catch (final Throwable t) {
        logger.wrn(t, "Discarding invocation since it failed to be recycled");
        discardInternal(invocation);
        InterruptedInvocationException.throwIfInterrupt(t);
        return;
      }

      if (!canRecycle) {
        logger.dbg("Discarding invocation since it cannot be recycled");
        discardInternal(invocation);
        return;
      }

      final boolean hasDelayed;
      synchronized (mMutex) {
        final int coreInvocations = mCoreInvocations;
        final SimpleQueue<Invocation<IN, OUT>> invocations = mInvocations;
        if (invocations.size() < coreInvocations) {
          logger.dbg("recycling invocation instance [%d/%d]: %s", invocations.size() + 1,
              coreInvocations, invocation);
          invocations.add(invocation);

        } else {
          logger.wrn("discarding invocation instance [%d/%d]: %s", coreInvocations, coreInvocations,
              invocation);
          AbstractRoutine.this.discard(invocation);
        }

        hasDelayed = !mObservers.isEmpty();
        --mRunningCount;
        mMutex.notifyAll();
      }

      if (hasDelayed) {
        createInternal(null);
      }
    }

    @SuppressWarnings("ConstantConditions")
    private boolean createInternal(@Nullable final InvocationObserver<IN, OUT> observer) {
      InvocationObserver<IN, OUT> invocationObserver = observer;
      try {
        Invocation<IN, OUT> invocation;
        synchronized (mMutex) {
          final SimpleQueue<InvocationObserver<IN, OUT>> observers = mObservers;
          if (observer == null) {
            if (observers.isEmpty()) {
              return false;
            }

            invocationObserver = observers.removeFirst();
          }

          if ((observer == null) || ((mRunningCount + observers.size()) < mMaxInvocations)) {
            final int coreInvocations = mCoreInvocations;
            final SimpleQueue<Invocation<IN, OUT>> invocations = mInvocations;
            if (!invocations.isEmpty()) {
              invocation = invocations.removeFirst();
              mLogger.dbg("reusing invocation instance [%d/%d]: %s", invocations.size() + 1,
                  coreInvocations, invocation);

            } else {
              mLogger.dbg("creating invocation instance [1/%d]", coreInvocations);
              invocation = newInvocation();
            }

            if (invocation != null) {
              ++mRunningCount;
            }

          } else {
            observers.add(new DelayedObserver<IN, OUT>(invocationObserver, mManagerRunner));
            return false;
          }
        }

        if (invocation != null) {
          invocationObserver.onCreate(invocation);

        } else {
          mLogger.err("null invocation instance returned");
          invocationObserver.onError(new NullPointerException("null invocation returned"));
          return false;
        }

      } catch (final InterruptedInvocationException e) {
        throw e;

      } catch (final Throwable t) {
        mLogger.err(t, "error while creating a new invocation instance [%d]", mMaxInvocations);
        invocationObserver.onError(t);
        return false;
      }

      return true;
    }

    private void discardInternal(@NotNull final Invocation<IN, OUT> invocation) {
      final boolean hasDelayed;
      synchronized (mMutex) {
        AbstractRoutine.this.discard(invocation);
        hasDelayed = !mObservers.isEmpty();
        --mRunningCount;
        mMutex.notifyAll();
      }

      if (hasDelayed) {
        createInternal(null);
      }
    }
  }
}
