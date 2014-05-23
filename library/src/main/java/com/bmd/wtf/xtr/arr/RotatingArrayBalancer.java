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
package com.bmd.wtf.xtr.arr;

import com.bmd.wtf.src.Spring;

import java.util.List;

/**
 * Implementation of an {@link ArrayBalancer} discharging data by rotating through all the streams
 * in the array.
 * <p/>
 * Created by davide on 5/15/14.
 *
 * @param <DATA> The data type.
 */
public class RotatingArrayBalancer<DATA> extends AbstractArrayBalancer<DATA, DATA> {

    private int mStreamNumber = -1;

    @Override
    public Object onDischarge(final List<Spring<DATA>> springs, final DATA drop) {

        mStreamNumber = (mStreamNumber + 1) % springs.size();

        springs.get(mStreamNumber).discharge(drop);

        return null;
    }
}