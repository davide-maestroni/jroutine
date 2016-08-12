/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Reflection utility class.
 * <p>
 * Created by davide-maestroni on 09/09/2014.
 */
public class Reflection {

    /**
     * Constant defining an empty argument array for methods or constructors.
     */
    public static final Object[] NO_ARGS = new Object[0];

    /**
     * Constant defining an empty parameter type array for method discovery.
     */
    public static final Class<?>[] NO_PARAMS = new Class[0];

    private static final HashMap<Class<?>, Class<?>> sBoxingClasses =
            new HashMap<Class<?>, Class<?>>(9);

    /**
     * Avoid explicit instantiation.
     */
    protected Reflection() {
        ConstantConditions.avoid();
    }

    /**
     * Returns the specified objects as an array of arguments.
     *
     * @param args the argument objects.
     * @return the array.
     */
    @NotNull
    public static Object[] asArgs(@Nullable final Object... args) {
        return (args != null) ? args : Reflection.NO_ARGS;
    }

    /**
     * Returns the class boxing the specified primitive type.
     * <p>
     * If the passed class does not represent a primitive type the same class is returned.
     *
     * @param type the primitive type.
     * @return the boxing class.
     */
    @NotNull
    public static Class<?> boxingClass(@NotNull final Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }

        return sBoxingClasses.get(type);
    }

    /**
     * Returns a clone of the specified array of argument objects.
     * <br>
     * The cloning is safe, that is, if {@code null} is passed, an empty array is returned.
     *
     * @param args the argument objects.
     * @return the array.
     */
    @NotNull
    public static Object[] cloneArgs(@Nullable final Object... args) {
        return (args != null) ? args.clone() : Reflection.NO_ARGS;
    }

    /**
     * Finds the constructor of the specified class best matching the passed arguments.
     * <p>
     * Note that clashing of signature is automatically avoided, since constructors are not
     * identified by their name. Hence the best match will always be unique in the class.
     *
     * @param type   the target class.
     * @param args   the constructor arguments.
     * @param <TYPE> the target type.
     * @return the best matching constructor.
     * @throws IllegalArgumentException if no constructor taking the specified objects as
     *                                  parameters was found.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <TYPE> Constructor<TYPE> findBestMatchingConstructor(
            @NotNull final Class<TYPE> type, @NotNull final Object... args) {
        Constructor<?> constructor = findBestMatchingConstructor(type.getConstructors(), args);
        if (constructor == null) {
            constructor = findBestMatchingConstructor(type.getDeclaredConstructors(), args);
            if (constructor == null) {
                throw new IllegalArgumentException(
                        "no suitable constructor found for type: " + type.getName());
            }
        }

        return (Constructor<TYPE>) makeAccessible(constructor);
    }

    /**
     * Finds the method of the specified class best matching the passed arguments.
     * <p>
     * Note that the method is searched only among the ones explicitly declared by the target class.
     *
     * @param type the target class.
     * @param args the constructor arguments.
     * @return the best matching method.
     * @throws IllegalArgumentException if no method or more than ones, taking the
     *                                  specified objects as parameters, were found.
     */
    @NotNull
    public static Method findBestMatchingMethod(@NotNull final Class<?> type,
            @NotNull final Object... args) {
        final Method[] declaredMethods = type.getDeclaredMethods();
        final ArrayList<Method> publicMethods = new ArrayList<Method>();
        final ArrayList<Method> protectedMethods = new ArrayList<Method>();
        final ArrayList<Method> defaultMethods = new ArrayList<Method>();
        final ArrayList<Method> privateMethods = new ArrayList<Method>();
        for (final Method declaredMethod : declaredMethods) {
            final int modifiers = declaredMethod.getModifiers();
            if (Modifier.isPublic(modifiers)) {
                publicMethods.add(declaredMethod);

            } else if (Modifier.isProtected(modifiers)) {
                protectedMethods.add(declaredMethod);

            } else if (Modifier.isPrivate(modifiers)) {
                privateMethods.add(declaredMethod);

            } else {
                defaultMethods.add(declaredMethod);
            }
        }

        Method method = findBestMatchingMethod(publicMethods, args);
        if (method == null) {
            method = findBestMatchingMethod(protectedMethods, args);
            if (method == null) {
                method = findBestMatchingMethod(defaultMethods, args);
                if (method == null) {
                    method = findBestMatchingMethod(privateMethods, args);
                    if (method == null) {
                        throw new IllegalArgumentException(
                                "no suitable method found for type: " + type.getName());
                    }
                }
            }
        }

        return makeAccessible(method);
    }

    /**
     * Finds the method matching the specified parameters.
     * <p>
     * Note that the returned method may not be accessible.
     *
     * @param type           the target class.
     * @param name           the method name.
     * @param parameterTypes the method parameter types.
     * @return the matching method.
     * @throws IllegalArgumentException if no method matching the specified parameters was
     *                                  found.
     */
    @NotNull
    public static Method findMethod(@NotNull final Class<?> type, @NotNull final String name,
            @NotNull final Class<?>... parameterTypes) {
        Method method;
        try {
            method = type.getMethod(name, parameterTypes);

        } catch (final NoSuchMethodException ignored) {
            try {
                method = type.getDeclaredMethod(name, parameterTypes);

            } catch (final NoSuchMethodException e) {
                throw new IllegalArgumentException(e);
            }
        }

        return method;
    }

    /**
     * Checks if the specified class is static or is a top level class.
     *
     * @param type the class.
     * @return whether the class has a static scope.
     */
    public static boolean hasStaticScope(@NotNull final Class<?> type) {
        return ((type.getEnclosingClass() == null) || Modifier.isStatic(type.getModifiers()));
    }

    /**
     * Checks if the class of the specified instance is static or is a top level class.
     *
     * @param instance the instance.
     * @return whether the instance has a static scope.
     */
    public static boolean hasStaticScope(@NotNull final Object instance) {
        return hasStaticScope(instance.getClass());
    }

    /**
     * Makes the specified constructor accessible.
     *
     * @param constructor the constructor instance.
     * @return the constructor.
     */
    @NotNull
    public static Constructor<?> makeAccessible(@NotNull final Constructor<?> constructor) {
        if (!constructor.isAccessible()) {
            AccessController.doPrivileged(new SetAccessibleConstructorAction(constructor));
        }

        return constructor;
    }

    /**
     * Makes the specified method accessible.
     *
     * @param method the method instance.
     * @return the method.
     */
    @NotNull
    public static Method makeAccessible(@NotNull final Method method) {
        if (!method.isAccessible()) {
            AccessController.doPrivileged(new SetAccessibleMethodAction(method));
        }

        return method;
    }

    /**
     * Creates a new instance of the specified class by invoking its constructor best matching the
     * specified arguments.
     *
     * @param type   the target class.
     * @param args   the constructor arguments.
     * @param <TYPE> the target type.
     * @return the new instance.
     * @throws IllegalArgumentException if no matching constructor was found or an error
     *                                  occurred during the instantiation.
     */
    @NotNull
    public static <TYPE> TYPE newInstanceOf(@NotNull final Class<TYPE> type,
            @NotNull final Object... args) {
        try {
            return findBestMatchingConstructor(type, args).newInstance(args);

        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static int computeConfidence(@NotNull final Class<?>[] params,
            @NotNull final Object[] args) {
        final int length = params.length;
        final int argsLength = args.length;
        if (length != argsLength) {
            return -1;
        }

        int confidence = 0;
        for (int i = 0; i < argsLength; ++i) {
            final Object contextArg = args[i];
            final Class<?> param = params[i];
            if (contextArg != null) {
                final Class<?> boxingClass = boxingClass(param);
                if (!boxingClass.isInstance(contextArg)) {
                    confidence = -1;
                    break;
                }

                if (contextArg.getClass().equals(boxingClass)) {
                    ++confidence;
                }

            } else if (param.isPrimitive()) {
                confidence = -1;
                break;
            }
        }

        return confidence;
    }

    @Nullable
    private static Constructor<?> findBestMatchingConstructor(
            @NotNull final Constructor<?>[] constructors, @NotNull final Object[] args) {
        Constructor<?> bestMatch = null;
        boolean isClash = false;
        int maxConfidence = -1;
        for (final Constructor<?> constructor : constructors) {
            final Class<?>[] params = constructor.getParameterTypes();
            final int confidence = computeConfidence(params, args);
            if (confidence < 0) {
                continue;
            }

            if ((bestMatch == null) || (confidence > maxConfidence)) {
                isClash = false;
                bestMatch = constructor;
                maxConfidence = confidence;

            } else if (confidence == maxConfidence) {
                isClash = true;
            }
        }

        if (isClash) {
            throw new IllegalArgumentException(
                    "more than one constructor found for arguments: " + Arrays.toString(args));
        }

        return bestMatch;
    }

    @Nullable
    private static Method findBestMatchingMethod(@NotNull final ArrayList<Method> methods,
            @NotNull final Object[] args) {
        Method bestMatch = null;
        boolean isClash = false;
        int maxConfidence = -1;
        for (final Method method : methods) {
            final Class<?>[] params = method.getParameterTypes();
            final int confidence = computeConfidence(params, args);
            if (confidence < 0) {
                continue;
            }

            if ((bestMatch == null) || (confidence > maxConfidence)) {
                isClash = false;
                bestMatch = method;
                maxConfidence = confidence;

            } else if (confidence == maxConfidence) {
                isClash = true;
            }
        }

        if (isClash) {
            throw new IllegalArgumentException(
                    "more than one method found for arguments: " + Arrays.toString(args));
        }

        return bestMatch;
    }

    /**
     * Privileged action used to grant accessibility to a constructor.
     */
    private static class SetAccessibleConstructorAction implements PrivilegedAction<Void> {

        private final Constructor<?> mmConstructor;

        /**
         * Constructor.
         *
         * @param constructor the constructor instance.
         */
        private SetAccessibleConstructorAction(@NotNull final Constructor<?> constructor) {
            mmConstructor = constructor;
        }

        public Void run() {
            mmConstructor.setAccessible(true);
            return null;
        }
    }

    /**
     * Privileged action used to grant accessibility to a method.
     */
    private static class SetAccessibleMethodAction implements PrivilegedAction<Void> {

        private final Method mMethod;

        /**
         * Constructor.
         *
         * @param method the method instance.
         */
        private SetAccessibleMethodAction(@NotNull final Method method) {
            mMethod = method;
        }

        public Void run() {
            mMethod.setAccessible(true);
            return null;
        }
    }

    static {
        final HashMap<Class<?>, Class<?>> boxMap = sBoxingClasses;
        boxMap.put(boolean.class, Boolean.class);
        boxMap.put(byte.class, Byte.class);
        boxMap.put(char.class, Character.class);
        boxMap.put(double.class, Double.class);
        boxMap.put(float.class, Float.class);
        boxMap.put(int.class, Integer.class);
        boxMap.put(long.class, Long.class);
        boxMap.put(short.class, Short.class);
        boxMap.put(void.class, Void.class);
    }
}
