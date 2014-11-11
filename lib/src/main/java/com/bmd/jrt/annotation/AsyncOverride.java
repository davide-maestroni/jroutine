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
package com.bmd.jrt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate methods whose parameters or output are passed asynchronously.
 * <p/>
 * The only use case in which this annotation is useful, is when an interface is used as a mirror
 * of another class methods. The interface can take some input parameters as output channels whose
 * output will be passed to the mirrored method.<br/>
 * In this case, the specified types indicate the parameter types expected by the target method.
 * <p/>
 * For example, a method taking two integers:
 * <p/>
 * <pre>
 *     <code>
 *
 *         public int sum(int i1, int i2);
 *     </code>
 * </pre>
 * can be mirrored by a method defined as:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;AsyncOverride({int.class, int.class})
 *         public int sum(OutputChannel&lt;Integer&gt; i1, int i2);
 *     </code>
 * </pre>
 * <p/>
 * Additionally, the interface can return the result as an output channel whose output data will be
 * passed only when available.
 * <p/>
 * For example, a method returning an integer:
 * <p/>
 * <pre>
 *     <code>
 *
 *         public int sum(int i1, int i2);
 *     </code>
 * </pre>
 * can be mirrored by a method defined as:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;AsyncOverride(result = true)
 *         public OutputChannel&lt;Integer&gt; sum(int i1, int i2);
 *     </code>
 * </pre>
 * <p/>
 * The interface can also take an array or collection of input parameters which will be passed in a
 * parallel way to the mirrored method, so that the method will be called several times, one for
 * each input value.<br/>
 * In this case, the specified type indicates the parameter type expected by the target method.
 * <p/>
 * For example, a method taking an integer:
 * <p/>
 * <pre>
 *     <code>
 *
 *         public int square(int i);
 *     </code>
 * </pre>
 * can be mirrored by a method defined as:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;AsyncOverride(value = int.class, parallel = true, result = true)
 *         public OutputChannel&lt;Integer&gt; square(int... i);
 *     </code>
 * </pre>
 * <p/>
 * or:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;AsyncOverride(value = int.class, parallel = true, result = true)
 *         public OutputChannel&lt;Integer&gt; square(List&lt;Integer&gt; i);
 *     </code>
 * </pre>
 * <p/>
 * Note that the described overrides can be composed in any way, like, for example:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;AsyncOverride(value = int.class, parallel = true, result = true)
 *         public OutputChannel&lt;Integer&gt; square(OutputChannel&lt;Integer&gt; i);
 *     </code>
 * </pre>
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.bmd.jrt.annotation.AsyncOverride *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 11/11/14.
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncOverride {

    /**
     * If the inputs must be passed in a parallel way.
     *
     * @return whether the inputs are passed in a parallel way.
     */
    boolean parallel() default false;

    /**
     * If the results are returned through an output channel.
     *
     * @return whether results are passed through an output channel.
     */
    boolean result() default false;

    /**
     * The list of the overridden method parameter types.
     *
     * @return the list of types.
     */
    Class<?>[] value() default {};
}
