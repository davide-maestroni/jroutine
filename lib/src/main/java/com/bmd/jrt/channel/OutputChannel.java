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
     * By default the timeout is set to a few seconds to avoid unexpected deadlocks.
     *
     * @param timeout the maximum timeout.
     * @return this channel.
     * @throws NullPointerException                if the specified timeout is null.
     * @throws IllegalStateException               if this channel is already bound to a consumer.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted with an
     *                                             exception.
     */
    @Nonnull
    public OutputChannel<OUTPUT> afterMax(@Nonnull TimeDuration timeout);

    /**
     * Tells the channel to wait at max the specified time duration for the next result to be
     * available.
     * <p/>
     * By default the timeout is set to a few seconds to avoid unexpected deadlocks.
     *
     * @param timeout  the maximum timeout value.
     * @param timeUnit the timeout time unit.
     * @return this channel.
     * @throws IllegalArgumentException            if the specified timeout is negative.
     * @throws IllegalStateException               if this channel is already bound to a consumer.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted with an
     *                                             exception.
     */
    @Nonnull
    public OutputChannel<OUTPUT> afterMax(long timeout, @Nonnull TimeUnit timeUnit);

    /**
     * Binds the specified consumer to this channel. After the call all the output will be passed
     * only to the consumer and not returned to readers.
     *
     * @param consumer the consumer instance.
     * @return this channel.
     * @throws NullPointerException                if the specified consumer is null.
     * @throws IllegalStateException               if this channel is already bound to a consumer.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted with an
     *                                             exception.
     */
    @Nonnull
    public OutputChannel<OUTPUT> bind(@Nonnull OutputConsumer<OUTPUT> consumer);

    /**
     * Tells the channel to throw a {@link ReadDeadLockException} in case no result is available
     * before the timeout has elapsed.
     *
     * @return this channel.
     * @throws IllegalStateException               if this channel is already bound to a consumer.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted with an
     *                                             exception.
     * @see #afterMax(com.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     */
    @Nonnull
    public OutputChannel<OUTPUT> eventuallyDeadLock();

    /**
     * Tells the channel to not wait for results to be available.
     * <p/>
     * By default the timeout is set to a few seconds to avoid unexpected deadlocks.
     *
     * @return this channel.
     * @throws IllegalStateException               if this channel is already bound to a consumer.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted with an
     *                                             exception.
     */
    @Nonnull
    public OutputChannel<OUTPUT> immediately();

    /**
     * @return
     */
    public boolean isBound();

    /**
     * Checks if the routine is complete waiting at the maximum for the set timeout.
     *
     * @return whether the routine execution has complete.
     * @see #afterMax(com.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     */
    public boolean isComplete();

    /**
     * Tells the channel to not throw any exception in case no result is available before the
     * timeout has elapsed.
     *
     * @return this channel.
     * @see #afterMax(com.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     */
    @Nonnull
    public OutputChannel<OUTPUT> neverDeadLock();

    /**
     * Reads all the results by waiting for the routine to complete at the maximum for the set
     * timeout.
     *
     * @return this channel.
     * @throws IllegalStateException               if this channel is already bound to a consumer.
     * @throws ReadDeadLockException               if the channel is set to throw an exception when
     *                                             the timeout elapses.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted with an
     *                                             exception.
     * @see #afterMax(com.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyDeadLock()
     * @see #neverDeadLock()
     */
    @Nonnull
    public List<OUTPUT> readAll();

    /**
     * Reads all the results by waiting for the routine to complete at the maximum for the set
     * timeout, and put them into the specified collection.
     *
     * @param results the collection to fill.
     * @return this channel.
     * @throws NullPointerException                if the specified collection is null.
     * @throws IllegalStateException               if this channel is already bound to a consumer.
     * @throws ReadDeadLockException               if the channel is set to throw an exception when
     *                                             the timeout elapses.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted with an
     *                                             exception.
     * @see #afterMax(com.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyDeadLock()
     * @see #neverDeadLock()
     */
    @Nonnull
    public OutputChannel<OUTPUT> readAllInto(@Nonnull Collection<? super OUTPUT> results);

    /**
     * Reads the first available result by waiting at the maximum for the set timeout.
     *
     * @return the first available result.
     * @throws IllegalStateException               if this channel is already bound to a consumer.
     * @throws ReadDeadLockException               if the channel is set to throw an exception when
     *                                             the timeout elapses.
     * @throws com.bmd.jrt.common.RoutineException if the execution has been aborted with an
     *                                             exception.
     * @see #afterMax(com.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     * @see #immediately()
     * @see #eventuallyDeadLock()
     * @see #neverDeadLock()
     */
    public OUTPUT readFirst();

    /**
     * @param consumer
     * @return
     */
    @Nonnull
    public OutputChannel<OUTPUT> unbind(@Nullable OutputConsumer<OUTPUT> consumer);
}
