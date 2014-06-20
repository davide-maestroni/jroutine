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

import com.bmd.wtf.flg.Gate;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.lps.LeapGenerator;
import com.bmd.wtf.xtr.rpd.Rapids.Generator;

import java.lang.reflect.Constructor;

/**
 * Created by davide on 6/19/14.
 */
class RapidGenerators {

    private RapidGenerators() {

    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> generator(
            final Gate<? extends Leap<SOURCE, IN, OUT>> gate, final Object... contextArgs) {

        //noinspection unchecked
        return generator((Class<? extends Leap<SOURCE, IN, OUT>>) gate.getRawType(), contextArgs);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> generator(
            final Leap<SOURCE, IN, OUT>... leaps) {

        final Leap<SOURCE, IN, OUT>[] leapList = leaps.clone();

        return new LeapGenerator<SOURCE, IN, OUT>() {

            @Override
            public Leap<SOURCE, IN, OUT> start(final int fallNumber) {

                return leapList[fallNumber];
            }
        };
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> generator(
            final Class<? extends Leap<SOURCE, IN, OUT>> type, final Object... contextArgs) {

        Constructor<?> bestMatch = findContextConstructor(type.getConstructors(), contextArgs);

        if (bestMatch == null) {

            bestMatch = findContextConstructor(type.getDeclaredConstructors(), contextArgs);

            if (bestMatch == null) {

                throw new IllegalArgumentException(
                        "no suitable constructor found for type " + type);
            }

            if (!bestMatch.isAccessible()) {

                bestMatch.setAccessible(true);
            }
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

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> generator(
            final Class<? extends Leap<SOURCE, IN, OUT>> type) {

        Constructor<?> bestMatch = findConstructor(type.getConstructors());

        if (bestMatch == null) {

            bestMatch = findConstructor(type.getDeclaredConstructors());

            if (bestMatch == null) {

                throw new IllegalArgumentException(
                        "no suitable constructor found for type " + type);
            }

            if (!bestMatch.isAccessible()) {

                bestMatch.setAccessible(true);
            }
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

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> generator(
            final Gate<? extends Leap<SOURCE, IN, OUT>> gate) {

        //noinspection unchecked
        return generator((Class<? extends Leap<SOURCE, IN, OUT>>) gate.getRawType());
    }

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

    private static Constructor<?> findContextConstructor(final Constructor<?>[] constructors,
            final Object[] contextArgs) {

        final int argsLength = contextArgs.length;

        final Class<?>[] contextClasses = new Class[argsLength];

        for (int i = 0; i < argsLength; i++) {

            contextClasses[i] = contextArgs[i].getClass();
        }

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

                        if (!params[i].equals(contextClasses[i])) {

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

                        if (!params[i].equals(contextClasses[i])) {

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
}