/*
 * Copyright 2017 Davide Maestroni
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

package com.github.dm.jrt.core.executor;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * Dynamic scheduled executor unit tests.
 * <p>
 * Created by davide-maestroni on 05/14/2016.
 */
public class DynamicScheduledThreadPoolExecutorServiceTest {

  @Test
  public void testUnsupportedMethods() {

    final DynamicScheduledThreadPoolExecutorService executor =
        new DynamicScheduledThreadPoolExecutorService(1, 1, 1, TimeUnit.SECONDS);
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
