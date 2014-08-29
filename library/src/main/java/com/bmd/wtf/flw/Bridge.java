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
 * A bridge allows to access a gate instance from outside the waterfall in a thread safe way.
 * <p/>
 * Created by davide on 6/13/14.
 *
 * @param <TYPE> the backed gate type.
 */
public interface Bridge<TYPE> {

    /**
     * Tells the bridge to fail if the condition is not met before the specified time has elapsed.
     *
     * @param maxDelay the maximum delay in the specified time unit.
     * @param timeUnit the delay time unit.
     * @return this bridge.
     * @see #when(ConditionEvaluator)
     */
    public Bridge<TYPE> afterMax(long maxDelay, TimeUnit timeUnit);

    /**
     * Tells the bridge to wait indefinitely for the condition to be met.
     *
     * @return this bridge.
     * @see #when(ConditionEvaluator)
     */
    public Bridge<TYPE> eventually();

    /**
     * Tells the bridge to throw the specified exception if the maximum delay elapses before the
     * condition is met.
     *
     * @param exception the exception to be thrown.
     * @return this bridge.
     * @see #when(ConditionEvaluator)
     */
    public Bridge<TYPE> eventuallyThrow(RuntimeException exception);

    /**
     * Gets the bridge classification.
     *
     * @return the bridge classification.
     */
    public Classification<TYPE> getClassification();

    /**
     * Tells the bridge to fail if the condition is not immediately met.
     *
     * @return this bridge.
     */
    public Bridge<TYPE> immediately();

    /**
     * Performs the specified action by passing the variadic arguments as parameters.
     *
     * @param action   the action to perform on the gate backing this bridge.
     * @param args     the action arguments.
     * @param <RESULT> the result type.
     * @return the action result.
     * @throws DelayInterruptedException if the calling thread is interrupted.
     */
    //TODO: visit
    public <RESULT> RESULT perform(Action<RESULT, ? super TYPE> action, Object... args);

    /**
     * Sets the condition to be met by the gate backing this bridge.
     * <p/>
     * A null condition (as by default) is always immediately met.
     *
     * @param evaluator the condition evaluator.
     * @return this bridge.
     */
    public Bridge<TYPE> when(ConditionEvaluator<? super TYPE> evaluator);

    /**
     * Interface defining an action to be performed on the backed gate.
     *
     * @param <RESULT> the result type.
     * @param <TYPE>   the gate type.
     */
    public interface Action<RESULT, TYPE> {

        /**
         * Performs this action on the specified gate.
         *
         * @param gate the gate instance.
         * @param args the action arguments.
         * @return the action result.
         */
        public RESULT doOn(TYPE gate, Object... args);
    }

    /**
     * Condition evaluator.
     *
     * @param <TYPE> the gate type.
     */
    public interface ConditionEvaluator<TYPE> {

        /**
         * Checks if this condition is satisfied by the specified gate.
         *
         * @param gate the gate instance.
         * @return whether the condition is satisfied.
         */
        public boolean isSatisfied(TYPE gate);
    }
}