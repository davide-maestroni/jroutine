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
package com.bmd.wtf.xtr.dam;

import java.util.List;

/**
 * Created by davide on 6/11/14.
 */
public interface CollectorBasin<SOURCE, DATA> {

    public CollectorBasin<SOURCE, DATA> all();

    public CollectorBasin<SOURCE, DATA> collectData(List<DATA> bucket);

    public CollectorBasin<SOURCE, DATA> collectData(int streamNumber, List<DATA> bucket);

    public CollectorBasin<SOURCE, DATA> collectUnhandled(List<Throwable> bucket);

    public CollectorBasin<SOURCE, DATA> collectUnhandled(int streamNumber, List<Throwable> bucket);

    public CollectorBasin<SOURCE, DATA> empty();

    public CollectorBasin<SOURCE, DATA> max(int maxCount);

    public DATA pullData();

    public DATA pullData(int streamNumber);

    public Throwable pullUnhandled();

    public Throwable pullUnhandled(int streamNumber);
}