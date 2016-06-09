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

/**
 * Class implementing a fence local to a thread.
 * <br>
 * A local fence instance is useful to detect whether a piece of code is called within a specific
 * code range.
 * <p>
 * Created by davide-maestroni on 06/08/2016.
 */
public class LocalFence {

    private final LocalCount mCount = new LocalCount();

    /**
     * Enters the fence on the current thread.
     */
    public void enter() {

        mCount.get().incValue();
    }

    /**
     * Exits the fence on the current thread.
     */
    public void exit() {

        mCount.get().decValue();
    }

    /**
     * Checks if the current thread is inside the fence.
     *
     * @return whether the thread is in the fence.
     */
    public boolean isInside() {

        return mCount.get().getValue() > 0;
    }

    /**
     * Count implementation.
     */
    private static class Count {

        private int mCount;

        private void decValue() {

            mCount = ConstantConditions.notNegative("count", mCount - 1);
        }

        private int getValue() {

            return mCount;
        }

        private void incValue() {

            ++mCount;
        }
    }

    /**
     * Local count implementation.
     */
    private static class LocalCount extends ThreadLocal<Count> {

        @Override
        protected Count initialValue() {

            return new Count();
        }
    }
}
