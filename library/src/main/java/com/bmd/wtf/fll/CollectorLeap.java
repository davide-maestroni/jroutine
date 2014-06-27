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

import com.bmd.wtf.flw.Gate.Action;
import com.bmd.wtf.flw.River;
import com.bmd.wtf.lps.FreeLeap;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by davide on 6/13/14.
 */
class CollectorLeap<SOURCE, DATA> extends FreeLeap<SOURCE, DATA> {

    private static final Action<Boolean, CollectorLeap<?, ?>> ACTION_EMPTY =
            new Action<Boolean, CollectorLeap<?, ?>>() {

                @Override
                public Boolean doOn(final CollectorLeap<?, ?> collector, final Object... args) {

                    return collector.isEmpty();
                }
            };

    private final Action<DATA, CollectorLeap<SOURCE, DATA>> ACTION_PULL =
            new Action<DATA, CollectorLeap<SOURCE, DATA>>() {

                @Override
                public DATA doOn(final CollectorLeap<SOURCE, DATA> collector,
                        final Object... args) {

                    return collector.mData.remove(0);
                }
            };

    private final CopyOnWriteArrayList<DATA> mData = new CopyOnWriteArrayList<DATA>();

    private static final Action<Void, CollectorLeap<?, ?>> ACTION_PULL_ALL =
            new Action<Void, CollectorLeap<?, ?>>() {

                @Override
                public Void doOn(final CollectorLeap<?, ?> collector, final Object... args) {

                    final CopyOnWriteArrayList<?> data = collector.mData;

                    //noinspection unchecked
                    ((List) args[0]).addAll(data);

                    data.clear();

                    return null;
                }
            };

    private boolean mIsComplete;

    public boolean isComplete() {

        return mIsComplete;
    }

    public boolean isEmpty() {

        return (mData.isEmpty() && mIsComplete);
    }

    public Action<Boolean, CollectorLeap<?, ?>> isEmptyAction() {

        return ACTION_EMPTY;
    }

    @Override
    public void onDischarge(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber) {

        upRiver.deviate();
        downRiver.deviate();

        mIsComplete = true;
    }

    @Override
    public void onPush(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber, final DATA drop) {

        mData.add(drop);
    }

    @Override
    public void onUnhandled(final River<SOURCE, DATA> upRiver, final River<SOURCE, DATA> downRiver,
            final int fallNumber, final Throwable throwable) {

        upRiver.deviate();
        downRiver.deviate();

        mIsComplete = true;
    }

    public Action<DATA, CollectorLeap<SOURCE, DATA>> pullAction() {

        return ACTION_PULL;
    }

    public Action<Void, CollectorLeap<?, ?>> pullAllAction() {

        return ACTION_PULL_ALL;
    }

    public int size() {

        return mData.size();
    }
}