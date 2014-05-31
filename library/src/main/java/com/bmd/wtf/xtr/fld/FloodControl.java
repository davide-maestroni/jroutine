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
package com.bmd.wtf.xtr.fld;

import com.bmd.wtf.src.Floodgate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashSet;
import java.util.WeakHashMap;

/**
 * The flood control provides a convenient way of communication with the internal waterfall flows,
 * by inserting into it {@link Levee}s controlled by one or more {@link FloodObserver}s.
 * <p/>
 * The observers are always accessed through the flood controllers to ensure that concurrency
 * issues and synchronization are automatically taking care of.
 * <p/>
 * Created by davide on 5/5/14.
 *
 * @param <IN>         The input data type.
 * @param <OUT>        The output data type.
 * @param <CONTROLLER> The controller type.
 */
public class FloodControl<IN, OUT, CONTROLLER extends FloodObserver<IN, OUT>> {

    private static Method EQUALS_METHOD;

    static {

        try {

            EQUALS_METHOD = Object.class.getDeclaredMethod("equals", Object.class);

        } catch (final NoSuchMethodException e) {

            // Should never happen
        }
    }

    private final Class<CONTROLLER> mFaçade;

    private final WeakHashMap<Levee<IN, OUT, CONTROLLER>, CONTROLLER> mLevees =
            new WeakHashMap<Levee<IN, OUT, CONTROLLER>, CONTROLLER>();

    private final Object mMutex = new Object();

    /**
     * Creates a flood control handled by controllers presenting the specified façade.
     * <p/>
     * Note that only interfaces can be passed as façade.
     *
     * @param façade The controller façade.
     */
    public FloodControl(final Class<CONTROLLER> façade) {

        if (!façade.isInterface()) {

            throw new IllegalArgumentException(façade.getSimpleName() + " is not an interface");
        }

        mFaçade = façade;
    }

    /**
     * Returns one of the controllers managing the levees created by this flood control.
     * <p/>
     * Note that, in case no levee was built through this instance, an exception will be thrown.
     *
     * @return One of the controller instances.
     */
    public CONTROLLER controller() {

        synchronized (mMutex) {

            return mLevees.values().iterator().next();
        }
    }

    /**
     * Returns the list of the controllers managing the levees created by this flood control.
     *
     * @return The list of controllers.
     */
    public Collection<CONTROLLER> controllers() {

        synchronized (mMutex) {

            return new HashSet<CONTROLLER>(mLevees.values());
        }
    }

    /**
     * Returns the controller managing all the levees created through the specified observer
     * instance.
     *
     * @param observer   The observer instance.
     * @param <OBSERVER> The observer type.
     * @return The controller or <code>null</code>.
     */
    public <OBSERVER extends CONTROLLER> CONTROLLER controlling(final OBSERVER observer) {

        if (observer == null) {

            throw new IllegalArgumentException("the observer instance cannot be null");
        }

        synchronized (mMutex) {

            for (final CONTROLLER controller : mLevees.values()) {

                if (observer.equals(((ObserverInvocationHandler) Proxy.getInvocationHandler(controller)).mObserver)) {

                    return controller;
                }
            }

            return null;
        }
    }

    /**
     * Creates a new levee controlled by the specified observer.
     * <p/>
     * Note that the same observer instance can be safely used to control different levees.
     *
     * @param observer   The observer instance.
     * @param <OBSERVER> The observer type.
     * @return The newly created levee.
     */
    public <OBSERVER extends CONTROLLER> Levee<IN, OUT, CONTROLLER> leveeControlledBy(final OBSERVER observer) {

        if (observer == null) {

            throw new IllegalArgumentException("the observer instance cannot be null");
        }

        final Class<CONTROLLER> façade = mFaçade;

        final Object mutex = new Object();

        //noinspection unchecked
        final CONTROLLER controller = (CONTROLLER) Proxy
                .newProxyInstance(façade.getClassLoader(), new Class<?>[]{façade},
                                  new ObserverInvocationHandler(observer, mutex));

        final ObserverLevee<IN, OUT, CONTROLLER> levee =
                new ObserverLevee<IN, OUT, CONTROLLER>(observer, mutex, controller);

        synchronized (mMutex) {

            mLevees.put(levee, controller);
        }

        return levee;
    }

    /**
     * Invocation handler wrapping an observer instance.
     */
    private static class ObserverInvocationHandler implements InvocationHandler {

        private final Object mMutex;

        private FloodObserver<?, ?> mObserver;

        public ObserverInvocationHandler(final FloodObserver<?, ?> observer, final Object mutex) {

            mObserver = observer;
            mMutex = mutex;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

            synchronized (mMutex) {

                if (method.equals(EQUALS_METHOD)) {

                    return areEqual(proxy, args[0]);
                }

                return method.invoke(mObserver, args);
            }
        }

        private boolean areEqual(final Object proxy, final Object other) {

            if (proxy == other) {

                return true;
            }

            if (Proxy.isProxyClass(other.getClass())) {

                final InvocationHandler invocationHandler = Proxy.getInvocationHandler(other);

                if (invocationHandler instanceof ObserverInvocationHandler) {

                    return mObserver.equals(((ObserverInvocationHandler) invocationHandler).mObserver);
                }
            }

            return false;
        }
    }

    /**
     * Levee wrapping an observer instance.
     *
     * @param <IN>         The input data type.
     * @param <OUT>        The output data type.
     * @param <CONTROLLER> The controller tye.
     */
    private static class ObserverLevee<IN, OUT, CONTROLLER extends FloodObserver<IN, OUT>>
            implements Levee<IN, OUT, CONTROLLER> {

        private final CONTROLLER mController;

        private final Object mMutex;

        private final FloodObserver<IN, OUT> mObserver;

        public ObserverLevee(final FloodObserver<IN, OUT> observer, final Object mutex, final CONTROLLER controller) {

            mObserver = observer;
            mMutex = mutex;
            mController = controller;
        }

        @Override
        public CONTROLLER controller() {

            return mController;
        }

        @Override
        public void onDischarge(final Floodgate<IN, OUT> gate, final IN drop) {

            synchronized (mMutex) {

                mObserver.onDischarge(gate, drop);
            }
        }

        @Override
        public void onDrop(final Floodgate<IN, OUT> gate, final Object debris) {

            synchronized (mMutex) {

                mObserver.onDrop(gate, debris);
            }
        }

        @Override
        public void onFlush(final Floodgate<IN, OUT> gate) {

            synchronized (mMutex) {

                mObserver.onFlush(gate);
            }
        }
    }
}