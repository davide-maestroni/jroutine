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
 * This annotation is used to customize methods that are to be invoked in an asynchronous way.
 * <p/>
 * Note that the piece of code inside such methods will be automatically protected so to avoid
 * concurrency issues. Though, other parts of the code inside the same class will be not.<br/>
 * In order to avoid unexpected behavior, it is advisable to avoid using the same class fields
 * (unless immutable) in protected and non-protected code, or to call synchronous methods through
 * the framework as well.<br/>
 * Through this annotation, it is possible to exclude single methods from this kind of protection by
 * indicating them as having a different share group. Each group has a name associated, and every
 * method within a specific group is protected so that shared class members can be safely accessed
 * only from the other methods sharing the same group name. That means that the invocation of
 * methods within the same group cannot happen in parallel. In a dual way, methods belonging to
 * different groups can be invoked in parallel but should not access the same members to avoid
 * concurrency issues.
 * <p/>
 * Finally, be aware that a method might need to be made accessible in order to be called. That
 * means that, in case a {@link java.lang.SecurityManager} is installed, a security exception might
 * be raised based on the specific policy implemented.
 * <p/>
 * When used to annotate a class instead of a method, its attributes are applied to all the class
 * methods unless specific values are set in the method annotation.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.bmd.jrt.annotation.Share *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 1/26/15.
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Share {

    /**
     * Constant indicating the default share group, that is, all member fields are protected.
     */
    static final String ALL = Share.class.getCanonicalName() + ".ALL";

    /**
     * Constant indicating that no member field needs to be protected.
     */
    static final String NONE = "";

    /**
     * The share group name associated with the annotated method.
     *
     * @return the group name.
     */
    String value();
}
