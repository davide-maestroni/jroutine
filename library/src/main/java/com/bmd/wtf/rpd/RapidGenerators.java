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
package com.bmd.wtf.rpd;

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.crr.CurrentGenerator;
import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.lps.LeapGenerator;
import com.bmd.wtf.rpd.RapidAnnotations.Generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility class providing a factory of current generators.
 * <p/>
 * Created by davide on 6/19/14.
 */
class RapidGenerators {

    /**
     * Avoid direct instantiation.
     */
    private RapidGenerators() {

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
     *
     * @param type        The current type.
     * @param contextArgs The arguments to be passed to the constructor.
     * @return The newly created current generator.
     */
    public static CurrentGenerator currentGenerator(final Class<? extends Current> type,
            final Object... contextArgs) {

        Constructor<?> bestMatch = findContextConstructor(type.getConstructors(), contextArgs);

        if (bestMatch == null) {

            bestMatch = findContextConstructor(type.getDeclaredConstructors(), contextArgs);

            if (bestMatch == null) {

                throw new IllegalArgumentException(
                        "no suitable constructor found for type " + type);
            }
        }

        if (!bestMatch.isAccessible()) {

            bestMatch.setAccessible(true);
        }

        final Constructor<?> constructor = bestMatch;
        final int length = constructor.getParameterTypes().length;

        if (length > contextArgs.length) {

            return new CurrentGenerator() {

                @Override
                public Current create(final int fallNumber) {

                    try {

                        final Object[] args = new Object[length];

                        System.arraycopy(contextArgs, 0, args, 0, length - 1);

                        args[length - 1] = fallNumber;

                        return (Current) constructor.newInstance(args);

                    } catch (final InstantiationException e) {

                        throw new RapidException(e);

                    } catch (final InvocationTargetException e) {

                        throw new RapidException(e.getCause());

                    } catch (final IllegalAccessException e) {

                        throw new RapidException(e);
                    }
                }
            };
        }

        return new CurrentGenerator() {

            @Override
            public Current create(final int fallNumber) {

                try {

                    return (Current) constructor.newInstance(contextArgs);

                } catch (final InstantiationException e) {

                    throw new RapidException(e);

                } catch (final InvocationTargetException e) {

                    throw new RapidException(e.getCause());

                } catch (final IllegalAccessException e) {

                    throw new RapidException(e);
                }
            }
        };
    }

    /**
     * Creates and returns a current generator which instantiates objects of the specified
     * classification through a method taking the specified parameters. A method taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a method annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable method is not found, an exception will be thrown.
     * <p/>
     * Note that a method might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     *
     * @param generator      The generator object whose method will be called.
     * @param classification The current classification.
     * @param args           The arguments to be passed to the method.
     * @return The newly created current generator.
     */
    public static CurrentGenerator currentGenerator(final Object generator,
            final Classification<? extends Current> classification, final Object... args) {

        final Class<?> type = classification.getRawType();

        Method bestMatch = findMethod(generator.getClass().getMethods(), type, args);

        if (bestMatch == null) {

            bestMatch = findMethod(generator.getClass().getDeclaredMethods(), type, args);

            if (bestMatch == null) {

                throw new IllegalArgumentException("no suitable method found for type " + type);
            }
        }

        if (!bestMatch.isAccessible()) {

            bestMatch.setAccessible(true);
        }

        final Method method = bestMatch;
        final int length = method.getParameterTypes().length;

        if (length > args.length) {

            return new CurrentGenerator() {

                @Override
                public Current create(final int fallNumber) {

                    try {

                        final Object[] args = new Object[length];

                        System.arraycopy(args, 0, args, 0, length - 1);

                        args[length - 1] = fallNumber;

                        return (Current) method.invoke(generator, args);

                    } catch (final InvocationTargetException e) {

                        throw new RapidException(e.getCause());

                    } catch (final IllegalAccessException e) {

                        throw new RapidException(e);
                    }
                }
            };
        }

        return new CurrentGenerator() {

            @Override
            public Current create(final int fallNumber) {

                try {

                    return (Current) method.invoke(generator, args);

                } catch (final InvocationTargetException e) {

                    throw new RapidException(e.getCause());

                } catch (final IllegalAccessException e) {

                    throw new RapidException(e);
                }
            }
        };
    }

    /**
     * Creates and returns a leap generator which instantiates objects of the specified
     * classification through a method taking the specified parameters. A method taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a method annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable method is not found, an exception will be thrown.
     * <p/>
     * Note that a method might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     *
     * @param generator      The generator object whose method will be called.
     * @param classification The leap classification.
     * @param args           The arguments to be passed to the method.
     * @return The newly created leap generator.
     */
    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Object generator,
            final Classification<? extends Leap<SOURCE, IN, OUT>> classification,
            final Object... args) {

        final Class<?> type = classification.getRawType();

        Method bestMatch = findMethod(generator.getClass().getMethods(), type, args);

        if (bestMatch == null) {

            bestMatch = findMethod(generator.getClass().getDeclaredMethods(), type, args);

            if (bestMatch == null) {

                throw new IllegalArgumentException("no suitable method found for type " + type);
            }
        }

        if (!bestMatch.isAccessible()) {

            bestMatch.setAccessible(true);
        }

        final Method method = bestMatch;
        final int length = method.getParameterTypes().length;

        if (length > args.length) {

            return new LeapGenerator<SOURCE, IN, OUT>() {

                @Override
                public Leap<SOURCE, IN, OUT> start(final int fallNumber) {

                    try {

                        final Object[] args = new Object[length];

                        System.arraycopy(args, 0, args, 0, length - 1);

                        args[length - 1] = fallNumber;

                        //noinspection unchecked
                        return (Leap<SOURCE, IN, OUT>) method.invoke(generator, args);

                    } catch (final InvocationTargetException e) {

                        throw new RapidException(e.getCause());

                    } catch (final IllegalAccessException e) {

                        throw new RapidException(e);
                    }
                }
            };
        }

        return new LeapGenerator<SOURCE, IN, OUT>() {

            @Override
            public Leap<SOURCE, IN, OUT> start(final int fallNumber) {

                try {

                    //noinspection unchecked
                    return (Leap<SOURCE, IN, OUT>) method.invoke(generator, args);

                } catch (final InvocationTargetException e) {

                    throw new RapidException(e.getCause());

                } catch (final IllegalAccessException e) {

                    throw new RapidException(e);
                }
            }
        };
    }

