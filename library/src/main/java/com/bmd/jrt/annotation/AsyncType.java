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
 *         public int sum(&#64;AsyncType(int.class) OutputChannel&lt;Integer&gt; i1, int i2);
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
 *         &#64;AsyncType(int.class)
 *         public OutputChannel&lt;Integer&gt; sum(int i1, int i2);
 *     </code>
 * </pre>
 * <p/>
 * The interface can also return an array or list of outputs:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;AsyncType(int.class)
 *         public List&lt;Integer&gt; sum(int i1, int i2);
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
 *              &#64;com.bmd.jrt.annotation.AsyncType *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 11/11/14.
 */
@Inherited
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncType {

    /**
     * TODO
     *
     * @return
     */
    boolean aggregate() default false;

    /**
     * The type of the overridden parameter.
     *
     * @return the parameter type.
     */
    Class<?> value();
}
