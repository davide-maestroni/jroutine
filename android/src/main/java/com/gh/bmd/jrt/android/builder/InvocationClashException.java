/*
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
package com.gh.bmd.jrt.android.builder;

import com.gh.bmd.jrt.common.RoutineException;

/**
 * Exception indicating a clash of routine invocations with the same ID.
 * <p/>
 * Created by davide-maestroni on 12/14/14.
 */
public class InvocationClashException extends RoutineException {

    private final int mId;

    /**
     * Constructor.
     *
     * @param id the loader ID.
     */
    public InvocationClashException(final int id) {

        mId = id;
    }

    /**
     * Returns the loader ID.
     *
     * @return the loader ID.
     */
    public int getId() {

        return mId;
    }
}
