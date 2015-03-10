/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.invocation;

import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.ClassToken;

import junit.framework.TestCase;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invocations unit tests.
 * <p/>
 * Created by davide on 2/16/15.
 */
public class InvocationsTest extends TestCase {

    @SuppressWarnings({"NullArgumentToVariableArgMethod", "ConstantConditions"})
    public void testInvocationFactory() {

        final InvocationFactory<Object, Object> factory =
                Invocations.withArgs((Object[]) null).factoryOf(TestInvocation.class);

        assertThat(factory.newInvocation()).isExactlyInstanceOf(TestInvocation.class);

        try {

            Invocations.factoryOf((ClassToken<TestInvocation>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Invocations.factoryOf((Class<TestInvocation>) null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            Invocations.factoryOf((TestInvocation) null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class TestInvocation extends StatelessInvocation<Object, Object> {

        @Override
        public void onInput(final Object o, @Nonnull final ResultChannel<Object> result) {

        }
    }
}
