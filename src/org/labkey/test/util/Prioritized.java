package org.labkey.test.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Prioritized
{
    /**
     * Test class priority. Test runner will use this annotation to sort test classes.
     * Higher priority values will run first. Tests will run in order from highest to lowest. Non-annotated tests are
     * treated as having a priority of zero.
     */
    double priority() default 0.0;
}
