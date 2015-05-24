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
package com.gh.bmd.jrt.android.proxy.annotation;

import com.gh.bmd.jrt.proxy.annotation.Proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate interfaces used as templates to generate proxy classes,
 * enabling asynchronous calls to the target instance methods, bound to a context lifecycle.<br/>
 * The target class is specified in the annotation value. A proxy class implementing the annotated
 * interface will be generated according to the specific annotation attributes.
 * <p/>
 * The routines used for calling the methods will honor the attributes specified in any optional
 * {@link com.gh.bmd.jrt.annotation.Alias}, {@link com.gh.bmd.jrt.annotation.Input},
 * {@link com.gh.bmd.jrt.annotation.Inputs}, {@link com.gh.bmd.jrt.annotation.Output},
 * {@link com.gh.bmd.jrt.annotation.Timeout} and {@link com.gh.bmd.jrt.annotation.TimeoutAction}, as
 * well as v4 builder annotations defined for each interface method.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.gh.bmd.jrt.android.proxy.annotation.V4Proxy *;
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
 * Created by davide-maestroni on 06/05/15.
 *
 * @see com.gh.bmd.jrt.proxy.annotation.Proxy
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface V4Proxy {

    /**
     * Constant indicating the default generated class name prefix.
     */
    String DEFAULT_CLASS_PREFIX = "V4Proxy_";

    /**
     * Constant indicating the default generated class name suffix.
     */
    String DEFAULT_CLASS_SUFFIX = "";

    /**
     * The generated class name. By default the name is obtained by the interface simple name,
     * prepending all the outer class names in case it is not a top level class.
     *
     * @return the class name.
     */
    String generatedClassName() default Proxy.DEFAULT;

    /**
     * The generated class package. By default it is the same as the interface.
     *
     * @return the package.
     */
    String generatedClassPackage() default Proxy.DEFAULT;

    /**
     * The generated class name prefix.
     *
     * @return the name prefix.
     */
    String generatedClassPrefix() default DEFAULT_CLASS_PREFIX;

    /**
     * The generated class name suffix.
     *
     * @return the name suffix.
     */
    String generatedClassSuffix() default DEFAULT_CLASS_SUFFIX;

    /**
     * The wrapped class.
     *
     * @return the class.
     */
    Class<?> value();
}
