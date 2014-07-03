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
package com.bmd.wtf.flw;

import java.util.concurrent.TimeUnit;

/**
 * Basic component of the waterfall.
 * <p/>
 * A stream defines the way the data are fed into the waterfall, and it represents the link
 * between the falls.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <DATA> The data type.
 */
public interface Stream<DATA> {

    /**
     * Discharges the stream, that is, it informs the fed fall that no more data drops are likely
     * to come.
     * <p/>
     * Be aware that the call may be postponed until the fall discharges all the data drops,
     * including the delayed ones.
     *
     * @return This stream.
     */
    public Stream<DATA> discharge();

    /**
     * Forwards the specified unhandled exception into the waterfall flow.
     *
     * @param throwable The thrown exception.
     * @return This stream.
     */
    public Stream<DATA> forward(Throwable throwable);

    /**
     * Pushes the specified data into the waterfall flow.
     *
     * @param drops The data drops.
     * @return This stream.
     */
    public Stream<DATA> push(DATA... drops);

    /**
     * Pushes the data returned by the specified iterable into the waterfall flow.
     *
     * @param drops The data drops iterable.
     * @return This stream.
     */
    public Stream<DATA> push(Iterable<? extends DATA> drops);

    /**
     * Pushes the specified data into the waterfall flow.
     *
     * @param drop The data drop.
     * @return This stream.
     */
    public Stream<DATA> push(DATA drop);

    /**
     * Pushes the data returned by the specified iterable into the waterfall flow, after the
     * specified time has elapsed.
     *
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drops    The data drops iterable.
     * @return This stream.
     */
    public Stream<DATA> pushAfter(long delay, TimeUnit timeUnit, Iterable<? extends DATA> drops);

    /**
     * Pushes the specified data into the waterfall flow, after the specified time has elapsed.
     *
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drop     The data drop.
     * @return This stream.
     */
    public Stream<DATA> pushAfter(long delay, TimeUnit timeUnit, DATA drop);

    /**
     * Pushes the specified data into the waterfall flow, after the specified time has elapsed.
     *
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drops    The data drops.
     * @return This stream.
     */
    public Stream<DATA> pushAfter(long delay, TimeUnit timeUnit, DATA... drops);
}