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
package com.bmd.wtf.src;

import java.util.concurrent.TimeUnit;

/**
 * Basic component of a {@link com.bmd.wtf.Waterfall}.
 * <p/>
 * A spring defines the way the data are fed into the waterfall.
 * <p/>
 * Note that a waterfall may have several springs feeding its streams.
 * <p/>
 * Created by davide on 2/25/14.
 *
 * @param <DATA> The data type.
 */
public interface Spring<DATA> {

    /**
     * Discharges the specified data drop into the waterfall.
     *
     * @param drop The drop of data to discharge.
     * @return This spring.
     */
    public Spring<DATA> discharge(DATA drop);

    /**
     * Discharges the specified data drops into the waterfall.
     *
     * @param drops The drops of data to discharge.
     * @return This spring.
     */
    public Spring<DATA> discharge(DATA... drops);

    /**
     * Discharges the data drops returned by the specified iterable instance into the waterfall.
     *
     * @param drops The iterable returning the drops of data to discharge.
     * @return This spring.
     */
    public Spring<DATA> discharge(Iterable<? extends DATA> drops);

    /**
     * Discharges the specified data drop into the waterfall, after the specified time has elapsed.
     *
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drop     The drop of data to discharge.
     * @return This spring.
     */
    public Spring<DATA> dischargeAfter(long delay, TimeUnit timeUnit, DATA drop);

    /**
     * Discharges the specified data drops into the waterfall, after the specified time has
     * elapsed.
     *
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drops    The drops of data to discharge.
     * @return This spring.
     */
    public Spring<DATA> dischargeAfter(long delay, TimeUnit timeUnit, DATA... drops);

    /**
     * Discharges the data drops returned by the specified iterable instance into the waterfall,
     * after the specified time has elapsed.
     *
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drops    The iterable returning the drops of data to discharge.
     * @return This spring.
     */
    public Spring<DATA> dischargeAfter(long delay, TimeUnit timeUnit, Iterable<? extends DATA> drops);

    /**
     * Discharges the specified debris into the waterfall.
     *
     * @param debris The debris to drop downstream.
     * @return This spring.
     */
    public Spring<DATA> drop(Object debris);

    /**
     * Drops the specified debris into the waterfall, after the specified time has elapsed.
     *
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param debris   The debris to drop downstream.
     * @return This spring.
     */
    public Spring<DATA> dropAfter(long delay, TimeUnit timeUnit, Object debris);

    /**
     * Exhaust this spring, that is, all the streams fed only by it are detached from the waterfall
     * and no more data will flow through them and through this spring.
     */
    public void exhaust();

    /**
     * Flushes the spring, that is, it informs the fed streams that no more data drops are likely
     * to come.
     * <p/>
     * Be aware that the call may block until the pool fed by this spring will discharge all the
     * data drops, included the delayed ones.<br/>
     * Be also aware that, in case more than one flushes is expected, based on waterfall topology,
     * the total number should be checked downstream before further propagating the flush.
     */
    public void flush();
}