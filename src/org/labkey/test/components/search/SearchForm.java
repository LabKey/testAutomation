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
package org.labkey.test.components.search;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.Component;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.SearchHelper;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SearchForm extends Component<SearchForm.Elements>
{
    private final WebDriverWrapper _driver;
    private final WebElement _componentElement;

    public SearchForm(WebDriver driver, SearchContext parent)
    {
        _driver = new WebDriverWrapperImpl(driver);
        _componentElement = Locator.byClass("lk-search-form").findElement(parent);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    @LogMethod
    public SearchResultsPage searchFor(@LoggedParam String searchTerm)
    {
        SearchHelper.waitForIndexer();

        _driver.setFormElement(elementCache().searchBox(), searchTerm);
        _driver.clickAndWait(elementCache().searchButton());

        return new SearchResultsPage(_driver.getDriver());
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected class Elements extends Component.ElementCache
    {
        private WebElement searchBox()
        {
            return Locator.input("q").findElement(this);
        }

        private WebElement searchButton()
        {
            return Locator.css("a.search-overlay.fa-search").findElement(this);
        }

        private WebElement helpLink()
        {
            return Locator.css("a[target=labkeyHelp]").findElement(this);
        }
    }
}
