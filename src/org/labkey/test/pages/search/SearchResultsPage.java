/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test.pages.search;

import org.labkey.test.Locator;
import org.labkey.test.components.search.SearchForm;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.search.HasSearchResults;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class SearchResultsPage extends LabKeyPage<SearchResultsPage.Elements> implements HasSearchResults
{
    public SearchResultsPage(WebDriver test)
    {
        super(test);
    }

    public SearchForm searchForm()
    {
        return new SearchForm(getDriver(), getDriver());
    }

    public Integer getResultCount()
    {
        String countStr = elementCache().resultsCount().getText();
        Pattern pattern = Pattern.compile("Found (\\d+) results?");
        Matcher matcher = pattern.matcher(countStr);

        assertTrue("Unable to parse result count: " + countStr, matcher.find());
        return Integer.parseInt(matcher.group(1));
    }

    public Optional<WebElement> getResultsPanel()
    {
        return elementCache().getSearchResultsPanel();
    }

    public Optional<WebElement> getFolderResultsPanel()
    {
        return elementCache().getFolderSearchResultsPanel();
    }

    public List<WebElement> getResults()
    {
        return elementCache().searchResultCards();
    }

    public void openAdvancedOptions()
    {
        if (!isAdvancedOptionsOpen())
        {
            elementCache().advancedOptionsToggle.click();
            waitFor(this::isAdvancedOptionsOpen, "Advanced options panel did not expand", WAIT_FOR_JAVASCRIPT);
        }
    }

    public void closeAdvancedOptions()
    {
        if (isAdvancedOptionsOpen())
        {
            elementCache().advancedOptionsToggle.click();
            waitFor(()-> !isAdvancedOptionsOpen(), "Advanced options panel did not collapse", WAIT_FOR_JAVASCRIPT);
        }
    }

    public boolean isAdvancedOptionsOpen()
    {
        return Locator.tagContainingText("h5", "Scope").findElement(getDriver()).isDisplayed();
    }

    public SearchResultsPage setSearchScope(String scope)
    {
        openAdvancedOptions();
        checkRadioButton(Locator.radioButtonByName("scope").containing(scope));
        return this;
    }

    public SearchResultsPage setSearchCategories(String... categories)
    {
        openAdvancedOptions();
        for(int i=0; i< categories.length; i++)
        {
            checkCheckbox(Locator.checkboxByNameAndValue("category", categories[i]));
        }
        return this;
    }

    public boolean hasResultLocatedBy(Locator resultLoc)
    {
        boolean inResultsPanel = getResultsPanel().map(resultLoc::existsIn).orElse(false);
        boolean inFolderResultsPanel = getFolderResultsPanel().map(resultLoc::existsIn).orElse(false);

        return inResultsPanel || inFolderResultsPanel;
    }

    public static Locator resultsCountLocator(int count)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(count).append(" result");
        if (count != 1)
        {
            sb.append("s");
        }
        return Locator.byClass("labkey-search-results-counts").childTag("div").withText(sb.toString());
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected class Elements extends LabKeyPage.ElementCache
    {
        public WebElement advancedOptionsToggle = Locator.tagWithClass("a", "search-advanced-toggle").waitForElement(this, WAIT_FOR_JAVASCRIPT);

        WebElement resultsCount()
        {
            return Locator.css(".labkey-search-results-counts div").index(0).findElement(this);
        }

        WebElement pageCount()
        {
            return Locator.css(".labkey-search-results-counts div").index(1).findElement(this);
        }

        List<WebElement> searchResultCards()    // Todo: wrap these into a better component type
        {
            return Locator.tagWithClass("div", "labkey-search-result").findElements(this);
        }

        private WebElement searchResultsPanel = Locator.byClass("labkey-search-results").findWhenNeeded(this);

        public Optional<WebElement> getSearchResultsPanel()
        {
            return Locator.byClass("labkey-search-results").findOptionalElement(this);
        }

        public Optional<WebElement> getFolderSearchResultsPanel()
        {
            return Locator.byClass("labkey-folders-search-results").findOptionalElement(this);
        }
    }
}
