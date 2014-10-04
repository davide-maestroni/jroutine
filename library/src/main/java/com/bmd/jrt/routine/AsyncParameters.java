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
package com.bmd.jrt.routine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate methods whose parameters are passed asynchronously.<br/>
 * The only use case in which this annotation is useful, is when an interface is used as a mirror
 * of another class methods. The interface can take some input parameters as output channels whose
 * output will be passed to the mirrored method, only when available.<br/>
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
 *         &#64;AsyncParameters({int.class, int.class})
 *         public int sum(OutputChannel&lt;Integer&gt; i1, int i2);
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 10/1/14.
 *
 * @see Async
 * @see AsyncResult
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncParameters {

    /**
     * The list of the overridden method parameter types.
     *
     * @return the list of types.
     */
    Class<?>[] value();
}