/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.wtf.xtr.rpd;

import com.bmd.wtf.flg.Gate;
import com.bmd.wtf.lps.Leap;
import com.bmd.wtf.lps.LeapGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by davide on 6/19/14.
 */
public class Rapids {

    private Rapids() {

    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> generator(
            final Gate<? extends Leap<SOURCE, IN, OUT>> gate, final Object... contextArgs) {

        return RapidGenerators.generator(gate, contextArgs);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> generator(
            final Leap<SOURCE, IN, OUT>... leaps) {

        return RapidGenerators.generator(leaps);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> generator(
            final Class<? extends Leap<SOURCE, IN, OUT>> type, final Object... contextArgs) {

        return RapidGenerators.generator(type, contextArgs);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> generator(
            final Class<? extends Leap<SOURCE, IN, OUT>> type) {

        return RapidGenerators.generator(type);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> generator(
            final Gate<? extends Leap<SOURCE, IN, OUT>> gate) {

        return RapidGenerators.generator(gate);
    }

    @Target({ElementType.CONSTRUCTOR})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Generator {

    }
}