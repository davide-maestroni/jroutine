/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.channel;

/**
 * Data class storing information about the origin of the data.
 * <p/>
 * Created by davide-maestroni on 02/26/2016.
 *
 * @param <DATA> the data type.
 */
public class Selectable<DATA> {

    /**
     * The data object.
     */
    public final DATA data;

    /**
     * The origin channel index.
     */
    public final int index;

    /**
     * Constructor.
     *
     * @param data  the data object.
     * @param index the channel index.
     */
    public Selectable(final DATA data, final int index) {

        this.data = data;
        this.index = index;
    }

    /**
     * Returns the data object casted to the specific type.
     *
     * @param <TYPE> the data type.
     * @return the data object.
     */
    @SuppressWarnings("unchecked")
    public <TYPE extends DATA> TYPE data() {

        return (TYPE) data;
    }

    @Override
    public int hashCode() {

        // AUTO-GENERATED CODE
        int result = data != null ? data.hashCode() : 0;
        result = 31 * result + index;
        return result;
    }

    @Override
    public boolean equals(final Object o) {

        // AUTO-GENERATED CODE
        if (this == o) {
            return true;
        }

        if (!(o instanceof Selectable)) {
            return false;
        }

        final Selectable<?> that = (Selectable<?>) o;
        return index == that.index && !(data != null ? !data.equals(that.data) : that.data != null);
    }

    @Override
    public String toString() {

        // AUTO-GENERATED CODE
        return "Selectable{" +
                "data=" + data +
                ", index=" + index +
                '}';
    }
}
