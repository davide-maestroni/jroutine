package com.github.dm.jrt.annotation;

import com.github.dm.jrt.builder.InvocationConfiguration;
import com.github.dm.jrt.log.Log.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Through this annotation it is possible to indicate the log level of the wrapping routine.
 * <p/>
 * This annotation is used to decorate methods that are to be invoked in an asynchronous way.<br/>
 * Note that the piece of code inside such methods will be automatically protected so to avoid
 * concurrency issues. Though, other parts of the code inside the same class will be not.<br/>
 * In order to prevent unexpected behaviors, it is advisable to avoid using the same class fields
 * (unless immutable) in protected and non-protected code, or to call synchronous methods through
 * routines as well.
 * <p/>
 * Finally, be aware that a method might need to be made accessible in order to be called. That
 * means that, in case a {@link SecurityManager} is installed, a security exception might
 * be raised based on the specific policy implemented.
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the following rules to your Proguard file (if employing it for shrinking or obfuscation):
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.github.dm.jrt.annotation.LogLevel *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide-maestroni on 11/03/2015.
 *
 * @see InvocationConfiguration InvocationConfiguration
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogLevel {

    /**
     * The routine log level.
     *
     * @return the log level.
     */
    Level value();
}