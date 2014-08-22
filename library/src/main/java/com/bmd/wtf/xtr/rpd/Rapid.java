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
package com.bmd.wtf.xtr.rpd;

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.crr.CurrentGenerator;
import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.flw.Dam;
import com.bmd.wtf.gts.Gate;
import com.bmd.wtf.gts.GateGenerator;
import com.bmd.wtf.spr.Spring;
import com.bmd.wtf.spr.SpringGenerator;

/**
 * This class is meant to provide utility methods for employing rapid classes, which use reflection
 * to simplify the building of the waterfall chain at the expense of a small performance loss.
 * <p/>
 * Even if the use of reflection in Java may slow down code execution
 * (see <a href="http://docs.oracle.com/javase/tutorial/reflect/index.html">The Reflection API</a>),
 * it is also true that this library has been designed to support parallel programming, which
 * usually involve long running tasks (such as network, disk IO, heavy processing, etc.). In such
 * cases the reflection intrinsic overhead will not have any significant impact on the overall
 * performance.
 * <p/>
 * Created by davide on 6/19/14.
 */
public class Rapid {

    /**
     * Avoid direct instantiation.
     */
    private Rapid() {

    }

    /**
     * Creates and returns a current generator which instantiates objects of the specified
     * type through a method taking the specified parameters. A method taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a method annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable method is not found, an exception will be thrown.
     * <p/>
     * Note that a method might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param generator the generator object whose method will be called.
     * @param type      the current type.
     * @param args      the arguments to be passed to the method.
     * @return the newly created current generator.
     */
    public static CurrentGenerator currentGenerator(final Object generator,
            final Class<? extends Current> type, final Object... args) {

        return RapidGenerators.currentGenerator(generator, Classification.ofType(type), args);
    }

    /**
     * Creates and returns a current generator which instantiates objects of the specified
     * type through a constructor taking the specified parameters. A constructor taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a constructor annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable constructor is not found, an exception will be thrown.
     * <p/>
     * Note that a constructor might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param current     the current object to instantiate.
     * @param contextArgs the arguments to be passed to the constructor.
     * @return the newly created current generator.
     */
    public static CurrentGenerator currentGenerator(final Current current,
            final Object... contextArgs) {

        return RapidGenerators.currentGenerator(current.getClass(), contextArgs);
    }

    /**
     * Creates and returns a current generator which instantiates objects of the specified
     * classification through a constructor taking the specified parameters. A constructor taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a constructor annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable constructor is not found, an exception will be thrown.
     * <p/>
     * Note that a constructor might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param classification the current classification.
     * @param contextArgs    the arguments to be passed to the constructor.
     * @return the newly created current generator.
     */
    public static CurrentGenerator currentGenerator(
            final Classification<? extends Current> classification, final Object... contextArgs) {

        return RapidGenerators.currentGenerator(classification.getRawType(), contextArgs);
    }

    /**
     * Creates and returns a current generator which instantiates objects of the specified
     * type through a constructor taking the specified parameters. A constructor taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a constructor annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable constructor is not found, an exception will be thrown.
     * <p/>
     * Note that a constructor might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param type        the current type.
     * @param contextArgs the arguments to be passed to the constructor.
     * @return the newly created current generator.
     */
    public static CurrentGenerator currentGenerator(final Class<? extends Current> type,
            final Object... contextArgs) {

        return RapidGenerators.currentGenerator(type, contextArgs);
    }

    /**
     * Creates and returns a rapid dam wrapping the specified gate.
     *
     * @param dam    the waterfall to wrap.
     * @param <TYPE> the gate type.
     * @return the newly created rapid gate.
     */
    public static <TYPE> RapidDam<TYPE> dam(final Dam<TYPE> dam) {

        return new DefaultRapidDam<TYPE>(dam);
    }

    /**
     * Creates and returns a gate generator which instantiates objects of the specified
     * classification through a method taking the specified parameters. A method taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a method annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable method is not found, an exception will be thrown.
     * <p/>
     * Note that a method might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param generator      the generator object whose method will be called.
     * @param classification the gate classification.
     * @param args           the arguments to be passed to the method.
     * @param <IN>           the input data type.
     * @param <OUT>          the output data type.
     * @return the newly created gate generator.
     */
    public static <IN, OUT> GateGenerator<IN, OUT> gateGenerator(final Object generator,
            final Classification<? extends Gate<IN, OUT>> classification, final Object... args) {

        return RapidGenerators.gateGenerator(generator, classification, args);
    }

