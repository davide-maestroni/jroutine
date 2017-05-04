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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Scheduled executor unit tests.
 * <p>
 * Created by davide-maestroni on 05/24/2016.
 */
public class ScheduledThreadPoolExecutorServiceTest {

  @Test
  public void testEquals() {

    final ExecutorService pool = Executors.newCachedThreadPool();
    final ScheduledThreadPoolExecutorService executor =
        new ScheduledThreadPoolExecutorService(pool);
    assertThat(executor).isEqualTo(executor);
    assertThat(executor).isNotEqualTo(null);
    assertThat(executor).isNotEqualTo("test");
    assertThat(executor).isNotEqualTo(
        new ScheduledThreadPoolExecutorService(Executors.newCachedThreadPool()));
    assertThat(executor).isEqualTo(new ScheduledThreadPoolExecutorService(pool));
    assertThat(executor.hashCode()).isEqualTo(
        new ScheduledThreadPoolExecutorService(pool).hashCode());
  }

  @Test
  public void testUnsupportedMethods() throws InterruptedException {

    final ScheduledThreadPoolExecutorService executor =
        new ScheduledThreadPoolExecutorService(Executors.newCachedThreadPool());
    try {
      executor.schedule(new Callable<Object>() {

        public Object call() throws Exception {

          return null;
        }
      }, 0, TimeUnit.MILLISECONDS);
      fail();

    } catch (final UnsupportedOperationException ignored) {

    }

    final Semaphore semaphore = new Semaphore(0);
    executor.scheduleAtFixedRate(new Runnable() {

      public void run() {
        semaphore.release();
      }
    }, 0, 1, TimeUnit.SECONDS);
    semaphore.acquire();

    executor.scheduleWithFixedDelay(new Runnable() {

      public void run() {
        semaphore.release();
      }
    }, 0, 1, TimeUnit.SECONDS);
    semaphore.acquire();
    executor.shutdown();
  }
}
