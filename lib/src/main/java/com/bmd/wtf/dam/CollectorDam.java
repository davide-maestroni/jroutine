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
package com.bmd.wtf.dam;

import com.bmd.wtf.src.Floodgate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of a {@link com.bmd.wtf.dam.Dam} which collects all the data and objects flowing
 * through it.
 * <p/>
 * Created by davide on 2/28/14.
 *
 * @param <DATA> The data type.
 */
public class CollectorDam<DATA> extends OpenDam<DATA> {

    private CopyOnWriteArrayList<DATA> mDrops = new CopyOnWriteArrayList<DATA>();

    private CopyOnWriteArrayList<Object> mPulledDebris = new CopyOnWriteArrayList<Object>();

    private CopyOnWriteArrayList<Object> mPushedDebris = new CopyOnWriteArrayList<Object>();

    /**
     * Returns the list of data drops discharged through this <code>Dam</code> in the arrival
     * order, after removing them from the internal storage.
     *
     * @return The list of data drops.
     */
    public List<DATA> collect() {

        final ArrayList<DATA> values = new ArrayList<DATA>(mDrops);

        mDrops.removeAll(values);

        return values;
    }

    /**
     * Returns the next data drop discharged through this <code>Dam</code> in the arrival order,
     * after removing it from the internal storage.
     *
     * @return The data drop or <code>null</code>.
     */
    public DATA collectNext() {

        try {

            return mDrops.remove(0);

        } catch (final IndexOutOfBoundsException e) {

            // Ignore it
        }

        return null;
    }

    /**
     * Returns the next debris pulled upstream through this dam in the arrival order, after
     * removing it from the internal storage.
     *
     * @return The pulled debris or <code>null</code>.
     */
    public Object collectNextPulledDebris() {

        try {

            return mPulledDebris.remove(0);

        } catch (final IndexOutOfBoundsException e) {

            // Ignore it
        }

        return null;
    }

    /**
     * Returns the next debris pushed downstream through this dam in the arrival order, after
     * removing it from the internal storage.
     *
     * @return The pushed debris or <code>null</code>.
     */
    public Object collectNextPushedDebris() {

        try {

            return mPushedDebris.remove(0);

        } catch (final IndexOutOfBoundsException e) {

            // Ignore it
        }

        return null;
    }

    /**
     * Returns the list of debris pulled upstream through this dam in the arrival order, after
     * removing them from the internal storage.
     *
     * @return The list of pulled debris.
     */
    public List<Object> collectPulledDebris() {

        final ArrayList<Object> objects = new ArrayList<Object>(mPulledDebris);

        mPulledDebris.removeAll(objects);

        return objects;
    }

    /**
     * Returns the list of debris pushed downstream through this dam in the arrival order, after
     * removing them from the internal storage.
     *
     * @return The list of pushed debris.
     */
    public List<Object> collectPushedDebris() {

        final ArrayList<Object> objects = new ArrayList<Object>(mPushedDebris);

        mPushedDebris.removeAll(objects);

        return objects;
    }

    @Override
    public Object onDischarge(final Floodgate<DATA, DATA> gate, final DATA drop) {

        mDrops.add(drop);

        return super.onDischarge(gate, drop);
    }

    @Override
    public Object onPullDebris(final Floodgate<DATA, DATA> gate, final Object debris) {

        mPulledDebris.add(debris);

        return super.onPullDebris(gate, debris);
    }

    @Override
    public Object onPushDebris(final Floodgate<DATA, DATA> gate, final Object debris) {

        mPushedDebris.add(debris);

        return super.onPushDebris(gate, debris);
    }
}