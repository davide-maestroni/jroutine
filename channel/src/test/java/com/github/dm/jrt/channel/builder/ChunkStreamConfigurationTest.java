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

package com.github.dm.jrt.channel.builder;

import com.github.dm.jrt.channel.builder.ChunkStreamConfiguration.Builder;
import com.github.dm.jrt.channel.builder.ChunkStreamConfiguration.CloseActionType;

import org.junit.Test;

import static com.github.dm.jrt.channel.builder.ChunkStreamConfiguration.builder;
import static com.github.dm.jrt.channel.builder.ChunkStreamConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Output stream configuration unit tests.
 * <p>
 * Created by davide-maestroni on 01/02/2017.
 */
public class ChunkStreamConfigurationTest {

  @Test
  public void testBufferSizeEquals() {
    final ChunkStreamConfiguration configuration = builder().withChunkSize(11)
                                                            .withCorePoolSize(17)
                                                            .withOnClose(
                                                                 CloseActionType.CLOSE_CHANNEL)
                                                            .configured();
    assertThat(configuration).isNotEqualTo(builder().withChunkSize(3).configured());
    assertThat(configuration.builderFrom().withChunkSize(27).configured()).isNotEqualTo(
        builder().withChunkSize(27).configured());
  }

  @Test
  public void testBufferSizeError() {
    try {
      builder().withChunkSize(0);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }

    try {
      builder().withChunkSize(-1);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }

  @Test
  public void testBuildFrom() {
    final ChunkStreamConfiguration configuration = builder().withChunkSize(11)
                                                            .withCorePoolSize(17)
                                                            .withOnClose(
                                                                 CloseActionType.CLOSE_CHANNEL)
                                                            .configured();
    assertThat(builderFrom(configuration).configured().hashCode()).isEqualTo(
        configuration.hashCode());
    assertThat(builderFrom(configuration).configured()).isEqualTo(configuration);
    assertThat(builderFrom(null).configured().hashCode()).isEqualTo(
        ChunkStreamConfiguration.defaultConfiguration().hashCode());
    assertThat(builderFrom(null).configured()).isEqualTo(
        ChunkStreamConfiguration.defaultConfiguration());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testBuildNullPointerError() {
    try {
      new Builder<Object>(null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      new Builder<Object>(null, ChunkStreamConfiguration.defaultConfiguration());
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testBuilderFromEquals() {
    final ChunkStreamConfiguration configuration = builder().withChunkSize(11)
                                                            .withCorePoolSize(17)
                                                            .withOnClose(
                                                                 CloseActionType.CLOSE_CHANNEL)
                                                            .configured();
    assertThat(builder().with(configuration).configured()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().configured()).isEqualTo(configuration);
    assertThat(configuration.builderFrom().with(null).configured()).isEqualTo(
        ChunkStreamConfiguration.defaultConfiguration());
  }

  @Test
  public void testCloseActionEquals() {
    final ChunkStreamConfiguration configuration = builder().withChunkSize(11)
                                                            .withCorePoolSize(17)
                                                            .withOnClose(
                                                                 CloseActionType.CLOSE_CHANNEL)
                                                            .configured();
    assertThat(configuration).isNotEqualTo(
        builder().withOnClose(CloseActionType.FLUSH_STREAM).configured());
    assertThat(configuration.builderFrom()
                            .withOnClose(CloseActionType.FLUSH_STREAM)
                            .configured()).isNotEqualTo(
        builder().withOnClose(CloseActionType.FLUSH_STREAM).configured());
  }

  @Test
  public void testPoolSizeEquals() {
    final ChunkStreamConfiguration configuration = builder().withChunkSize(11)
                                                            .withCorePoolSize(17)
                                                            .withOnClose(
                                                                 CloseActionType.CLOSE_CHANNEL)
                                                            .configured();
    assertThat(configuration).isNotEqualTo(builder().withCorePoolSize(0).configured());
    assertThat(configuration.builderFrom().withCorePoolSize(0).configured()).isNotEqualTo(
        builder().withCorePoolSize(0).configured());
  }

  @Test
  public void testPoolSizeError() {
    try {
      builder().withCorePoolSize(-1);
      fail();

    } catch (final IllegalArgumentException ignored) {
    }
  }
}