    /**
     * Creates and returns a leap generator which instantiates objects of the specified
     * type through a constructor taking the specified parameters. A constructor taking
     * an additional Integer parameter (that is, the fall number) is preferred to the default one.
     * A one taking a primitive int is preferred to the Integer. Finally, a constructor annotated
     * with {@link RapidAnnotations.Generator} is preferred to the not annotated ones.<br/>
     * In case a suitable constructor is not found, an exception will be thrown.
     * <p/>
     * Note that a constructor might need to be made accessible in order to be called via
     * reflection. That means that, in case a {@link java.lang.SecurityManager} is installed, a
     * security exception might be raised based on the specific policy implemented.
     *
     * @param type        The leap type.
     * @param contextArgs The arguments to be passed to the constructor.
     * @return The newly created leap generator.
     */
    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Class<? extends Leap<SOURCE, IN, OUT>> type, final Object... contextArgs) {

        Constructor<?> bestMatch = findContextConstructor(type.getConstructors(), contextArgs);

        if (bestMatch == null) {

            bestMatch = findContextConstructor(type.getDeclaredConstructors(), contextArgs);

            if (bestMatch == null) {

                throw new IllegalArgumentException(
                        "no suitable constructor found for type " + type);
            }
        }

        if (!bestMatch.isAccessible()) {

            bestMatch.setAccessible(true);
        }

        final Constructor<?> constructor = bestMatch;
        final int length = constructor.getParameterTypes().length;

        if (length > contextArgs.length) {

            return new LeapGenerator<SOURCE, IN, OUT>() {

                @Override
                public Leap<SOURCE, IN, OUT> start(final int fallNumber) {

                    try {

                        final Object[] args = new Object[length];

                        System.arraycopy(contextArgs, 0, args, 0, length - 1);

                        args[length - 1] = fallNumber;

                        //noinspection unchecked
                        return (Leap<SOURCE, IN, OUT>) constructor.newInstance(args);

                    } catch (final InstantiationException e) {

                        throw new RapidException(e);

                    } catch (final InvocationTargetException e) {

                        throw new RapidException(e.getCause());

                    } catch (final IllegalAccessException e) {

                        throw new RapidException(e);
                    }
                }
            };
        }

        return new LeapGenerator<SOURCE, IN, OUT>() {

            @Override
            public Leap<SOURCE, IN, OUT> start(final int fallNumber) {

                try {

                    //noinspection unchecked
                    return (Leap<SOURCE, IN, OUT>) constructor.newInstance(contextArgs);

                } catch (final InstantiationException e) {

                    throw new RapidException(e);

                } catch (final InvocationTargetException e) {

                    throw new RapidException(e.getCause());

                } catch (final IllegalAccessException e) {

                    throw new RapidException(e);
                }
            }
        };
    }

    @SuppressWarnings("ConstantConditions")
    private static Constructor<?> findContextConstructor(final Constructor<?>[] constructors,
            final Object[] contextArgs) {

        final int argsLength = contextArgs.length;

        Constructor<?> annotatedIntCtor = null;
        Constructor<?> annotatedIntegerCtor = null;
        Constructor<?> annotatedDefaultCtor = null;

        int annotatedIntCtorConfidence = 0;
        int annotatedIntegerCtorConfidence = 0;
        int annotatedDefaultCtorConfidence = 0;

        Constructor<?> intCtor = null;
        Constructor<?> integerCtor = null;
        Constructor<?> defaultCtor = null;

        int intCtorConfidence = 0;
        int integerCtorConfidence = 0;
        int defaultCtorConfidence = 0;

        for (final Constructor<?> constructor : constructors) {

            final Class<?>[] params = constructor.getParameterTypes();
            final int length = params.length;

            int confidence = 0;

            boolean isValid = (length >= argsLength);

            if (!isValid) {

                continue;
            }

            for (int i = 0; i < argsLength; ++i) {

                final Object contextArg = contextArgs[i];
                final Class<?> param = params[i];

                if (contextArg != null) {

                    final Class<?> boxedClass = Rapids.boxedClass(param);

                    if (!boxedClass.isInstance(contextArg)) {

                        isValid = false;

                        break;
                    }

                    if (boxedClass.equals(contextArg.getClass())) {

                        ++confidence;
                    }

                } else if (param.isPrimitive()) {

                    isValid = false;

                    break;
                }
            }

            if (!isValid) {

                continue;
            }

            final boolean isAnnotated = constructor.isAnnotationPresent(Generator.class);

            if (length == argsLength) {

                if (isAnnotated) {

                    if ((annotatedDefaultCtor == null) || (confidence
                            > annotatedDefaultCtorConfidence)) {

                        annotatedDefaultCtor = constructor;
                        annotatedDefaultCtorConfidence = confidence;
                    }

                } else {

                    if ((defaultCtor == null) || (confidence > defaultCtorConfidence)) {

                        defaultCtor = constructor;
                        defaultCtorConfidence = confidence;
                    }
                }

            } else if (length == (argsLength + 1)) {

                final Class<?> param = params[argsLength];

                if (param.equals(int.class)) {

                    if (isAnnotated) {

                        if ((annotatedIntCtor == null) || (confidence
                                > annotatedIntCtorConfidence)) {

                            annotatedIntCtor = constructor;
                            annotatedIntCtorConfidence = confidence;
                        }

                    } else {

                        if ((intCtor == null) || (confidence > intCtorConfidence)) {

                            intCtor = constructor;
                            intCtorConfidence = confidence;
                        }
                    }

                } else if (param.equals(Integer.class)) {

                    if (isAnnotated) {

                        if ((annotatedIntegerCtor == null) || (confidence
                                > annotatedIntegerCtorConfidence)) {

                            annotatedIntegerCtor = constructor;
                            annotatedIntegerCtorConfidence = confidence;
                        }

                    } else {

                        if ((integerCtor == null) || (confidence > integerCtorConfidence)) {

                            integerCtor = constructor;
                            integerCtorConfidence = confidence;
                        }
                    }
                }
            }
        }

        if (annotatedIntCtor != null) {

            return annotatedIntCtor;

        } else if (annotatedIntegerCtor != null) {

            return annotatedIntegerCtor;

        } else if (annotatedDefaultCtor != null) {

            return annotatedDefaultCtor;

        } else if (intCtor != null) {

            return intCtor;

        } else if (integerCtor != null) {

            return integerCtor;
        }

        return defaultCtor;
    }

    @SuppressWarnings("ConstantConditions")
    private static Method findMethod(final Method[] methods, final Class<?> resultType,
            final Object[] args) {

        final int argsLength = args.length;

        Method annotatedIntMethod = null;
        Method annotatedIntegerMethod = null;
        Method annotatedDefaultMethod = null;

        int annotatedIntMethodConfidence = 0;
        int annotatedIntegerMethodConfidence = 0;
        int annotatedDefaultMethodConfidence = 0;

        Method intMethod = null;
        Method integerMethod = null;
        Method defaultMethod = null;

        int intMethodConfidence = 0;
        int integerMethodConfidence = 0;
        int defaultMethodConfidence = 0;

        for (final Method method : methods) {

            final Class<?> returnType = method.getReturnType();

            if (!resultType.isAssignableFrom(returnType)) {

                continue;
            }

            final Class<?>[] params = method.getParameterTypes();
            final int length = params.length;

            int confidence = 0;

            boolean isValid = (length >= argsLength);

            if (!isValid) {

                continue;
            }

            for (int i = 0; i < argsLength; ++i) {

                final Object contextArg = args[i];
                final Class<?> param = params[i];

                if (contextArg != null) {

                    final Class<?> boxedClass = Rapids.boxedClass(param);

                    if (!boxedClass.isInstance(contextArg)) {

                        isValid = false;

                        break;
                    }

                    if (boxedClass.equals(contextArg.getClass())) {

                        ++confidence;
                    }

                } else if (param.isPrimitive()) {

                    isValid = false;

                    break;
                }
            }

            if (!isValid) {

                continue;
            }

            final boolean isAnnotated = method.isAnnotationPresent(Generator.class);

            if (length == argsLength) {

                if (isAnnotated) {

                    if ((annotatedDefaultMethod == null) || (confidence
                            > annotatedDefaultMethodConfidence)) {

                        annotatedDefaultMethod = method;
                        annotatedDefaultMethodConfidence = confidence;
                    }

                } else {

                    if ((defaultMethod == null) || (confidence > defaultMethodConfidence)) {

                        defaultMethod = method;
                        defaultMethodConfidence = confidence;
                    }
                }

            } else if (length == (argsLength + 1)) {

                final Class<?> param = params[argsLength];

                if (param.equals(int.class)) {

                    if (isAnnotated) {

                        if ((annotatedIntMethod == null) || (confidence
                                > annotatedIntMethodConfidence)) {

                            annotatedIntMethod = method;
                            annotatedIntMethodConfidence = confidence;
                        }

                    } else {

                        if ((intMethod == null) || (confidence > intMethodConfidence)) {

                            intMethod = method;
                            intMethodConfidence = confidence;
                        }
                    }

                } else if (param.equals(Integer.class)) {

                    if (isAnnotated) {

                        if ((annotatedIntegerMethod == null) || (confidence
                                > annotatedIntegerMethodConfidence)) {

                            annotatedIntegerMethod = method;
                            annotatedIntegerMethodConfidence = confidence;
                        }

                    } else {

                        if ((integerMethod == null) || (confidence > integerMethodConfidence)) {

                            integerMethod = method;
                            integerMethodConfidence = confidence;
                        }
                    }
                }
            }
        }

        if (annotatedIntMethod != null) {

            return annotatedIntMethod;

        } else if (annotatedIntegerMethod != null) {

            return annotatedIntegerMethod;

        } else if (annotatedDefaultMethod != null) {

            return annotatedDefaultMethod;

        } else if (intMethod != null) {

            return intMethod;

        } else if (integerMethod != null) {

            return integerMethod;
        }

        return defaultMethod;
    }
}