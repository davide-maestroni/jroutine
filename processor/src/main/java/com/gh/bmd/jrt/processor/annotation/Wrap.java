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
package com.gh.bmd.jrt.processor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate interfaces used as templates to generate wrapper classes
 * enabling asynchronous calls to the target instance methods.
 * <p/>
 * The target class is specified in the annotation attribute. A wrapper class implementing the
 * annotated interface will be generated in the interface package and its name will be obtained by
 * prepending "JRoutine_" to the interface simple name.<br/>
 * The routines used for calling the methods will honor the attributes specified in any optional
 * {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout},
 * {@link com.gh.bmd.jrt.annotation.TimeoutAction} and {@link com.gh.bmd.jrt.annotation.Pass}
 * annotations defined for each interface method.
 * <p/>
 * Special care must be taken when dealing with wrappers of generic classes. First of all, the
 * wrapper interface must declare the same generic types as the wrapped class or interface.
 * Additionally, the generic parameters must be declared as Object in order for the wrapper
 * interface methods to match the target ones.<br/>
 * Be also aware that it is responsibility of the caller to ensure that the same instance is not
 * wrapped around two different generic interfaces.<br/>
 * For example, a class of the type:
 * <pre>
 *     <code>
 *
 *             public class MyList&lt;TYPE&gt; {
 *
 *                 private final ArrayList&lt;TYPE&gt; mList = new ArrayList&lt;TYPE&gt;();
 *
 *                 public void add(final TYPE element) {
 *
 *                     mList.add(element);
 *                 }
 *
 *                 public TYPE get(final int i) {
 *
 *                     return mList.get(i);
 *                 }
 *             }
 *     </code>
 * </pre>
 * can be correctly wrapped by an interface of the type:
 * <pre>
 *     <code>
 *
 *             &#64;Wrap(MyList.class)
 *             public interface MyListAsync&lt;TYPE&gt; {
 *
 *                 void add(Object element);
 *
 *                 TYPE get(int i);
 *
 *                 &#64;Bind("get")
 *                 &#64;Pass(Object.class)
 *                 OutputChannel&lt;TYPE&gt; getAsync(int i);
 *
 *                 &#64;Bind("get")
 *                 &#64;Pass(Object.class)
 *                 List&lt;TYPE&gt; getList(int i);
 *             }
 *     </code>
 * </pre>
 * <p/>
 * Note that, you'll need to enable annotation pre-processing by adding the "jroutine-processor"
 * artifact or module to the specific project dependencies. Be sure also to include a proper rule in
 * your Proguard file, so to keep the name of all the classes implementing the specific mirror
 * interface, like, for example:
 * <pre>
 *     <code>
 *
 *         -keep public class * extends my.mirror.Interface {
 *              public &lt;init&gt;;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 11/3/14.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Wrap {

    /**
     * The list of wrapped classes.
     *
     * @return the wrapped classes.
     */
    Class<?> value();
}