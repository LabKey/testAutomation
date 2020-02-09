/*
 * Copyright (c) 2011-2019 LabKey Corporation
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
package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.util.search.SearchResultsQueue;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

public class SearchHelper extends BaseSearchHelper<SearchHelper, SearchResultsPage>
{
    public SearchHelper(BaseWebDriverTest test, SearchResultsQueue queue)
    {
        super(test, queue);
    }

    public SearchHelper(BaseWebDriverTest test)
    {
        this(test, new SearchResultsQueue());
    }

    @Override
    protected SearchHelper getThis()
    {
        return this;
    }

    @Override
    protected void beforeVerify(String containerPath)
    {
        beginAt(WebTestHelper.buildURL("search", containerPath, "search"));
    }

    @Override
    protected Locator getNoResultsLocator()
    {
        return SearchResultsPage.resultsCountLocator(0);
    }

    @Override
    public SearchResultsPage doSearch(String searchTerm)
    {
        TestLogger.log("Searching for: '" + searchTerm + "'.");

        WebElement searchInput = Locator.input("q").findElementOrNull(Locators.bodyPanel().findElement(getDriver()));
        if (searchInput != null) // Search results page or search webpart
        {
            setFormElement(searchInput, searchTerm);
            doAndWaitForPageToLoad(() -> searchInput.sendKeys(Keys.ENTER));
            return new SearchResultsPage(getDriver());
        }
        else // Use header search
        {
            return new SiteNavBar(getDriver()).search(searchTerm);
        }
    }
}
