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

import com.bmd.wtf.flw.Gate.ConditionEvaluator;
import com.bmd.wtf.rpd.RapidAnnotations.Condition;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Condition evaluator implementation which calls via reflection the gate method matching with the
 * specified arguments.
 * <p/>
 * A suitable method is identified among the ones taking the specified arguments and returning
 * a boolean value. The methods annotated with {@link RapidAnnotations.Condition} are analyzed
 * first.<br/>
 * If more than one method matching the above requirements is found, an exception will be
 * thrown.
 * <p/>
 * Created by davide on 7/11/14.
 *
 * @param <TYPE> The gate type.
 */
class GateConditionEvaluator<TYPE> implements ConditionEvaluator<TYPE> {

    private final Object[] mArgs;

    private volatile Method mCondition;

    /**
     * Constructor.
     *
     * @param args The arguments to be passed to the gate method.
     */
    public GateConditionEvaluator(final Object[] args) {

        mArgs = args.clone();
    }

    @SuppressWarnings("ConstantConditions")
    private static Method findCondition(final Method[] methods, final Object[] args) {

        Method conditionMethod = null;

        boolean isAnnotated = false;
        boolean isClashing = false;

        int confidenceLevel = 0;

        final int length = args.length;

        for (final Method method : methods) {

            if (boolean.class.equals(method.getReturnType())) {

                final Class<?>[] params = method.getParameterTypes();

                if (length != params.length) {

                    continue;
                }

                boolean isMatching = true;

                for (int i = 0; i < length && isMatching; ++i) {

                    final Object arg = args[i];
                    final Class<?> param = params[i];

                    if ((arg != null) ? !Rapids.boxedClass(param).isInstance(arg)
                            : param.isPrimitive()) {

                        isMatching = false;

                        break;
                    }
                }

                if (isMatching) {

                    if (conditionMethod == null) {

                        isAnnotated = method.isAnnotationPresent(Condition.class);

                    } else if (isAnnotated) {

                        if (!method.isAnnotationPresent(Condition.class)) {

                            continue;
                        }

                    } else if (method.isAnnotationPresent(Condition.class)) {

                        conditionMethod = null;

                        isAnnotated = true;
                        isClashing = false;
                    }

                    int confidence = 0;

                    for (int i = 0; i < length && isMatching; ++i) {

                        final Object arg = args[i];
                        final Class<?> param = params[i];

                        if ((arg != null) && Rapids.boxedClass(param).equals(arg.getClass())) {

                            ++confidence;
                        }
                    }

                    if ((conditionMethod == null) || (confidence > confidenceLevel)) {

                        conditionMethod = method;
                        confidenceLevel = confidence;

                    } else if (confidence == confidenceLevel) {

                        isClashing = true;
                    }
                }
            }
        }

        if (isClashing) {

            throw new IllegalArgumentException(
                    "more than one condition method found for arguments: " + Arrays.toString(args));
        }

        return conditionMethod;
    }

    @Override
    public boolean isSatisfied(final TYPE leap) {

        try {

            return (Boolean) getCondition(leap).invoke(leap, mArgs);

        } catch (final InvocationTargetException e) {

            throw new RapidException(e.getCause());

        } catch (final IllegalAccessException e) {

            throw new RapidException(e);
        }
    }

    private Method getCondition(final TYPE leap) {

        if (mCondition == null) {

            final Object[] args = mArgs;
            final Class<?> type = leap.getClass();

            Method condition = findCondition(type.getMethods(), args);

            if (condition == null) {

                condition = findCondition(type.getDeclaredMethods(), args);

                if (condition == null) {

                    throw new IllegalArgumentException("no suitable method found for type " + type);
                }
            }

            if (!condition.isAccessible()) {

                condition.setAccessible(true);
            }

            mCondition = condition;
        }

        return mCondition;
    }
}