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

package com.github.dm.jrt.android.v11.core;

import com.github.dm.jrt.android.core.builder.LoaderChannelBuilder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.MissingLoaderException;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.dm.jrt.android.v11.core.LoaderInvocation.clearLoader;
import static com.github.dm.jrt.core.config.InvocationConfiguration.builderFromOutput;

/**
 * Default implementation of a Loader channel builder.
 * <p>
 * Created by davide-maestroni on 01/14/2015.
 */
class DefaultLoaderChannelBuilder implements LoaderChannelBuilder {

  private final LoaderSource mLoaderSource;

  private ChannelConfiguration mChannelConfiguration = ChannelConfiguration.defaultConfiguration();

  private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param loaderSource the Loader source.
   */
  DefaultLoaderChannelBuilder(@NotNull final LoaderSource loaderSource) {
    mLoaderSource = ConstantConditions.notNull("Loader source", loaderSource);
  }

  @Override
  public void clear(@Nullable final Object... inputs) {
    final LoaderSource loaderSource = mLoaderSource;
    if (loaderSource.getComponent() != null) {
      final List<Object> inputList;
      if (inputs == null) {
        inputList = Collections.emptyList();

      } else {
        inputList = Arrays.asList(inputs);
      }

      clearLoader(loaderSource, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO),
          inputList);
    }
  }

  @Override
  public void clear(@Nullable final Iterable<?> inputs) {
    final LoaderSource loaderSource = mLoaderSource;
    if (loaderSource.getComponent() != null) {
      final List<Object> inputList;
      if (inputs == null) {
        inputList = Collections.emptyList();

      } else {
        inputList = new ArrayList<Object>();
        for (final Object input : inputs) {
          inputList.add(input);
        }
      }

      clearLoader(loaderSource, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO),
          inputList);
    }
  }

  @Override
  public void clear() {
    final LoaderSource loaderSource = mLoaderSource;
    if (loaderSource.getComponent() != null) {
      clearLoader(loaderSource, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO));
    }
  }

  @Override
  public void clear(@Nullable final Object input) {
    final LoaderSource loaderSource = mLoaderSource;
    if (loaderSource.getComponent() != null) {
      clearLoader(loaderSource, mLoaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO),
          Collections.singletonList(input));
    }
  }

  @NotNull
  @Override
  public <OUT> Channel<?, OUT> ofType() {
    final LoaderConfiguration loaderConfiguration = mLoaderConfiguration;
    final int loaderId = loaderConfiguration.getLoaderIdOrElse(LoaderConfiguration.AUTO);
    if (loaderId == LoaderConfiguration.AUTO) {
      throw new IllegalArgumentException("the Loader ID must not be generated");
    }

    final LoaderSource loaderSource = mLoaderSource;
    final Object component = loaderSource.getComponent();
    if (component == null) {
      final Channel<OUT, OUT> channel = JRoutineCore.channel().ofType();
      channel.abort(new MissingLoaderException(loaderId));
      return channel.close();
    }

    final MissingLoaderInvocationFactory<OUT> factory =
        new MissingLoaderInvocationFactory<OUT>(loaderId);
    final DefaultLoaderRoutineBuilder builder = new DefaultLoaderRoutineBuilder(loaderSource);
    return builder.withConfiguration(builderFromOutput(mChannelConfiguration).configuration())
                  .withConfiguration(loaderConfiguration)
                  .of(factory)
                  .invoke()
                  .close();
  }

  @NotNull
  @Override
  public ChannelConfiguration.Builder<? extends LoaderChannelBuilder> withChannel() {
    final ChannelConfiguration config = mChannelConfiguration;
    return new ChannelConfiguration.Builder<LoaderChannelBuilder>(this, config);
  }

  @NotNull
  @Override
  public LoaderChannelBuilder withConfiguration(@NotNull final ChannelConfiguration configuration) {
    mChannelConfiguration = ConstantConditions.notNull("channel configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderChannelBuilder withConfiguration(@NotNull final LoaderConfiguration configuration) {
    mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends LoaderChannelBuilder> withLoader() {
    final LoaderConfiguration config = mLoaderConfiguration;
    return new LoaderConfiguration.Builder<LoaderChannelBuilder>(this, config);
  }
}
