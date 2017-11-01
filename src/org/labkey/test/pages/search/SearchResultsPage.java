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

import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.components.search.SearchForm;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class SearchResultsPage extends LabKeyPage
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
        String countStr = elements().resultsCount().getText();
        Pattern pattern = Pattern.compile("Found (\\d+) results?");
        Matcher matcher = pattern.matcher(countStr);

        assertTrue("Unable to parse result count: " + countStr, matcher.find());
        return Integer.parseInt(matcher.group(1));
    }

    public List<WebElement> getResults()
    {
        return elements().searchResultCards();
    }

    public void openAdvancedOptions()
    {
        if (!isAdvancedOptionsOpen())
            elements().advancedOptionsToggle.click();
            waitFor(()-> isAdvancedOptionsOpen(), WAIT_FOR_JAVASCRIPT);
    }

    public void closeAdvancedOptions()
    {
        if (isAdvancedOptionsOpen())
            elements().advancedOptionsToggle.click();
        waitFor(()-> !isAdvancedOptionsOpen(), WAIT_FOR_JAVASCRIPT);
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

    Elements elements()
    {
        return new Elements();
    }

    private class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getDriver();
        }

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
    }
}
