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
package com.bmd.wtf.fll;

import com.bmd.wtf.flw.River;
import com.bmd.wtf.gts.AbstractGate;
import com.bmd.wtf.spr.Spring;

/**
 * Gate wrapping a spring.
 * <p/>
 * Created by davide on 8/21/14.
 *
 * @param <DATA> the spring data type.
 */
class SpringGate<DATA> extends AbstractGate<Void, DATA> {

    private final Spring<DATA> mSpring;

    /**
     * Constructor.
     *
     * @param spring the wrapped spring instance.
     */
    public SpringGate(final Spring<DATA> spring) {

        if (spring == null) {

            throw new IllegalArgumentException("the spring cannot be null");
        }

        mSpring = spring;
    }

    @Override
    public int hashCode() {

        return mSpring.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof SpringGate)) {

            return false;
        }

        final SpringGate that = (SpringGate) o;

        return mSpring.equals(that.mSpring);
    }

    @Override
    public void onFlush(final River<Void> upRiver, final River<DATA> downRiver,
            final int fallNumber) {

        final Spring<DATA> spring = mSpring;

        while (spring.hasDrops()) {

            downRiver.push(spring.nextDrop());
        }

        super.onFlush(upRiver, downRiver, fallNumber);
    }

    @Override
    public void onPush(final River<Void> upRiver, final River<DATA> downRiver, final int fallNumber,
            final Void drop) {

        downRiver.forward(new IllegalStateException());
    }
}