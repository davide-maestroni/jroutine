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

package com.github.dm.jrt.core.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Constant conditions unit tests.
 * <p/>
 * Created by davide-maestroni on 03/27/2016.
 */
public class ConstantConditionsTest {

    @Test
    public void testNullity() {

        assertThat(ConstantConditions.notNull("test", this)).isEqualTo(this);
        assertThat(ConstantConditions.notNull(this)).isEqualTo(this);
        try {
            ConstantConditions.notNull("test", null);
            fail();

        } catch (final NullPointerException e) {
            assertThat(e.getMessage()).contains("test");
        }

        try {
            ConstantConditions.notNull(null);
            fail();

        } catch (final NullPointerException e) {
            assertThat(e.getMessage()).contains("object");
        }
    }
}
