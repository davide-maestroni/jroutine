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

import com.bmd.wtf.src.Pool;

import java.util.concurrent.TimeUnit;

/**
 * Basic component of a {@link com.bmd.wtf.Waterfall}.
 * <p/>
 * A current is responsible for transporting a flow of data feeding a {@link com.bmd.wtf.src.Pool}.
 * <p/>
 * Its implementation may be synchronous or employ one or more separate threads. For this reason
 * it should always be thread safe.
 * <p/>
 * Created by davide on 2/26/14.
 */
public interface Current {

    /**
     * This method is called when a data drop must be discharged through the current.
     *
     * @param pool   The pool instance to be used to discharge data into the waterfall.
     * @param drop   The drop of data to discharge.
     * @param <DATA> The data type.
     */
    public <DATA> void discharge(Pool<DATA> pool, DATA drop);

    /**
     * This method is called when a data drop must be discharged through the current, after the
     * specified time has elapsed.
     *
     * @param pool     The pool instance to be used to discharge data into the waterfall.
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drop     The drop of data to discharge.
     * @param <DATA>   The data type.
     */
    public <DATA> void dischargeAfter(Pool<DATA> pool, long delay, TimeUnit timeUnit, DATA drop);

    /**
     * This method is called when data drops must be discharged through the current, after the
     * specified time has elapsed.
     *
     * @param pool     The pool instance to be used to discharge data into the waterfall.
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drops    The iterable returning the drops of data to discharge.
     * @param <DATA>   The data type.
     */
    public <DATA> void dischargeAfter(Pool<DATA> pool, long delay, TimeUnit timeUnit,
            Iterable<? extends DATA> drops);

    /**
     * This method is called when data must be flushed through the current.
     *
     * @param pool The pool instance to be used to discharge data into the waterfall.
     */
    public void flush(Pool<?> pool);

    /**
     * This method is called when an object must be pulled upstream through the current.
     *
     * @param pool   The pool instance to be used to discharge data into the waterfall.
     * @param debris The debris to pull.
     */
    public void pull(Pool<?> pool, Object debris);

    /**
     * This method is called when an object must be pushed downstream through the current.
     *
     * @param pool   The pool instance to be used to discharge data into the waterfall.
     * @param debris The debris to push.
     */
    public void push(Pool<?> pool, Object debris);
}