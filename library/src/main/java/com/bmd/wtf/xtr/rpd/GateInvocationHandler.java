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

import com.bmd.wtf.flw.Gate;
import com.bmd.wtf.flw.Gate.Action;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Invocation handler used to handle a gate proxy method invocations.
 * <p/>
 * Created by davide on 7/4/14.
 *
 * @param <TYPE> the leap type.
 */
class GateInvocationHandler<TYPE> implements InvocationHandler, Action<Object, TYPE> {

    private final Gate<TYPE> mGate;

    /**
     * Constructor.
     *
     * @param gate the gate instance.
     */
    public GateInvocationHandler(final Gate<TYPE> gate) {

        mGate = gate;
    }

    @Override
    public Object doOn(final TYPE leap, final Object... args) {

        try {

            return ((Method) args[0]).invoke(leap, (Object[]) args[1]);

        } catch (final InvocationTargetException e) {

            throw new RapidException(e.getCause());

        } catch (final IllegalAccessException e) {

            throw new RapidException(e);
        }
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws
            Throwable {

        return mGate.perform(this, method, args);
    }
}