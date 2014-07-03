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
package com.bmd.wtf.xtr.rpd;

import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.fll.Waterfall;
import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.flw.River;

import java.util.concurrent.TimeUnit;

/**
 * Interface with the functionalities of both a gate and a river.
 * <p/>
 * Note however that, unlikely gate or river objects, as a result of a method call a new instance
 * might be returned.
 * <p/>
 * Created by davide on 6/20/14.
 *
 * @param <SOURCE> The source data type.
 * @param <MOUTH>  The mouth data type.
 * @param <IN>     The input data type.
 * @param <OUT>    The output data type.
 * @param <TYPE>   The gate type.
 */
public interface RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> extends Gate<TYPE>, River<SOURCE, IN> {

    /**
     * Deviates the river and return this instance.
     *
     * @return The rapid gate.
     * @see #deviate()
     */
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDeviate();

    /**
     * Deviates the specified river stream and return this instance.
     *
     * @param streamNumber The number identifying the target stream.
     * @return The rapid gate.
     * @see #deviateStream(int)
     */
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDeviate(int streamNumber);

    /**
     * Drains the river and return this instance.
     *
     * @return The rapid gate.
     * @see #drain()
     */
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDrain();

    /**
     * Drains the specified river stream and return this instance.
     *
     * @param streamNumber The number identifying the target stream.
     * @return The rapid gate.
     * @see #drainStream(int)
     */
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterDrain(int streamNumber);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> afterMax(long maxDelay, TimeUnit timeUnit);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> eventually();

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> eventuallyThrow(RuntimeException exception);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> immediately();

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> when(ConditionEvaluator<? super TYPE> evaluator);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> discharge();

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> forward(Throwable throwable);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(IN... drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(Iterable<? extends IN> drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> push(IN drop);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(long delay, TimeUnit timeUnit,
            Iterable<? extends IN> drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(long delay, TimeUnit timeUnit,
            IN drop);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushAfter(long delay, TimeUnit timeUnit,
            IN... drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> dischargeStream(int streamNumber);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> forwardStream(int streamNumber,
            Throwable throwable);

    @Override
    public <NTYPE> RapidGate<SOURCE, MOUTH, IN, OUT, NTYPE> on(Class<NTYPE> gateType);

    @Override
    public <NTYPE> RapidGate<SOURCE, MOUTH, IN, OUT, NTYPE> on(NTYPE leap);

    @Override
    public <NTYPE> RapidGate<SOURCE, MOUTH, IN, OUT, NTYPE> on(
            Classification<NTYPE> gateClassification);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushStream(int streamNumber, IN... drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushStream(int streamNumber,
            Iterable<? extends IN> drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushStream(int streamNumber, IN drop);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushStreamAfter(int streamNumber, long delay,
            TimeUnit timeUnit, Iterable<? extends IN> drops);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushStreamAfter(int streamNumber, long delay,
            TimeUnit timeUnit, IN drop);

    @Override
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> pushStreamAfter(int streamNumber, long delay,
            TimeUnit timeUnit, IN... drops);

    @Override
    public RapidGate<SOURCE, MOUTH, SOURCE, OUT, TYPE> source();

    /**
     * Returns the river mouth.
     *
     * @return The river mouth.
     */
    public RapidGate<SOURCE, MOUTH, MOUTH, OUT, TYPE> mouth();

    /**
     * Returns the gate of type set by calling {@link #on(com.bmd.wtf.fll.Classification)} or
     * {@link #on(Class)} methods wrapped so to be accessed in a thread safe way.
     * <p/>
     * Note that, in order to correctly work, the specified gate type must be an interface.
     *
     * @return The wrapped gate.
     */
    public TYPE perform();

    /**
     * Returns the backing waterfall.
     *
     * @return The waterfall.
     */
    public Waterfall<SOURCE, MOUTH, OUT> waterfall();

    /**
     * Sets the condition to be met by this gate by searching a suitable method via reflection.
     * <p/>
     * A suitable method is identified among the ones taking the specified arguments and returning
     * a boolean value. The methods annotated with {@link RapidAnnotations.Condition} are analyzed
     * first.<br/>
     * If more than one method matching the above requirements is found, an exception will be
     * thrown.
     *
     * @param args The arguments to be passed to the method.
     * @return The rapid gate.
     */
    public RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> when(Object... args);
}