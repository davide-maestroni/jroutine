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

package com.github.dm.jrt.android.core.config;

import android.os.Looper;

import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.log.Log;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class storing the Service configuration.
 * <p>
 * Each instance is immutable, thus, in order to modify an existing configuration, a new builder
 * must be created from it.
 * <p>
 * The configuration allows to set:
 * <ul>
 * <li>The class of the executor to be employed to execute the invocation in the configured Service.
 * It must declare a default constructor to be correctly instantiated.</li>
 * <li>The class of the logger to be employed by the invocations executed in the configured Service.
 * It must declare a default constructor to be correctly instantiated.</li>
 * <li>The Looper to employ to deliver the Service messages (by default the main thread one). Note
 * that, in any case, the outputs will be collected through the configured executor.</li>
 * </ul>
 * <p>
 * Created by davide-maestroni on 04/20/2015.
 */
public final class ServiceConfiguration extends DeepEqualObject {

  private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

  private static final ServiceConfiguration sDefaultConfiguration = builder().buildConfiguration();

  private final Object[] mExecutorArgs;

  private final Class<? extends ScheduledExecutor> mExecutorClass;

  private final Object[] mLogArgs;

  private final Class<? extends Log> mLogClass;

  private final Looper mLooper;

  /**
   * Constructor.
   *
   * @param looper        the Looper instance.
   * @param executorClass the executor class.
   * @param executorArgs  the executor constructor args.
   * @param logClass      the log class.
   * @param logArgs       the log constructor args.
   */
  private ServiceConfiguration(@Nullable final Looper looper,
      @Nullable final Class<? extends ScheduledExecutor> executorClass,
      @Nullable final Object[] executorArgs, @Nullable final Class<? extends Log> logClass,
      @Nullable final Object[] logArgs) {
    super(asArgs(looper, executorClass, executorArgs, logClass, logArgs));
    mLooper = looper;
    mExecutorClass = executorClass;
    mExecutorArgs = executorArgs;
    mLogClass = logClass;
    mLogArgs = logArgs;
  }

  /**
   * Returns a Service configuration builder.
   *
   * @return the builder.
   */
  @NotNull
  public static Builder<ServiceConfiguration> builder() {
    return new Builder<ServiceConfiguration>(sDefaultConfigurable);
  }

  /**
   * Returns a Service configuration builder initialized with the specified configuration.
   *
   * @param initialConfiguration the initial configuration.
   * @return the builder.
   */
  @NotNull
  public static Builder<ServiceConfiguration> builderFrom(
      @Nullable final ServiceConfiguration initialConfiguration) {
    return (initialConfiguration == null) ? builder()
        : new Builder<ServiceConfiguration>(sDefaultConfigurable, initialConfiguration);
  }

  /**
   * Returns a configuration with all the options set to their default.
   *
   * @return the configuration instance.
   */
  @NotNull
  public static ServiceConfiguration defaultConfiguration() {
    return sDefaultConfiguration;
  }

  private static Object[] cloneOrNull(@Nullable final Object[] args) {
    return (args != null) ? args.clone() : null;
  }

  /**
   * Returns a Service configuration builder initialized with this configuration.
   *
   * @return the builder.
   */
  @NotNull
  public Builder<ServiceConfiguration> builderFrom() {
    return builderFrom(this);
  }

  /**
   * Returns the arguments to be passed to the executor constructor.
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the constructor arguments.
   */
  public Object[] getExecutorArgsOrElse(@Nullable final Object... valueIfNotSet) {
    final Object[] executorArgs = mExecutorArgs;
    return (executorArgs != null) ? cloneOrNull(executorArgs) : valueIfNotSet;
  }

  /**
   * Returns the executor class (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the executor class instance.
   */
  public Class<? extends ScheduledExecutor> getExecutorClassOrElse(
      @Nullable final Class<? extends ScheduledExecutor> valueIfNotSet) {
    final Class<? extends ScheduledExecutor> executorClass = mExecutorClass;
    return (executorClass != null) ? executorClass : valueIfNotSet;
  }

  /**
   * Returns the arguments to be passed to the log constructor.
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the constructor arguments.
   */
  public Object[] getLogArgsOrElse(@Nullable final Object... valueIfNotSet) {
    final Object[] logArgs = mLogArgs;
    return (logArgs != null) ? cloneOrNull(logArgs) : valueIfNotSet;
  }

  /**
   * Returns the log class (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the log class instance.
   */
  public Class<? extends Log> getLogClassOrElse(
      @Nullable final Class<? extends Log> valueIfNotSet) {
    final Class<? extends Log> logClass = mLogClass;
    return (logClass != null) ? logClass : valueIfNotSet;
  }

  /**
   * Returns the Looper used for dispatching the messages from the Service (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the Looper instance.
   */
  public Looper getMessageLooperOrElse(@Nullable final Looper valueIfNotSet) {
    final Looper looper = mLooper;
    return (looper != null) ? looper : valueIfNotSet;
  }

