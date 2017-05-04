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

package com.github.dm.jrt.retrofit;

import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.reflect.util.InvocationReflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;

import retrofit2.CallAdapter;
import retrofit2.CallAdapter.Factory;

/**
 * Implementation of a Call adapter factory supporting {@code Routine} and {@code StreamRoutine}
 * return types.
 * <br>
 * Note that the generated routines must be invoked and the returned channel closed before any
 * result is produced.
 * <p>
 * Created by davide-maestroni on 03/26/2016.
 */
@SuppressWarnings("WeakerAccess")
public class RoutineAdapterFactory extends AbstractAdapterFactory {

  private static final RoutineAdapterFactory sFactory =
      new RoutineAdapterFactory(ScheduledExecutors.defaultExecutor(),
          InvocationConfiguration.defaultConfiguration(), null);

  /**
   * Constructor.
   *
   * @param executor        the executor instance.
   * @param configuration   the invocation configuration.
   * @param delegateFactory the delegate factory.
   */
  private RoutineAdapterFactory(@NotNull final ScheduledExecutor executor,
      @NotNull final InvocationConfiguration configuration,
      @Nullable final Factory delegateFactory) {
    super(executor, configuration, delegateFactory);
  }

  /**
   * Returns the default factory instance.
   *
   * @return the factory instance.
   */
  @NotNull
  public static RoutineAdapterFactory defaultFactory() {
    return sFactory;
  }

  /**
   * Returns builder of adapter factories.
   *
   * @return the builder instance.
   */
  @NotNull
  public static Builder factory() {
    return factoryOn(ScheduledExecutors.defaultExecutor());
  }

  /**
   * Returns builder of adapter factories employing the specified executor.
   *
   * @param executor the executor instance.
   * @return the builder instance.
   */
  @NotNull
  public static Builder factoryOn(@NotNull final ScheduledExecutor executor) {
    return new Builder(executor);
  }

  /**
   * Builder of routine adapter factory instances.
   * <p>
   * The options set through the builder configuration will be applied to all the routine handling
   * the Retrofit calls, unless they are overwritten by specific annotations.
   *
   * @see InvocationReflection#withAnnotations(InvocationConfiguration, Annotation...)
   */
  public static class Builder implements InvocationConfigurable<Builder> {

    private final ScheduledExecutor mExecutor;

    private InvocationConfiguration mConfiguration = InvocationConfiguration.defaultConfiguration();

    private CallAdapter.Factory mDelegateFactory;

    /**
     * Constructor.
     *
     * @param executor the executor instance.
     */
    private Builder(@NotNull final ScheduledExecutor executor) {
      mExecutor = ConstantConditions.notNull("executor instance", executor);
    }

    /**
     * Builds and return a new factory instance.
     *
     * @return the factory instance.
     */
    @NotNull
    public RoutineAdapterFactory create() {
      return new RoutineAdapterFactory(mExecutor, mConfiguration, mDelegateFactory);
    }

    @NotNull
    public Builder withConfiguration(@NotNull final InvocationConfiguration configuration) {
      mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
      return this;
    }

    /**
     * Sets the delegate factory to be used to execute the calls.
     *
     * @param factory the factory instance.
     * @return this builder.
     */
    @NotNull
    public Builder withDelegate(@Nullable final CallAdapter.Factory factory) {
      mDelegateFactory = factory;
      return this;
    }

    @NotNull
    public InvocationConfiguration.Builder<? extends Builder> withInvocation() {
      return new InvocationConfiguration.Builder<Builder>(this, mConfiguration);
    }
  }
}
