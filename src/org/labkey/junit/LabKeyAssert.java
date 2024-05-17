package org.labkey.junit;

import org.assertj.core.api.Assertions;

import java.util.Collection;

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
        Assertions.assertThat(actual).as(message).containsExactlyInAnyOrderElementsOf(expected);
    }
}
