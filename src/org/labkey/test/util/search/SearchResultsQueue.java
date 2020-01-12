package org.labkey.test.util.search;

import org.labkey.test.Locator;
import org.labkey.test.util.SearchHelper;

import java.util.HashMap;
import java.util.Map;

public class SearchResultsQueue
{
    private final Map<String, SearchItem> _searchQueue = new HashMap<>();

    public SearchResultsQueue() { }

    public void clearSearchQueue()
    {
        _searchQueue.clear();
    }

    /**
     * Add searchTerm and all expected results to list of terms to search for.
     * If searchTerm is already in the list, replaces the expected results.
     * @param expectedResults Elements expected to be found. If empty, verifySearchResults will assert that there are no results
     * @see SearchHelper#verifySearchResults(String, String)
     */
    public void enqueueSearchItem(String searchTerm, Locator... expectedResults)
    {
        enqueueSearchItem(searchTerm, false, expectedResults);
    }

    public void enqueueSearchItem(String searchTerm, boolean isFile, Locator... expectedResults)
    {
        _searchQueue.put(searchTerm, new SearchItem(isFile, expectedResults));
    }

    public Map<String, SearchItem> getQueuedItems()
    {
        HashMap<String, SearchItem> searchQueuePlus = new HashMap<>(_searchQueue);
        searchQueuePlus.put(SearchHelper.getUnsearchableValue(), new SearchItem());
        return searchQueuePlus;
    }

    public boolean isEmpty()
    {
        return _searchQueue.isEmpty();
    }

    public static class SearchItem
    {
        private final Locator[] _searchResults;
        private final boolean _file; // is this search expecting a file?

        private SearchItem(boolean file, Locator... results)
        {
            _searchResults = results;
            _file = file;
        }

        private SearchItem()
        {
            this(false);
        }

        public Locator[] getExpectedResults()
        {
            return _searchResults;
        }

        public boolean expectFileInResults()
        {
            return _file;
        }
    }
}
