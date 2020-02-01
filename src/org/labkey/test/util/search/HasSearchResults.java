package org.labkey.test.util.search;

import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.util.List;

public interface HasSearchResults
{
    boolean hasResultLocatedBy(Locator resultLoc);
    List<WebElement> getResults();
}
