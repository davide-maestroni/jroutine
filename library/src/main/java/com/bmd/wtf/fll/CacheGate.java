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

import java.util.ArrayList;
import java.util.List;

/**
 * Gate caching separately all the data drops flowing through each stream of the fall.
 * <p/>
 * The cached data of every stream is cleared each time the stream is flushed.
 * <p/>
 * Created by davide on 8/27/14.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class CacheGate<IN, OUT> extends AbstractGate<IN, OUT> {

    private final List<IN>[] mCache;

    /**
     * Constructor.
     *
     * @param fallCount the fall count.
     */
    public CacheGate(final int fallCount) {

        final int length = Math.max(1, fallCount);

        //noinspection unchecked
        final List<IN>[] lists = new List[length];

        for (int i = 0; i < length; i++) {

            lists[i] = new ArrayList<IN>();
        }

        mCache = lists;
    }

    @Override
    public void onFlush(final River<IN> upRiver, final River<OUT> downRiver, final int fallNumber) {

        onClear(mCache[fallNumber], fallNumber, upRiver, downRiver);

        super.onFlush(upRiver, downRiver, fallNumber);
    }

    @Override
    public void onPush(final River<IN> upRiver, final River<OUT> downRiver, final int fallNumber,
            final IN drop) {

        mCache[fallNumber].add(drop);
    }

    /**
     * This method is called every time the cache of a specific stream needs to be cleared.
     *
     * @param cache        the cached data.
     * @param streamNumber the number identifying the stream.
     * @param upRiver      the upstream river instance.
     * @param downRiver    the downstream river instance.
     */
    protected void onClear(final List<IN> cache, final int streamNumber, final River<IN> upRiver,
            final River<OUT> downRiver) {

        cache.clear();
    }
}