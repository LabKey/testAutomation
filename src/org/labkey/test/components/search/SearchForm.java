/*
 * Copyright (c) 2015 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.SearchHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

public class SearchForm extends Component
{
    private final BaseWebDriverTest _test;
    private final WebElement _componentElement;

    public SearchForm(BaseWebDriverTest test, SearchContext parent)
    {
        _test = test;
        _componentElement = parent.findElement(By.cssSelector(".labkey-search-form"));
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

        _test.setFormElement(elements().searchBox(), searchTerm);
        _test.clickAndWait(elements().searchButton());

        return new SearchResultsPage(_test);
    }

    private Elements elements()
    {
        return new Elements();
    }

    private class Elements extends ComponentElements
    {
        Elements()
        {
            super(getComponentElement());
        }

        private WebElement searchBox()
        {
            return findElement(Locator.id("query"));
        }

        private WebElement searchButton()
        {
            return findElement(Locator.lkButton("Search"));
        }

        private WebElement helpLink()
        {
            return findElement(Locator.css("a[target=labkeyHelp]"));
        }
    }
}
