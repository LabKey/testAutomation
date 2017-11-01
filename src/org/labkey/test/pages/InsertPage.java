/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

package org.labkey.test.pages;

import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.openqa.selenium.WebDriver;

public class InsertPage extends LabKeyPage
{
    private final String _title;

    public InsertPage(WebDriver driver, String title)
    {
        super(driver);
        _title = title;
        waitForReady();
    }

    public InsertPage(WebDriver driver)
    {
        this(driver, null);
    }

    protected void waitForReady()
    {
        if (_title != null)
            waitForElement(elements().title.withText(_title));
        else
            waitForElement(elements().title);
    }

    protected Elements elements()
    {
        return new Elements();
    }

    protected class Elements
    {
        public Locator.XPathLocator title = Locators.bodyTitle();
        public Locator.XPathLocator body = Locators.bodyPanel();
        public Locator.XPathLocator submit = body.append(Locator.lkButton("Submit"));
        public Locator.XPathLocator cancel = body.append(Locator.lkButton("Cancel"));
    }
}
