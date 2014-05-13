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
package com.bmd.wtf.bdr;

import com.bmd.wtf.src.Spring;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * This class implements a {@link com.bmd.wtf.src.Spring}, that is the very first input of data
 * and objects of the whole waterfall.
 * <p/>
 * Note however that several waterfalls may be merged or joined in different ways. Eventually,
 * more than one spring might feed the same {@link com.bmd.wtf.dam.Dam}.
 * <p/>
 * Created by davide on 3/2/14.
 *
 * @param <DATA> The data type.
 */
class DataSpring<DATA> implements Spring<DATA> {

    private Stream<DATA, ?, DATA> mOutStream;

    @Override
    public Spring<DATA> discharge(final DATA drop) {

        mOutStream.discharge(drop);

        return this;
    }

    @Override
    public Spring<DATA> discharge(final DATA... drops) {

        if (drops != null) {

            mOutStream.discharge(Arrays.asList(drops));
        }

        return this;
    }

    @Override
    public Spring<DATA> discharge(final Iterable<? extends DATA> drops) {

        if (drops != null) {

            mOutStream.discharge(drops);
        }

        return this;
    }

    @Override
    public Spring<DATA> dischargeAfter(final long delay, final TimeUnit timeUnit, final DATA drop) {

        mOutStream.dischargeAfter(delay, timeUnit, drop);

        return this;
    }

    @Override
    public Spring<DATA> dischargeAfter(final long delay, final TimeUnit timeUnit,
            final DATA... drops) {

        if (drops != null) {

            mOutStream.dischargeAfter(delay, timeUnit, Arrays.asList(drops));
        }

        return this;
    }

    @Override
    public Spring<DATA> dischargeAfter(final long delay, final TimeUnit timeUnit,
            final Iterable<? extends DATA> drops) {

        if (drops != null) {

            mOutStream.dischargeAfter(delay, timeUnit, drops);
        }

        return this;
    }

    @Override
    public void exhaust() {

        mOutStream.drain(true);
    }

    @Override
    public void flush() {

        mOutStream.flush();
    }

    Spring<DATA> setOutStream(final Stream<DATA, ?, DATA> stream) {

        mOutStream = stream;

        return this;
    }
}