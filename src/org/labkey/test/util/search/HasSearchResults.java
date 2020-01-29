package org.labkey.test.util.search;

import org.labkey.test.Locator;

public interface HasSearchResults
{
    boolean hasResultLocatedBy(Locator resultLoc);
}
