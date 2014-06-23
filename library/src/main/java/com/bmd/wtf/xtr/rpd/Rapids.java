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

import com.bmd.wtf.crr.Current;
import com.bmd.wtf.crr.CurrentGenerator;
import com.bmd.wtf.fll.Classification;
import com.bmd.wtf.fll.Waterfall;
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

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> asGenerator(
            final Leap<SOURCE, IN, OUT>... leaps) {

        return RapidGenerators.leapGenerator(leaps);
    }

    public static CurrentGenerator asGenerator(final Current... currents) {

        return RapidGenerators.currentGenerator(currents);
    }

    public static CurrentGenerator currentGenerator(final Object generator,
            final Classification<? extends Current> classification, final Object... args) {

        return RapidGenerators.currentGenerator(generator, classification, args);
    }

    public static CurrentGenerator currentGenerator(final Current current,
            final Object... contextArgs) {

        return RapidGenerators.currentGenerator(current.getClass(), contextArgs);
    }

    public static CurrentGenerator currentGenerator(
            final Classification<? extends Current> classification, final Object... contextArgs) {

        return RapidGenerators.currentGenerator(classification, contextArgs);
    }

    public static CurrentGenerator currentGenerator(final Class<? extends Current> type,
            final Object... contextArgs) {

        return RapidGenerators.currentGenerator(type, contextArgs);
    }

    public static CurrentGenerator currentGenerator(final Class<? extends Current> type) {

        return RapidGenerators.currentGenerator(type);
    }

    public static CurrentGenerator currentGenerator(
            final Classification<? extends Current> classification) {

        return RapidGenerators.currentGenerator(classification);
    }

    public static <SOURCE, MOUTH, IN, OUT, TYPE> RapidGate<SOURCE, MOUTH, IN, OUT, TYPE> gate(
            final Waterfall<SOURCE, MOUTH, OUT> waterfall) {

        return new WaterfallRapidGate<SOURCE, MOUTH, IN, OUT, TYPE>(waterfall);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Object generator,
            final Classification<? extends Leap<SOURCE, IN, OUT>> classification,
            final Object... args) {

        return RapidGenerators.leapGenerator(generator, classification, args);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Leap<SOURCE, IN, OUT> leap, final Object... contextArgs) {

        //noinspection unchecked
        return RapidGenerators
                .leapGenerator((Class<? extends Leap<SOURCE, IN, OUT>>) leap.getClass(),
                               contextArgs);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Classification<? extends Leap<SOURCE, IN, OUT>> classification,
            final Object... contextArgs) {

        return RapidGenerators.leapGenerator(classification, contextArgs);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Class<? extends Leap<SOURCE, IN, OUT>> type, final Object... contextArgs) {

        return RapidGenerators.leapGenerator(type, contextArgs);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Class<? extends Leap<SOURCE, IN, OUT>> type) {

        return RapidGenerators.leapGenerator(type);
    }

    public static <SOURCE, IN, OUT> LeapGenerator<SOURCE, IN, OUT> leapGenerator(
            final Classification<? extends Leap<SOURCE, IN, OUT>> classification) {

        return RapidGenerators.leapGenerator(classification);
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Condition {}

    @Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Generator {}
}