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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.util.search.SearchAdminAPIHelper;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class SearchHelper
{
    private final BaseWebDriverTest _test;
    private final int maxTries;

    private static Map<String, SearchItem> _searchQueue = new HashMap<>();

    public SearchHelper(BaseWebDriverTest test, int maxTries)
    {
        _test = test;
        this.maxTries = maxTries;
    }

    public SearchHelper(BaseWebDriverTest test)
    {
        this(test, 12);
    }
    private static final Locator noResultsLocator = Locator.byClass("labkey-search-results-counts").withText("Found 0 results");
    private static final String unsearchableValue = "UNSEARCHABLE";

    public void initialize()
    {
        clearSearchQueue();
        SearchAdminAPIHelper.deleteIndex(_test.getDriver());
    }

    public void clearSearchQueue()
    {
        _searchQueue.clear();
        enqueueSearchItem(getUnsearchableValue());
    }

    /**
     * Add this value to documents or objects that are expected to be ignored by the search indexer
     */
    public static String getUnsearchableValue()
    {
        return unsearchableValue;
    }

    @LogMethod(quiet = true)
    public static void waitForIndexer()
    {
        // Invoke a special server action that waits until all previous indexer tasks are complete
        int response = WebTestHelper.getHttpResponse(WebTestHelper.buildURL("search", "waitForIndexer")).getResponseCode();
        assertEquals("WaitForIndexer action timed out", HttpStatus.SC_OK, response);
    }

    @LogMethod
    public void verifySearchResults(@LoggedParam String container, boolean crawlResults)
    {
        // Note: adding this "waitForIndexer()" call should eliminate the need for sleep() and retry below.
        waitForIndexer();

        for (int i = 1; i <= maxTries; i++)
        {
            _test.log("Verify search results, attempt " + i);
            Map<String, SearchItem> notFound = verifySearchItems(_searchQueue, container, i == maxTries, crawlResults);
            if (notFound.isEmpty())
                break;

            _test.log(String.format("Bad search results for %s. Waiting %d seconds before trying again...", notFound.keySet().toString(), i*5));
            WebDriverWrapper.sleep(i*5000);
        }
    }

    // Does not wait for indexer... caller should do so
    private Map<String, SearchItem> verifySearchItems(Map<String, SearchItem> items, String container, boolean failOnError, boolean crawlResults)
    {
        _test.log("Verifying " + items.size() + " items");
        Map<String, SearchItem> notFound = new HashMap<>();
        DeferredErrorCollector errorCollector = new DeferredErrorCollector(_test).withScreenshot("searchResults");
        for (String searchTerm : items.keySet())
        {
            SearchItem item = items.get(searchTerm);
            List<Locator> expectedResults = new ArrayList<>(Arrays.asList(item._searchResults));

            SearchResultsPage resultsPage = searchFor(searchTerm, false); // We already waited for the indexer in calling method

            final boolean expectResults = expectedResults.size() > 0 && expectedResults.get(0) != noResultsLocator;
            if (container != null && expectResults)
            {
                if ( _test.isElementPresent(Locator.linkContainingText("@files")) )
                {
                    if(container.contains("@files"))
                    {
                        expectedResults.add(Locator.linkWithText(container));
                    }
                    else
                    {
                        expectedResults.add(Locator.linkWithText(container + (item._file ? "/@files" : "")));
                    }
                }
                else
                {
                    expectedResults.add(Locator.linkWithText(container));
                }
            }
            else if (expectResults && item._file)
            {
                expectedResults.add(Locator.linkContainingText("/@files"));
            }

            List<Locator> missingResults = new ArrayList<>();

            for (Locator loc : expectedResults)
            {
                if (!loc.existsIn(resultsPage.getResultsPanel()) && !loc.existsIn(resultsPage.getFolderResultsPanel()))
                {
                    missingResults.add(loc);
                    if (!failOnError)
                    {
                        // Stop checking for search results if we don't need them for a failure message
                        break;
                    }
                }
            }

            if (!missingResults.isEmpty())
            {
                if (failOnError)
                {
                    errorCollector.error("Incorrect search results for [\"" + searchTerm + "\"]. Missing results: " + missingResults.toString());
                }
                else
                {
                    notFound.put(searchTerm, item);
                    continue;
                }
            }

            if (crawlResults)
                throw new IllegalArgumentException("Search result crawling not yet implemented");
        }

        errorCollector.recordResults();

        if (notFound.isEmpty())
            _test.log("All items were found");
        else
            _test.log(notFound.size() + " items were not found.");

        return notFound;
    }

    public void verifyNoSearchResults()
    {
        waitForIndexer();

        _test.log("Verify null search results.");
        for (String searchTerm : _searchQueue.keySet())
        {
            searchFor(searchTerm, false);
            _test.assertElementPresent(noResultsLocator);
        }
    }

    public void assertNoSearchResult(String searchTerm)
    {
        long startTime = System.currentTimeMillis();
        List<WebElement> results;

        do {
            SearchResultsPage searchResultsPage = searchFor(searchTerm);
            results = searchResultsPage.getResults();
        } while (System.currentTimeMillis() - startTime < _test.defaultWaitForPage && !results.isEmpty());

        if (!results.isEmpty())
        {
            Assert.fail("Found unwanted search results for '" + searchTerm + "': ['" + StringUtils.join(_test.getTexts(results).toArray(), "', '") + "']");
        }
    }

    /**
     * Add searchTerm and all expected results to list of terms to search for.
     * If searchTerm is already in the list, replaces the expected results.
     * @param expectedResults Omit if expecting 0 results for the search
     */
    public void enqueueSearchItem(String searchTerm, Locator... expectedResults)
    {
        enqueueSearchItem(searchTerm, false, expectedResults);
    }

    public void enqueueSearchItem(String searchTerm, boolean isFile, Locator... expectedResults)
    {
        if(expectedResults.length == 0)
        {
            expectedResults = new Locator[] {noResultsLocator};
        }
        _searchQueue.put(searchTerm, new SearchItem(isFile, expectedResults));
    }

    // This method always waits for the indexer queue to empty before issuing search query
    public SearchResultsPage searchFor(String searchTerm)
    {
        return searchFor(searchTerm, true);
    }

    // This method waits for the indexer queue to empty iff waitForIndex == true
    public SearchResultsPage searchFor(String searchTerm, boolean waitForIndexer)
    {
        if (waitForIndexer)
            waitForIndexer();

        _test.log("Searching for: '" + searchTerm + "'.");

        Optional<WebElement> bodyInput = Locators.bodyPanel().append(Locator.input("q")).findOptionalElement(_test.getDriver());
        if (bodyInput.isPresent()) // Search results page or search webpart
        {
            _test.setFormElement(bodyInput.get(), searchTerm);
            _test.doAndWaitForPageToLoad(() -> bodyInput.get().sendKeys(Keys.ENTER));
            return new SearchResultsPage(_test.getDriver());
        }
        else // Use header search
        {
            return new SiteNavBar(_test.getDriver()).search(searchTerm);
        }
    }

    public void searchForSubjects(String searchTerm)
    {
        _test.log("Searching for subject: '" + searchTerm + "'.");
        if (!_test.isElementPresent(Locator.id("query")) )
            _test.goToModule("Search");
        if (_test.getAttribute(Locator.id("adv-search-btn"), "src").contains("plus"))
            _test.click(Locator.id("adv-search-btn"));

        _test.checkCheckbox(Locator.checkboxByName("category").index(2));

        _test.setFormElement(Locator.id("query"), searchTerm);
        _test.clickButton("Search");
    }

    public static class SearchItem
    {
        public final Locator[] _searchResults;
        public final boolean _file; // is this search expecting a file?

        public SearchItem(boolean file, Locator... results)
        {
            _searchResults = results;
            _file = file;
        }
    }
}
