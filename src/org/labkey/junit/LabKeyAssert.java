package org.labkey.junit;

import org.junit.Assert;

import java.util.Collection;
import java.util.stream.Collectors;

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
        if (expected != null)
        {
            expected = expected.stream().sorted().collect(Collectors.toList());
        }
        if (actual != null)
        {
            actual = actual.stream().sorted().collect(Collectors.toList());
        }
        Assert.assertEquals(message, expected, actual);
    }
}
