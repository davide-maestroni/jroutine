/*
 * Copyright (c) 2016. Davide Maestroni
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

package com.github.dm.jrt.stream.util;

import com.github.dm.jrt.stream.util.Backoff.ConstantBackoff;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.core.util.UnitDuration.millis;
import static com.github.dm.jrt.core.util.UnitDuration.seconds;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Backoff utility unit tests.
 * <p>
 * Created by davide-maestroni on 05/08/2016.
 */
public class BackoffTest {

    @Test
    public void testConstant() {

        final ConstantBackoff backoff = Backoff.times(2);
        assertThat(backoff.apply(1, null)).isEqualTo(0);
        assertThat(backoff.apply(2, null)).isEqualTo(0);
        assertThat(backoff.apply(3, null)).isNull();
        final Backoff backoff1 = Backoff.times(2).constantDelay(1, TimeUnit.SECONDS);
        assertThat(backoff1.apply(1, null)).isEqualTo(1000);
        assertThat(backoff1.apply(2, null)).isEqualTo(1000);
        assertThat(backoff1.apply(3, null)).isNull();
        final Backoff backoff2 = Backoff.times(3).constantDelay(seconds(1));
        assertThat(backoff2.apply(1, null)).isEqualTo(1000);
        assertThat(backoff2.apply(2, null)).isEqualTo(1000);
        assertThat(backoff2.apply(3, null)).isEqualTo(1000);
        assertThat(backoff2.apply(4, null)).isNull();
    }

    @Test
    public void testConstantCapped() {

        final Backoff backoff = Backoff.times(2).cappedTo(100, TimeUnit.MILLISECONDS);
        assertThat(backoff.apply(1, null)).isEqualTo(0);
        assertThat(backoff.apply(2, null)).isEqualTo(0);
        assertThat(backoff.apply(3, null)).isNull();
        final Backoff backoff1 =
                Backoff.times(2).constantDelay(1, TimeUnit.SECONDS).cappedTo(millis(500));
        assertThat(backoff1.apply(1, null)).isEqualTo(500);
        assertThat(backoff1.apply(2, null)).isEqualTo(500);
        assertThat(backoff1.apply(3, null)).isNull();
        final Backoff backoff2 = Backoff.times(3).constantDelay(seconds(1)).cappedTo(millis(700));
        assertThat(backoff2.apply(1, null)).isEqualTo(700);
        assertThat(backoff2.apply(2, null)).isEqualTo(700);
        assertThat(backoff2.apply(3, null)).isEqualTo(700);
        assertThat(backoff2.apply(4, null)).isNull();
    }

    @Test
    public void testConstantCappedJitted() {

        final Backoff backoff = Backoff.times(2).cappedTo(millis(100)).withJitter(0.5f);
        assertThat(backoff.apply(1, null)).isEqualTo(0);
        assertThat(backoff.apply(2, null)).isEqualTo(0);
        assertThat(backoff.apply(3, null)).isNull();
        final Backoff backoff1 =
                Backoff.times(2).constantDelay(seconds(1)).cappedTo(millis(500)).withJitter(0.5f);
        assertThat(backoff1.apply(1, null)).isBetween(250L, 500L);
        assertThat(backoff1.apply(2, null)).isBetween(250L, 500L);
        assertThat(backoff1.apply(3, null)).isNull();
        final Backoff backoff2 =
                Backoff.times(3).constantDelay(seconds(1)).withJitter(0.3f).cappedTo(millis(700));
        assertThat(backoff2.apply(1, null)).isEqualTo(700);
        assertThat(backoff2.apply(2, null)).isEqualTo(700);
        assertThat(backoff2.apply(3, null)).isEqualTo(700);
        assertThat(backoff2.apply(4, null)).isNull();
    }

    @Test
    public void testConstantJitted() {

        final Backoff backoff = Backoff.times(2).withJitter(1f);
        assertThat(backoff.apply(1, null)).isEqualTo(0);
        assertThat(backoff.apply(2, null)).isEqualTo(0);
        assertThat(backoff.apply(3, null)).isNull();
        final Backoff backoff1 =
                Backoff.times(2).constantDelay(1, TimeUnit.SECONDS).withJitter(0.7f);
        assertThat(backoff1.apply(1, null)).isBetween(300L, 1000L);
        assertThat(backoff1.apply(2, null)).isBetween(300L, 1000L);
        assertThat(backoff1.apply(3, null)).isNull();
        final Backoff backoff2 = Backoff.times(3).constantDelay(seconds(1)).withJitter(0.4f);
        assertThat(backoff2.apply(1, null)).isBetween(600L, 1000L);
        assertThat(backoff2.apply(2, null)).isBetween(600L, 1000L);
        assertThat(backoff2.apply(3, null)).isBetween(600L, 1000L);
        assertThat(backoff2.apply(4, null)).isNull();
    }
}
