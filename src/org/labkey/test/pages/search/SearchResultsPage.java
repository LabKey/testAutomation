package org.labkey.test.pages.search;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.components.search.SearchForm;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class SearchResultsPage extends LabKeyPage
{
    public SearchResultsPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public SearchForm searchForm()
    {
        return new SearchForm(_test, _test.getDriver());
    }

    public Integer getResultCount()
    {
        String countStr = elements().resultsCount().getText();
        Pattern pattern = Pattern.compile("Found (\\d+) results?");
        Matcher matcher = pattern.matcher(countStr);

        assertTrue("Unable to parse result count: " + countStr, matcher.find());
        return Integer.parseInt(matcher.group(1));
    }

    Elements elements()
    {
        return new Elements();
    }

    private class Elements extends ComponentElements
    {
        Elements()
        {
            super(_test.getDriver());
        }

        WebElement resultsCount()
        {
            return findElement(Locator.css("table.labkey-search-results-counts td").index(0));
        }

        WebElement pageCount()
        {
            return findElement(Locator.css("table.labkey-search-results-counts td").index(1));
        }
    }
}
