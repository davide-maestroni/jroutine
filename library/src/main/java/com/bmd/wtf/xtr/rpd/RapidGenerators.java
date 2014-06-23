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
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.lps.LeapGenerator;
import com.bmd.wtf.xtr.rpd.Rapids.Generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by davide on 6/19/14.
 */
class RapidGenerators {

    private RapidGenerators() {

    }

    public static CurrentGenerator currentGenerator(final Current... currents) {

        final Current[] currentList = currents.clone();

        return new CurrentGenerator() {

            @Override
            public Current create(final int fallNumber) {

                return currentList[fallNumber];
            }
        };
    }

    public static CurrentGenerator currentGenerator(
            final Classification<? extends Current> classification, final Object... contextArgs) {

        //noinspection unchecked
        return currentGenerator((Class<? extends Current>) classification.getRawType(),
                                contextArgs);
    }

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

                    } catch (final Throwable t) {

                        throw new RuntimeException(t);
                    }
                }
            };
        }

        return new CurrentGenerator() {

            @Override
            public Current create(final int fallNumber) {

                try {

                    return (Current) constructor.newInstance(contextArgs);

                } catch (final Throwable t) {

                    throw new RuntimeException(t);
                }
            }
        };
    }

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

                    } catch (final Throwable t) {

                        throw new RuntimeException(t);
                    }
                }
            };
        }

        return new CurrentGenerator() {

            @Override
            public Current create(final int fallNumber) {

                try {

                    return (Current) method.invoke(generator, args);

                } catch (final Throwable t) {

                    throw new RuntimeException(t);
                }
            }
        };
    }

    public static CurrentGenerator currentGenerator(final Class<? extends Current> type) {

        Constructor<?> bestMatch = findConstructor(type.getConstructors());

        if (bestMatch == null) {

            bestMatch = findConstructor(type.getDeclaredConstructors());

            if (bestMatch == null) {

                throw new IllegalArgumentException(
                        "no suitable constructor found for type " + type);
            }
        }

        if (!bestMatch.isAccessible()) {

            bestMatch.setAccessible(true);
        }

        final Constructor<?> constructor = bestMatch;

        if (constructor.getParameterTypes().length > 0) {

            return new CurrentGenerator() {

                @Override
                public Current create(final int fallNumber) {

                    try {

                        return (Current) constructor.newInstance(fallNumber);

                    } catch (final Throwable t) {

                        throw new RuntimeException(t);
                    }
                }
            };
        }

        return new CurrentGenerator() {

            @Override
            public Current create(final int fallNumber) {

                try {

                    return (Current) constructor.newInstance();

                } catch (final Throwable t) {

                    throw new RuntimeException(t);
                }
            }
        };
    }

    public static CurrentGenerator currentGenerator(
            final Classification<? extends Current> classification) {

        //noinspection unchecked
        return currentGenerator((Class<? extends Current>) classification.getRawType());
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Classification<? extends Leap<SOURCE, IN, OUT>> classification,
            final Object... contextArgs) {

        //noinspection unchecked
        return leapGenerator((Class<? extends Leap<SOURCE, IN, OUT>>) classification.getRawType(),
                             contextArgs);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Leap<SOURCE, IN, OUT>... leaps) {

        final Leap<SOURCE, IN, OUT>[] leapList = leaps.clone();

        return new LeapGenerator<SOURCE, IN, OUT>() {

            @Override
            public Leap<SOURCE, IN, OUT> start(final int fallNumber) {

                return leapList[fallNumber];
            }
        };
    }

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

                    } catch (final Throwable t) {

                        throw new RuntimeException(t);
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

                } catch (final Throwable t) {

                    throw new RuntimeException(t);
                }
            }
        };
    }

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

                    } catch (final Throwable t) {

                        throw new RuntimeException(t);
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

                } catch (final Throwable t) {

                    throw new RuntimeException(t);
                }
            }
        };
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Class<? extends Leap<SOURCE, IN, OUT>> type) {

        Constructor<?> bestMatch = findConstructor(type.getConstructors());

        if (bestMatch == null) {

            bestMatch = findConstructor(type.getDeclaredConstructors());

            if (bestMatch == null) {

                throw new IllegalArgumentException(
                        "no suitable constructor found for type " + type);
            }
        }

        if (!bestMatch.isAccessible()) {

            bestMatch.setAccessible(true);
        }

        final Constructor<?> constructor = bestMatch;

        if (constructor.getParameterTypes().length > 0) {

            return new LeapGenerator<SOURCE, IN, OUT>() {

                @Override
                public Leap<SOURCE, IN, OUT> start(final int fallNumber) {

                    try {

                        //noinspection unchecked
                        return (Leap<SOURCE, IN, OUT>) constructor.newInstance(fallNumber);

                    } catch (final Throwable t) {

                        throw new RuntimeException(t);
                    }
                }
            };
        }

        return new LeapGenerator<SOURCE, IN, OUT>() {

            @Override
            public Leap<SOURCE, IN, OUT> start(final int fallNumber) {

                try {

                    //noinspection unchecked
                    return (Leap<SOURCE, IN, OUT>) constructor.newInstance();

                } catch (final Throwable t) {

                    throw new RuntimeException(t);
                }
            }
        };
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Classification<? extends Leap<SOURCE, IN, OUT>> classification) {

        //noinspection unchecked
        return leapGenerator((Class<? extends Leap<SOURCE, IN, OUT>>) classification.getRawType());
    }

    @SuppressWarnings("ConstantConditions")
    private static Constructor<?> findConstructor(final Constructor<?>[] constructors) {

        Constructor<?> annotatedIntConstructor = null;
        Constructor<?> annotatedIntegerConstructor = null;
        Constructor<?> annotatedDefaultConstructor = null;

        Constructor<?> intConstructor = null;
        Constructor<?> integerConstructor = null;
        Constructor<?> defaultConstructor = null;

        for (final Constructor<?> constructor : constructors) {

            final Class<?>[] params = constructor.getParameterTypes();

            if (constructor.isAnnotationPresent(Generator.class)) {

                boolean isValid = true;

                final int length = params.length;

                if (length == 0) {

                    annotatedDefaultConstructor = constructor;

                } else if (length == 1) {

                    final Class<?> param = params[0];

                    if (param.equals(int.class)) {

                        annotatedIntConstructor = constructor;

                    } else if (param.equals(Integer.class)) {

                        annotatedIntegerConstructor = constructor;

                    } else {

                        isValid = false;
                    }

                } else {

                    isValid = false;
                }

                if (!isValid) {

                    throw new IllegalArgumentException("annotated constructor is not valid");
                }

            } else {

                final int length = params.length;

                if (length == 0) {

                    defaultConstructor = constructor;

                } else if (length == 1) {

                    final Class<?> param = params[0];

                    if (param.equals(int.class)) {

                        intConstructor = constructor;

                    } else if (param.equals(Integer.class)) {

                        integerConstructor = constructor;
                    }
                }
            }
        }

        if (annotatedIntConstructor != null) {

            return annotatedIntConstructor;

        } else if (annotatedIntegerConstructor != null) {

            return annotatedIntegerConstructor;

        } else if (annotatedDefaultConstructor != null) {

            return annotatedDefaultConstructor;

        } else if (intConstructor != null) {

            return intConstructor;

        } else if (integerConstructor != null) {

            return integerConstructor;
        }

        return defaultConstructor;
    }

    @SuppressWarnings("ConstantConditions")
    private static Constructor<?> findContextConstructor(final Constructor<?>[] constructors,
            final Object[] contextArgs) {

        final int argsLength = contextArgs.length;

        Constructor<?> annotatedIntConstructor = null;
        Constructor<?> annotatedIntegerConstructor = null;
        Constructor<?> annotatedDefaultConstructor = null;

        Constructor<?> intConstructor = null;
        Constructor<?> integerConstructor = null;
        Constructor<?> defaultConstructor = null;

        for (final Constructor<?> constructor : constructors) {

            final Class<?>[] params = constructor.getParameterTypes();

            if (constructor.isAnnotationPresent(Generator.class)) {

                final int length = params.length;

                boolean isValid = (length >= argsLength);

                if (isValid) {

                    for (int i = 0; i < argsLength; i++) {

                        final Object contextArg = contextArgs[i];
                        final Class<?> param = params[i];

                        if ((contextArg != null) ? !param.isInstance(contextArg)
                                : param.isPrimitive()) {

                            isValid = false;

                            break;
                        }
                    }
                }

                if (isValid) {

                    if (length == argsLength) {

                        annotatedDefaultConstructor = constructor;

                    } else if (length == (argsLength + 1)) {

                        final Class<?> param = params[argsLength];

                        if (param.equals(int.class)) {

                            annotatedIntConstructor = constructor;

                        } else if (param.equals(Integer.class)) {

                            annotatedIntegerConstructor = constructor;

                        } else {

                            isValid = false;
                        }

                    } else {

                        isValid = false;
                    }
                }

                if (!isValid) {

                    throw new IllegalArgumentException("annotated constructor is not valid");
                }

            } else {

                final int length = params.length;

                boolean isValid = (length >= argsLength);

                if (isValid) {

                    for (int i = 0; i < argsLength; i++) {

                        final Object contextArg = contextArgs[i];
                        final Class<?> param = params[i];

                        if ((contextArg != null) ? !param.isInstance(contextArg)
                                : param.isPrimitive()) {

                            isValid = false;

                            break;
                        }
                    }
                }

                if (!isValid) {

                    continue;
                }

                if (length == argsLength) {

                    defaultConstructor = constructor;

                } else if (length == (argsLength + 1)) {

                    final Class<?> param = params[argsLength];

                    if (param.equals(int.class)) {

                        intConstructor = constructor;

                    } else if (param.equals(Integer.class)) {

                        integerConstructor = constructor;
                    }
                }
            }
        }

        if (annotatedIntConstructor != null) {

            return annotatedIntConstructor;

        } else if (annotatedIntegerConstructor != null) {

            return annotatedIntegerConstructor;

        } else if (annotatedDefaultConstructor != null) {

            return annotatedDefaultConstructor;

        } else if (intConstructor != null) {

            return intConstructor;

        } else if (integerConstructor != null) {

            return integerConstructor;
        }

        return defaultConstructor;
    }

    @SuppressWarnings("ConstantConditions")
    private static Method findMethod(final Method[] methods, final Class<?> resultType,
            final Object[] args) {

        final int argsLength = args.length;

        final Class<?>[] argClasses = new Class[argsLength];

        for (int i = 0; i < argsLength; i++) {

            argClasses[i] = args[i].getClass();
        }

        Class<?> annotatedBestMatch = null;
        Class<?> bestMatch = null;

        for (final Method method : methods) {

            final Class<?> returnType = method.getReturnType();

            if (!resultType.isAssignableFrom(returnType)) {

                continue;
            }

            final Class<?>[] params = method.getParameterTypes();

            if (method.isAnnotationPresent(Generator.class)) {

                final int length = params.length;

                boolean isValid = (length >= argsLength);

                if (isValid) {

                    for (int i = 0; i < argsLength; i++) {

                        if (!params[i].equals(argClasses[i])) {

                            isValid = false;

                            break;
                        }
                    }
                }

                if (isValid) {

                    if (length > (argsLength + 1)) {

                        isValid = false;

                    } else if (length == (argsLength + 1)) {

                        final Class<?> param = params[argsLength];

                        if (!param.equals(int.class) && !param.equals(Integer.class)) {

                            isValid = false;
                        }
                    }

                    if (isValid) {

                        if ((annotatedBestMatch == null) || returnType
                                .isAssignableFrom(annotatedBestMatch)) {

                            annotatedBestMatch = returnType;
                        }
                    }
                }

            } else {

                final int length = params.length;

                boolean isValid = (length >= argsLength);

                if (isValid) {

                    for (int i = 0; i < argsLength; i++) {

                        if (!params[i].equals(argClasses[i])) {

                            isValid = false;

                            break;
                        }
                    }
                }

                if (isValid) {

                    if (length > (argsLength + 1)) {

                        isValid = false;

                    } else if (length == (argsLength + 1)) {

                        final Class<?> param = params[argsLength];

                        if (!param.equals(int.class) && !param.equals(Integer.class)) {

                            isValid = false;
                        }
                    }

                    if (isValid) {

                        if ((bestMatch == null) || returnType.isAssignableFrom(bestMatch)) {

                            bestMatch = returnType;
                        }
                    }
                }
            }
        }

        final Class<?> methodReturnType;

        if ((annotatedBestMatch != null) && ((bestMatch == null) || annotatedBestMatch
                .isAssignableFrom(bestMatch))) {

            methodReturnType = annotatedBestMatch;

        } else if (bestMatch != null) {

            methodReturnType = bestMatch;

        } else {

            return null;
        }

        Method annotatedIntMethod = null;
        Method annotatedIntegerMethod = null;
        Method annotatedDefaultMethod = null;

        Method intMethod = null;
        Method integerMethod = null;
        Method defaultMethod = null;

        for (final Method method : methods) {

            if (!methodReturnType.equals(method.getReturnType())) {

                continue;
            }

            final Class<?>[] params = method.getParameterTypes();

            if (method.isAnnotationPresent(Generator.class)) {

                final int length = params.length;

                boolean isValid = (length >= argsLength);

                if (isValid) {

                    for (int i = 0; i < argsLength; i++) {

                        if (!params[i].equals(argClasses[i])) {

                            isValid = false;

                            break;
                        }
                    }
                }

                if (isValid) {

                    if (length == argsLength) {

                        annotatedDefaultMethod = method;

                    } else if (length == (argsLength + 1)) {

                        final Class<?> param = params[argsLength];

                        if (param.equals(int.class)) {

                            annotatedIntMethod = method;

                        } else if (param.equals(Integer.class)) {

                            annotatedIntegerMethod = method;

                        } else {

                            isValid = false;
                        }

                    } else {

                        isValid = false;
                    }
                }

                if (!isValid) {

                    throw new IllegalArgumentException("annotated constructor is not valid");
                }

            } else {

                final int length = params.length;

                boolean isValid = (length >= argsLength);

                if (isValid) {

                    for (int i = 0; i < argsLength; i++) {

                        if (!params[i].equals(argClasses[i])) {

                            isValid = false;

                            break;
                        }
                    }
                }

                if (!isValid) {

                    continue;
                }

                if (length == argsLength) {

                    defaultMethod = method;

                } else if (length == (argsLength + 1)) {

                    final Class<?> param = params[argsLength];

                    if (param.equals(int.class)) {

                        intMethod = method;

                    } else if (param.equals(Integer.class)) {

                        integerMethod = method;
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