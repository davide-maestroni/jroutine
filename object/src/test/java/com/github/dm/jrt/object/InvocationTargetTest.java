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

package com.github.dm.jrt.object;

import org.junit.Test;

import static com.github.dm.jrt.object.InvocationTarget.classOfType;
import static com.github.dm.jrt.object.InvocationTarget.instance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Invocation target unit test.
 * <p>
 * Created by davide-maestroni on 08/22/2015.
 */
public class InvocationTargetTest {

    @Test
    public void testClassTarget() {

        final InvocationTarget<TargetClass> target = classOfType(TargetClass.class);
        assertThat(target.getTarget()).isSameAs(TargetClass.class);
        assertThat(target.getTargetClass()).isSameAs(TargetClass.class);
        assertThat(target.isAssignableTo(TargetClass.class)).isTrue();
        assertThat(target.isOfType(TargetClass.class)).isTrue();
        assertThat(target.isAssignableTo(TestClass.class)).isTrue();
        assertThat(target.isOfType(TestClass.class)).isFalse();
        assertThat(target.isAssignableTo(String.class)).isFalse();
        assertThat(target.isOfType(String.class)).isFalse();
    }

    @Test
    public void testClassTargetEquals() {

        final InvocationTarget<TargetClass> target = classOfType(TargetClass.class);
        assertThat(target).isEqualTo(target);
        assertThat(target).isNotEqualTo("");
        assertThat(target.hashCode()).isEqualTo(classOfType(TargetClass.class).hashCode());
        assertThat(target).isEqualTo(classOfType(TargetClass.class));
        assertThat(target.hashCode()).isNotEqualTo(classOfType(TestClass.class).hashCode());
        assertThat(target).isNotEqualTo(classOfType(TestClass.class));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testClassTargetError() {

        try {

            classOfType(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testInstanceTarget() {

        final TargetClass t = new TargetClass();
        final InvocationTarget<TargetClass> target = instance(t);
        assertThat(target.getTarget()).isSameAs(t);
        assertThat(target.getTargetClass()).isSameAs(TargetClass.class);
        assertThat(target.isAssignableTo(TargetClass.class)).isTrue();
        assertThat(target.isOfType(TargetClass.class)).isTrue();
        assertThat(target.isAssignableTo(TestClass.class)).isTrue();
        assertThat(target.isOfType(TestClass.class)).isTrue();
        assertThat(target.isAssignableTo(String.class)).isFalse();
        assertThat(target.isOfType(String.class)).isFalse();
    }

    @Test
    public void testInstanceTargetEquals() {

        final TargetClass t = new TargetClass();
        final InvocationTarget<TargetClass> target = instance(t);
        assertThat(target).isEqualTo(target);
        assertThat(target).isNotEqualTo("");
        assertThat(target.hashCode()).isEqualTo(instance(t).hashCode());
        assertThat(target).isEqualTo(instance(t));
        assertThat(target.hashCode()).isNotEqualTo(instance(new TestClass()).hashCode());
        assertThat(target).isNotEqualTo(instance(new TestClass()));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInstanceTargetError() {

        try {

            instance(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class TargetClass extends TestClass {

    }

    private static class TestClass {

    }
}
