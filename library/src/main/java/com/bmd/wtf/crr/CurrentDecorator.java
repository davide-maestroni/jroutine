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
package com.bmd.wtf.crr;

import com.bmd.wtf.src.Pool;

import java.util.concurrent.TimeUnit;

/**
 * {@link Current} decorator class.
 * <p/>
 * Created by davide on 3/11/14.
 */
public class CurrentDecorator implements Current {

    private final Current mCurrent;

    /**
     * Default constructor.
     *
     * @param wrapped The wrapped current.
     */
    public CurrentDecorator(final Current wrapped) {

        mCurrent = wrapped;
    }

    @Override
    public <DATA> void discharge(final Pool<DATA> pool, final DATA drop) {

        mCurrent.discharge(pool, drop);
    }

    @Override
    public <DATA> void dischargeAfter(final Pool<DATA> pool, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mCurrent.dischargeAfter(pool, delay, timeUnit, drop);
    }

    @Override
    public <DATA> void dischargeAfter(final Pool<DATA> pool, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mCurrent.dischargeAfter(pool, delay, timeUnit, drops);
    }

    @Override
    public void drop(final Pool<?> pool, final Object debris) {

        mCurrent.drop(pool, debris);
    }

    @Override
    public void dropAfter(final Pool<?> pool, final long delay, final TimeUnit timeUnit,
            final Object debris) {

        mCurrent.dropAfter(pool, delay, timeUnit, debris);
    }

    @Override
    public void flush(final Pool<?> pool) {

        mCurrent.flush(pool);
    }

    /**
     * Returns the wrapped current.
     *
     * @return The wrapped instance.
     */
    protected Current wrapped() {

        return mCurrent;
    }
}