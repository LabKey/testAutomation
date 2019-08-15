/*
 * Copyright (c) 2019 LabKey Corporation
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

