/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.util;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Ordering;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

/**
 * Use this annotation to control test execution order.<br>
 * Tests will run in order from lowest to highest. Non-annotated tests/classes have an ordering value of zero.<br>
 * In order to use this to control execution order of test methods within a class, annotate the test class with
 * {@code @OrderWith(Order.Sorter.class)} <br><br>
 * Example:<br>
 * <pre>{@code
 *     @OrderWith(Order.Sorter.class)
 *     public class SomeTest
 *     {
 *         @Order(1)
 *         public void firstTest() { }
 *
 *         @Order(2)
 *         public void secondTest() { }
 *     }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Order
{
    double value() default 0.0;

    final class Sorter extends org.junit.runner.manipulation.Sorter implements Ordering.Factory
    {
        public Sorter() {
            super(COMPARATOR);
        }

        @Override
        public Ordering create(Context context) {
            return this;
        }

        private static final Comparator<Description> COMPARATOR = Comparator.comparingDouble(t1 -> {
            Order a1 = t1.getAnnotation(Order.class);
            return a1 != null ? a1.value() : 0.0;
        });
    }
}
