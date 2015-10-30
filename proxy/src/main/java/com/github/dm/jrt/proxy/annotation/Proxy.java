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
package com.github.dm.jrt.proxy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate interfaces used as templates to generate proxy classes
 * enabling asynchronous calls of the target instance methods.<br/>
 * The target class is specified in the annotation value. A proxy class implementing the annotated
 * interface will be generated according to the specific annotation attributes.
 * <p/>
 * The routines used for calling the methods will honor the attributes specified in any optional
 * <i>{@code com.github.dm.jrt.annotation.*}</i> annotations defined for each interface method.
 * <p/>
 * Special care must be taken when dealing with proxies of generic classes. First of all, the
 * proxy interface must declare the same generic types as the wrapped class or interface.
 * Additionally, the generic parameters must be declared as {@code Object} in order for the proxy
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
 *             &#64;Proxy(MyList.class)
 *             public interface MyListAsync&lt;TYPE&gt; {
 *
 *                 void add(Object element);
 *
 *                 TYPE get(int i);
 *
 *                 &#64;Alias("get")
 *                 &#64;Output
 *                 OutputChannel&lt;TYPE&gt; getAsync(int i);
 *
 *                 &#64;Alias("get")
 *                 &#64;Output(OutputMode.COLLECTION)
 *                 List&lt;TYPE&gt; getList(int i);
 *             }
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
 *              &#64;com.github.dm.jrt.proxy.annotation.Proxy *;
 *         }
 *     </code>
 * </pre>
 * Be sure also to include a proper rule in your Proguard file, so to keep the name of all the
 * classes implementing the specific mirror interface, like, for example:
 * <pre>
 *     <code>
 *
 *         -keep public class * extends my.mirror.Interface {
 *              public &lt;init&gt;;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide-maestroni on 11/03/2014.
 *
 * @see com.github.dm.jrt.annotation.Alias Alias
 * @see com.github.dm.jrt.annotation.CoreInstances CoreInstances
 * @see com.github.dm.jrt.annotation.Input Input
 * @see com.github.dm.jrt.annotation.InputMaxSize InputMaxSize
 * @see com.github.dm.jrt.annotation.InputOrder InputOrder
 * @see com.github.dm.jrt.annotation.Inputs Inputs
 * @see com.github.dm.jrt.annotation.InputTimeout InputTimeout
 * @see com.github.dm.jrt.annotation.MaxInstances MaxInstances
 * @see com.github.dm.jrt.annotation.Invoke Invoke
 * @see com.github.dm.jrt.annotation.Output Output
 * @see com.github.dm.jrt.annotation.OutputMaxSize OutputMaxSize
 * @see com.github.dm.jrt.annotation.OutputOrder OutputOrder
 * @see com.github.dm.jrt.annotation.OutputTimeout OutputTimeout
 * @see com.github.dm.jrt.annotation.Priority Priority
 * @see com.github.dm.jrt.annotation.ReadTimeout ReadTimeout
 * @see com.github.dm.jrt.annotation.ReadTimeoutAction ReadTimeoutAction
 * @see com.github.dm.jrt.annotation.SharedFields SharedFields
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Proxy {

    /**
     * Constant indicating a default class name or package.
     */
    String DEFAULT = "*";

    /**
     * Constant indicating the default generated class name prefix.
     */
    String DEFAULT_CLASS_PREFIX = "Proxy_";

    /**
     * Constant indicating the default generated class name suffix.
     */
    String DEFAULT_CLASS_SUFFIX = "";

    /**
     * The generated class name. By default the name is obtained by the interface simple name,
     * prepending all the outer class names in case it is not a top level class.
     * <p/>
     * For instance, an interface named <code>MyItf</code> defined inside a class named
     * <code>MyClass</code>, will result in the generation of a class named
     * <code>Proxy_MyClass_MyItf</code>.
     *
     * @return the class name.
     */
    String className() default DEFAULT;

    /**
     * The generated class package. By default it is the same as the interface one.
     *
     * @return the package.
     */
    String classPackage() default DEFAULT;

    /**
     * The generated class name prefix.
     *
     * @return the name prefix.
     */
    String classPrefix() default DEFAULT_CLASS_PREFIX;

    /**
     * The generated class name suffix.
     *
     * @return the name suffix.
     */
    String classSuffix() default DEFAULT_CLASS_SUFFIX;

    /**
     * The wrapped class.
     *
     * @return the class.
     */
    Class<?> value();
}
