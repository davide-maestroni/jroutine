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
package com.gh.bmd.jrt.android.processor.v4.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate interfaces used as templates to generate proxy classes
 * enabling asynchronous calls to the target instance methods, bound to a context lifecycle.
 * <p/>
 * The target class is specified in the annotation attribute. A proxy class implementing the
 * annotated interface will be generated within the interface package and its name will be obtained
 * by appending "{@value #CLASS_NAME_SUFFIX}" to the interface simple name. In case the specific
 * interface is not a top level class, the simple name of the outer classes will be prepended to the
 * interface one.<br/>
 * The routines used for calling the methods will honor the attributes specified in any optional
 * {@link com.gh.bmd.jrt.annotation.Bind}, {@link com.gh.bmd.jrt.annotation.Timeout},
 * {@link com.gh.bmd.jrt.annotation.TimeoutAction} and {@link com.gh.bmd.jrt.annotation.Pass}, as
 * well as v4 builder annotations defined for each interface method.
 * <p/>
 * Created by davide on 06/05/15.
 *
 * @see com.gh.bmd.jrt.processor.annotation.Proxy
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface V4Proxy {

    /**
     * Constant indicating the generated class name suffix.
     */
    String CLASS_NAME_SUFFIX = "_V4Proxy";

    /**
     * The wrapped class.
     *
     * @return the class.
     */
    Class<?> value();
}
