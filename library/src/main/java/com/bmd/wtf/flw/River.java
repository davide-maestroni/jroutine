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
 * A river object represents a collection of streams merging into the same waterfall or originating
 * from the same fall.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <DATA> the data type.
 */
public interface River<DATA> extends Stream<DATA> {

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
     * @param streamNumber the number identifying the target stream.
     * @see #deviate()
     */
    public void deviateStream(int streamNumber);

    /**
     * Drains the river by removing from the waterfall all the falls and rivers fed only by this
     * one.
     */
    public void drain();

    /**
     * Drains the specified river stream by removing from the waterfall all the falls and rivers
     * fed only by the specific stream.
     *
     * @param streamNumber the number identifying the target stream.
     */
    public void drainStream(int streamNumber);

    @Override
    public River<DATA> exception(Throwable throwable);

    @Override
    public River<DATA> flush();

    @Override
    public River<DATA> flush(DATA... drops);

    @Override
    public River<DATA> flush(Iterable<? extends DATA> drops);

    @Override
    public River<DATA> flush(DATA drop);

    @Override
    public River<DATA> flushAfter(long delay, TimeUnit timeUnit, Iterable<? extends DATA> drops);

    @Override
    public River<DATA> flushAfter(long delay, TimeUnit timeUnit, DATA drop);

    @Override
    public River<DATA> flushAfter(long delay, TimeUnit timeUnit, DATA... drops);

    @Override
    public River<DATA> push(DATA... drops);

    @Override
    public River<DATA> push(Iterable<? extends DATA> drops);

    @Override
    public River<DATA> push(DATA drop);

    @Override
    public River<DATA> pushAfter(long delay, TimeUnit timeUnit, Iterable<? extends DATA> drops);

    @Override
    public River<DATA> pushAfter(long delay, TimeUnit timeUnit, DATA drop);

    @Override
    public River<DATA> pushAfter(long delay, TimeUnit timeUnit, DATA... drops);

    /**
     * Flushes the specific river stream, that is, it informs the fed fall that no more data
     * drops are likely to come.
     * <p/>
     * Be aware that the call may be postponed until the fall flushes all the data drops,
     * including the delayed ones.
     *
     * @param streamNumber the number identifying the target stream.
     * @return this river.
     */
    public River<DATA> flushStream(int streamNumber);

    /**
     * Pushes the specified data into the specific river stream flow and then flushes it.
     *
     * @param streamNumber the number identifying the target stream.
     * @param drops        the data drops.
     * @return this river.
     */
    public River<DATA> flushStream(int streamNumber, DATA... drops);

    /**
     * Pushes the data returned by the specified iterable into the specific river stream flow and
     * then flushes it.
     *
     * @param streamNumber the number identifying the target stream.
     * @param drops        the data drops iterable.
     * @return this river.
     */
    public River<DATA> flushStream(int streamNumber, Iterable<? extends DATA> drops);

    /**
     * Pushes the specified data into the specific river stream flow and then flushes it.
     *
     * @param streamNumber the number identifying the target stream.
     * @param drop         the data drop.
     * @return this river.
     */
    public River<DATA> flushStream(int streamNumber, DATA drop);

    /**
     * Pushes the specified data into the specific river stream flow, after the specified time has
     * elapsed, and then flushes it.
     *
     * @param streamNumber the number identifying the target stream.
     * @param delay        the delay in <code>timeUnit</code> time units.
     * @param timeUnit     the delay time unit.
     * @param drops        the data drops.
     * @return this river.
     */
    public River<DATA> flushStreamAfter(int streamNumber, long delay, TimeUnit timeUnit,
            DATA... drops);

    /**
     * Pushes the data returned by the specified iterable into the specific river stream flow,
     * after the specified time has elapsed, and then flushes it.
     *
     * @param streamNumber the number identifying the target stream.
     * @param delay        the delay in <code>timeUnit</code> time units.
     * @param timeUnit     the delay time unit.
     * @param drops        the data drops iterable.
     * @return this river.
     */
    public River<DATA> flushStreamAfter(int streamNumber, long delay, TimeUnit timeUnit,
            Iterable<? extends DATA> drops);

    /**
     * Pushes the specified data into the specific river stream flow, after the specified time has
     * elapsed, and then flushes it.
     *
     * @param streamNumber the number identifying the target stream.
     * @param delay        the delay in <code>timeUnit</code> time units.
     * @param timeUnit     the delay time unit.
     * @param drop         the data drop.
     * @return this river.
     */
    public River<DATA> flushStreamAfter(int streamNumber, long delay, TimeUnit timeUnit, DATA drop);

    /**
     * Pushes the specified data into the specific river stream flow.
     *
     * @param streamNumber the number identifying the target stream.
     * @param drops        the data drops.
     * @return this river.
     */
    public River<DATA> pushStream(int streamNumber, DATA... drops);

    /**
     * Pushes the data returned by the specified iterable into the specific river stream flow.
     *
     * @param streamNumber the number identifying the target stream.
     * @param drops        the data drops iterable.
     * @return this river.
     */
    public River<DATA> pushStream(int streamNumber, Iterable<? extends DATA> drops);

    /**
     * Pushes the specified data into the specific river stream flow.
     *
     * @param streamNumber the number identifying the target stream.
     * @param drop         the data drop.
     * @return this river.
     */
    public River<DATA> pushStream(int streamNumber, DATA drop);

    /**
     * Pushes the data returned by the specified iterable into the specific river stream flow,
     * after the specified time has elapsed.
     *
     * @param streamNumber the number identifying the target stream.
     * @param delay        the delay in <code>timeUnit</code> time units.
     * @param timeUnit     the delay time unit.
     * @param drops        the data drops iterable.
     * @return this river.
     */
    public River<DATA> pushStreamAfter(int streamNumber, long delay, TimeUnit timeUnit,
            Iterable<? extends DATA> drops);

    /**
     * Pushes the specified data into the specific river stream flow, after the specified time has
     * elapsed.
     *
     * @param streamNumber the number identifying the target stream.
     * @param delay        the delay in <code>timeUnit</code> time units.
     * @param timeUnit     the delay time unit.
     * @param drop         the data drop.
     * @return this river.
     */
    public River<DATA> pushStreamAfter(int streamNumber, long delay, TimeUnit timeUnit, DATA drop);

    /**
     * Pushes the specified data into the specific river stream flow, after the specified time has
     * elapsed.
     *
     * @param streamNumber the number identifying the target stream.
     * @param delay        the delay in <code>timeUnit</code> time units.
     * @param timeUnit     the delay time unit.
     * @param drops        the data drops.
     * @return this river.
     */
    public River<DATA> pushStreamAfter(int streamNumber, long delay, TimeUnit timeUnit,
            DATA... drops);

    /**
     * Returns the size of this river, that is the number of streams which compose it.
     *
     * @return the river size.
     */
    public int size();

    /**
     * Forwards the specified unhandled exception into the specific river stream flow.
     *
     * @param streamNumber the number identifying the target stream.
     * @param throwable    the thrown exception.
     * @return this river.
     */
    public River<DATA> streamException(int streamNumber, Throwable throwable);
}