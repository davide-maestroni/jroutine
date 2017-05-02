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

package com.github.dm.jrt.reflect.config;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class storing the method wrapper configuration.
 * <p>
 * Each instance is immutable, thus, in order to modify an existing configuration, a new builder
 * must be created from it.
 * <p>
 * The configuration allows to set:
 * <ul>
 * <li>The set of fields which are shared by the target methods and need to be synchronized. By
 * default the access to all the fields is protected. Note, however, that methods sharing the same
 * fields will never be executed in parallel.</li>
 * </ul>
 * <p>
 * Created by davide-maestroni on 04/20/2015.
 */
public final class WrapperConfiguration extends DeepEqualObject {

  private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

  private static final WrapperConfiguration sDefaultConfiguration = builder().buildConfiguration();

  private final Set<String> mFieldNames;

  /**
   * Constructor.
   *
   * @param fieldNames the shared field names.
   */
  private WrapperConfiguration(@Nullable final Set<String> fieldNames) {
    super(asArgs(fieldNames));
    mFieldNames = (fieldNames != null) ? Collections.unmodifiableSet(fieldNames) : null;
  }

  /**
   * Returns a wrapper configuration builder.
   *
   * @return the builder.
   */
  @NotNull
  public static Builder<WrapperConfiguration> builder() {
    return new Builder<WrapperConfiguration>(sDefaultConfigurable);
  }

  /**
   * Returns a wrapper configuration builder initialized with the specified configuration.
   *
   * @param initialConfiguration the initial wrapper configuration.
   * @return the builder.
   */
  @NotNull
  public static Builder<WrapperConfiguration> builderFrom(
      @Nullable final WrapperConfiguration initialConfiguration) {
    return (initialConfiguration == null) ? builder()
        : new Builder<WrapperConfiguration>(sDefaultConfigurable, initialConfiguration);
  }

  /**
   * Returns a configuration with all the options set to their default.
   *
   * @return the configuration instance.
   */
  @NotNull
  public static WrapperConfiguration defaultConfiguration() {
    return sDefaultConfiguration;
  }

  /**
   * Returns a wrapper configuration builder initialized with this configuration.
   *
   * @return the builder.
   */
  @NotNull
  public Builder<WrapperConfiguration> builderFrom() {
    return builderFrom(this);
  }

  /**
   * Returns the shared field names (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the field names.
   */
  public Set<String> getSharedFieldsOrElse(@Nullable final Set<String> valueIfNotSet) {
    final Set<String> fieldNames = mFieldNames;
    return (fieldNames != null) ? fieldNames : valueIfNotSet;
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
    TYPE withConfiguration(@NotNull WrapperConfiguration configuration);
  }

  /**
   * Builder of wrapper configurations.
   *
   * @param <TYPE> the configurable object type.
   */
  public static final class Builder<TYPE> {

    private final Configurable<? extends TYPE> mConfigurable;

    private Set<String> mFieldNames;

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
     * @param initialConfiguration the initial wrapper configuration.
     */
    public Builder(@NotNull final Configurable<? extends TYPE> configurable,
        @NotNull final WrapperConfiguration initialConfiguration) {
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
     * @param configuration the wrapper configuration.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withPatch(@Nullable final WrapperConfiguration configuration) {
      if (configuration == null) {
        return this;
      }

      final Set<String> fieldNames = configuration.mFieldNames;
      if (fieldNames != null) {
        withSharedFields(fieldNames);
      }

      return this;
    }

    /**
     * Sets the shared field names to empty, that is, no field is shared.
     *
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withSharedFields() {
      mFieldNames = Collections.emptySet();
      return this;
    }

    /**
     * Sets the shared field names. A null value means that all fields are shared.
     *
     * @param fieldNames the field names.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withSharedFields(@Nullable final String... fieldNames) {
      if (fieldNames != null) {
        final HashSet<String> set = new HashSet<String>();
        Collections.addAll(set, fieldNames);
        mFieldNames = Collections.unmodifiableSet(set);

      } else {
        mFieldNames = null;
      }

      return this;
    }

    /**
     * Sets the shared field names. A null value means that all fields are shared.
     *
     * @param fieldNames the field names.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withSharedFields(@Nullable final Iterable<? extends String> fieldNames) {
      if (fieldNames != null) {
        final HashSet<String> set = new HashSet<String>();
        for (final String fieldName : fieldNames) {
          set.add(fieldName);
        }

        mFieldNames = Collections.unmodifiableSet(set);

      } else {
        mFieldNames = null;
      }

      return this;
    }

    @NotNull
    private WrapperConfiguration buildConfiguration() {
      return new WrapperConfiguration(mFieldNames);
    }

    private void setConfiguration(@NotNull final WrapperConfiguration configuration) {
      mFieldNames = configuration.mFieldNames;
    }
  }

  /**
   * Default configurable implementation.
   */
  private static class DefaultConfigurable implements Configurable<WrapperConfiguration> {

    @NotNull
    public WrapperConfiguration withConfiguration(
        @NotNull final WrapperConfiguration configuration) {
      return configuration;
    }
  }
}
