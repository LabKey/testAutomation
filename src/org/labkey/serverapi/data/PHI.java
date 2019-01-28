package org.labkey.serverapi.data;

import org.jetbrains.annotations.Nullable;

public enum PHI
{
    // Important: Must be in order of least to most restrictive level so ordinal reflects each level's rank.
    NotPHI( "Not PHI"),
    Limited( "Limited PHI"),
    PHI( "Full PHI"),
    Restricted( "Restricted PHI");

    public static PHI fromString(@Nullable String value)
    {
        for (PHI phi : values())
            if (phi.name().equals(value))
                return phi;

        return null;
    }

    private final String _label;

    PHI( String label)
    {
        _label = label;
    }

    public int getRank()
    {
        return ordinal();
    }

    public boolean isLevelAllowed(PHI maxLevelAllowed)
    {
        return ordinal() <= maxLevelAllowed.ordinal();
    }

    public boolean isExportLevelAllowed(PHI level)
    {
        return ordinal() <= level.ordinal();
    }

    public String getLabel()
    {
        return _label;
    }
}

