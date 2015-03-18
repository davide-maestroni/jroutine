/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.channel;

import com.gh.bmd.jrt.time.TimeDuration;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface defining an output channel, that is the channel used to read the routine invocation
 * output data.
 * <p/>
 * Note that the delivery order of the output data might not be guaranteed.
 * <p/>
 * Created by davide on 9/4/14.
 *
 * @param <OUTPUT> the output data type.
 */
public interface OutputChannel<OUTPUT> extends Channel, Iterable<OUTPUT> {

    /**
     * Tells the channel to wait at max the specified time duration for the next result to be
     * available.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout the maximum timeout.
     * @return this channel.
     * @throws com.gh.bmd.jrt.common.RoutineException if the execution has been aborted.
     * @throws java.lang.IllegalStateException        if this channel is already bound to a
     *                                                consumer.
     * @throws java.lang.NullPointerException         if the specified timeout is null.
     */
    @Nonnull
    OutputChannel<OUTPUT> afterMax(@Nonnull TimeDuration timeout);

    /**
     * Tells the channel to wait at max the specified time duration for the next result to be
     * available.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @param timeout  the maximum timeout value.
     * @param timeUnit the timeout time unit.
     * @return this channel.
     * @throws com.gh.bmd.jrt.common.RoutineException if the execution has been aborted.
     * @throws java.lang.IllegalArgumentException     if the specified timeout is negative.
     * @throws java.lang.IllegalStateException        if this channel is already bound to a
     *                                                consumer.
     */
    @Nonnull
    OutputChannel<OUTPUT> afterMax(long timeout, @Nonnull TimeUnit timeUnit);

    /**
     * Binds the specified consumer to this channel. After the call all the output will be passed
     * only to the consumer and not returned to readers.
     *
     * @param consumer the consumer instance.
     * @return this channel.
     * @throws java.lang.IllegalStateException if this channel is already bound to a consumer.
     * @throws java.lang.NullPointerException  if the specified consumer is null.
     */
    @Nonnull
    OutputChannel<OUTPUT> bind(@Nonnull OutputConsumer<? super OUTPUT> consumer);

    /**
     * Checks if the routine is complete waiting at the maximum for the set timeout.
     *
     * @return whether the routine execution has complete.
     * @see #afterMax(com.gh.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     */
    boolean checkComplete();

    /**
     * Tells the channel to wait indefinitely for results to be available.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @return this channel.
     * @throws com.gh.bmd.jrt.common.RoutineException if the execution has been aborted.
     * @throws java.lang.IllegalStateException        if this channel is already bound to a
     *                                                consumer.
     */
    @Nonnull
    OutputChannel<OUTPUT> eventually();

    /**
     * Tells the channel to abort the invocation execution in case no result is available before
     * the timeout has elapsed.
     *
     * @return this channel.
     * @see #afterMax(com.gh.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     */
    @Nonnull
    OutputChannel<OUTPUT> eventuallyAbort();

    /**
     * Tells the channel to throw a {@link ReadDeadlockException} in case no result is available
     * before the timeout has elapsed.
     * <p/>
     * This is the default behavior.
     *
     * @return this channel.
     * @see #afterMax(com.gh.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     */
    @Nonnull
    OutputChannel<OUTPUT> eventuallyDeadlock();

    /**
     * Tells the channel to break execution in case no result is available before the timeout has
     * elapsed.
     * <p/>
     * By default an exception will be thrown.
     *
     * @return this channel.
     * @see #afterMax(com.gh.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     */
    @Nonnull
    OutputChannel<OUTPUT> eventuallyExit();

    /**
     * Tells the channel to not wait for results to be available.
     * <p/>
     * By default the timeout is set to 0 to avoid unexpected deadlocks.
     *
     * @return this channel.
     * @throws com.gh.bmd.jrt.common.RoutineException if the execution has been aborted.
     * @throws java.lang.IllegalStateException        if this channel is already bound to a
     *                                                consumer.
     */
    @Nonnull
    OutputChannel<OUTPUT> immediately();

    /**
     * Checks if this channel is bound to a consumer.
     *
     * @return whether the channel is bound.
     */
    boolean isBound();

    /**
     * Reads all the results by waiting for the routine to complete at the maximum for the set
     * timeout.
     *
     * @return this channel.
     * @throws com.gh.bmd.jrt.channel.ReadDeadlockException if the channel is set to throw an
     *                                                      exception when the timeout elapses.
     * @throws com.gh.bmd.jrt.common.RoutineException       if the execution has been aborted.
     * @throws java.lang.IllegalStateException              if this channel is already bound to a
     *                                                      consumer.
     * @see #afterMax(com.gh.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyDeadlock()
     * @see #eventuallyExit()
     */
    @Nonnull
    List<OUTPUT> readAll();

    /**
     * Reads all the results by waiting for the routine to complete at the maximum for the set
     * timeout, and put them into the specified collection.
     *
     * @param results the collection to fill.
     * @return this channel.
     * @throws com.gh.bmd.jrt.channel.ReadDeadlockException if the channel is set to throw an
     *                                                      exception when the timeout elapses.
     * @throws com.gh.bmd.jrt.common.RoutineException       if the execution has been aborted.
     * @throws java.lang.IllegalStateException              if this channel is already bound to a
     *                                                      consumer.
     * @throws java.lang.NullPointerException               if the specified collection is null.
     * @see #afterMax(com.gh.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyDeadlock()
     * @see #eventuallyExit()
     */
    @Nonnull
    OutputChannel<OUTPUT> readAllInto(@Nonnull Collection<? super OUTPUT> results);

    /**
     * Reads the first available result by waiting at the maximum for the set timeout.
     *
     * @return the first available result.
     * @throws com.gh.bmd.jrt.channel.ReadDeadlockException if the channel is set to throw an
     *                                                      exception when the timeout elapses.
     * @throws com.gh.bmd.jrt.common.RoutineException       if the execution has been aborted.
     * @throws java.lang.IllegalStateException              if this channel is already bound to a
     *                                                      consumer.
     * @throws java.util.NoSuchElementException             if no output is available (it might be
     *                                                      thrown also in the case the read timeout
     *                                                      elapses and no deadlock exception is set
     *                                                      to be thrown).
     * @see #afterMax(com.gh.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyDeadlock()
     * @see #eventuallyExit()
     */
    OUTPUT readNext();

    /**
     * Unbinds the specified consumer from this channel. After the call the output will returned to
     * readers.
     *
     * @param consumer the consumer instance.
     * @return this channel.
     */
    @Nonnull
    OutputChannel<OUTPUT> unbind(@Nullable OutputConsumer<? super OUTPUT> consumer);
}
