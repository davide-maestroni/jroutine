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

package com.github.dm.jrt.channel;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flow unit test.
 * <p>
 * Created by davide-maestroni on 02/24/2016.
 */
public class FlowTest {

  @Test
  public void testEquals() {
    final Flow<Object> flow = new Flow<Object>(0, "test");
    assertThat(flow).isEqualTo(flow);
    assertThat(flow).isNotEqualTo(null);
    assertThat(flow).isNotEqualTo("test");
    assertThat(flow).isNotEqualTo(new Flow<Object>(0, "test1"));
    assertThat(flow).isNotEqualTo(new Flow<Object>(1, "test"));
    assertThat(flow).isEqualTo(new Flow<Object>(0, "test"));
    assertThat(flow.hashCode()).isEqualTo(new Flow<Object>(0, "test").hashCode());
    assertThat(flow.toString()).isEqualTo(new Flow<Object>(0, "test").toString());
  }
}
