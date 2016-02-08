/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.test.pages.search;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.components.search.SearchForm;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class SearchResultsPage extends LabKeyPage
{
    public SearchResultsPage(WebDriver test)
    {
        super(test);
    }

    public SearchForm searchForm()
    {
        return new SearchForm(getDriver(), getDriver());
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
        @Override
        protected SearchContext getContext()
        {
            return getDriver();
        }

        WebElement resultsCount()
        {
            return Locator.css("table.labkey-search-results-counts td").index(0).findElement(this);
        }

        WebElement pageCount()
        {
            return Locator.css("table.labkey-search-results-counts td").index(1).findElement(this);
        }
    }
}
