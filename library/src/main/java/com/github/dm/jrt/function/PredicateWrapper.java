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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Returns a predicate wrapper testing for equality to the specified object.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param targetRef the target reference.
     * @param <IN>      the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    public static <IN> PredicateWrapper<IN> isEqual(@Nullable final Object targetRef) {

        if (targetRef == null) {
            return isNull();
        }

        return new PredicateWrapper<IN>(new EqualToPredicate<IN>(targetRef));
    }

    /**
     * Returns a predicate wrapper testing whether the passed inputs are instances of the specified
     * class.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param type the class type.
     * @param <IN> the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    public static <IN> PredicateWrapper<IN> isInstanceOf(@NotNull final Class<?> type) {

        if (type == null) {
            throw new NullPointerException("the type must not be null");
        }

        return new PredicateWrapper<IN>(new InstanceOfPredicate<IN>(type));
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
     * Returns a predicate wrapper testing for identity to the specified object.<br/>
     * The returned object will support concatenation and comparison.
     *
     * @param targetRef the target reference.
     * @param <IN>      the input data type.
     * @return the predicate wrapper.
     */
    @NotNull
    public static <IN> PredicateWrapper<IN> isSame(@Nullable final Object targetRef) {

        if (targetRef == null) {
            return isNull();
        }

        return new PredicateWrapper<IN>(new SameAsPredicate<IN>(targetRef));
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
    @SuppressWarnings("ConstantConditions")
    public PredicateWrapper<IN> and(@NotNull final Predicate<? super IN> other) {

        final List<Predicate<?>> predicates = mPredicates;
        final ArrayList<Predicate<?>> newPredicates =
                new ArrayList<Predicate<?>>(predicates.size() + 4);
        newPredicates.add(OPEN_PREDICATE);
        newPredicates.addAll(predicates);
        newPredicates.add(AND_PREDICATE);
        if (other instanceof PredicateWrapper) {
            newPredicates.addAll(((PredicateWrapper<?>) other).mPredicates);

        } else if (other == null) {
            throw new NullPointerException("the predicate must not be null");

        } else {
            newPredicates.add(other);
        }

        newPredicates.add(CLOSE_PREDICATE);
        return new PredicateWrapper<IN>(new AndPredicate<IN>(mPredicate, other), newPredicates);
    }

    @Override
    public int hashCode() {

        return mPredicates.hashCode();
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
    @SuppressWarnings("ConstantConditions")
    public PredicateWrapper<IN> or(@NotNull final Predicate<? super IN> other) {

        final List<Predicate<?>> predicates = mPredicates;
        final ArrayList<Predicate<?>> newPredicates =
                new ArrayList<Predicate<?>>(predicates.size() + 4);
        newPredicates.add(OPEN_PREDICATE);
        newPredicates.addAll(predicates);
        newPredicates.add(OR_PREDICATE);
        if (other instanceof PredicateWrapper) {
            newPredicates.addAll(((PredicateWrapper<?>) other).mPredicates);

        } else if (other == null) {
            throw new NullPointerException("the predicate must not be null");

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
     * Predicate implementation testing for equality.
     *
     * @param <IN> the input data type.
     */
    private static class EqualToPredicate<IN> implements Predicate<IN> {

        private final Object mOther;

        /**
         * Constructor.
         *
         * @param other the other object to test against.
         */
        private EqualToPredicate(@NotNull final Object other) {

            mOther = other;
        }

        @Override
        public int hashCode() {

            return mOther.hashCode();
        }

        public boolean test(final IN in) {

            return mOther.equals(in);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof EqualToPredicate)) {
                return false;
            }

            final EqualToPredicate<?> that = (EqualToPredicate<?>) o;
            return mOther.equals(that.mOther);
        }
    }

    /**
     * Predicate testing whether an object is an instance of a specific class.
     *
     * @param <IN> the input data type.
     */
    private static class InstanceOfPredicate<IN> implements Predicate<IN> {

        private final Class<?> mType;

        /**
         * Constructor.
         *
         * @param type the class type.
         */
        private InstanceOfPredicate(@NotNull final Class<?> type) {

            mType = type;
        }

        @Override
        public int hashCode() {

            return mType.hashCode();
        }

        public boolean test(final IN in) {

            return mType.isInstance(in);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof InstanceOfPredicate)) {
                return false;
            }

            final InstanceOfPredicate<?> that = (InstanceOfPredicate<?>) o;
            return mType.equals(that.mType);
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

    /**
     * Predicate implementation testing for identity.
     *
     * @param <IN> the input data type.
     */
    private static class SameAsPredicate<IN> implements Predicate<IN> {

        private final Object mOther;

        /**
         * Constructor.
         *
         * @param other the other object to test against.
         */
        private SameAsPredicate(@NotNull final Object other) {

            mOther = other;
        }

        public boolean test(final IN in) {

            return (mOther == in);
        }

        @Override
        public int hashCode() {

            return mOther.hashCode();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof SameAsPredicate)) {
                return false;
            }

            final SameAsPredicate<?> that = (SameAsPredicate<?>) o;
            return (mOther == that.mOther);
        }
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof PredicateWrapper)) {
            return false;
        }

        final PredicateWrapper<?> that = (PredicateWrapper<?>) o;
        return mPredicates.equals(that.mPredicates);
    }

    public boolean test(final IN in) {

        return mPredicate.test(in);
    }
}
