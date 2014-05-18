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
package com.bmd.wtf.src;

/**
 * A pool instance gives a {@link com.bmd.wtf.crr.Current} a way to discharge data into the
 * {@link com.bmd.wtf.Waterfall} flow both upstream and downstream.
 * <p/>
 * Its methods must be called as a result of the invocation of the relative current ones, inside
 * the thread or queue handled by the specific implementation.
 * <p/>
 * Created by davide on 2/27/14.
 *
 * @param <DATA> The data type.
 */
public interface Pool<DATA> {

    /**
     * Discharges the specified data drop into the waterfall.
     *
     * @param drop The drop of data to discharge.
     */
    public void discharge(DATA drop);

    /**
     * Flushes the pool, that is, it informs the fed streams that no more data drops are likely to
     * come.
     */
    public void flush();

    /**
     * Pulls upstream the specified debris.
     *
     * @param debris The debris instance to pull.
     */
    public void pull(Object debris);

    /**
     * Pushes downstream the specified debris.
     *
     * @param debris The debris instance to push.
     */
    public void push(Object debris);
}