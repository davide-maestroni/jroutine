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
import com.bmd.wtf.flw.Gate;

import java.util.concurrent.TimeUnit;

/**
 * Interface extending a gate.
 * <p/>
 * The implementing class provides methods to access the gate leap via reflection by wrapping it in
 * a proxy object so to make every call thread safe.<br/>
 * In order for that to correctly work, the leap instance must be accessed only through the
 * implemented interface methods.
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
    public RapidGate<TYPE> when(final ConditionEvaluator<? super TYPE> evaluator);

    /**
     * Returns the gate wrapped so to be accessed in a thread safe way.
     * <p/>
     * Note that the gate type must be an interface. An exception will be thrown otherwise.
     *
     * @return The wrapped gate.
     */
    public TYPE perform();

    /**
     * Returns the gate wrapped so to be accessed in a thread safe way.
     * <p/>
     * Note that the specified gate type must be an interface. An exception will be thrown
     * otherwise.
     *
     * @param gateClass The gate class.
     * @param <NTYPE>   The new gate type.
     * @return The wrapped gate.
     */
    public <NTYPE> NTYPE performAs(Class<NTYPE> gateClass);

    /**
     * Returns the gate wrapped so to be accessed in a thread safe way.
     * <p/>
     * Note that the specified gate type must be an interface. An exception will be thrown
     * otherwise.
     *
     * @param gateClassification The gate classification.
     * @param <NTYPE>            The new gate type.
     * @return The wrapped gate.
     */
    public <NTYPE> NTYPE performAs(Classification<NTYPE> gateClassification);

    /**
     * Sets the condition to be met by this gate by searching a suitable method via reflection.
     * <p/>
     * A suitable method is identified among the ones taking the specified arguments as parameters
     * and returning a boolean value. The methods annotated with
     * {@link RapidAnnotations.GateCondition} are analyzed first.<br/>
     * If more than one method matching the above requirements is found, an exception will be
     * thrown.
     *
     * @param args The arguments to be passed to the method.
     * @return The rapid gate.
     */
    public RapidGate<TYPE> whenSatisfies(final Object... args);
}