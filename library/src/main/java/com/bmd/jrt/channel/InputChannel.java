/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.jrt.channel;

import com.bmd.jrt.time.TimeDuration;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining an input channel.
 * <p/>
 * Note that the delivery order of the input data might not be guaranteed.
 * <p/>
 * Created by davide on 9/4/14.
 *
 * @param <INPUT> the input data type.
 */
public interface InputChannel<INPUT> extends Channel {

    /**
     * Tells the channel to delay the transfer of data of the specified time duration.<br/>
     * Note that an abort execution will be delayed as well. Note, however, that a delayed abort
     * will not prevent the invocation to complete, as input data do.
     *
     * @param delay the delay.
     * @return this channel.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted.
     * @throws java.lang.IllegalStateException     if this channel is already closed.
     * @throws java.lang.NullPointerException      if the specified delay is null.
     */
    @Nonnull
    public InputChannel<INPUT> after(@Nonnull TimeDuration delay);

    /**
     * Tells the channel to delay the transfer of data of the specified time duration.<br/>
     * Note that an abort execution will be delayed as well. Note, however, that a delayed abort
     * will not prevent the invocation to complete, as input data do.
     *
     * @param delay    the delay value.
     * @param timeUnit the delay time unit.
     * @return this channel.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted.
     * @throws java.lang.IllegalArgumentException  if the specified delay is negative.
     * @throws java.lang.IllegalStateException     if this channel is already closed.
     */
    @Nonnull
    public InputChannel<INPUT> after(long delay, @Nonnull TimeUnit timeUnit);

    /**
     * Tells the channel to not delay the transfer of data.
     *
     * @return this channel.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted.
     * @throws java.lang.IllegalStateException     if this channel is already closed.
     */
    @Nonnull
    public InputChannel<INPUT> now();

    /**
     * Passes the specified channel to this one.
     * <p/>
     * Note that this channel will be bound as a result of the call, thus preventing any other
     * reader to get data from the output channel.
     *
     * @param channel the output channel.
     * @return this channel.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted.
     * @throws java.lang.IllegalStateException     if this channel is already closed.
     * @see com.bmd.jrt.channel.OutputChannel#bind(OutputConsumer)
     */
    @Nonnull
    public InputChannel<INPUT> pass(@Nullable OutputChannel<INPUT> channel);

    /**
     * Passes the data returned by the specified iterable to this channel.
     *
     * @param inputs the iterable returning the input data.
     * @return this channel.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted.
     * @throws java.lang.IllegalStateException     if this channel is already closed.
     */
    @Nonnull
    public InputChannel<INPUT> pass(@Nullable Iterable<? extends INPUT> inputs);

    /**
     * Passes the specified input to this channel.
     *
     * @param input the input.
     * @return this channel.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted.
     * @throws java.lang.IllegalStateException     if this channel is already closed.
     */
    @Nonnull
    public InputChannel<INPUT> pass(@Nullable INPUT input);

    /**
     * Passes the specified input data to this channel.
     *
     * @param inputs the input data.
     * @return this channel.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted.
     * @throws java.lang.IllegalStateException     if this channel is already closed.
     */
    @Nonnull
    public InputChannel<INPUT> pass(@Nullable INPUT... inputs);
}
