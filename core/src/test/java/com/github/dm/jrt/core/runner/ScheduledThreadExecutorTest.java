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

package com.github.dm.jrt.core.runner;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Scheduled executor unit tests.
 * <p>
 * Created by davide-maestroni on 05/24/2016.
 */
public class ScheduledThreadExecutorTest {

    @Test
    public void testEquals() {

        final ExecutorService pool = Executors.newCachedThreadPool();
        final ScheduledThreadExecutor executor = new ScheduledThreadExecutor(pool);
        assertThat(executor).isEqualTo(executor);
        assertThat(executor).isNotEqualTo(null);
        assertThat(executor).isNotEqualTo("test");
        assertThat(executor).isNotEqualTo(
                new ScheduledThreadExecutor(Executors.newCachedThreadPool()));
        assertThat(executor).isEqualTo(new ScheduledThreadExecutor(pool));
        assertThat(executor.hashCode()).isEqualTo(new ScheduledThreadExecutor(pool).hashCode());
    }

    @Test
    public void testUnsupportedMethods() {

        final ScheduledThreadExecutor executor =
                new ScheduledThreadExecutor(Executors.newCachedThreadPool());
        try {
            executor.schedule(new Callable<Object>() {

                public Object call() throws Exception {

                    return null;
                }
            }, 0, TimeUnit.MILLISECONDS);
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        try {
            executor.scheduleAtFixedRate(new Runnable() {

                public void run() {

                }
            }, 0, 1, TimeUnit.SECONDS);
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }

        try {
            executor.scheduleWithFixedDelay(new Runnable() {

                public void run() {

                }
            }, 0, 1, TimeUnit.SECONDS);
            fail();

        } catch (final UnsupportedOperationException ignored) {

        }
    }
}
