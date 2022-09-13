package org.labkey.junit;

import org.assertj.core.api.Assertions;
import org.junit.Assert;

import java.util.Collection;
import java.util.Collections;

public class LabKeyAssert
{
    private LabKeyAssert()
    {
        // prevent instantiation
    }

    /**
     * Assert that two collections are equal when sorted.
     */
    public static <T> void assertEqualsSorted(String message, Collection<T> expected, Collection<T> actual)
    {
        if (expected == null || actual == null)
        {
            Assert.assertEquals(message, expected, actual);
        }
        else
        {
            // Can't call 'toArray' on a generic collection and retain type info. Convert to object collection.
            Collection<Object> actualObjects = Collections.unmodifiableCollection(actual);
            Assertions.assertThat(actualObjects).as(message).containsExactlyInAnyOrder(expected.toArray());
        }
    }
}
