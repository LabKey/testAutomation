/*
 * Copyright (c) 2010-2015 LabKey Corporation
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
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SearchHelper
{
    protected BaseWebDriverTest _test;

    private static LinkedList<SearchItem> _searchQueue = new LinkedList<>();
    public SearchHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void initialize()
    {
        _searchQueue.clear();
        deleteIndex();
    }

    @LogMethod(quiet = true)
    public static void waitForIndexer()
    {
        try
        {
            // Invoke a special server action that waits until all previous indexer tasks are complete
            int response = WebTestHelper.getHttpGetResponse(WebTestHelper.buildURL("search", "waitForIndexer"));
            assertEquals("WaitForIndexer action timed out", HttpStatus.SC_OK, response);
        }
        catch (IOException e)
        {
            throw new RuntimeException("WaitForIndexer action failed", e);
        }
    }

    public void verifySearchResults(String container, boolean crawlResults)
    {
        _test.log("Verify search results.");

        // Note: adding this "waitForIndexer()" call should eliminate the need for sleep() and retrie below.  TODO: Remove
        waitForIndexer();

        List<SearchItem> notFound = verifySearchItems(_searchQueue, container, crawlResults);

        if (!notFound.isEmpty())
        {
            _test.sleep(5000);
            notFound = verifySearchItems(notFound, container, crawlResults);

            if (!notFound.isEmpty())
            {
                _test.sleep(10000);
                notFound = verifySearchItems(notFound, container, crawlResults);

                assertTrue("These items were not found: " + notFound.toString(), notFound.isEmpty());
            }
        }
    }

    // Does not wait for indexer... caller should do so
    private List<SearchItem> verifySearchItems(List<SearchItem> items, String container, boolean crawlResults)
    {
        _test.log("Verifying " + items.size() + " items");
        LinkedList<SearchItem> notFound = new LinkedList<>();

        for ( SearchItem item : items)
        {
            searchFor(item._searchTerm, false);  // We already waited for the indexer in calling method

            if(item._searchResults.length == 0)
            {
                _test.assertTextPresent("Found 0 results");
            }
            else
            {
                boolean success = true;

                for( Locator loc : item._searchResults )
                {
                    if (!_test.isElementPresent(loc))
                    {
                        success = false;
                        break;
                    }
                }

                if (!success)
                {
                    notFound.add(item);
                    continue;
                }

                if ( container != null )
                {
                    if ( _test.isElementPresent(Locator.linkContainingText("@files")) )
                        if(container.contains("@files"))
                            _test.assertElementPresent(Locator.linkWithText(container));
                        else
                            _test.assertElementPresent(Locator.linkWithText(container + (item._file ? "/@files" : "")));
                    else
                        _test.assertElementPresent(Locator.linkWithText(container));
                }

                if ( crawlResults )
                {
                    throw new IllegalArgumentException("Search result crawling not yet implemented");
                }
            }
        }

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
        for( SearchItem item : _searchQueue )
        {
            searchFor(item._searchTerm, false);
            for ( Locator loc : item._searchResults )
            {
                _test.assertElementNotPresent(loc);
            }
        }
    }

    public void assertNoSearchResult(String searchTerm)
    {
        long startTime = System.currentTimeMillis();
        List<WebElement> results = new ArrayList<>();

        do {
            searchFor(searchTerm);
            results = Locator.css("#searchResults a.labkey-search-title").findElements(_test.getDriver());
        } while (System.currentTimeMillis() - startTime < _test.defaultWaitForPage && !results.isEmpty());

        if (!results.isEmpty())
        {
            Assert.fail("Found unwanted search results for '" + searchTerm + "': ['" + StringUtils.join(_test.getTexts(results).toArray(), "', '") + "']");
        }
    }

    /**
     * Add searchTerm and all expected results to list of terms to search for.
     * If searchTerm is already in the list, replaces the expected results.
     * @param searchTerm
     * @param expectedResults Omit if expecting 0 results for the search
     */
    public void enqueueSearchItem(String searchTerm, Locator... expectedResults)
    {
        if (_searchQueue.contains(searchTerm))
            _searchQueue.remove(searchTerm);
        _searchQueue.add(new SearchItem(searchTerm, false, expectedResults));
    }

    public void enqueueSearchItem(String searchTerm, boolean file, Locator... expectedResults)
    {
        if (_searchQueue.contains(searchTerm))
            _searchQueue.remove(searchTerm);
        _searchQueue.add(new SearchItem(searchTerm, file, expectedResults));
    }

    // This method always waits for the indexer queue to empty before issuing search query
    public void searchFor(String searchTerm)
    {
        searchFor(searchTerm, true);
    }

    // This method waits for the indexer queue to empty iff waitForIndex == true
    public void searchFor(String searchTerm, boolean waitForIndexer)
    {
        if (waitForIndexer)
            waitForIndexer();

        _test.log("Searching for: '" + searchTerm + "'.");
        if ( _test.isElementPresent(Locator.id("query")) )
        {
            _test.setFormElement(Locator.id("query"), searchTerm);
            _test.clickButton("Search");
        }
        else
        {
            _test.setFormElement(Locator.id("search-input"), searchTerm);
            _test.pressEnter(Locator.id("search-input"));
            _test.waitForElement(Locator.id("query"));
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

    public void deleteIndex()
    {
        _test.ensureAdminMode();
        _test.goToAdmin();
        _test.clickAndWait(Locator.linkWithText("full-text search"));
        _test.clickButton("Delete Index");
    }

    public static class SearchItem
    {
        public String _searchTerm;
        public Locator[] _searchResults;
        public boolean _file; // is this search expecting a file?

        public SearchItem(String term, boolean file, Locator... results)
        {
            _searchTerm = term;
            _searchResults = results;
            _file = file;
        }

        @Override
        public String toString()
        {
            return _searchTerm;
        }
    }
}
