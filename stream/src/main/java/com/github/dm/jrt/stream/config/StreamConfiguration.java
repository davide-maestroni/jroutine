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

package com.github.dm.jrt.stream.config;

import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

/**
 * Class storing a stream configuration.
 * <p>
 * Created by davide-maestroni on 01/29/2017.
 */
public class StreamConfiguration {

  private final InvocationConfiguration mConfiguration;

  private final InvocationConfiguration mStreamConfiguration;

  private volatile ChannelConfiguration mChannelConfiguration;

  private volatile InvocationConfiguration mInvocationConfiguration;

  /**
   * Constructor.
   *
   * @param streamConfiguration the stream invocation configuration.
   * @param nextConfiguration   the next invocation configuration.
   */
  public StreamConfiguration(@NotNull final InvocationConfiguration streamConfiguration,
      @NotNull final InvocationConfiguration nextConfiguration) {
    mStreamConfiguration =
        ConstantConditions.notNull("stream invocation configuration", streamConfiguration);
    mConfiguration = ConstantConditions.notNull("next invocation configuration", nextConfiguration);
  }

  /**
   * Gets the configuration that will override the stream one only for the next concatenated
   * routine.
   *
   * @return the invocation configuration.
   */
  @NotNull
  public InvocationConfiguration getNextInvocationConfiguration() {
    return mConfiguration;
  }

  /**
   * Gets the configuration that will be applied to all the concatenated routines.
   *
   * @return the invocation configuration.
   */
  @NotNull
  public InvocationConfiguration getStreamInvocationConfiguration() {
    return mStreamConfiguration;
  }

  /**
   * Gets the combination of stream and current configurations as a channel one.
   *
   * @return the channel configuration.
   */
  @NotNull
  public ChannelConfiguration toChannelConfiguration() {
    if (mChannelConfiguration == null) {
      mChannelConfiguration = toInvocationConfiguration().outputConfigurationBuilder().configuration();
    }

    return mChannelConfiguration;
  }

  /**
   * Gets the combination of stream and current configurations as an invocation one.
   *
   * @return the invocation configuration.
   */
  @NotNull
  public InvocationConfiguration toInvocationConfiguration() {
    if (mInvocationConfiguration == null) {
      mInvocationConfiguration =
          mStreamConfiguration.builderFrom().withPatch(mConfiguration).configuration();
    }

    return mInvocationConfiguration;
  }
}
