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
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.util.search.SearchAdminAPIHelper;
import org.labkey.test.util.search.SearchResultsQueue;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SearchHelper extends WebDriverWrapper
{
    private final BaseWebDriverTest _test;
    private final SearchResultsQueue _searchResultsQueue;

    private int maxTries = 4;

    public SearchHelper(BaseWebDriverTest test, SearchResultsQueue queue)
    {
        _test = test;
        _searchResultsQueue = queue;
    }

    public SearchHelper(BaseWebDriverTest test)
    {
        this(test, new SearchResultsQueue());
    }

    public SearchHelper setMaxTries(int maxTries)
    {
        this.maxTries = Math.max(maxTries, 1);
        return this;
    }

    public void initialize()
    {
        clearSearchQueue();
        SearchAdminAPIHelper.deleteIndex(getDriver());
    }

    public void clearSearchQueue()
    {
        _searchResultsQueue.clearSearchQueue();
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return _test.getDriver();
    }

    private Locator getNoResultsLocator()
    {
        return SearchResultsPage.resultsCountLocator(0);
    }

    /**
     * Add this value to documents or objects that are expected to be ignored by the search indexer
     */
    public static String getUnsearchableValue()
    {
        return "UNSEARCHABLE";
    }

    /**
     * @deprecated We have no immediate plans to implement search result crawling
     */
    @Deprecated (forRemoval = true)
    public void verifySearchResults(String container, boolean crawlResults)
    {
        verifySearchResults(container);
    }

    /**
     * @see #verifySearchResults(String, String)
     */
    public void verifySearchResults(@LoggedParam String expectedResultsContainer)
    {
        verifySearchResults(expectedResultsContainer, "searchResults");
    }

    /**
     * Search for all enqueued search items and verify expected results. If any searches don't produce the expected
     * results within a set number of retries (see constructor), takes screenshots of each failed search and throws
     * {@link AssertionError}
     * @param expectedResultsContainer Full container path of expected search results
     * @param baseScreenshotName Short description to identify screenshots
     */
    @LogMethod
    public void verifySearchResults(@LoggedParam String expectedResultsContainer, @LoggedParam @NotNull String baseScreenshotName)
    {
        // Note: adding this "waitForIndexer()" call should eliminate the need for sleep() and retry below.
        SearchAdminAPIHelper.waitForIndexer();

        verifySearchResults(expectedResultsContainer, baseScreenshotName, _searchResultsQueue, maxTries);
    }

    private void verifySearchResults(String expectedResultsContainer, @NotNull String baseScreenshotName, SearchResultsQueue items, int retries)
    {
        if (items.isEmpty())
        {
            throw new IllegalArgumentException("Search queue is empty, nothing to verify");
        }
        if (expectedResultsContainer != null && !expectedResultsContainer.startsWith("/"))
        {
            expectedResultsContainer = "/" + expectedResultsContainer;
        }

        for (int i = 1; i <= retries; i++)
        {
            TestLogger.log("Verify search results, attempt " + i);
            final boolean lastTry = i == retries;
            List<String> notFound = verifySearchItems(items, expectedResultsContainer, lastTry, baseScreenshotName);
            if (notFound.isEmpty())
                break;

            if (!lastTry)
            {
                TestLogger.log(String.format("Bad search results for %s. Waiting %d seconds before trying again...", notFound.toString(), i*5));
                WebDriverWrapper.sleep(i*5000);
            }
        }
    }

    // Does not wait for indexer... caller should do so
    private List<String> verifySearchItems(SearchResultsQueue queue, String expectedResultsContainer, boolean failOnError, String baseScreenshotName)
    {
        Map<String, SearchResultsQueue.SearchItem> items = queue.getQueuedItems();
        TestLogger.log("Verifying " + items.size() + " items");
        List<String> notFound = new ArrayList<>();
        DeferredErrorCollector errorCollector = _test.checker().withScreenshot(baseScreenshotName);
        for (String searchTerm : items.keySet())
        {
            SearchResultsQueue.SearchItem item = items.get(searchTerm);
            List<Locator> expectedResults = new ArrayList<>(Arrays.asList(item.getExpectedResults()));

            SearchResultsPage resultsPage = searchFor(searchTerm, false); // We already waited for the indexer in calling method

            if (expectedResults.isEmpty())
            {
                expectedResults.add(getNoResultsLocator());
            }
            else
            {
                addExpectedContainerLink(expectedResultsContainer, item, expectedResults);
            }

            List<Locator> missingResults = new ArrayList<>();

            for (Locator loc : expectedResults)
            {
                if (!resultsPage.hasResultLocatedBy(loc))
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
                    errorCollector.error(baseScreenshotName + ": Incorrect search results for [\"" + searchTerm + "\"]. Missing results: \n" +
                            missingResults.stream().map(Locator::toString).collect(Collectors.joining("\n")));
                }
                else
                {
                    notFound.add(searchTerm);
                }
            }
        }

        if (notFound.isEmpty())
            TestLogger.log("All items were found");
        else
            TestLogger.log(notFound.size() + " items were not found.");

        return notFound;
    }

    private void addExpectedContainerLink(String expectedResultsContainer, SearchResultsQueue.SearchItem item, List<Locator> expectedResults)
    {
        if (expectedResultsContainer != null)
        {
            if ( Locator.linkContainingText("@files").findOptionalElement(getDriver()).isPresent() )
            {
                if(expectedResultsContainer.contains("@files"))
                {
                    expectedResults.add(Locator.linkWithText(expectedResultsContainer));
                }
                else
                {
                    expectedResults.add(Locator.linkWithText(expectedResultsContainer + (item.expectFileInResults() ? "/@files" : "")));
                }
            }
            else
            {
                expectedResults.add(Locator.linkWithText(expectedResultsContainer));
            }
        }
        else if (item.expectFileInResults())
        {
            expectedResults.add(Locator.linkContainingText("/@files"));
        }
    }

    public void verifyNoSearchResults()
    {
        SearchAdminAPIHelper.waitForIndexer();

        Map<String, SearchResultsQueue.SearchItem> queuedItems = _searchResultsQueue.getQueuedItems();
        SearchResultsQueue noResultsQueue = new SearchResultsQueue();
        TestLogger.log("Verify empty search results for previously queued items.");
        for (String searchTerm : queuedItems.keySet())
        {
            noResultsQueue.enqueueSearchItem(searchTerm);
        }
        verifySearchResults(null, "noResults", noResultsQueue, maxTries);
    }

    public void assertNoSearchResult(String searchTerm)
    {
        long startTime = System.currentTimeMillis();
        List<WebElement> results;

        do {
            SearchResultsPage searchResultsPage = searchFor(searchTerm);
            results = searchResultsPage.getResults();
        } while (System.currentTimeMillis() - startTime < BaseWebDriverTest.WAIT_FOR_PAGE && !results.isEmpty());

        if (!results.isEmpty())
        {
            Assert.fail("Found unwanted search results for '" + searchTerm + "': ['" + StringUtils.join(results.stream().map(WebElement::getText).collect(Collectors.toList()), "', '") + "']");
        }
    }

    /**
     * Add searchTerm and all expected results to list of terms to search for.
     * If searchTerm is already in the list, replaces the expected results.
     * @param expectedResults Elements expected to be found. If empty, verifySearchResults will assert that there are no results
     * @see #verifySearchResults(String, String)
     */
    public void enqueueSearchItem(String searchTerm, Locator... expectedResults)
    {
        enqueueSearchItem(searchTerm, false, expectedResults);
    }

    public void enqueueSearchItem(String searchTerm, boolean isFile, Locator... expectedResults)
    {
        _searchResultsQueue.enqueueSearchItem(searchTerm, isFile, expectedResults);
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
            SearchAdminAPIHelper.waitForIndexer();

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
