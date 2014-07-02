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

import com.bmd.wtf.fll.Classification;

import java.util.concurrent.TimeUnit;

/**
 * A river object represents a collection of streams merging the same waterfall into or originating
 * from the same fall.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <SOURCE> The source data type.
 * @param <DATA>   The data type.
 */
public interface River<SOURCE, DATA> extends Stream<DATA> {

    /**
     * Deviates the flow of the river by effectively preventing any coming data to be pushed
     * further downstream.
     *
     * @see #deviateStream(int)
     */
    public void deviate();

    /**
     * Deviates the flow of the specified river stream by effectively preventing any coming data
     * to be pushed further downstream.
     *
     * @param streamNumber The number identifying the target stream.
     * @see #deviate()
     */
    public void deviateStream(int streamNumber);

    @Override
    public River<SOURCE, DATA> discharge();

    @Override
    public River<SOURCE, DATA> forward(Throwable throwable);

    @Override
    public River<SOURCE, DATA> push(DATA... drops);

    @Override
    public River<SOURCE, DATA> push(Iterable<? extends DATA> drops);

    @Override
    public River<SOURCE, DATA> push(DATA drop);

    @Override
    public River<SOURCE, DATA> pushAfter(long delay, TimeUnit timeUnit,
            Iterable<? extends DATA> drops);

    @Override
    public River<SOURCE, DATA> pushAfter(long delay, TimeUnit timeUnit, DATA drop);

    @Override
    public River<SOURCE, DATA> pushAfter(long delay, TimeUnit timeUnit, DATA... drops);

    /**
     * Discharges the specific river stream, that is, it informs the fed fall that no more data
     * drops are likely to come.
     * <p/>
     * Be aware that the call may be postponed until the fall discharges all the data drops,
     * including the delayed ones.
     *
     * @param streamNumber The number identifying the target stream.
     * @return This river.
     */
    public River<SOURCE, DATA> dischargeStream(int streamNumber);

    /**
     * Drains the river by removing from the waterfall all the falls and rivers fed only by this
     * one.
     */
    public void drain();

    /**
     * Drains the specified river stream by removing from the waterfall all the falls and rivers
     * fed only by the specific stream.
     *
     * @param streamNumber The number identifying the target stream.
     */
    public void drainStream(int streamNumber);

    /**
     * Forwards the specified unhandled exception into the specific river stream flow.
     *
     * @param streamNumber The number identifying the target stream.
     * @param throwable    The thrown exception.
     * @return This river.
     */
    public River<SOURCE, DATA> forwardStream(int streamNumber, Throwable throwable);

    /**
     * Returns a gate handling a specific leap.
     * <p/>
     * If no gate can be created an exception will be thrown.
     *
     * @param gateType The gate type.
     * @param <TYPE>   The leap type.
     * @return The gate.
     */
    public <TYPE> Gate<TYPE> on(Class<TYPE> gateType);

    /**
     * Returns a gate handling a specific leap.
     * <p/>
     * If no gate can be created an exception will be thrown.
     *
     * @param gateClassification The gate classification.
     * @param <TYPE>             The leap type.
     * @return The gate.
     */
    public <TYPE> Gate<TYPE> on(Classification<TYPE> gateClassification);

    /**
     * Pushes the specified data into the specific river stream flow.
     *
     * @param streamNumber The number identifying the target stream.
     * @param drops        The data drops.
     * @return This stream.
     */
    public River<SOURCE, DATA> pushStream(int streamNumber, DATA... drops);

    /**
     * Pushes the data returned by the specified iterable into the specific river stream flow.
     *
     * @param streamNumber The number identifying the target stream.
     * @param drops        The data drops iterable.
     * @return This stream.
     */
    public River<SOURCE, DATA> pushStream(int streamNumber, Iterable<? extends DATA> drops);

    /**
     * Pushes the specified data into the specific river stream flow.
     *
     * @param streamNumber The number identifying the target stream.
     * @param drop         The data drop.
     * @return This stream.
     */
    public River<SOURCE, DATA> pushStream(int streamNumber, DATA drop);

    /**
     * Pushes the data returned by the specified iterable into the specific river stream flow,
     * after the specified time has elapsed.
     *
     * @param streamNumber The number identifying the target stream.
     * @param delay        The delay in <code>timeUnit</code> time units.
     * @param timeUnit     The delay time unit.
     * @param drops        The data drops iterable.
     * @return This stream.
     */
    public River<SOURCE, DATA> pushStreamAfter(int streamNumber, long delay, TimeUnit timeUnit,
            Iterable<? extends DATA> drops);

    /**
     * Pushes the specified data into the specific river stream flow, after the specified time has
     * elapsed.
     *
     * @param streamNumber The number identifying the target stream.
     * @param delay        The delay in <code>timeUnit</code> time units.
     * @param timeUnit     The delay time unit.
     * @param drop         The data drop.
     * @return This stream.
     */
    public River<SOURCE, DATA> pushStreamAfter(int streamNumber, long delay, TimeUnit timeUnit,
            DATA drop);

    /**
     * Pushes the specified data into the specific river stream flow, after the specified time has
     * elapsed.
     *
     * @param streamNumber The number identifying the target stream.
     * @param delay        The delay in <code>timeUnit</code> time units.
     * @param timeUnit     The delay time unit.
     * @param drops        The data drops.
     * @return This stream.
     */
    public River<SOURCE, DATA> pushStreamAfter(int streamNumber, long delay, TimeUnit timeUnit,
            DATA... drops);

    public int size();

    /**
     * Returns the river source.
     *
     * @return The river source.
     */
    public River<SOURCE, SOURCE> source();
}