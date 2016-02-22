/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.object.common;

/**
 * Interface defining a reentrant mutex.
 * <p/>
 * Created by davide-maestroni on 10/01/2015.
 */
public interface Mutex {

    /**
     * Empty mutex implementation.<br/>
     * This mutex does not implement any kind of synchronization.
     */
    Mutex NO_MUTEX = new Mutex() {

        public void acquire() {}

        public void release() {}
    };

    /**
     * Acquires this mutex.<br/>
     * The calling thread will block until the resource becomes available.
     *
     * @throws java.lang.InterruptedException if the current thread is interrupted.
     */
    void acquire() throws InterruptedException;

    /**
     * Releases this mutex.<br/>
     * If the calling thread is not the holder of the mutex, an exception will be thrown.
     */
    void release();
}
