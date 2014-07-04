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

import java.util.concurrent.TimeUnit;

/**
 * Base abstract implementation of a river.
 * <p/>
 * Created by davide on 7/3/14.
 *
 * @param <SOURCE> The source data type.
 * @param <DATA>   The data type.
 */
public abstract class AbstractRiver<SOURCE, DATA> implements River<SOURCE, DATA> {

    @Override
    public River<SOURCE, DATA> discharge(final DATA... drops) {

        return push(drops).discharge();
    }

    @Override
    public River<SOURCE, DATA> discharge(final Iterable<? extends DATA> drops) {

        return push(drops).discharge();
    }

    @Override
    public River<SOURCE, DATA> discharge(final DATA drop) {

        return push(drop).discharge();
    }

    @Override
    public River<SOURCE, DATA> dischargeAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        return pushAfter(delay, timeUnit, drops).discharge();
    }

    @Override
    public River<SOURCE, DATA> dischargeAfter(final long delay, final TimeUnit timeUnit,
            final DATA drop) {

        return pushAfter(delay, timeUnit, drop).discharge();
    }

    @Override
    public River<SOURCE, DATA> dischargeAfter(final long delay, final TimeUnit timeUnit,
            final DATA... drops) {

        return pushAfter(delay, timeUnit, drops).discharge();
    }

    @Override
    public River<SOURCE, DATA> dischargeStream(final int streamNumber, final DATA... drops) {

        return pushStream(streamNumber, drops).dischargeStream(streamNumber);
    }

    @Override
    public River<SOURCE, DATA> dischargeStream(final int streamNumber,
            final Iterable<? extends DATA> drops) {

        return pushStream(streamNumber, drops).dischargeStream(streamNumber);
    }

    @Override
    public River<SOURCE, DATA> dischargeStream(final int streamNumber, final DATA drop) {

        return pushStream(streamNumber, drop).dischargeStream(streamNumber);
    }

    @Override
    public River<SOURCE, DATA> dischargeStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        return pushStreamAfter(streamNumber, delay, timeUnit, drops).dischargeStream(streamNumber);
    }

    @Override
    public River<SOURCE, DATA> dischargeStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        return pushStreamAfter(streamNumber, delay, timeUnit, drop).dischargeStream(streamNumber);
    }

    @Override
    public River<SOURCE, DATA> dischargeStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        return pushStreamAfter(streamNumber, delay, timeUnit, drops).dischargeStream(streamNumber);
    }
}