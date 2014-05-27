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
 * Implementation of a {@link Dam} which collects all the data and objects flowing through it.
 * <p/>
 * Created by davide on 2/28/14.
 *
 * @param <DATA> The data type.
 */
public class CollectorDam<DATA> extends OpenDam<DATA> {

    private CopyOnWriteArrayList<Object> mDebris = new CopyOnWriteArrayList<Object>();

    private CopyOnWriteArrayList<DATA> mDrops = new CopyOnWriteArrayList<DATA>();

    /**
     * Returns the list of data drops discharged through this dam in the arrival order, after
     * removing them from the internal storage.
     *
     * @return The list of data drops.
     */
    public List<DATA> collect() {

        final ArrayList<DATA> values = new ArrayList<DATA>(mDrops);

        mDrops.removeAll(values);

        return values;
    }

    /**
     * Returns the list of debris dropped downstream through this dam in the arrival order, after
     * removing them from the internal storage.
     *
     * @return The list of pushed debris.
     */
    public List<Object> collectDebris() {

        final ArrayList<Object> objects = new ArrayList<Object>(mDebris);

        mDebris.removeAll(objects);

        return objects;
    }

    /**
     * Returns the next data drop discharged through this dam in the arrival order, after removing
     * it from the internal storage.
     *
     * @return The data drop or <code>null</code>.
     */
    public DATA collectNext() {

        try {

            return mDrops.remove(0);

        } catch (final IndexOutOfBoundsException ignored) {

        }

        return null;
    }

    /**
     * Returns the next debris dropped downstream through this dam in the arrival order, after
     * removing it from the internal storage.
     *
     * @return The pushed debris or <code>null</code>.
     */
    public Object collectNextDebris() {

        try {

            return mDebris.remove(0);

        } catch (final IndexOutOfBoundsException ignored) {

        }

        return null;
    }

    @Override
    public void onDischarge(final Floodgate<DATA, DATA> gate, final DATA drop) {

        mDrops.add(drop);

        super.onDischarge(gate, drop);
    }

    @Override
    public void onDrop(final Floodgate<DATA, DATA> gate, final Object debris) {

        mDebris.add(debris);

        super.onDrop(gate, debris);
    }
}