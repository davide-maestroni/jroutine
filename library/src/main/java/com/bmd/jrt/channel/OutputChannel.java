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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interface defining an output channel, that is the channel used to read the routine output data.
 * <p/>
 * Created by davide on 9/4/14.
 *
 * @param <OUTPUT> the output type.
 */
public interface OutputChannel<OUTPUT> extends Channel, Iterable<OUTPUT> {

    /**
     * Tells the channel to wait at max the specified time duration for the next result to be
     * available.
     *
     * @param timeout the maximum timeout.
     * @return this channel.
     */
    public OutputChannel<OUTPUT> afterMax(TimeDuration timeout);

    /**
     * Tells the channel to wait at max the specified time duration for the next result to be
     * available.
     *
     * @param timeout  the maximum timeout value.
     * @param timeUnit the timeout time unit.
     * @return this channel.
     */
    public OutputChannel<OUTPUT> afterMax(long timeout, TimeUnit timeUnit);

    /**
     * Binds the specified consumer to this channel. After the call all the output will be passed
     * only to the consumer and not returned to readers.
     *
     * @param consumer the consumer instance.
     * @return this channel.
     */
    public OutputChannel<OUTPUT> bind(OutputConsumer<OUTPUT> consumer);

    /**
     * Tells the channel to throw the specified exception in case no result is available before the
     * timeout has elapsed.
     *
     * @param exception the exception to throw.
     * @return this channel.
     */
    public OutputChannel<OUTPUT> eventuallyThrow(RuntimeException exception);

    /**
     * Tells the channel to not wait for results to be available.
     *
     * @return this channel.
     */
    public OutputChannel<OUTPUT> immediately();

    /**
     * Reads all the results waiting for the routine to complete at the maximum for the set
     * timeout.
     *
     * @return this channel.
     * @see #afterMax(com.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     */
    public List<OUTPUT> readAll();

    /**
     * Reads all the results waiting for the routine to complete at the maximum for the set
     * timeout, and put them into the specified list.
     *
     * @param results the list to fill.
     * @return this channel.
     * @see #afterMax(com.bmd.jrt.time.TimeDuration)
     * @see #afterMax(long, java.util.concurrent.TimeUnit)
     */
    public OutputChannel<OUTPUT> readAllInto(List<OUTPUT> results);

    /**
     * Waits for the routine to complete at the maximum for the set timeout.
     *
     * @return whether the routine execution has complete.
     */
    public boolean waitDone();
}