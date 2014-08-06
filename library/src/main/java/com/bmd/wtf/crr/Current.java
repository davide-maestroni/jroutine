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
package com.bmd.wtf.crr;

import com.bmd.wtf.flw.Fall;
import com.bmd.wtf.flw.Stream;

import java.util.concurrent.TimeUnit;

/**
 * Basic component of a waterfall.
 * <p/>
 * A current is responsible for transporting a flow of data feeding a fall.
 * <p/>
 * The specific implementation may be synchronous or employ one or more separate threads. For this
 * reason its methods should always be thread safe.
 * <p/>
 * Created by davide on 6/7/14.
 */
public interface Current {

    /**
     * This method is called when data must be flushed through the current.
     *
     * @param fall   The fall instance to be used to push data into the waterfall.
     * @param origin The origin stream.
     * @param <DATA> The data type.
     */
    public <DATA> void flush(Fall<DATA> fall, Stream<DATA> origin);

    /**
     * This method is called when an unhandled exception must be forwarded through the current.
     *
     * @param fall      The fall instance to be used to push data into the waterfall.
     * @param throwable The unhandled exception.
     */
    public void forward(Fall<?> fall, Throwable throwable);

    /**
     * This method is called when a data drop must be pushed through the current.
     *
     * @param fall   The fall instance to be used to push data into the waterfall.
     * @param drop   The data drop.
     * @param <DATA> The data type.
     */
    public <DATA> void push(Fall<DATA> fall, DATA drop);

    /**
     * This method is called when a data drop must be pushed through the current, after the
     * specified time has elapsed.
     *
     * @param fall     The fall instance to be used to push data into the waterfall.
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drop     The data drop.
     * @param <DATA>   The data type.
     */
    public <DATA> void pushAfter(Fall<DATA> fall, long delay, TimeUnit timeUnit, DATA drop);

    /**
     * This method is called when several data drops must be pushed through the current, after the
     * specified time has elapsed.
     *
     * @param fall     The fall instance to be used to push data into the waterfall.
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drops    The data drop iterable.
     * @param <DATA>   The data type.
     */
    public <DATA> void pushAfter(Fall<DATA> fall, long delay, TimeUnit timeUnit,
            Iterable<? extends DATA> drops);
}