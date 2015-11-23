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
package com.github.dm.jrt.function;

import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class wrapping a predicate instance.
 * <p/>
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN> the input data type.
 */
public class PredicateWrapper<IN> implements Predicate<IN> {

    private static final LogicalPredicate AND_PREDICATE = new LogicalPredicate();

    private static final LogicalPredicate CLOSE_PREDICATE = new LogicalPredicate();

    private static final LogicalPredicate NEGATE_PREDICATE = new LogicalPredicate();

    private static final LogicalPredicate OPEN_PREDICATE = new LogicalPredicate();

    private static final LogicalPredicate OR_PREDICATE = new LogicalPredicate();

    private static final PredicateWrapper<Object> sNegative =
            new PredicateWrapper<Object>(new Predicate<Object>() {

                public boolean test(final Object o) {

                    return false;
                }
            });

    private static final PredicateWrapper<Object> sNotNull =
            new PredicateWrapper<Object>(new Predicate<Object>() {

                public boolean test(final Object o) {

                    return (o != null);
                }
            });

    private final Predicate<? super IN> mPredicate;

    private final List<Predicate<?>> mPredicates;

    private static final PredicateWrapper<Object> sIsNull = sNotNull.negate();

    private static final PredicateWrapper<Object> sPositive = sNegative.negate();

    /**
     * Constructor.
     *
     * @param predicate the core predicate.
     */
    @SuppressWarnings("ConstantConditions")
    PredicateWrapper(@NotNull final Predicate<? super IN> predicate) {

        this(predicate, Collections.<Predicate<?>>singletonList(predicate));

        if (predicate == null) {

            throw new NullPointerException("the predicate must not be null");
        }
    }

    /**
     * Constructor.
     *
     * @param predicate  the core predicate.
     * @param predicates the list of wrapped predicates.
     */
    private PredicateWrapper(@NotNull final Predicate<? super IN> predicate,
            @NotNull final List<Predicate<?>> predicates) {

        mPredicate = predicate;
        mPredicates = predicates;
    }

    /**
     * Returns a predicate wrapper returning true when the passed argument is null.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> PredicateWrapper<IN> isNull() {

        return (PredicateWrapper<IN>) sIsNull;
    }

    /**
     * Returns a predicate wrapper always returning the false.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> PredicateWrapper<IN> negative() {

        return (PredicateWrapper<IN>) sNegative;
    }

    /**
     * Returns a predicate wrapper returning true when the passed argument is not null.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> PredicateWrapper<IN> notNull() {

        return (PredicateWrapper<IN>) sNotNull;
    }

    /**
     * Returns a predicate wrapper always returning the true.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN> PredicateWrapper<IN> positive() {

        return (PredicateWrapper<IN>) sPositive;
    }

    /**
     * Returns a composed predicate wrapper that represents a short-circuiting logical AND of this
     * predicate and another.
     *
     * @param other a predicate that will be logically-ANDed with this predicate.
     * @return the composed predicate.
     */
    @NotNull
    public PredicateWrapper<IN> and(@NotNull final Predicate<? super IN> other) {

        final Class<? extends Predicate> otherClass = other.getClass();
        final List<Predicate<?>> predicates = mPredicates;
        final ArrayList<Predicate<?>> newPredicates =
                new ArrayList<Predicate<?>>(predicates.size() + 4);
        newPredicates.add(OPEN_PREDICATE);
        newPredicates.addAll(predicates);
        newPredicates.add(AND_PREDICATE);

        if (otherClass == PredicateWrapper.class) {

            newPredicates.addAll(((PredicateWrapper<?>) other).mPredicates);

        } else {

            newPredicates.add(other);
        }

        newPredicates.add(CLOSE_PREDICATE);
        return new PredicateWrapper<IN>(new AndPredicate<IN>(mPredicate, other), newPredicates);
    }

