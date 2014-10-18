package com.bmd.jrt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate methods whose output is passed asynchronously.
 * <p/>
 * The only use case in which this annotation is useful, is when an interface is used as a mirror
 * of another class methods. The interface can return the result as output channels whose output
 * will be passed only when available.
 * <p/>
 * For example, a method returning an integer:
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
 *         &#64;AsyncResult
 *         public OutputChannel&lt;Integer&gt; sum(int i1, int i2);
 *     </code>
 * </pre>
 * <p/>
 * Remember also that, in order for the annotation to properly work at run time, you will need to
 * add the proper rules to your Proguard file if employing it for shrinking or obfuscation:
 * <pre>
 *     <code>
 *
 *         -keepattributes RuntimeVisibleAnnotations
 *
 *         -keepclassmembers class ** {
 *              &#64;com.bmd.jrt.annotation.AsyncResult *;
 *         }
 *     </code>
 * </pre>
 * <p/>
 * Created by davide on 10/1/14.
 *
 * @see Async
 * @see AsyncParameters
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncResult {

}