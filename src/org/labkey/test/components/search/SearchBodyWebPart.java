package org.labkey.test.components.search;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.BodyWebPart;

public class SearchBodyWebPart extends BodyWebPart
{
    public SearchBodyWebPart(BaseWebDriverTest test)
    {
        super(test, "Search");
    }

    public SearchForm searchForm()
    {
        return new SearchForm(_test, getComponentElement());
    }
}
