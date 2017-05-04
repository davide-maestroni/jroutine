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

package com.github.dm.jrt.rx;

import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.rx.builder.FlowableBuilder;
import com.github.dm.jrt.rx.builder.RxChannelBuilder;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.executor.ScheduledExecutors.defaultExecutor;

/**
 * Utility class integrating the JRoutine classes with RxJava ones.
 * <p>
 * The example below shows how it's possible to create an Flowable instance from a channel:
 * <pre><code>
 * JRoutineFlowable.flowable().of(myChannel).subscribe(getAction());
 * </code></pre>
 * <p>
 * In a dual way, a channel can be created from an Flowable:
 * <pre><code>
 * JRoutineFlowable.channel().of(myFlowable).consume(getConsumer());
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 02/09/2017.
 */
@SuppressWarnings("WeakerAccess")
public class JRoutineFlowable {

  /**
   * Avoid explicit instantiation.
   */
  protected JRoutineFlowable() {
    ConstantConditions.avoid();
  }

  /**
   * Returns a builder of channels fed by an RxJava object.
   *
   * @return the channel builder.
   */
  @NotNull
  public static RxChannelBuilder channel() {
    return channelOn(defaultExecutor());
  }

  /**
   * Returns a builder of channels fed by an RxJava object, running on the specified executor.
   *
   * @param executor the executor instance.
   * @return the channel builder.
   */
  @NotNull
  public static RxChannelBuilder channelOn(@NotNull final ScheduledExecutor executor) {
    return new DefaultRxChannelBuilder(executor);
  }

  /**
   * Returns a builder of Flowables fed by the specified channel.
   *
   * @return the Flowable builder.
   */
  @NotNull
  public static FlowableBuilder flowable() {
    return new DefaultFlowableBuilder();
  }
}
