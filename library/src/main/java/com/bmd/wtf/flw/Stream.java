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

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 6/7/14.
 */
public interface Stream<DATA> {

    public Stream<DATA> discharge();

    public Stream<DATA> forward(Throwable throwable);

    public Stream<DATA> push(DATA... drops);

    public Stream<DATA> push(Iterable<? extends DATA> drops);

    public Stream<DATA> push(DATA drop);

    public Stream<DATA> pushAfter(long delay, TimeUnit timeUnit, Iterable<? extends DATA> drops);

    public Stream<DATA> pushAfter(long delay, TimeUnit timeUnit, DATA drop);

    public Stream<DATA> pushAfter(long delay, TimeUnit timeUnit, DATA... drops);
}