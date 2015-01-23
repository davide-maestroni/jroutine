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
 * of another class methods. The interface can take an array or collection of input parameters
 * which will be passed in a parallel way to the mirrored method, so that the method will be called
 * several times, one for each input value.<br/>
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
 *         &#64;AsyncType(int.class)
 *         public OutputChannel&lt;Integer&gt; square(&#64;ParallelType(int.class) int... i);
 *     </code>
 * </pre>
 * <p/>
 * or:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;AsyncType(int.class)
 *         public List&lt;Integer&gt; square(&#64;ParallelType(int.class) List&lt;Integer&gt; i);
 *     </code>
 * </pre>
 * <p/>
 * Note that the this annotation is applicable only when the target method takes a single parameter
 * as input.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.bmd.jrt.annotation.ParallelType *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 11/11/14.
 *
 * @see AsyncType
 */
@Inherited
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParallelType {

    /**
     * The type of the overridden parameter.
     *
     * @return the parameter type.
     */
    Class<?> value();
}
