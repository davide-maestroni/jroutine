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
 * enabling asynchronous calls to the target instance methods in a dedicated service.<br/>
 * The target class is specified in the annotation value. A proxy class implementing the annotated
 * interface will be generated according to the specific annotation attributes.
 * <p/>
 * The routines used for calling the methods will honor the attributes specified in any optional
 * {@link com.gh.bmd.jrt.annotation.Alias}, {@link com.gh.bmd.jrt.annotation.Timeout},
 * {@link com.gh.bmd.jrt.annotation.TimeoutAction} and {@link com.gh.bmd.jrt.annotation.Param}.
 * <p/>
 * Created by davide-maestroni on 13/05/15.
 *
 * @see com.gh.bmd.jrt.proxy.annotation.Proxy
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceProxy {

    /**
     * Constant indicating the default generated class name prefix.
     */
    String DEFAULT_CLASS_PREFIX = "ServiceProxy_";

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
