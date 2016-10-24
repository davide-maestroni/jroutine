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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Throttling runner unit test.
 * <p>
 * Created by davide-maestroni on 09/03/2016.
 */
public class ThrottlingTest {

  @Test
  public void testThrottling() throws InterruptedException {
    final Semaphore semaphore = new Semaphore(0);
    final TestExecution execution = new TestExecution(semaphore);
    final Runner runner = Runners.throttlingRunner(Runners.sharedRunner(), 1);
    for (int i = 0; i < 100; i++) {
      runner.run(execution, 0, TimeUnit.MILLISECONDS);
    }

    assertThat(semaphore.tryAcquire(100, 20, TimeUnit.SECONDS)).isTrue();
    assertThat(TestExecution.sFailed.get()).isFalse();
  }

  private static class TestExecution implements Execution {

    private static AtomicInteger sCount = new AtomicInteger();

    private static AtomicBoolean sFailed = new AtomicBoolean();

    private final Semaphore mSemaphore;

    TestExecution(final Semaphore semaphore) {
      mSemaphore = semaphore;
    }

    public void run() {
      final int before = sCount.incrementAndGet();
      if (before > 1) {
        sFailed.set(true);
      }

      try {
        millis(100).sleepAtLeast();

      } catch (Throwable ignored) {
      }

      final int after = sCount.decrementAndGet();
      if (after > 0) {
        sFailed.set(true);
      }

      mSemaphore.release();
    }
  }
}
