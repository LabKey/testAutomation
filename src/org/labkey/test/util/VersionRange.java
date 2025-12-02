package org.labkey.test.util;

public class VersionRange
{
    private final Version eariestVersion;
    private final Version latestVersion;

    public VersionRange(Version eariestVersion, Version latestVersion)
    {
        this.eariestVersion = eariestVersion;
        this.latestVersion = latestVersion;
    }

    public static VersionRange from(String version)
    {
        return new VersionRange(new Version(version), null);
    }

    public static VersionRange until(String version)
    {
        return new VersionRange(null, new Version(version));
    }

    public static VersionRange versionRange(String earliestVersion, String latestVersion)
    {
        Version earliest = earliestVersion == null ? null : new Version(earliestVersion);
        Version latest = latestVersion == null ? null : new Version(latestVersion);
        return new VersionRange(earliest, latest);
    }

    public boolean contains(Version version)
    {
        return (eariestVersion == null || eariestVersion.compareTo(version) <= 0) &&
            (latestVersion == null || latestVersion.compareTo(version) >= 0);
    }
}
