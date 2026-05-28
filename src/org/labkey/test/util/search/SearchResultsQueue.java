/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test.util.search;

import org.labkey.test.Locator;
import org.labkey.test.util.SearchHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        enqueueSearchItem(searchTerm, null, expectedResults);
    }

    public void enqueueSearchItem(String searchTerm, String filePath, Locator... expectedResults)
    {
        _searchQueue.put(searchTerm, new SearchItem(filePath, expectedResults));
    }

    public void addUnwantedResult(String searchTerm, Locator unexpectedResults)
    {
        _searchQueue.get(searchTerm).addUnwantedResult(unexpectedResults);
    }

    public Map<String, SearchItem> getQueuedItems()
    {
        HashMap<String, SearchItem> searchQueuePlus = new HashMap<>(_searchQueue);
        searchQueuePlus.put(SearchHelper.getUnsearchableValue(), new SearchItem(null));
        return searchQueuePlus;
    }

    public boolean isEmpty()
    {
        return _searchQueue.isEmpty();
    }

    public static class SearchItem
    {
        private final List<Locator> _searchResults;
        private final List<Locator> _unwantedResults = new ArrayList<>();
        private final String _filePath;

        private SearchItem(String filePath, Locator... results)
        {
            _searchResults = Arrays.asList(results);
            _filePath = filePath;
        }

        public List<Locator> getExpectedResults()
        {
            return _searchResults;
        }

        public boolean expectFileInResults()
        {
            return _filePath != null;
        }

        public String getFilePath()
        {
            return _filePath;
        }

        public List<Locator> getUnwantedResults()
        {
            return _unwantedResults;
        }

        public void addUnwantedResult(Locator unwantedResult)
        {
            _unwantedResults.add(unwantedResult);
        }
    }
}
