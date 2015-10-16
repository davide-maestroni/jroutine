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
package com.github.dm.jrt.functional;

import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Class wrapping a predicate instance.
 * <p/>
 * Created by davide-maestroni on 10/16/2015.
 *
 * @param <IN> the input data type.
 */
public class PredicateChain<IN> implements Predicate<IN> {

    private static final Predicate<?> OPEN_PREDICATE = new Predicate<Object>() {

        public boolean test(final Object o) {

            return false;
        }
    };

    private static final Predicate<?> CLOSE_PREDICATE = new Predicate<Object>() {

        public boolean test(final Object o) {

            return false;
        }
    };

    private static final Predicate<?> AND_PREDICATE = new Predicate<Object>() {

        public boolean test(final Object o) {

            return false;
        }
    };

    private static final Predicate<?> OR_PREDICATE = new Predicate<Object>() {

        public boolean test(final Object o) {

            return false;
        }
    };

    private static final Predicate<?> NEGATE_PREDICATE = new Predicate<Object>() {

        public boolean test(final Object o) {

            return false;
        }
    };

    private final Predicate<? super IN> mPredicate;

    private final List<Predicate<?>> mPredicates;

    /**
     * Constructor.
     *
     * @param predicate  the core predicate.
     * @param predicates the list of wrapped predicates.
     */
    @SuppressWarnings("ConstantConditions")
    PredicateChain(@NotNull final Predicate<? super IN> predicate,
            @NotNull final List<Predicate<?>> predicates) {

        if (predicate == null) {

            throw new IllegalArgumentException("the predicate must not be null");
        }

        if (predicates.isEmpty()) {

            throw new IllegalArgumentException("the list of predicates must not be empty");
        }

        mPredicate = predicate;
        mPredicates = predicates;
    }

    /**
     * Checks if this consumer chain has a static context.
     *
     * @return whether this instance has a static context.
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

        int result = 0;

        for (final Predicate<?> predicate : mPredicates) {

            result = 31 * result + predicate.getClass().hashCode();
        }

        return result;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {

            return false;
        }

        final PredicateChain<?> that = (PredicateChain<?>) o;
        final List<Predicate<?>> thisPredicates = mPredicates;
        final List<Predicate<?>> thatPredicates = that.mPredicates;
        final int size = thisPredicates.size();

        if (size != thatPredicates.size()) {

            return false;
        }

        for (int i = 0; i < size; ++i) {

            if (thisPredicates.get(i).getClass() != thatPredicates.get(i).getClass()) {

                return false;
            }
        }

        return true;
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND of this predicate
     * and another.
     *
     * @param other a predicate that will be logically-ANDed with this predicate.
     * @return the composed predicate.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public PredicateChain<IN> and(@NotNull final Predicate<? super IN> other) {

        final Class<? extends Predicate> otherClass = other.getClass();
        final List<Predicate<?>> predicates = mPredicates;
        final ArrayList<Predicate<?>> newPredicates =
                new ArrayList<Predicate<?>>(predicates.size() + 4);
        newPredicates.add(OPEN_PREDICATE);
        newPredicates.addAll(predicates);
        newPredicates.add(CLOSE_PREDICATE);
        newPredicates.add(AND_PREDICATE);

        if (otherClass == PredicateChain.class) {

            newPredicates.add(OPEN_PREDICATE);
            newPredicates.addAll(((PredicateChain<? super IN>) other).mPredicates);
            newPredicates.add(CLOSE_PREDICATE);

        } else {

            newPredicates.add(other);
        }

        return new PredicateChain<IN>(new AndPredicate<IN>(mPredicate, other), newPredicates);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of this predicate
     * and another.
     *
     * @param other a predicate that will be logically-ORed with this predicate.
     * @return the composed predicate.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public PredicateChain<IN> or(@NotNull final Predicate<? super IN> other) {

        final Class<? extends Predicate> otherClass = other.getClass();
        final List<Predicate<?>> predicates = mPredicates;
        final ArrayList<Predicate<?>> newPredicates =
                new ArrayList<Predicate<?>>(predicates.size() + 4);
        newPredicates.add(OPEN_PREDICATE);
        newPredicates.addAll(predicates);
        newPredicates.add(CLOSE_PREDICATE);
        newPredicates.add(OR_PREDICATE);

        if (otherClass == PredicateChain.class) {

            newPredicates.add(OPEN_PREDICATE);
            newPredicates.addAll(((PredicateChain<? super IN>) other).mPredicates);
            newPredicates.add(CLOSE_PREDICATE);

        } else {

            newPredicates.add(other);
        }

        return new PredicateChain<IN>(new OrPredicate<IN>(mPredicate, other), newPredicates);
    }

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return the negated predicate.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public PredicateChain<IN> negate() {

        final List<Predicate<?>> predicates = mPredicates;
        final ArrayList<Predicate<?>> newPredicates =
                new ArrayList<Predicate<?>>(predicates.size() + 3);
        newPredicates.add(NEGATE_PREDICATE);
        newPredicates.add(OPEN_PREDICATE);
        newPredicates.addAll(predicates);
        newPredicates.add(CLOSE_PREDICATE);

        return new PredicateChain<IN>(new NegatePredicate<IN>(mPredicate), newPredicates);
    }

    public boolean test(final IN in) {

        return mPredicate.test(in);
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
}
