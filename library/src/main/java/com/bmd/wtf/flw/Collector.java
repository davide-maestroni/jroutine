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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A collector instance is used each time data are pulled from the waterfall mouth.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <DATA> The data type.
 */
public interface Collector<DATA> extends Iterator<DATA> {

    /**
     * Tells the collector to fail if no data is available before the specified time has elapsed.
     *
     * @param maxDelay The maximum delay in the specified time unit.
     * @param timeUnit The delay time unit.
     * @return This collector.
     */
    public Collector<DATA> afterMax(long maxDelay, TimeUnit timeUnit);

    /**
     * Collects all the available data and return them into an arrival ordered list.
     * <p/>
     * Note that the function will block until all the expected data are available or the maximum
     * delay has elapsed.
     *
     * @return The list of collected data.
     */
    public List<DATA> all();

    /**
     * Collects all the available data by filling the specified list in arrival order.
     * <p/>
     * Note that the function will block until all the expected data are available or the maximum
     * delay has elapsed.
     *
     * @param data The list to fill.
     * @return This collector.
     */
    public Collector<DATA> allInto(List<DATA> data);

    /**
     * Tells the collector to wait indefinitely for data to be available.
     *
     * @return This collector.
     */
    public Collector<DATA> eventually();

    /**
     * Tells the collector to throw the specified exception if the maximum delay elapses before any
     * data is available.
     *
     * @param exception The exception to be thrown.
     * @return This collector.
     */
    public Collector<DATA> eventuallyThrow(RuntimeException exception);

    /**
     * Collects the first available data drop by adding it to the specified list.
     *
     * @param data The list to fill.
     * @return This collector.
     */
    public Collector<DATA> nextInto(List<DATA> data);

    /**
     * Tells the collector to fail if no data is immediately available.
     *
     * @return This collector.
     */
    public Collector<DATA> now();
}