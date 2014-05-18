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
package com.bmd.wtf.flw;

import com.bmd.wtf.src.Pool;

import java.util.concurrent.TimeUnit;

/**
 * {@link Flow} decorator class.
 * <p/>
 * Created by davide on 3/11/14.
 */
public class FlowDecorator implements Flow {

    private final Flow mFlow;

    /**
     * Default constructor.
     *
     * @param wrapped The wrapped flow.
     */
    public FlowDecorator(final Flow wrapped) {

        mFlow = wrapped;
    }

    @Override
    public <DATA> void discharge(final Pool<DATA> pool, final DATA drop) {

        mFlow.discharge(pool, drop);
    }

    @Override
    public <DATA> void dischargeAfter(final Pool<DATA> pool, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        mFlow.dischargeAfter(pool, delay, timeUnit, drop);
    }

    @Override
    public <DATA> void dischargeAfter(final Pool<DATA> pool, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        mFlow.dischargeAfter(pool, delay, timeUnit, drops);
    }

    @Override
    public void flush(final Pool<?> pool) {

        mFlow.flush(pool);
    }

    @Override
    public void pull(final Pool<?> pool, final Object debris) {

        mFlow.pull(pool, debris);
    }

    @Override
    public void push(final Pool<?> pool, final Object debris) {

        mFlow.push(pool, debris);
    }

    /**
     * Returns the wrapped flow.
     *
     * @return The wrapped instance.
     */
    protected Flow wrapped() {

        return mFlow;
    }
}