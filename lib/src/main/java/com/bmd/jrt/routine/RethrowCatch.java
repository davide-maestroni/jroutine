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
package com.bmd.jrt.routine;

import javax.annotation.Nonnull;

/**
 * Implementation of a catch clause simply rethrowing the caught exception.
 * <p/>
 * Created by davide on 11/8/14.
 */
public class RethrowCatch implements Catch {

    @Override
    public void exception(@Nonnull final RoutineInvocationException ex) {

        throw ex;
    }

    @Override
    public int hashCode() {

        return RethrowCatch.class.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {

        return (obj != null) && (obj.getClass().equals(RethrowCatch.class) || super.equals(obj));
    }
}
