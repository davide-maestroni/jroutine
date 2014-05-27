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
 * A gate represents {@link Spring} with the ability to recharge the same kind of data after a
 * given delay.<br/>
 * Its methods can be called only inside a {@link com.bmd.wtf.dam.Dam} and only from the same
 * thread where the dam methods are invoked. Any different usage will cause an exception to be
 * thrown.
 * <p/>
 * Created by davide on 2/25/14.
 *
 * @param <IN>  The input data type.
 * @param <OUT> The output data type.
 */
public interface Floodgate<IN, OUT> extends Spring<OUT> {

    @Override
    public Floodgate<IN, OUT> discharge(OUT drop);

    @Override
    public Floodgate<IN, OUT> discharge(OUT... drops);

    @Override
    public Floodgate<IN, OUT> discharge(Iterable<? extends OUT> drops);

    @Override
    public Floodgate<IN, OUT> dischargeAfter(long delay, TimeUnit timeUnit, OUT drop);

    @Override
    public Floodgate<IN, OUT> dischargeAfter(long delay, TimeUnit timeUnit, OUT... drops);

    @Override
    public Floodgate<IN, OUT> dischargeAfter(long delay, TimeUnit timeUnit,
            Iterable<? extends OUT> drops);

    @Override
    public Floodgate<IN, OUT> drop(Object debris);

    @Override
    public Floodgate<IN, OUT> dropAfter(long delay, TimeUnit timeUnit, Object debris);

    /**
     * Drains the gate, that is, the associated {@link Pool} is detached
     * from the waterfall and no more data will flow through it and through this gate.
     */
    public void drain();

    /**
     * Discharges again the specified data drop into the same {@link Pool}
     * associated with this gate, after the specified time has elapsed.
     *
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drop     The drop of data to discharge.
     * @return This gate.
     */
    public Floodgate<IN, OUT> rechargeAfter(long delay, TimeUnit timeUnit, IN drop);

    /**
     * Discharges again the specified data drops into the same {@link Pool} associated with this
     * gate, after the specified time has elapsed.
     *
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drops    The drops of data to discharge.
     * @return This gate.
     */
    public Floodgate<IN, OUT> rechargeAfter(long delay, TimeUnit timeUnit, IN... drops);

    /**
     * Discharges again the data drops returned by the specified iterable object into the same
     * {@link Pool} associated with this gate, after the specified time
     * has elapsed.
     *
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param drops    The iterable returning the drops of data to discharge.
     * @return This gate.
     */
    public Floodgate<IN, OUT> rechargeAfter(long delay, TimeUnit timeUnit,
            Iterable<? extends IN> drops);

    /**
     * Drops again the specified debris into the same {@link Pool} associated with this gate,
     * after the specified time has elapsed.
     *
     * @param delay    The delay in <code>timeUnit</code> time units.
     * @param timeUnit The delay time unit.
     * @param debris   The debris to drop downstream.
     * @return This gate.
     */
    public Floodgate<IN, OUT> redropAfter(long delay, TimeUnit timeUnit, Object debris);
}