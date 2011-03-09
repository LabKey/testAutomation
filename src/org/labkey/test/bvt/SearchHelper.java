/*
 * Copyright (c) 2010-2011 LabKey Corporation
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
package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.util.LinkedList;

/**
 * User: Trey Chadick
 * Date: Apr 30, 2010
 * Time: 7:53:50 AM
 */
public class SearchHelper
{
    private static LinkedList<SearchItem> _searchQueue = new LinkedList<SearchItem>();

    public static void initialize(BaseSeleniumWebTest test)
    {
        _searchQueue.clear();
        deleteIndex(test);
    }
        
    public static void verifySearchResults(BaseSeleniumWebTest test, String container, boolean crawlResults)
    {
        test.log("Verify search results.");
        for ( SearchItem item : _searchQueue )
        {
            searchFor(test, item._searchTerm);
            for( Locator loc : item._searchResults )
            {
                test.assertElementPresent(loc);
            }
            if ( container != null )
            {
                if ( test.isLinkPresentContainingText("@files") )
                    test.assertLinkPresentWithText(container + (item._file ? "/@files" : ""));
                else
                    test.assertLinkPresentWithText(container);
            }
            if ( crawlResults )
            {
                test.fail("Search result crawling not yet implemented");
            }
        }
    }

    public static void verifyNoSearchResults(BaseSeleniumWebTest test)
    {
        test.log("Verify null search results.");
        for( SearchItem item : _searchQueue )
        {
            searchFor(test, item._searchTerm);
            for ( Locator loc : item._searchResults )
            {
                test.assertElementNotPresent(loc);
            }
        }
    }

    public static void enqueueSearchItem(String searchTerm, Locator... expectedResults)
    {
        _searchQueue.add(new SearchItem(searchTerm, false, expectedResults));
    }

    public static void enqueueSearchItem(String searchTerm, boolean file, Locator... expectedResults)
    {
        _searchQueue.add(new SearchItem(searchTerm, file, expectedResults));
    }

    public static void searchFor ( BaseSeleniumWebTest test, String searchTerm )
    {
        test.log("Searching for: '" + searchTerm + "'.");
        if ( test.isElementPresent(Locator.id("query")) )
        {
            test.setFormElement(Locator.id("query"), searchTerm);
            test.clickNavButton("Search");
        }
        else
        {
            test.setFormElement(Locator.id("headerSearchInput"), searchTerm);
            test.clickAndWait(Locator.xpath("//input[@src = '/labkey/_images/search.png']"));
        }
    }

    public static void deleteIndex ( BaseSeleniumWebTest test )
    {
        test.ensureAdminMode();
        test.goToAdmin();
        test.clickLinkWithText("full-text search");
        test.clickNavButton("Delete Index");
    }

    public static class SearchItem
    {
        public String _searchTerm;
        public Locator[] _searchResults;
        public boolean _file; // is this search expecting a file?

        public SearchItem(String term, boolean file, Locator... results)
        {
            _searchTerm = term;
            _searchResults = results.clone();
            _file = file;
        }
    }
}
