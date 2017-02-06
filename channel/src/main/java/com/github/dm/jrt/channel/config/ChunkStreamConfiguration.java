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

package com.github.dm.jrt.channel.config;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class storing the output stream configuration.
 * <p>
 * Each instance is immutable, thus, in order to modify an existing configuration, a new builder
 * must be created from it.
 * <p>
 * The configuration allows to set:
 * <ul>
 * <li>The size of each byte chunk used to transfer data.</li>
 * <li>The maximum number of recycled chunks.</li>
 * <li>The type of action to be taken when the output stream is closed.</li>
 * </ul>
 * <p>
 * Created by davide-maestroni on 01/01/2017.
 */
@SuppressWarnings("WeakerAccess")
public class ChunkStreamConfiguration extends DeepEqualObject {

  /**
   * Constant indicating the default value of an integer attribute.
   */
  public static final int DEFAULT = Integer.MIN_VALUE;

  private static final DefaultConfigurable sDefaultConfigurable = new DefaultConfigurable();

  private static final ChunkStreamConfiguration sDefaultConfiguration =
      builder().buildConfiguration();

  private final int mChunkSize;

  private final CloseActionType mCloseAction;

  private final int mCorePoolSize;

  /**
   * Constructor.
   *
   * @param chunkSize    the chunk size in bytes.
   * @param corePoolSize the core pool size.
   * @param closeAction  the close action.
   */
  private ChunkStreamConfiguration(final int chunkSize, final int corePoolSize,
      @Nullable final CloseActionType closeAction) {
    super(asArgs(chunkSize, corePoolSize, closeAction));
    mChunkSize = chunkSize;
    mCorePoolSize = corePoolSize;
    mCloseAction = closeAction;
  }

  /**
   * Returns an output stream configuration builder.
   *
   * @return the builder.
   */
  @NotNull
  public static Builder<ChunkStreamConfiguration> builder() {
    return new Builder<ChunkStreamConfiguration>(sDefaultConfigurable);
  }

  /**
   * Returns an output stream configuration builder initialized with the specified configuration.
   *
   * @param initialConfiguration the initial output stream configuration.
   * @return the builder.
   */
  @NotNull
  public static Builder<ChunkStreamConfiguration> builderFrom(
      @Nullable final ChunkStreamConfiguration initialConfiguration) {
    return (initialConfiguration == null) ? builder()
        : new Builder<ChunkStreamConfiguration>(sDefaultConfigurable, initialConfiguration);
  }

  /**
   * Returns a configuration with all the options set to their default.
   *
   * @return the configuration instance.
   */
  @NotNull
  public static ChunkStreamConfiguration defaultConfiguration() {
    return sDefaultConfiguration;
  }

  /**
   * Returns an output stream configuration builder initialized with this configuration.
   *
   * @return the builder.
   */
  @NotNull
  public Builder<ChunkStreamConfiguration> builderFrom() {
    return builderFrom(this);
  }

  /**
   * Returns the size of the data chunks (DEFAULT by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the chunk size.
   */
  public int getChunkSizeOrElse(final int valueIfNotSet) {
    final int chunkSize = mChunkSize;
    return (chunkSize != DEFAULT) ? chunkSize : valueIfNotSet;
  }

  /**
   * Returns the type of action to be taken when the output stream is closed (null by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the close action.
   */
  public CloseActionType getCloseActionTypeOrElse(@Nullable final CloseActionType valueIfNotSet) {
    final CloseActionType closeAction = mCloseAction;
    return (closeAction != null) ? closeAction : valueIfNotSet;
  }

  /**
   * Returns the maximum number of chunks retained in the pool (DEFAULT by default).
   *
   * @param valueIfNotSet the default value if none was set.
   * @return the core size.
   */
  public int getCorePoolSizeOrElse(final int valueIfNotSet) {
    final int corePoolSize = mCorePoolSize;
    return (corePoolSize != DEFAULT) ? corePoolSize : valueIfNotSet;
  }

  /**
   * Enumeration indicating the type of action to be taken when the output stream is closed.
   */
  public enum CloseActionType {

