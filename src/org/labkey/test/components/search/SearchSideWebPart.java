package org.labkey.test.components.search;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.SideWebPart;

public class SearchSideWebPart extends SideWebPart
{
    public SearchSideWebPart(BaseWebDriverTest test)
    {
        super(test, "Search");
    }

    public SearchForm searchForm()
    {
        return new SearchForm(_test, getComponentElement());
    }
}
