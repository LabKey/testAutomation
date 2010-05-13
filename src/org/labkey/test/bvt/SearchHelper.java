package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * User: Trey Chadick
 * Date: Apr 30, 2010
 * Time: 7:53:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class SearchHelper
{
    private static LinkedList<SearchItem> _searchQueue = new LinkedList<SearchItem>();

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
                test.assertLinkPresentWithText(container + (item._file ? "/@files" : ""));
            }
            if ( crawlResults )
            {

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
