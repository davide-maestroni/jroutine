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

package com.github.dm.jrt.operator;

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Factory of invitations sorting the input using a specific comparator.
 * <p>
 * Created by davide-maestroni on 05/06/2016.
 *
 * @param <DATA> the data type.
 */
class SortByInvocationFactory<DATA> extends InvocationFactory<DATA, DATA> {

    private final Comparator<? super DATA> mComparator;

    /**
     * Constructor.
     *
     * @param comparator the comparator instance.
     */
    SortByInvocationFactory(@NotNull final Comparator<? super DATA> comparator) {
        super(asArgs(ConstantConditions.notNull("comparator instance", comparator)));
        mComparator = comparator;
    }

    @NotNull
    @Override
    public Invocation<DATA, DATA> newInvocation() {
        return new SortByInvocation<DATA>(mComparator);
    }

    /**
     * Invocation sorting the input using a specific comparator.
     *
     * @param <DATA> the data type.
     */
    private static class SortByInvocation<DATA> extends TemplateInvocation<DATA, DATA> {

        private final Comparator<? super DATA> mComparator;

        private ArrayList<DATA> mList;

        /**
         * Constructor.
         *
         * @param comparator the comparator instance.
         */
        private SortByInvocation(@NotNull final Comparator<? super DATA> comparator) {
            mComparator = comparator;
        }

        @Override
        public void onComplete(@NotNull final Channel<DATA, ?> result) {
            final ArrayList<DATA> list = mList;
            Collections.sort(list, mComparator);
            result.pass(list);
        }

        @Override
        public void onInput(final DATA input, @NotNull final Channel<DATA, ?> result) {
            mList.add(input);
        }

        @Override
        public void onRecycle(final boolean isReused) {
            mList = null;
        }

        @Override
        public void onRestart() {
            mList = new ArrayList<DATA>();
        }
    }
}
