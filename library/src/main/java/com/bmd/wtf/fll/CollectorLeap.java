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

import com.bmd.wtf.flw.FloatingException;
import com.bmd.wtf.flw.Gate.Action;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.FreeLeap;

import java.util.ArrayList;
import java.util.List;

/**
 * Leap implementation used to collect data pulled from a waterfall.
 * <p/>
 * Created by davide on 6/13/14.
 *
 * @param <DATA> The data type.
 */
class CollectorLeap<DATA> extends FreeLeap<DATA> {

    private static final Action<Boolean, CollectorLeap<?>> ACTION_EMPTY =
            new Action<Boolean, CollectorLeap<?>>() {

                @Override
                public Boolean doOn(final CollectorLeap<?> collector, final Object... args) {

                    return collector.isEmpty();
                }
            };

    private final ArrayList<DATA> mData = new ArrayList<DATA>();

    private boolean mIsComplete;

    private Throwable mUnhandled;

    private final Action<DATA, CollectorLeap<DATA>> ACTION_PULL =
            new Action<DATA, CollectorLeap<DATA>>() {

                @Override
                public DATA doOn(final CollectorLeap<DATA> collector, final Object... args) {

                    final Throwable throwable = collector.mUnhandled;

                    if (throwable != null) {

                        throw new FloatingException(throwable);
                    }

                    return collector.mData.remove(0);
                }
            };

    private static final Action<Void, CollectorLeap<?>> ACTION_PULL_ALL =
            new Action<Void, CollectorLeap<?>>() {

                @Override
                public Void doOn(final CollectorLeap<?> collector, final Object... args) {

                    final Throwable throwable = collector.mUnhandled;

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

    /**
     * Checks if the collection is complete, that is, if data have been flushed.
     *
     * @return Whether collection is complete.
     */
    public boolean isComplete() {

        return mIsComplete;
    }

    /**
     * Returns an action to check if the internal data collection is empty.
     *
     * @return The action.
     */
    public Action<Boolean, CollectorLeap<?>> isEmptyAction() {

        return ACTION_EMPTY;
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

    @Override
    public void onUnhandled(final River<DATA> upRiver, final River<DATA> downRiver,
            final int fallNumber, final Throwable throwable) {

        upRiver.deviate();
        downRiver.deviate();

        mUnhandled = throwable;
        mIsComplete = true;
    }

    /**
     * Returns an action to pull the next element from the internal collection.
     *
     * @return The action.
     */
    public Action<DATA, CollectorLeap<DATA>> pullAction() {

        return ACTION_PULL;
    }

    /**
     * Returns an action to pull all the elements of the internal collection.
     *
     * @return The action.
     */
    public Action<Void, CollectorLeap<?>> pullAllAction() {

        return ACTION_PULL_ALL;
    }

    /**
     * Returns the size of the internal collection.
     *
     * @return The size.
     */
    public int size() {

        return mData.size();
    }

    private boolean isEmpty() {

        return (mData.isEmpty() && mIsComplete);
    }
}