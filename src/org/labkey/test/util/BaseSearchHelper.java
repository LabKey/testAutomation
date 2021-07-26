package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.search.HasSearchResults;
import org.labkey.test.util.search.SearchAdminAPIHelper;
import org.labkey.test.util.search.SearchResultsQueue;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseSearchHelper<H extends BaseSearchHelper<H, SearchResults>, SearchResults extends HasSearchResults> extends WebDriverWrapper
{
    protected final BaseWebDriverTest _test;
    protected final SearchResultsQueue _searchResultsQueue;
    private int maxTries = 5;

    public BaseSearchHelper(BaseWebDriverTest test, SearchResultsQueue queue)
    {
        _test = test;
        _searchResultsQueue = queue;
    }

    /**
     * Add this value to documents or objects that are expected to be ignored by the search indexer
     */
    public static String getUnsearchableValue()
    {
        return "UNSEARCHABLE";
    }

    protected abstract H getThis();

    public H setMaxTries(int maxTries)
    {
        this.maxTries = Math.max(maxTries, 1);
        return getThis();
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

    protected abstract Locator getNoResultsLocator();

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

    protected abstract void beforeVerify(String containerPath);

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

        beforeVerify(expectedResultsContainer);

        for (int i = 1; i <= retries; i++)
        {
            TestLogger.log("Verify search results, attempt " + i);
            final boolean lastTry = i == retries;
            Set<String> notFound = verifySearchItems(items, expectedResultsContainer, lastTry, baseScreenshotName);
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
    private Set<String> verifySearchItems(SearchResultsQueue queue, String expectedResultsContainer, boolean failOnError, String baseScreenshotName)
    {
        Map<String, SearchResultsQueue.SearchItem> items = queue.getQueuedItems();
        TestLogger.log("Verifying " + items.size() + " items");
        Set<String> notFound = new HashSet<>();
        DeferredErrorCollector errorCollector = _test.checker().withScreenshot(baseScreenshotName);
        for (String searchTerm : items.keySet())
        {
            SearchResultsQueue.SearchItem item = items.get(searchTerm);
            List<Locator> expectedResults = new ArrayList<>(item.getExpectedResults());

            SearchResults resultsPage = searchFor(searchTerm, false); // We already waited for the indexer in calling method

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

            List<Locator> wrongResults = new ArrayList<>();
            for (Locator loc : item.getUnwantedResults())
            {
                if (resultsPage.hasResultLocatedBy(loc))
                {
                    wrongResults.add(loc);
                    if (!failOnError)
                    {
                        // Stop checking for search results if we don't need them for a failure message
                        break;
                    }
                }
            }

            if (!missingResults.isEmpty() || !wrongResults.isEmpty())
            {
                if (failOnError)
                {
                    String missingResultSummary = missingResults.stream().map(Locator::toString).collect(Collectors.joining("\n"));
                    String wrongResultSummary = missingResults.stream().map(Locator::toString).collect(Collectors.joining("\n"));
                    errorCollector.error(baseScreenshotName + ": Incorrect search results for [\"" + searchTerm + "\"]. " +
                            (missingResults.isEmpty() ? "" : "Missing results: \n" + missingResultSummary + "\n") +
                            (wrongResults.isEmpty() ? "" : "Unexpected results: \n" + wrongResultSummary));
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

    protected void addExpectedContainerLink(String expectedResultsContainer, SearchResultsQueue.SearchItem item, List<Locator> expectedResults)
    {
        expectedResults.add(Locator.linkWithText(expectedResultsContainer));
        if (item.expectFileInResults())
        {
            StringBuilder atFilesPath = new StringBuilder();
            atFilesPath.append("/@files");
            if (!item.getFilePath().isBlank())
            {
                atFilesPath.append("/");
                atFilesPath.append(item.getFilePath());
            }
            if (expectedResultsContainer == null)
            {
                expectedResults.add(Locator.tag("a").endsWith(atFilesPath.toString()));
            }
            else
            {
                expectedResults.add(Locator.linkWithText(expectedResultsContainer + atFilesPath));
            }
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
            SearchResults searchResultsPage = searchFor(searchTerm);
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
        enqueueSearchItem(searchTerm, null, expectedResults);
    }

    public void enqueueSearchItem(String searchTerm, boolean isFile, Locator... expectedResults)
    {
        enqueueSearchItem(searchTerm, isFile ? "" : null, expectedResults);
    }

    public void enqueueSearchItem(String searchTerm, String filePath, Locator... expectedResults)
    {
        _searchResultsQueue.enqueueSearchItem(searchTerm, filePath, expectedResults);
    }

    public void addUnwantedResult(String searchTerm, Locator unexpectedResults)
    {
        _searchResultsQueue.addUnwantedResult(searchTerm, unexpectedResults);
    }

    // This method always waits for the indexer queue to empty before issuing search query
    public SearchResults searchFor(String searchTerm)
    {
        return searchFor(searchTerm, true);
    }

    // This method waits for the indexer queue to empty iff waitForIndex == true
    public SearchResults searchFor(String searchTerm, boolean waitForIndexer)
    {
        if (waitForIndexer)
            SearchAdminAPIHelper.waitForIndexer();

        return doSearch(searchTerm);
    }

    protected abstract SearchResults doSearch(String searchTerm);
}
