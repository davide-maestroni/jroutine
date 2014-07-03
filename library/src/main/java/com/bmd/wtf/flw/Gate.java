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
 * A gate instance allows to access a leap instance chained to the waterfall in a thread safe way.
 * <p/>
 * Created by davide on 6/13/14.
 *
 * @param <TYPE> The backed leap type.
 */
public interface Gate<TYPE> {

    /**
     * Tells the gate to fail if the condition is not met before the specified time has elapsed.
     *
     * @param maxDelay The maximum delay in the specified time unit.
     * @param timeUnit The delay time unit.
     * @return This gate.
     * @see #when(ConditionEvaluator)
     */
    public Gate<TYPE> afterMax(long maxDelay, TimeUnit timeUnit);

    /**
     * Tells the gate to wait indefinitely for the condition to be met.
     *
     * @return This gate.
     * @see #when(ConditionEvaluator)
     */
    public Gate<TYPE> eventually();

    /**
     * Tells the gate to throw the specified exception if the maximum delay elapses before the
     * condition is met.
     *
     * @param exception The exception to be thrown.
     * @return This gate.
     * @see #when(ConditionEvaluator)
     */
    public Gate<TYPE> eventuallyThrow(RuntimeException exception);

    /**
     * Tells the collector to fail if the condition is not immediately met.
     *
     * @return This collector.
     */
    public Gate<TYPE> immediately();

    /**
     * Performs the specified action by passing the variadic arguments as parameters.
     *
     * @param action   The action to perform on the leap backing this gate.
     * @param args     The action arguments.
     * @param <RESULT> The result type.
     * @return The action result.
     */
    public <RESULT> RESULT perform(Action<RESULT, ? super TYPE> action, Object... args);

    /**
     * Sets the condition to be met by the leap backing this gate.
     * <p/>
     * A null condition (as by default) is always immediately met.
     *
     * @param evaluator The condition evaluator.
     * @return This gate.
     */
    public Gate<TYPE> when(ConditionEvaluator<? super TYPE> evaluator);

    /**
     * Interface defining an action to be performed on the backed leap.
     *
     * @param <RESULT> The result type.
     * @param <TYPE>   The leap type.
     */
    public interface Action<RESULT, TYPE> {

        /**
         * Performs this action on the specified leap.
         *
         * @param leap The leap instance.
         * @param args The action arguments.
         * @return The action result.
         */
        public RESULT doOn(TYPE leap, Object... args);
    }

    /**
     * Condition evaluator.
     *
     * @param <TYPE> The leap type.
     */
    public interface ConditionEvaluator<TYPE> {

        /**
         * Checks if this condition is satisfied by the specified leap.
         *
         * @param leap The leap instance.
         * @return Whether the condition is satisfied.
         */
        public boolean isSatisfied(TYPE leap);
    }
}