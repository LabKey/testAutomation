/*
 * Copyright (c) 2014 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

public class InsertPage
{
    protected BaseWebDriverTest _test;
    protected String _title;

    public InsertPage(BaseWebDriverTest test, String title)
    {
        _test = test;
        _title = title;
        waitForReady();
    }

    protected void waitForReady()
    {
        _test.waitForElement(elements().title.withText(_title));
    }

    protected Elements elements()
    {
        return new Elements();
    }

    protected class Elements
    {
        public Locator.XPathLocator title = Locator.tagWithId("span", "labkey-nav-trail-current-page");
        public Locator.XPathLocator body = Locator.tagWithClass("table", "labkey-proj");
        public Locator.XPathLocator submit = body.append(Locator.lkButton("Submit"));
        public Locator.XPathLocator cancel = body.append(Locator.lkButton("Cancel"));
    }
}
