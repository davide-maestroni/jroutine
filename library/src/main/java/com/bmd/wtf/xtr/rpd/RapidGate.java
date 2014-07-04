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
import com.bmd.wtf.flw.Gate;

import java.util.concurrent.TimeUnit;

/**
 * Interface extending a gate.
 * <p/>
 * Created by davide on 7/4/14.
 *
 * @param <TYPE> The backed leap type.
 */
public interface RapidGate<TYPE> extends Gate<TYPE> {

    @Override
    public RapidGate<TYPE> afterMax(long maxDelay, TimeUnit timeUnit);

    @Override
    public RapidGate<TYPE> eventually();

    @Override
    public RapidGate<TYPE> eventuallyThrow(RuntimeException exception);

    @Override
    public RapidGate<TYPE> immediately();

    @Override
    public RapidGate<TYPE> when(ConditionEvaluator<? super TYPE> evaluator);

    /**
     * Returns a new rapid gate of the specified type.
     *
     * @param gateClass The gate class.
     * @param <NTYPE>   The new gate type.
     * @return The new rapid gate.
     */
    public <NTYPE> RapidGate<NTYPE> as(Class<NTYPE> gateClass);

    /**
     * Returns a new rapid gate of the specified type.
     *
     * @param gateClassification The gate classification.
     * @param <NTYPE>            The new gate type.
     * @return The new rapid gate.
     */
    public <NTYPE> RapidGate<NTYPE> as(Classification<NTYPE> gateClassification);

    /**
     * Returns the leap of this type wrapped so to be accessed in a thread safe way.
     * <p/>
     * Note that, in order to correctly work, the specified gate type must be an interface.
     *
     * @return The wrapped gate.
     */
    public TYPE perform();
}