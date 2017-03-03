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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
 *
 * @param <IN> the input data type.
 */
@SuppressWarnings("WeakerAccess")
public class FlowableConfiguration<IN> extends DeepEqualObject {

  private static final DefaultConfigurable<?> sDefaultConfigurable =
      new DefaultConfigurable<Object>();

  private final BackpressureStrategy mBackpressure;

  private final List<IN> mInputs;

  /**
   * Constructor.
   *
   * @param backpressureStrategy the back-pressure strategy.
   * @param inputs               the input data.
   */
  private FlowableConfiguration(@Nullable final BackpressureStrategy backpressureStrategy,
      @Nullable final List<IN> inputs) {
    super(asArgs(backpressureStrategy, inputs));
    mBackpressure = backpressureStrategy;
    mInputs = inputs;
  }

  /**
   * Returns an Flowable configuration builder.
   *
   * @param <IN> the input data type.
   * @return the builder.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <IN> Builder<IN, FlowableConfiguration<IN>> builder() {
    return new Builder<IN, FlowableConfiguration<IN>>(
        (Configurable<IN, ? extends FlowableConfiguration<IN>>) sDefaultConfigurable);
  }

  /**
   * Returns an Flowable configuration builder initialized with the specified configuration.
   *
   * @param initialConfiguration the initial Flowable configuration.
   * @param <IN>                 the input data type.
   * @return the builder.
   */
  @NotNull
  @SuppressWarnings("unchecked")
  public static <IN> Builder<IN, FlowableConfiguration<IN>> builderFrom(
      @Nullable final FlowableConfiguration<IN> initialConfiguration) {
    return (initialConfiguration == null) ? FlowableConfiguration.<IN>builder()
        : new Builder<IN, FlowableConfiguration<IN>>(
            (Configurable<IN, ? extends FlowableConfiguration<IN>>) sDefaultConfigurable,
            initialConfiguration);
  }

  /**
   * Returns a configuration with all the options set to their default.
   *
   * @param <IN> the input data type.
   * @return the configuration instance.
   */
  @NotNull
  public static <IN> FlowableConfiguration<IN> defaultConfiguration() {
    return FlowableConfiguration.<IN>builder().buildConfiguration();
  }

  /**
   * Returns an Flowable configuration builder initialized with this configuration.
   *
   * @return the builder.
   */
  @NotNull
  public Builder<IN, FlowableConfiguration<IN>> builderFrom() {
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
   * Returns the inputs (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the inputs.
   */
  public Collection<? extends IN> getInputsOrElse(
      @Nullable final Collection<? extends IN> valueIfNotSet) {
    final List<IN> inputs = mInputs;
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
    TYPE apply(@NotNull FlowableConfiguration<IN> configuration);
  }

  /**
   * Builder of Flowable configurations.
   *
   * @param <IN>   the input data type.
   * @param <TYPE> the configurable object type.
   */
  public static final class Builder<IN, TYPE> {

    private final Configurable<IN, ? extends TYPE> mConfigurable;

    private BackpressureStrategy mBackpressure;

    private List<IN> mInputs;

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
     * @param initialConfiguration the initial Flowable configuration.
     */
    public Builder(@NotNull final Configurable<IN, ? extends TYPE> configurable,
        @NotNull final FlowableConfiguration<IN> initialConfiguration) {
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
     * Sets the back-pressure strategy to be applied.
     *
     * @param backpressure the back-pressure strategy.
     * @return this builder.
     */
    @NotNull
    public Builder<IN, TYPE> withBackpressure(@Nullable final BackpressureStrategy backpressure) {
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
      mInputs = Collections.singletonList(input);
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
      if (inputs != null) {
        final ArrayList<IN> inputList = new ArrayList<IN>();
        Collections.addAll(inputList, inputs);
        mInputs = Collections.unmodifiableList(inputList);

      } else {
        mInputs = null;
      }

      return this;
    }

    /**
     * Sets the inputs to be passed to the routine invocation.
     *
     * @param inputs the iterable returning the input data.
     * @return this builder.
     */
    @NotNull
    public Builder<IN, TYPE> withInputs(@Nullable final Iterable<? extends IN> inputs) {
      if (inputs != null) {
        final ArrayList<IN> inputList = new ArrayList<IN>();
        for (final IN input : inputs) {
          inputList.add(input);
        }

        mInputs = Collections.unmodifiableList(inputList);

      } else {
        mInputs = null;
      }

      return this;
    }

    /**
     * Applies the specified patch configuration to this builder. A null value means that all the
     * configuration options will be reset to their default, otherwise only the non-default
     * options will be applied.
     *
     * @param configuration the Flowable configuration.
     * @return this builder.
     */
    @NotNull
    public Builder<IN, TYPE> withPatch(@Nullable final FlowableConfiguration<IN> configuration) {
      if (configuration == null) {
        setConfiguration(FlowableConfiguration.<IN>defaultConfiguration());
        return this;
      }

      final BackpressureStrategy backpressure = configuration.mBackpressure;
      if (backpressure != null) {
        withBackpressure(backpressure);
      }

      final Iterable<? extends IN> inputs = configuration.mInputs;
      if (inputs != null) {
        withInputs(inputs);
      }

      return this;
    }

    @NotNull
    private FlowableConfiguration<IN> buildConfiguration() {
      return new FlowableConfiguration<IN>(mBackpressure, mInputs);
    }

    private void setConfiguration(@NotNull final FlowableConfiguration<IN> configuration) {
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
      implements Configurable<IN, FlowableConfiguration<IN>> {

    @NotNull
    public FlowableConfiguration<IN> apply(@NotNull final FlowableConfiguration<IN> configuration) {
      return configuration;
    }
  }
}
