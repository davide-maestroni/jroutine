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
package com.bmd.wtf.rpd;

import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.flw.Gate.ConditionEvaluator;

import java.util.concurrent.TimeUnit;

/**
 * Interface extending a gate. TODo
 * <p/>
 * Created by davide on 7/4/14.
 *
 * @param <TYPE> The backed leap type.
 */
public interface RapidGate<TYPE> {

    /**
     * Tells the gate to fail if the condition is not met before the specified time has elapsed.
     *
     * @param maxDelay The maximum delay in the specified time unit.
     * @param timeUnit The delay time unit.
     * @return This gate.
     * @see #when(ConditionEvaluator)
     */
    public RapidGate<TYPE> afterMax(long maxDelay, TimeUnit timeUnit);

    /**
     * Tells the gate to wait indefinitely for the condition to be met.
     *
     * @return This gate.
     * @see #when(ConditionEvaluator)
     */
    public RapidGate<TYPE> eventually();

    /**
     * Tells the gate to throw the specified exception if the maximum delay elapses before the
     * condition is met.
     *
     * @param exception The exception to be thrown.
     * @return This gate.
     * @see #when(ConditionEvaluator)
     */
    public RapidGate<TYPE> eventuallyThrow(RuntimeException exception);

    /**
     * Tells the collector to fail if the condition is not immediately met.
     *
     * @return This collector.
     */
    public RapidGate<TYPE> immediately();

    /**
     * Returns the gate wrapped so to be accessed in a thread safe way.
     * <p/>
     * Note that, in order to correctly work, the specified gate type must be an interface.
     *
     * @return The wrapped gate.
     */
    public TYPE perform();

    /**
     * Returns the gate wrapped so to be accessed in a thread safe way.
     * <p/>
     * Note that, in order to correctly work, the specified gate type must be an interface.
     *
     * @param gateClass The gate class.
     * @param <NTYPE>   The new gate type.
     * @return The wrapped gate.
     */
    public <NTYPE> NTYPE performAs(Class<NTYPE> gateClass);

    /**
     * Returns the gate wrapped so to be accessed in a thread safe way.
     * <p/>
     * Note that, in order to correctly work, the specified gate type must be an interface.
     *
     * @param gateClassification The gate classification.
     * @param <NTYPE>            The new gate type.
     * @return The wrapped gate.
     */
    public <NTYPE> NTYPE performAs(Classification<NTYPE> gateClassification);

    /**
     * Sets the condition to be met by the leap backing this gate.
     * <p/>
     * A null condition (as by default) is always immediately met.
     *
     * @param evaluator The condition evaluator.
     * @return This gate.
     */
    public RapidGate<TYPE> when(final ConditionEvaluator<? super TYPE> evaluator);

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
    public RapidGate<TYPE> whenSatisfies(final Object... args);
}