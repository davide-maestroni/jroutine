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

package com.github.dm.jrt.android.core;

import android.test.AndroidTestCase;

import com.github.dm.jrt.android.core.ContextInvocationTarget.ClassContextInvocationTarget;
import com.github.dm.jrt.android.core.ContextInvocationTarget.ObjectContextInvocationTarget;

import static com.github.dm.jrt.android.core.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.core.ContextInvocationTarget.instanceOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context invocation target unit test.
 * <p/>
 * Created by davide-maestroni on 09/14/2015.
 */
public class ContextInvocationTargetTest extends AndroidTestCase {

    public void testClassTarget() {

        final ClassContextInvocationTarget<TargetClass> target = classOfType(TargetClass.class);
        assertThat(target.getTargetClass()).isSameAs(TargetClass.class);
        assertThat(target.isAssignableTo(TargetClass.class)).isTrue();
        assertThat(target.isOfType(TargetClass.class)).isTrue();
        assertThat(target.isAssignableTo(TestClass.class)).isTrue();
        assertThat(target.isOfType(TestClass.class)).isFalse();
        assertThat(target.isAssignableTo(String.class)).isFalse();
        assertThat(target.isOfType(String.class)).isFalse();
    }

    public void testClassTargetEquals() {

        final ClassContextInvocationTarget<TargetClass> target = classOfType(TargetClass.class);
        assertThat(target).isEqualTo(target);
        assertThat(target).isNotEqualTo("");
        assertThat(target.hashCode()).isEqualTo(classOfType(TargetClass.class).hashCode());
        assertThat(target).isEqualTo(classOfType(TargetClass.class));
        assertThat(target.hashCode()).isNotEqualTo(classOfType(TestClass.class).hashCode());
        assertThat(target).isNotEqualTo(classOfType(TestClass.class));
    }

    @SuppressWarnings("ConstantConditions")
    public void testClassTargetError() {

        try {

            classOfType(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testObjectTarget() {

        final ObjectContextInvocationTarget<TargetClass> target = instanceOf(TargetClass.class);
        assertThat(target.getTargetClass()).isSameAs(TargetClass.class);
        assertThat(target.isAssignableTo(TargetClass.class)).isTrue();
        assertThat(target.isOfType(TargetClass.class)).isTrue();
        assertThat(target.isAssignableTo(TestClass.class)).isTrue();
        assertThat(target.isOfType(TestClass.class)).isTrue();
        assertThat(target.isAssignableTo(String.class)).isFalse();
        assertThat(target.isOfType(String.class)).isFalse();
    }

    public void testObjectTargetEquals() {

        final ObjectContextInvocationTarget<TargetClass> target = instanceOf(TargetClass.class);
        assertThat(target).isEqualTo(target);
        assertThat(target).isNotEqualTo("");
        assertThat(target.hashCode()).isEqualTo(instanceOf(TargetClass.class).hashCode());
        assertThat(target).isEqualTo(instanceOf(TargetClass.class));
        assertThat(target.hashCode()).isNotEqualTo(instanceOf(TestClass.class).hashCode());
        assertThat(target).isNotEqualTo(instanceOf(TestClass.class));
    }

    @SuppressWarnings("ConstantConditions")
    public void testObjectTargetError() {

        try {

            instanceOf(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class TargetClass extends TestClass {

    }

    private static class TestClass {

    }
}
