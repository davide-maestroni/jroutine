/*
 * Copyright 2017 Davide Maestroni
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

package com.github.dm.jrt.rx.config;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.reactivex.BackpressureStrategy;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class storing the Flowable configuration.
 * <p>
 * Each instance is immutable, thus, in order to modify an existing configuration, a new builder
 * must be created from it.
 * <p>
 * The configuration allows to set:
 * <ul>
 * <li>The back-pressure strategy to be applied.</li>
 * <li>The inputs to be passed to the routine invocation emitting the output data.</li>
 * </ul>
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 */
@SuppressWarnings("WeakerAccess")
public class FlowableConfiguration extends DeepEqualObject {

  private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

  private final BackpressureStrategy mBackpressure;

  /**
   * Constructor.
   *
   * @param backpressureStrategy the back-pressure strategy.
   */
  private FlowableConfiguration(@Nullable final BackpressureStrategy backpressureStrategy) {
    super(asArgs(backpressureStrategy));
    mBackpressure = backpressureStrategy;
  }

  /**
   * Returns an Flowable configuration builder.
   *
   * @return the builder.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static Builder<FlowableConfiguration> builder() {
    return new Builder<FlowableConfiguration>(sDefaultConfigurable);
  }

  /**
   * Returns an Flowable configuration builder initialized with the specified configuration.
   *
   * @param initialConfiguration the initial Flowable configuration.
   * @return the builder.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static Builder<FlowableConfiguration> builderFrom(
      @Nullable final FlowableConfiguration initialConfiguration) {
    return (initialConfiguration == null) ? FlowableConfiguration.builder()
        : new Builder<FlowableConfiguration>(sDefaultConfigurable, initialConfiguration);
  }

  /**
   * Returns a configuration with all the options set to their default.
   *
   * @return the configuration instance.
   */
  @NotNull
  public static FlowableConfiguration defaultConfiguration() {
    return FlowableConfiguration.builder().buildConfiguration();
  }

  /**
   * Returns an Flowable configuration builder initialized with this configuration.
   *
   * @return the builder.
   */
  @NotNull
  public Builder<FlowableConfiguration> builderFrom() {
    return builderFrom(this);
  }

  /**
   * Returns the back-pressure strategy (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the back-pressure strategy.
   */
  public BackpressureStrategy getBackpressureOrElse(
      @Nullable final BackpressureStrategy valueIfNotSet) {
    final BackpressureStrategy backpressure = mBackpressure;
    return (backpressure != null) ? backpressure : valueIfNotSet;
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
    TYPE withConfiguration(@NotNull FlowableConfiguration configuration);
  }

  /**
   * Builder of Flowable configurations.
   *
   * @param <TYPE> the configurable object type.
   */
  public static final class Builder<TYPE> {

    private final Configurable<? extends TYPE> mConfigurable;

    private BackpressureStrategy mBackpressure;

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
     * @param initialConfiguration the initial Flowable configuration.
     */
    public Builder(@NotNull final Configurable<? extends TYPE> configurable,
        @NotNull final FlowableConfiguration initialConfiguration) {
      mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
      setConfiguration(initialConfiguration);
    }

    /**
     * Applies this configuration and returns the configured object.
     *
     * @return the configured object.
     */
    @NotNull
    public TYPE configuration() {
      return mConfigurable.withConfiguration(buildConfiguration());
    }

    /**
     * Sets the back-pressure strategy to be applied.
     *
     * @param backpressure the back-pressure strategy.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withBackpressure(@Nullable final BackpressureStrategy backpressure) {
      mBackpressure = backpressure;
      return this;
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
     * Applies the specified patch configuration to this builder. Only the non-default options will
     * be applied. A null value will have no effect.
     *
     * @param configuration the Flowable configuration.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withPatch(@Nullable final FlowableConfiguration configuration) {
      if (configuration == null) {
        return this;
      }

      final BackpressureStrategy backpressure = configuration.mBackpressure;
      if (backpressure != null) {
        withBackpressure(backpressure);
      }

      return this;
    }

    @NotNull
    private FlowableConfiguration buildConfiguration() {
      return new FlowableConfiguration(mBackpressure);
    }

    private void setConfiguration(@NotNull final FlowableConfiguration configuration) {
      mBackpressure = configuration.mBackpressure;
    }
  }

  /**
   * Default configurable implementation.
   */
  private static class DefaultConfigurable implements Configurable<FlowableConfiguration> {

    @NotNull
    public FlowableConfiguration withConfiguration(
        @NotNull final FlowableConfiguration configuration) {
      return configuration;
    }
  }
}