  /**
   * Interface defining a configurable object.
   *
   * @param <TYPE> the configurable object type.
   */
  public interface Configurable<TYPE> {

    /**
     * Sets the specified configuration and returns the configurable instance.
     *
     * @param configuration the configuration.
     * @return the configurable instance.
     */
    @NotNull
    TYPE apply(@NotNull ServiceConfiguration configuration);
  }

  /**
   * Builder of Service configurations.
   *
   * @param <TYPE> the configurable object type.
   */
  public static final class Builder<TYPE> {

    private final Configurable<? extends TYPE> mConfigurable;

    private Object[] mExecutorArgs;

    private Class<? extends ScheduledExecutor> mExecutorClass;

    private Object[] mLogArgs;

    private Class<? extends Log> mLogClass;

    private Looper mLooper;

    /**
     * Constructor.
     *
     * @param configurable the configurable instance.
     */
    public Builder(@NotNull final Configurable<? extends TYPE> configurable) {
      mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
    }

    /**
     * Constructor.
     *
     * @param configurable         the configurable instance.
     * @param initialConfiguration the initial configuration.
     */
    public Builder(@NotNull final Configurable<? extends TYPE> configurable,
        @NotNull final ServiceConfiguration initialConfiguration) {
      mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
      setConfiguration(initialConfiguration);
    }

    /**
     * Applies this configuration and returns the configured object.
     *
     * @return the configured object.
     */
    @NotNull
    public TYPE apply() {
      return mConfigurable.apply(buildConfiguration());
    }

    /**
     * Resets all the options to their default values.
     *
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withDefaults() {
      setConfiguration(defaultConfiguration());
      return this;
    }

    /**
     * Sets the arguments to be passed to the executor constructor.
     *
     * @param args the argument objects.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withExecutorArgs(@Nullable final Object... args) {
      mExecutorArgs = cloneOrNull(args);
      return this;
    }

    /**
     * Sets the executor class. A null value means that it is up to the specific implementation to
     * choose a default executor.
     *
     * @param executorClass the executor class.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withExecutorClass(
        @Nullable final Class<? extends ScheduledExecutor> executorClass) {
      mExecutorClass = executorClass;
      return this;
    }

    /**
     * Sets the arguments to be passed to the log constructor.
     *
     * @param args the argument objects.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withLogArgs(@Nullable final Object... args) {
      mLogArgs = cloneOrNull(args);
      return this;
    }

    /**
     * Sets the log class. A null value means that it is up to the specific implementation to
     * choose a default class.
     *
     * @param logClass the log class.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withLogClass(@Nullable final Class<? extends Log> logClass) {
      mLogClass = logClass;
      return this;
    }

    /**
     * Sets the Looper on which the messages from the Service are dispatched. A null value means
     * that messages will be dispatched on the main thread (as by default).
     *
     * @param looper the Looper instance.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withMessageLooper(@Nullable final Looper looper) {
      mLooper = looper;
      return this;
    }

    /**
     * Applies the specified patch configuration to this builder. Only the non-default options will
     * be applied. A null value will have no effect.
     *
     * @param configuration the Service configuration.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withPatch(@Nullable final ServiceConfiguration configuration) {
      if (configuration == null) {
        return this;
      }

      final Looper looper = configuration.mLooper;
      if (looper != null) {
        withMessageLooper(looper);
      }

      final Class<? extends ScheduledExecutor> executorClass = configuration.mExecutorClass;
      if (executorClass != null) {
        withExecutorClass(executorClass);
      }

      final Object[] executorArgs = configuration.mExecutorArgs;
      if (executorArgs != null) {
        withExecutorArgs(executorArgs);
      }

      final Class<? extends Log> logClass = configuration.mLogClass;
      if (logClass != null) {
        withLogClass(logClass);
      }

      final Object[] logArgs = configuration.mLogArgs;
      if (logArgs != null) {
        withLogArgs(logArgs);
      }

      return this;
    }

    @NotNull
    private ServiceConfiguration buildConfiguration() {
      return new ServiceConfiguration(mLooper, mExecutorClass, mExecutorArgs, mLogClass, mLogArgs);
    }

    private void setConfiguration(@NotNull final ServiceConfiguration configuration) {
      mLooper = configuration.mLooper;
      mExecutorClass = configuration.mExecutorClass;
      mExecutorArgs = configuration.mExecutorArgs;
      mLogClass = configuration.mLogClass;
      mLogArgs = configuration.mLogArgs;
    }
  }

  /**
   * Default configurable implementation.
   */
  private static class DefaultConfigurable implements Configurable<ServiceConfiguration> {

    @NotNull
    public ServiceConfiguration apply(@NotNull final ServiceConfiguration configuration) {
      return configuration;
    }
  }
}
