/*
 * Copyright (c) 2010-2012 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.util.LinkedList;
import java.util.List;

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

        List<SearchItem> notFound = verifySearchItems(_searchQueue, test, container, crawlResults);

        if (!notFound.isEmpty())
        {
            test.sleep(5000);
            notFound = verifySearchItems(notFound, test, container, crawlResults);

            if (!notFound.isEmpty())
            {
                test.sleep(10000);
                notFound = verifySearchItems(notFound, test, container, crawlResults);

                Assert.assertTrue("These items were not found: " + notFound.toString(), notFound.isEmpty());
            }
        }
    }

    private static List<SearchItem> verifySearchItems(List<SearchItem> items, BaseSeleniumWebTest test, String container, boolean crawlResults)
    {
        test.log("Verifying " + items.size() + " items");
        LinkedList<SearchItem> notFound = new LinkedList<SearchItem>();

        for ( SearchItem item : items)
        {
            searchFor(test, item._searchTerm);

            if(item._searchResults==null)
            {
                test.assertTextPresent("Found 0 results");
            }
            else
            {
                boolean success = true;

                for( Locator loc : item._searchResults )
                {
                    if (!test.isElementPresent(loc))
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
                    if ( test.isLinkPresentContainingText("@files") )
                        if(container.contains("@files"))
                            test.assertLinkPresentWithText(container);
                        else
                            test.assertLinkPresentWithText(container + (item._file ? "/@files" : ""));
                    else
                        test.assertLinkPresentWithText(container);
                }

                if ( crawlResults )
                {
                    Assert.fail("Search result crawling not yet implemented");
                }
            }
        }

        if (notFound.isEmpty())
            test.log("All items were found");
        else
            test.log(notFound.size() + " items were not found.");

        return notFound;
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
            test.clickAndWait(Locator.xpath("//img[@src = '/labkey/_images/search.png']"));
        }
    }

    public static void searchForSubjects(BaseSeleniumWebTest test, String searchTerm)
    {
        test.log("Searching for subject: '" + searchTerm + "'.");
        if (!test.isElementPresent(Locator.id("query")) )
            test.goToModule("Search");
        if (test.getAttribute(Locator.id("adv-search-btn"), "src").contains("plus"))
            test.click(Locator.id("adv-search-btn"));

        test.checkCheckbox("category", 2);

        test.setFormElement(Locator.id("query"), searchTerm);
        test.clickNavButton("Search");
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
            if(results!=null)
                _searchResults = results.clone();
            else
                _searchResults = null;
            _file = file;
        }

        @Override
        public String toString()
        {
            return _searchTerm;
        }
    }
}
