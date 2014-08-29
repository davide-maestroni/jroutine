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

import com.bmd.wtf.flw.Bridge.Visitor;
import com.bmd.wtf.flw.FloatingException;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.gts.OpenGate;

import java.util.ArrayList;
import java.util.List;

/**
 * Gate implementation used to collect data pulled from a waterfall.
 * <p/>
 * Created by davide on 6/13/14.
 *
 * @param <DATA> the data type.
 */
class CollectorGate<DATA> extends OpenGate<DATA> {

    private static final Visitor<Boolean, CollectorGate<?>> EMPTY_VISITOR =
            new Visitor<Boolean, CollectorGate<?>>() {

                @Override
                public Boolean doInspect(final CollectorGate<?> collector, final Object... args) {

                    return collector.isEmpty();
                }
            };

    private final ArrayList<DATA> mData = new ArrayList<DATA>();

    private Throwable mException;

    private final Visitor<DATA, CollectorGate<DATA>> PULL_VISITOR =
            new Visitor<DATA, CollectorGate<DATA>>() {

                @Override
                public DATA doInspect(final CollectorGate<DATA> collector, final Object... args) {

                    final Throwable throwable = collector.mException;

                    if (throwable != null) {

                        throw new FloatingException(throwable);
                    }

                    return collector.mData.remove(0);
                }
            };

    private static final Visitor<Void, CollectorGate<?>> PULL_ALL_VISITOR =
            new Visitor<Void, CollectorGate<?>>() {

                @Override
                public Void doInspect(final CollectorGate<?> collector, final Object... args) {

                    final Throwable throwable = collector.mException;

                    if (throwable != null) {

                        throw new FloatingException(throwable);
                    }

                    final ArrayList<?> data = collector.mData;

                    //noinspection unchecked
                    ((List) args[0]).addAll(data);

                    data.clear();

                    return null;
                }
            };

    private boolean mIsComplete;

    /**
     * Checks if the collection is complete, that is, if data have been flushed.
     *
     * @return whether collection is complete.
     */
    public boolean isComplete() {

        return mIsComplete;
    }

    /**
     * Returns a visitor checking if the internal data collection is empty.
     *
     * @return the visitor.
     */
    public Visitor<Boolean, CollectorGate<?>> isEmptyVisitor() {

        return EMPTY_VISITOR;
    }

    @Override
    public void onException(final River<DATA> upRiver, final River<DATA> downRiver,
            final int fallNumber, final Throwable throwable) {

        upRiver.deviate();
        downRiver.deviate();

        mException = throwable;
        mIsComplete = true;
    }

    @Override
    public void onFlush(final River<DATA> upRiver, final River<DATA> downRiver,
            final int fallNumber) {

        upRiver.deviate();
        downRiver.deviate();

        mIsComplete = true;
    }

    @Override
    public void onPush(final River<DATA> upRiver, final River<DATA> downRiver, final int fallNumber,
            final DATA drop) {

        mData.add(drop);
    }

    /**
     * Returns a visitor pulling all the elements of the internal collection.
     *
     * @return the visitor.
     */
    public Visitor<Void, CollectorGate<?>> pullAllVisitor() {

        return PULL_ALL_VISITOR;
    }

    /**
     * Returns a visitor pulling the next element from the internal collection.
     *
     * @return the visitor.
     */
    public Visitor<DATA, CollectorGate<DATA>> pullVisitor() {

        return PULL_VISITOR;
    }

    /**
     * Returns the size of the internal collection.
     *
     * @return the size.
     */
    public int size() {

        return mData.size();
    }

    private boolean isEmpty() {

        return (mData.isEmpty() && mIsComplete);
    }
}