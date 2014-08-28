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
package com.bmd.wtf.flw;

/**
 * A fall instance gives a current a way to push data into the waterfall flow.
 * <p/>
 * Its methods must be called as a result of the invocation of the relative current ones, inside
 * the thread or queue handled by the specific implementation.
 * <p/>
 * Created by davide on 6/7/14.
 *
 * @param <DATA> the data type.
 */
public interface Fall<DATA> {

    /**
     * Forwards the specified unhandled exception into the waterfall flow.
     *
     * @param throwable the thrown exception.
     */
    public void exception(Throwable throwable);

    /**
     * Flushes the fall flow, that is, it informs the fed streams that no more data drops are
     * likely to come.
     *
     * @param origin the origin stream.
     */
    public void flush(Stream<DATA> origin);

    /**
     * Pushes the specified data into the waterfall flow.
     *
     * @param drop the data drop.
     */
    public void push(DATA drop);
}