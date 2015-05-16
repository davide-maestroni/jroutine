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
package com.gh.bmd.jrt.proxy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate interfaces used as templates to generate proxy classes
 * enabling asynchronous calls to the target instance methods.
 * <p/>
 * The target class is specified in the annotation attribute. A proxy class implementing the
 * annotated interface will be generated within the interface package and its name will be obtained
 * by appending "{@value #DEFAULT_CLASS_SUFFIX}" to the interface simple name. In case the specific
 * interface is not a top level class, the simple name of the outer classes will be prepended to the
 * interface one.<br/>
 * The routines used for calling the methods will honor the attributes specified in any optional
 * {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout},
 * {@link com.gh.bmd.jrt.annotation.TimeoutAction} and {@link com.gh.bmd.jrt.annotation.Param}
 * annotations defined for each interface method.
 * <p/>
 * Special care must be taken when dealing with proxies of generic classes. First of all, the
 * proxy interface must declare the same generic types as the wrapped class or interface.
 * Additionally, the generic parameters must be declared as <code>Object</code> in order for the
 * proxy interface methods to match the target ones.<br/>
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
 *             &#64;Proxy(MyList.class)
 *             public interface MyListAsync&lt;TYPE&gt; {
 *
 *                 void add(Object element);
 *
 *                 TYPE get(int i);
 *
 *                 &#64;Bind("get")
 *                 &#64;Param(Object.class)
 *                 OutputChannel&lt;TYPE&gt; getAsync(int i);
 *
 *                 &#64;Bind("get")
 *                 &#64;Param(Object.class)
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
@Retention(RetentionPolicy.RUNTIME)
public @interface Proxy {

    /**
     * TODO
     */
    String DEFAULT = "*";

    /**
     * Constant indicating the generated class name prefix.
     */
    String DEFAULT_CLASS_PREFIX = "";

    /**
     * Constant indicating the generated class name suffix.
     */
    String DEFAULT_CLASS_SUFFIX = "_Proxy";

    /**
     * TODO
     *
     * @return
     */
    String generatedClassName() default DEFAULT;

    /**
     * TODO
     *
     * @return
     */
    String generatedClassPackage() default DEFAULT;

    /**
     * TODO
     *
     * @return
     */
    String generatedClassPrefix() default DEFAULT_CLASS_PREFIX;

    /**
     * TODO
     *
     * @return
     */
    String generatedClassSuffix() default DEFAULT_CLASS_SUFFIX;

    /**
     * The wrapped class.
     *
     * @return the class.
     */
    Class<?> value();
}
