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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Class extending a cache data. Contrary to the parent class data from all streams are accumulated
 * and cleared only when all streams are flushed.
 * <p/>
 * Created by davide on 8/27/14.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
class CumulativeCacheGate<IN, OUT> extends CacheGate<IN, OUT> {

    private final List<List<IN>[]> mCache;

    private final int mSize;

    /**
     * Constructor.
     *
     * @param fallCount the fall count.
     */
    public CumulativeCacheGate(final int fallCount) {

        super(fallCount);

        mSize = Math.max(1, fallCount);
        mCache = new ArrayList<List<IN>[]>();
    }

    @Override
    protected void onClear(final List<IN> cache, final int streamNumber, final River<IN> upRiver,
            final River<OUT> downRiver) {

        boolean added = false;

        final Iterator<List<IN>[]> iterator = mCache.iterator();

        while (iterator.hasNext()) {

            final List<IN>[] data = iterator.next();

            if (data[streamNumber] == null) {

                data[streamNumber] = new ArrayList<IN>(cache);

                boolean isComplete = true;

                for (final List<IN> list : data) {

                    if (list == null) {

                        isComplete = false;

                        break;
                    }
                }

                if (isComplete) {

                    iterator.remove();

                    //noinspection unchecked
                    onClear(Arrays.asList(data), upRiver, downRiver);
                }

                added = true;
            }
        }

        if (!added) {

            //noinspection unchecked
            final List<IN>[] data = new List[mSize];
            data[streamNumber] = new ArrayList<IN>(cache);

            mCache.add(data);
        }
    }

    /**
     * This method is called every time the cache of a all the streams needs to be cleared.
     *
     * @param cache     the cached data.
     * @param upRiver   the upstream river instance.
     * @param downRiver the downstream river instance.
     */
    protected void onClear(final List<List<IN>> cache, final River<IN> upRiver,
            final River<OUT> downRiver) {

        for (final List<IN> list : cache) {

            list.clear();
        }
    }
}