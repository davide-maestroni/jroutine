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

import java.util.Arrays;
import java.util.Collections;

import rx.Emitter.BackpressureMode;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class storing the Observable configuration.
 * <p>
 * Each instance is immutable, thus, in order to modify an existing configuration, a new builder
 * must be created from it.
 * <p>
 * The configuration allows to set:
 * <ul>
 * <li>The back-pressure mode to be applied.</li>
 * <li>The inputs to be passed to the routine invocation emitting the output data.</li>
 * </ul>
 * <p>
 * Created by davide-maestroni on 02/08/2017.
 *
 * @param <IN> the input data type.
 */
@SuppressWarnings("WeakerAccess")
public class ObservableConfiguration<IN> extends DeepEqualObject {

  private static final DefaultConfigurable<?> sDefaultConfigurable =
      new DefaultConfigurable<Object>();

  private final BackpressureMode mBackpressure;

  private final Iterable<IN> mInputs;

  /**
   * Constructor.
   *
   * @param backpressureMode the back-pressure mode.
   * @param inputs           the input data.
   */
  private ObservableConfiguration(@Nullable final BackpressureMode backpressureMode,
      @Nullable final Iterable<IN> inputs) {
    super(asArgs(backpressureMode, inputs));
    mBackpressure = backpressureMode;
    mInputs = inputs;
  }

  /**
   * Returns an Observable configuration builder.
   *
   * @param <IN> the input data type.
   * @return the builder.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <IN> Builder<IN, ObservableConfiguration<IN>> builder() {
    return new Builder<IN, ObservableConfiguration<IN>>(
        (Configurable<IN, ? extends ObservableConfiguration<IN>>) sDefaultConfigurable);
  }

  /**
   * Returns an Observable configuration builder initialized with the specified configuration.
   *
   * @param initialConfiguration the initial Observable configuration.
   * @param <IN>                 the input data type.
   * @return the builder.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <IN> Builder<IN, ObservableConfiguration<IN>> builderFrom(
      @Nullable final ObservableConfiguration<IN> initialConfiguration) {
    return (initialConfiguration == null) ? ObservableConfiguration.<IN>builder()
        : new Builder<IN, ObservableConfiguration<IN>>(
            (Configurable<IN, ? extends ObservableConfiguration<IN>>) sDefaultConfigurable,
            initialConfiguration);
  }

  /**
   * Returns a configuration with all the options set to their default.
   *
   * @param <IN> the input data type.
   * @return the configuration instance.
   */
  @NotNull
  public static <IN> ObservableConfiguration<IN> defaultConfiguration() {
    return ObservableConfiguration.<IN>builder().buildConfiguration();
  }

  /**
   * Returns an Observable configuration builder initialized with this configuration.
   *
   * @return the builder.
   */
  @NotNull
  public Builder<IN, ObservableConfiguration<IN>> builderFrom() {
    return builderFrom(this);
  }

  /**
   * Returns the back-pressure mode (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the back-pressure mode.
   */
  public BackpressureMode getBackpressureOrElse(@Nullable final BackpressureMode valueIfNotSet) {
    final BackpressureMode backpressure = mBackpressure;
    return (backpressure != null) ? backpressure : valueIfNotSet;
  }

  /**
   * Returns the inputs (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the inputs.
   */
  public Iterable<IN> getInputsOrElse(@Nullable final Iterable<IN> valueIfNotSet) {
    final Iterable<IN> inputs = mInputs;
    return (inputs != null) ? inputs : valueIfNotSet;
  }

  /**
   * Interface defining a configurable object.
   *
   * @param <IN>   the input data type.
   * @param <TYPE> the configurable object type.
   */
  public interface Configurable<IN, TYPE> {

    /**
     * Sets the specified configuration and returns the configurable instance.
     *
     * @param configuration the configuration.
     * @return the configurable instance.
     */
    @NotNull
    TYPE apply(@NotNull ObservableConfiguration<IN> configuration);
  }

  /**
   * Builder of Observable configurations.
   *
   * @param <IN>   the input data type.
   * @param <TYPE> the configurable object type.
   */
  public static final class Builder<IN, TYPE> {

    private final Configurable<IN, ? extends TYPE> mConfigurable;

    private BackpressureMode mBackpressure;

    private Iterable<IN> mInputs;

    /**
     * Constructor.
     *
     * @param configurable the configurable instance.
     */
    public Builder(@NotNull final Configurable<IN, ? extends TYPE> configurable) {
      mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
    }

    /**
     * Constructor.
     *
     * @param configurable         the configurable instance.
     * @param initialConfiguration the initial Observable configuration.
     */
    public Builder(@NotNull final Configurable<IN, ? extends TYPE> configurable,
        @NotNull final ObservableConfiguration<IN> initialConfiguration) {
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
     * Applies the specified configuration to this builder. A null value means that all the
     * configuration options will be reset to their default, otherwise only the non-default
     * options will be applied.
     *
     * @param configuration the Observable configuration.
     * @return this builder.
     */
    @NotNull
    public Builder<IN, TYPE> with(@Nullable final ObservableConfiguration<IN> configuration) {
      if (configuration == null) {
        setConfiguration(ObservableConfiguration.<IN>defaultConfiguration());
        return this;
      }

      final BackpressureMode backpressure = configuration.mBackpressure;
      if (backpressure != null) {
        withBackpressure(backpressure);
      }

      final Iterable<IN> inputs = configuration.mInputs;
      if (inputs != null) {
        withInputs(inputs);
      }

      return this;
    }

    /**
     * Sets the back-pressure mode to be applied.
     *
     * @param backpressure the back-pressure mode.
     * @return this builder.
     */
    @NotNull
    public Builder<IN, TYPE> withBackpressure(@Nullable final BackpressureMode backpressure) {
      mBackpressure = backpressure;
      return this;
    }

    /**
     * Sets the input to be passed to the routine invocation.
     *
     * @param input the input.
     * @return this builder.
     */
    @NotNull
    public Builder<IN, TYPE> withInput(@Nullable final IN input) {
      mInputs = Collections.singleton(input);
      return this;
    }

    /**
     * Sets the inputs to be passed to the routine invocation.
     *
     * @param inputs the input data.
     * @return this builder.
     */
    @NotNull
    public Builder<IN, TYPE> withInputs(@Nullable final IN... inputs) {
      mInputs = (inputs != null) ? Arrays.asList(inputs) : null;
      return this;
    }

    /**
     * Sets the inputs to be passed to the routine invocation.
     *
     * @param inputs the iterable returning the input data.
     * @return this builder.
     */
    @NotNull
    public Builder<IN, TYPE> withInputs(@Nullable final Iterable<IN> inputs) {
      mInputs = inputs;
      return this;
    }

    @NotNull
    private ObservableConfiguration<IN> buildConfiguration() {
      return new ObservableConfiguration<IN>(mBackpressure, mInputs);
    }

    private void setConfiguration(@NotNull final ObservableConfiguration<IN> configuration) {
      mBackpressure = configuration.mBackpressure;
      mInputs = configuration.mInputs;
    }
  }

  /**
   * Default configurable implementation.
   *
   * @param <IN> the input data type.
   */
  private static class DefaultConfigurable<IN>
      implements Configurable<IN, ObservableConfiguration<IN>> {

    @NotNull
    public ObservableConfiguration<IN> apply(
        @NotNull final ObservableConfiguration<IN> configuration) {
      return configuration;
    }
  }
}