    /**
     * Checks if the predicates wrapped by this instance have a static context.
     *
     * @return whether the predicates have a static context.
     */
    public boolean hasStaticContext() {

        for (final Predicate<?> predicate : mPredicates) {

            if (!Reflection.hasStaticContext(predicate.getClass())) {

                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {

        return mPredicates.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {

            return false;
        }

        final PredicateWrapper<?> that = (PredicateWrapper<?>) o;
        return mPredicates.equals(that.mPredicates);
    }

    /**
     * Returns a predicate wrapper that represents the logical negation of this predicate.
     *
     * @return the negated predicate.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public PredicateWrapper<IN> negate() {

        final List<Predicate<?>> predicates = mPredicates;
        final int size = predicates.size();
        final ArrayList<Predicate<?>> newPredicates = new ArrayList<Predicate<?>>(size + 1);

        if (size == 1) {

            newPredicates.add(NEGATE_PREDICATE);
            newPredicates.add(predicates.get(0));

        } else {

            final Predicate<?> first = predicates.get(0);

            if (first == NEGATE_PREDICATE) {

                newPredicates.add(predicates.get(1));

            } else {

                newPredicates.add(first);

                for (int i = 1; i < size; ++i) {

                    final Predicate<?> predicate = predicates.get(i);

                    if (predicate == NEGATE_PREDICATE) {

                        ++i;

                    } else if (predicate == OR_PREDICATE) {

                        newPredicates.add(AND_PREDICATE);

                    } else if (predicate == AND_PREDICATE) {

                        newPredicates.add(OR_PREDICATE);

                    } else {

                        if ((predicate != OPEN_PREDICATE) && (predicate != CLOSE_PREDICATE)) {

                            newPredicates.add(NEGATE_PREDICATE);
                        }

                        newPredicates.add(predicate);
                    }
                }
            }
        }

        final Predicate<? super IN> predicate = mPredicate;

        if (predicate instanceof NegatePredicate) {

            return new PredicateWrapper<IN>(((NegatePredicate<? super IN>) predicate).mPredicate,
                                            newPredicates);
        }

        return new PredicateWrapper<IN>(new NegatePredicate<IN>(predicate), newPredicates);
    }

    /**
     * Returns a composed predicate wrapper that represents a short-circuiting logical OR of this
     * predicate and another.
     *
     * @param other a predicate that will be logically-ORed with this predicate.
     * @return the composed predicate.
     */
    @NotNull
    public PredicateWrapper<IN> or(@NotNull final Predicate<? super IN> other) {

        final Class<? extends Predicate> otherClass = other.getClass();
        final List<Predicate<?>> predicates = mPredicates;
        final ArrayList<Predicate<?>> newPredicates =
                new ArrayList<Predicate<?>>(predicates.size() + 4);
        newPredicates.add(OPEN_PREDICATE);
        newPredicates.addAll(predicates);
        newPredicates.add(OR_PREDICATE);

        if (otherClass == PredicateWrapper.class) {

            newPredicates.addAll(((PredicateWrapper<?>) other).mPredicates);

        } else {

            newPredicates.add(other);
        }

        newPredicates.add(CLOSE_PREDICATE);
        return new PredicateWrapper<IN>(new OrPredicate<IN>(mPredicate, other), newPredicates);
    }

    /**
     * Predicate implementation logically-ANDing the wrapped ones.
     *
     * @param <IN> the input data type.
     */
    private static final class AndPredicate<IN> implements Predicate<IN> {

        private final Predicate<? super IN> mOther;

        private final Predicate<? super IN> mPredicate;

        /**
         * Constructor.
         *
         * @param predicate the wrapped predicate.
         * @param other     the other predicate to be logically-ANDed.
         */
        private AndPredicate(@NotNull final Predicate<? super IN> predicate,
                @NotNull final Predicate<? super IN> other) {

            mPredicate = predicate;
            mOther = other;
        }

        public boolean test(final IN in) {

            return mPredicate.test(in) && mOther.test(in);
        }
    }

    /**
     * Class indicating a logical operation (like AND and OR).
     */
    private static class LogicalPredicate implements Predicate<Object> {

        public boolean test(final Object o) {

            throw new UnsupportedOperationException("should never be called");
        }
    }

    /**
     * Predicate implementation negating the wrapped one.
     *
     * @param <IN> the input data type.
     */
    private static final class NegatePredicate<IN> implements Predicate<IN> {

        private final Predicate<? super IN> mPredicate;

        /**
         * Constructor.
         *
         * @param predicate the wrapped predicate.
         */
        private NegatePredicate(@NotNull final Predicate<? super IN> predicate) {

            mPredicate = predicate;
        }

        public boolean test(final IN in) {

            return !mPredicate.test(in);
        }
    }

    /**
     * Predicate implementation logically-ORing the wrapped ones.
     *
     * @param <IN> the input data type.
     */
    private static final class OrPredicate<IN> implements Predicate<IN> {

        private final Predicate<? super IN> mOther;

        private final Predicate<? super IN> mPredicate;

        /**
         * Constructor.
         *
         * @param predicate the wrapped predicate.
         * @param other     the other predicate to be logically-ORed.
         */
        private OrPredicate(@NotNull final Predicate<? super IN> predicate,
                @NotNull final Predicate<? super IN> other) {

            mPredicate = predicate;
            mOther = other;
        }

        public boolean test(final IN in) {

            return mPredicate.test(in) || mOther.test(in);
        }
    }

    public boolean test(final IN in) {

        return mPredicate.test(in);
    }
}
