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
 * @param <DATA> The data type.
 */
abstract class AbstractRiver<DATA> implements River<DATA> {

    @Override
    public River<DATA> flush(final DATA... drops) {

        return push(drops).flush();
    }

    @Override
    public River<DATA> flush(final Iterable<? extends DATA> drops) {

        return push(drops).flush();
    }

    @Override
    public River<DATA> flush(final DATA drop) {

        return push(drop).flush();
    }

    @Override
    public River<DATA> flushAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        return pushAfter(delay, timeUnit, drops).flush();
    }

    @Override
    public River<DATA> flushAfter(final long delay, final TimeUnit timeUnit, final DATA drop) {

        return pushAfter(delay, timeUnit, drop).flush();
    }

    @Override
    public River<DATA> flushAfter(final long delay, final TimeUnit timeUnit, final DATA... drops) {

        return pushAfter(delay, timeUnit, drops).flush();
    }

    @Override
    public River<DATA> flushStream(final int streamNumber, final DATA... drops) {

        return pushStream(streamNumber, drops).flushStream(streamNumber);
    }

    @Override
    public River<DATA> flushStream(final int streamNumber, final Iterable<? extends DATA> drops) {

        return pushStream(streamNumber, drops).flushStream(streamNumber);
    }

    @Override
    public River<DATA> flushStream(final int streamNumber, final DATA drop) {

        return pushStream(streamNumber, drop).flushStream(streamNumber);
    }

    @Override
    public River<DATA> flushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA... drops) {

        return pushStreamAfter(streamNumber, delay, timeUnit, drops).flushStream(streamNumber);
    }

    @Override
    public River<DATA> flushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final Iterable<? extends DATA> drops) {

        return pushStreamAfter(streamNumber, delay, timeUnit, drops).flushStream(streamNumber);
    }

    @Override
    public River<DATA> flushStreamAfter(final int streamNumber, final long delay,
            final TimeUnit timeUnit, final DATA drop) {

        return pushStreamAfter(streamNumber, delay, timeUnit, drop).flushStream(streamNumber);
    }
}