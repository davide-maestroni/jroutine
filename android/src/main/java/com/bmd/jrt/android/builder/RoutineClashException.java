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
package com.bmd.jrt.android.builder;

import com.bmd.jrt.common.RoutineException;

/**
 * Exception indicating a clash of routine invocations with the same ID.
 * <p/>
 * Created by davide on 12/14/14.
 */
public class RoutineClashException extends RoutineException {

    private final int mId;

    /**
     * Constructor.
     *
     * @param id the invocation ID.
     */
    public RoutineClashException(final int id) {

        super(null);

        mId = id;
    }

    /**
     * Returns the invocation ID.
     *
     * @return the invocation ID.
     */
    public int getId() {

        return mId;
    }

    @Override
    public boolean needsUnwrap() {

        return false;
    }
}
