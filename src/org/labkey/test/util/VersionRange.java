package org.labkey.test.util;

/**
 * Represents a range of LabKey versions.
 * Used for checking if a specific version falls within an inclusive range.
 */
public class VersionRange
{
    private final Version earliestVersion;
    private final Version latestVersion;

    /**
     * Constructs a version range.
     *
     * @param earliestVersion The earliest version in the range (inclusive). If null, there is no lower bound.
     * @param latestVersion The latest version in the range (inclusive). If null, there is no upper bound.
     * @throws IllegalArgumentException if earliestVersion is after latestVersion.
     */
    public VersionRange(Version earliestVersion, Version latestVersion)
    {
        this.earliestVersion = earliestVersion;
        this.latestVersion = latestVersion;

        if (earliestVersion != null && latestVersion != null && earliestVersion.compareTo(latestVersion) > 0)
            throw new IllegalArgumentException("%s is after %s".formatted(earliestVersion, latestVersion));

    }

    /**
     * Creates a version range starting from the specified version with no upper bound.
     *
     * @param version The earliest version (inclusive).
     * @return A new {@link VersionRange}.
     */
    public static VersionRange from(String version)
    {
        return new VersionRange(new Version(version), null);
    }

    /**
     * Creates a version range ending at the specified version with no lower bound.
     *
     * @param version The latest version (inclusive).
     * @return A new {@link VersionRange}.
     */
    public static VersionRange until(String version)
    {
        return new VersionRange(null, new Version(version));
    }

    /**
     * Creates a version range with specified bounds.
     *
     * @param earliestVersion The earliest version (inclusive). If null, there is no lower bound.
     * @param latestVersion The latest version (inclusive). If null, there is no upper bound.
     * @return A new {@link VersionRange}.
     */
    public static VersionRange versionRange(String earliestVersion, String latestVersion)
    {
        Version earliest = earliestVersion == null ? null : new Version(earliestVersion);
        Version latest = latestVersion == null ? null : new Version(latestVersion);
        return new VersionRange(earliest, latest);
    }

    /**
     * Checks if the specified version is within this range (inclusive).
     *
     * @param version The version to check.
     * @return {@code true} if the version is within the range.
     */
    public boolean contains(Version version)
    {
        return (earliestVersion == null || earliestVersion.compareTo(version) <= 0) &&
            (latestVersion == null || latestVersion.compareTo(version) >= 0);
    }
}
