/*
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
package com.github.dm.jrt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to decorate methods that are to be invoked in an asynchronous way.<br/>
 * Note that the piece of code inside such methods will be automatically protected so to avoid
 * concurrency issues. Though, other parts of the code inside the same class will be not.<br/>
 * In order to prevent unexpected behaviors, it is advisable to avoid using the same class fields
 * (unless immutable) in protected and non-protected code, or to call synchronous methods through
 * routines as well.
 * <p/>
 * The only use case in which this annotation is useful, is when an interface is used as a proxy
 * of another class methods. The interface can take all its input parameters in an asynchronous way.
 * In such case, the values specified in the annotation will indicate the type of the parameters
 * expected by the target method.
 * <p/>
 * For example, a method taking two integers:
 * <p/>
 * <pre>
 *     <code>
 *
 *         public int sum(int i1, int i2);
 *     </code>
 * </pre>
 * can be proxied by a method defined as:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;Inputs({int.class, int.class})
 *         public InvocationChannel&lt;Integer, Integer&gt; sum();
 *     </code>
 * </pre>
 * The proxying method can also return the routine wrapping the target one, as:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;Inputs({int.class, int.class})
 *         public Routine&lt;Integer, Integer&gt; sum();
 *     </code>
 * </pre>
 * <p/>
 * In such case, it is up to the caller to invoke it in the proper mode.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.github.dm.jrt.annotation.Inputs *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide-maestroni on 05/22/2015.
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inputs {

    /**
     * The array of parameter types.
     *
     * @return the parameter types.
     */
    Class<?>[] value();
}
