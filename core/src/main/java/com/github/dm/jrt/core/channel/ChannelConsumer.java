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

package com.github.dm.jrt.core.channel;

import com.github.dm.jrt.core.error.RoutineException;

import org.jetbrains.annotations.NotNull;

/**
 * Interface defining a consumer that can be bound to a channel.
 * <br>
 * The same instance can be safely bound to different channels.
 * <p>
 * The typical lifecycle of a consumer object is the following:
 * <pre>
 *     <code>
 *
 *                     |     ---------
 *                     |    |         |
 *                     V    V         |
 *                ----------------    |
 *                |  onOutput()  |----
 *                ----------------
 *                     |    |
 *                     |    |
 *       |       ------      ------       |
 *       |      |                  |      |
 *       V      V                  V      V
 *   ----------------          ----------------
 *   | onComplete() |          |  onError()   |
 *   ----------------          ----------------
 *     </code>
 * </pre>
 * <p>
 * Created by davide-maestroni on 09/07/2014.
 *
 * @param <OUT> the output data type.
 */
public interface ChannelConsumer<OUT> {

  /**
   * Called when the channel closes after the invocation completes its execution.
   *
   * @throws java.lang.Exception if an unexpected error occurs.
   */
  void onComplete() throws Exception;

  /**
   * Called when the bounded channel transfer is aborted.
   *
   * @param error the reason of the abortion.
   * @throws java.lang.Exception if an unexpected error occurs.
   */
  void onError(@NotNull RoutineException error) throws Exception;

  /**
   * Called when an output is produced by the channel.
   *
   * @param output the output.
   * @throws java.lang.Exception if an unexpected error occurs.
   */
  void onOutput(OUT output) throws Exception;
}