    /**
     * Close channel.
     * <br>
     * As soon as the output stream is closed, the fed channel is closed as well.
     */
    CLOSE_CHANNEL,
    /**
     * Close output stream.
     * <br>
     * When the output stream is closed, the fed channel is left open.
     */
    CLOSE_STREAM,
    /**
     * Flush output stream.
     * <br>
     * The output stream is just flushed but not closed, so that it can be used to transfer
     * additional data.
     */
    FLUSH_STREAM,
    /**
     * Ignore the command.
     * <br>
     * The close method just do nothing.
     */
    IGNORE
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
    TYPE apply(@NotNull ChunkStreamConfiguration configuration);
  }

  /**
   * Builder of object configurations.
   *
   * @param <TYPE> the configurable object type.
   */
  public static final class Builder<TYPE> {

    private final Configurable<? extends TYPE> mConfigurable;

    private int mChunkSize;

    private CloseActionType mCloseAction;

    private int mCorePoolSize;

    /**
     * Constructor.
     *
     * @param configurable the configurable instance.
     */
    public Builder(@NotNull final Configurable<? extends TYPE> configurable) {
      mConfigurable = ConstantConditions.notNull("configurable instance", configurable);
      mChunkSize = DEFAULT;
      mCorePoolSize = DEFAULT;
    }

    /**
     * Constructor.
     *
     * @param configurable         the configurable instance.
     * @param initialConfiguration the initial output stream configuration.
     */
    public Builder(@NotNull final Configurable<? extends TYPE> configurable,
        @NotNull final ChunkStreamConfiguration initialConfiguration) {
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
     * @param configuration the output stream configuration.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> with(@Nullable final ChunkStreamConfiguration configuration) {
      if (configuration == null) {
        setConfiguration(defaultConfiguration());
        return this;
      }

      final int chunkSize = configuration.mChunkSize;
      if (chunkSize != DEFAULT) {
        withChunkSize(chunkSize);
      }

      final int poolSize = configuration.mCorePoolSize;
      if (poolSize != DEFAULT) {
        withCorePoolSize(poolSize);
      }

      final CloseActionType closeAction = configuration.mCloseAction;
      if (closeAction != null) {
        withOnClose(closeAction);
      }

      return this;
    }

    /**
     * Sets the size of the data chunks used to transfer bytes through the routine channels.
     *
     * @param chunkSize the chunk size.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the number is less than 1.
     */
    @NotNull
    public Builder<TYPE> withChunkSize(final int chunkSize) {
      if (chunkSize != DEFAULT) {
        ConstantConditions.positive("chunk size", chunkSize);
      }

      mChunkSize = chunkSize;
      return this;
    }

    /**
     * Sets the maximum number of chunks retained in the pool. Additional chunks created to fulfill
     * the bytes requirement will be discarded.
     *
     * @param poolSize the pool size.
     * @return this builder.
     * @throws java.lang.IllegalArgumentException if the number is less than 0.
     */
    @NotNull
    public Builder<TYPE> withCorePoolSize(final int poolSize) {
      if (poolSize != DEFAULT) {
        ConstantConditions.notNegative("pool size", poolSize);
      }

      mCorePoolSize = poolSize;
      return this;
    }

    /**
     * Sets the action to be taken when the output stream is closed. A null value means that it is
     * up to the specific implementation to choose a default one.
     *
     * @param closeAction the close action.
     * @return this builder.
     */
    @NotNull
    public Builder<TYPE> withOnClose(@Nullable final CloseActionType closeAction) {
      mCloseAction = closeAction;
      return this;
    }

    @NotNull
    private ChunkStreamConfiguration buildConfiguration() {
      return new ChunkStreamConfiguration(mChunkSize, mCorePoolSize, mCloseAction);
    }

    private void setConfiguration(@NotNull final ChunkStreamConfiguration configuration) {
      mChunkSize = configuration.mChunkSize;
      mCorePoolSize = configuration.mCorePoolSize;
      mCloseAction = configuration.mCloseAction;
    }
  }

  /**
   * Default configurable implementation.
   */
  private static class DefaultConfigurable implements Configurable<ChunkStreamConfiguration> {

    @NotNull
    public ChunkStreamConfiguration apply(@NotNull final ChunkStreamConfiguration configuration) {
      return configuration;
    }
  }
}
