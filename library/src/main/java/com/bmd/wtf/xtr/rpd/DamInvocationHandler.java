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
package com.bmd.wtf.xtr.rpd;

import com.bmd.wtf.flw.Dam;
import com.bmd.wtf.flw.Dam.Action;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Invocation handler used to handle a dam proxy method invocations.
 * <p/>
 * Created by davide on 7/4/14.
 *
 * @param <TYPE> the gate type.
 */
class DamInvocationHandler<TYPE> implements InvocationHandler, Action<Object, TYPE> {

    private final Dam<TYPE> mDam;

    /**
     * Constructor.
     *
     * @param dam the dam instance.
     */
    public DamInvocationHandler(final Dam<TYPE> dam) {

        mDam = dam;
    }

    @Override
    public Object doOn(final TYPE gate, final Object... args) {

        try {

            return ((Method) args[0]).invoke(gate, (Object[]) args[1]);

        } catch (final InvocationTargetException e) {

            throw new RapidException(e.getCause());

        } catch (final IllegalAccessException e) {

            throw new RapidException(e);
        }
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws
            Throwable {

        return mDam.perform(this, method, args);
    }
}