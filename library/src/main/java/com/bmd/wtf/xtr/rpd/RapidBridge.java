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
import com.bmd.wtf.flw.Bridge;

import java.util.concurrent.TimeUnit;

/**
 * Interface extending a bridge.
 * <p/>
 * The implementing class provides methods to access the bridge gate via reflection, by wrapping it
 * in a proxy object, so to make every call thread safe.<br/>
 * In order for that to correctly work, the gate instance must be accessed only through the
 * implemented interface methods.
 * <p/>
 * Created by davide on 7/4/14.
 *
 * @param <TYPE> the backed gate type.
 */
public interface RapidBridge<TYPE> extends Bridge<TYPE> {

    @Override
    public RapidBridge<TYPE> afterMax(long maxDelay, TimeUnit timeUnit);

    @Override
    public RapidBridge<TYPE> eventually();

    @Override
    public RapidBridge<TYPE> eventuallyThrow(RuntimeException exception);

    @Override
    public RapidBridge<TYPE> immediately();

    @Override
    public RapidBridge<TYPE> when(final ConditionEvaluator<? super TYPE> evaluator);

    /**
     * Returns the gate wrapped so to be accessed in a thread safe way.
     * <p/>
     * Note that the gate type must be an interface. An exception will be thrown otherwise.
     *
     * @return the wrapped gate.
     */
    public TYPE perform();

    /**
     * Returns the gate wrapped so to be accessed in a thread safe way.
     * <p/>
     * Note that the specified gate type must be an interface. An exception will be thrown
     * otherwise.
     *
     * @param gateClass the gate class.
     * @param <NTYPE>   the new gate type.
     * @return the wrapped gate.
     */
    public <NTYPE> NTYPE performAs(Class<NTYPE> gateClass);

    /**
     * Returns the gate wrapped so to be accessed in a thread safe way.
     * <p/>
     * Note that the specified gate type must be an interface. An exception will be thrown
     * otherwise.
     *
     * @param gateClassification the gate classification.
     * @param <NTYPE>            the new gate type.
     * @return the wrapped gate.
     */
    public <NTYPE> NTYPE performAs(Classification<NTYPE> gateClassification);

    /**
     * Sets the condition to be met by the gate by searching a suitable method via reflection.
     * <p/>
     * A suitable method is identified among the ones taking the specified arguments as parameters
     * and returning a boolean value. The methods annotated with
     * {@link RapidAnnotations.GateCondition} are analyzed first.<br/>
     * If more than one method matching the above requirements is found, an exception will be
     * thrown.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$GateCondition *;
     *         }
     *     </code>
     * </pre>
     * <p/>
     *
     * @param args the arguments to be passed to the method.
     * @return the rapid bridge.
     */
    public RapidBridge<TYPE> whenSatisfies(final Object... args);
}