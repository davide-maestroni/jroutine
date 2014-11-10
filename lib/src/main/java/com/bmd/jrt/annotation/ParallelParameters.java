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
 * This annotation is used to indicate methods invoked in a parallel way.
 * <p/>
 * The only use case in which this annotation is useful, is when an interface is used as a mirror
 * of another class methods. The interface can take an array or collection of input parameters
 * which will be passed in a parallel way to the mirrored method, so that the method will be called
 * several times, one for each input value.<br/>
 * In this case, the specified type indicate the parameter type expected by the target method.
 * <p/>
 * For example, a method taking an integer:
 * <p/>
 * <pre>
 *     <code>
 *
 *         public int sum(int i);
 *     </code>
 * </pre>
 * can be mirrored by a method defined as:
 * <p/>
 * <pre>
 *     <code>
 *
 *         &#64;AsyncResult
 *         &#64;ParallelParameter(int.class)
 *         public OutputChannel&lt;Integer&gt; sum(int... i);
 *     </code>
 * </pre>
 * <p/>
 * Note that, in order to properly distribute the computation into different threads, the annotated
 * method should not use any lock, that is, it should not use any class field unless immutable.<br/>
 * Moreover, since normally each input value will produce a result, it is advisable (even if not
 * mandatory) to use an output channel to read all the output data.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.bmd.jrt.annotation.ParallelParameter *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 11/9/14.
 *
 * @see Async
 * @see AsyncResult
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParallelParameters {

    Class<?> value();
}
