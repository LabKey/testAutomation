package org.labkey.test.util;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple class for parsing and comparing version numbers
 */
public class Version implements Comparable<Version>
{
    private final List<Integer> _version;

    private Version(List<Integer> version)
    {
        _version = validate(version);
    }

    public Version(Integer... version)
    {
        this(List.of(version));
    }

    public Version(String version)
    {
        this(Arrays.stream(version
                .split("-", 2)[0] // Remove snapshot suffix
                .split("\\.")) // Split the version into major, minor, patch, etc. parts
                .map(Integer::parseInt).toList());
    }

    public Version(Double version)
    {
        this(version.toString());
    }

    private static List<Integer> validate(List<Integer> versionParts)
    {
        List<Integer> partList = List.copyOf(versionParts);
        if (partList.isEmpty())
        {
            throw new IllegalArgumentException("Version must have at least one part");
        }
        for (Integer part : partList)
        {
            if (part < 0)
            {
                throw new IllegalArgumentException("Version parts must be non-negative");
            }
        }
        return partList;
    }

    public Version trim(int maxParts)
    {
        if (maxParts == _version.size())
            return this;
        else
            return new Version(_version.subList(0, maxParts));
    }

    public int size()
    {
        return _version.size();
    }

    @Override
    public int compareTo(@NotNull Version o)
    {
        int i = 0;
        for (; i < _version.size() && i < o._version.size(); i++)
        {
            Integer versionPart = _version.get(i);
            Integer otherVersionPart = o._version.get(i);
            int result = versionPart.compareTo(otherVersionPart);
            if (result != 0)
            {
                return result;
            }
        }
        // Treat the less specific version as higher
        if (i < _version.size())
            return -1; // this version is more specific
        if (i < o._version.size())
            return 1; // the other version is more specific
        return 0;
    }

    @Override
    public final boolean equals(Object o)
    {
        if (!(o instanceof Version version)) return false;

        return _version.equals(version._version);
    }

    @Override
    public int hashCode()
    {
        return _version.hashCode();
    }

    @Override
    public String toString()
    {
        return _version.stream().map(Object::toString).collect(Collectors.joining("."));
    }


    public static class VersionTest
    {
        @Test
        public void testConstructors()
        {
            Assert.assertEquals("Integer constructor comparison", new Version("25.7"), new Version(25, 7));
            Assert.assertEquals("Integer constructor comparison", new Version("25.7.3"), new Version(25, 7, 3));
            Assert.assertEquals("Double constructor comparison", new Version("25.7"), new Version(25.7));
        }

        @Test
        public void testCompareTo()
        {
            Assert.assertEquals("CompareTo equal version", 0, new Version("25.7").compareTo(new Version("25.7")));
            Assert.assertEquals("CompareTo earlier version", 1, new Version("25.7").compareTo(new Version("25.3")));
            Assert.assertEquals("CompareTo later version", -1, new Version("25.7").compareTo(new Version("25.11")));
            Assert.assertEquals("CompareTo less specific version", -1, new Version("25.7.0").compareTo(new Version("25.7")));
            Assert.assertEquals("CompareTo more specific version", 1, new Version("25.7.0").compareTo(new Version("25.7.0.0")));
        }
    }

}