    /**
     * Creates and returns a gate generator which instantiates objects of the specified
     * type through a constructor taking the specified parameters. A constructor taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a constructor annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable constructor is not found, an exception will be thrown.
     * <p/>
     * Note that a constructor might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param gate        the gate object to instantiate.
     * @param contextArgs the arguments to be passed to the constructor.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the newly created gate generator.
     */
    public static <IN, OUT> GateGenerator<IN, OUT> gateGenerator(final Gate<IN, OUT> gate,
            final Object... contextArgs) {

        //noinspection unchecked
        return RapidGenerators.gateGenerator((Class<? extends Gate<IN, OUT>>) gate.getClass(),
                                             contextArgs);
    }

    /**
     * Creates and returns a gate generator which instantiates objects of the specified
     * classification through a constructor taking the specified parameters. A constructor taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a constructor annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable constructor is not found, an exception will be thrown.
     * <p/>
     * Note that a constructor might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param classification the gate classification.
     * @param contextArgs    the arguments to be passed to the constructor.
     * @param <IN>           the input data type.
     * @param <OUT>          the output data type.
     * @return the newly created gate generator.
     */
    public static <IN, OUT> GateGenerator<IN, OUT> gateGenerator(
            final Classification<? extends Gate<IN, OUT>> classification,
            final Object... contextArgs) {

        return RapidGenerators.gateGenerator(classification.getRawType(), contextArgs);
    }

    /**
     * Creates and returns a gate generator which instantiates objects of the specified
     * type through a constructor taking the specified parameters. A constructor taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a constructor annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable constructor is not found, an exception will be thrown.
     * <p/>
     * Note that a constructor might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param type        the gate type.
     * @param contextArgs the arguments to be passed to the constructor.
     * @param <IN>        the input data type.
     * @param <OUT>       the output data type.
     * @return the newly created gate generator.
     */
    public static <IN, OUT> GateGenerator<IN, OUT> gateGenerator(
            final Class<? extends Gate<IN, OUT>> type, final Object... contextArgs) {

        return RapidGenerators.gateGenerator(type, contextArgs);
    }

    /**
     * Creates and returns a spring generator which instantiates objects of the specified
     * classification through a method taking the specified parameters. A method taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a method annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable method is not found, an exception will be thrown.
     * <p/>
     * Note that a method might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param generator      the generator object whose method will be called.
     * @param classification the spring classification.
     * @param args           the arguments to be passed to the method.
     * @param <DATA>         the data type.
     * @return the newly created spring generator.
     */
    public static <DATA> SpringGenerator<DATA> springGenerator(final Object generator,
            final Classification<? extends Spring<DATA>> classification, final Object... args) {

        return RapidGenerators.springGenerator(generator, classification, args);
    }

    /**
     * Creates and returns a spring generator which instantiates objects of the specified
     * type through a constructor taking the specified parameters. A constructor taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a constructor annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable constructor is not found, an exception will be thrown.
     * <p/>
     * Note that a constructor might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param spring      the spring object to instantiate.
     * @param contextArgs the arguments to be passed to the constructor.
     * @param <DATA>      the data type.
     * @return the newly created spring generator.
     */
    public static <DATA> SpringGenerator<DATA> springGenerator(final Spring<DATA> spring,
            final Object... contextArgs) {

        //noinspection unchecked
        return RapidGenerators.springGenerator((Class<? extends Spring<DATA>>) spring.getClass(),
                                               contextArgs);
    }

    /**
     * Creates and returns a spring generator which instantiates objects of the specified
     * classification through a constructor taking the specified parameters. A constructor taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a constructor annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable constructor is not found, an exception will be thrown.
     * <p/>
     * Note that a constructor might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param classification the spring classification.
     * @param contextArgs    the arguments to be passed to the constructor.
     * @param <DATA>         the data type.
     * @return the newly created spring generator.
     */
    public static <DATA> SpringGenerator<DATA> springGenerator(
            final Classification<? extends Spring<DATA>> classification,
            final Object... contextArgs) {

        return RapidGenerators.springGenerator(classification.getRawType(), contextArgs);
    }

    /**
     * Creates and returns a spring generator which instantiates objects of the specified
     * type through a constructor taking the specified parameters. A constructor taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a constructor annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable constructor is not found, an exception will be thrown.
     * <p/>
     * Note that a constructor might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     * <p/>
     * <b>Warning:</b> when employing annotation remember to add the proper rules to your Proguard
     * file:
     * <pre>
     *     <code>
     *         -keepattributes RuntimeVisibleAnnotations
     *
     *         -keepclassmembers class ** {
     *              &#64;com.bmd.wtf.xtr.rpd.RapidAnnotations$Generator *;
     *         }
     *     </code>
     * </pre>
     *
     * @param type        the spring type.
     * @param contextArgs the arguments to be passed to the constructor.
     * @param <DATA>      the data type.
     * @return the newly created spring generator.
     */
    public static <DATA> SpringGenerator<DATA> springGenerator(
            final Class<? extends Spring<DATA>> type, final Object... contextArgs) {

        return RapidGenerators.springGenerator(type, contextArgs);
    }
}