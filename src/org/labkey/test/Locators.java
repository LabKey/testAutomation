/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
package org.labkey.test;

public abstract class Locators
{
    public static final Locator.IdLocator folderMenu = Locator.id("folderBar");
    public static final Locator.XPathLocator labkeyError = Locator.byClass("labkey-error");
    public static final Locator.XPathLocator alertWarning = Locator.byClass("alert").withClass("alert-warning");
    public static final Locator signInLink = Locator.tagWithAttributeContaining("a", "href", "login.view");
    public static final Locator.XPathLocator folderTab = Locator.tagWithClass("div", "lk-nav-tabs-ct").append(Locator.tagWithClass("ul", "lk-nav-tabs")).childTag("li");

    public static Locator.XPathLocator headerContainer()
    {
        return Locator.byClass("lk-header-ct");
    }

    public static Locator.XPathLocator floatingHeaderContainer()
    {
        return headerContainer().withClass("box-shadow");
    }

    public static Locator.XPathLocator bodyPanel()
    {
        return Locator.tagWithClass("div", "lk-body-ct");
    }

    public static Locator.XPathLocator bodyTitle()
    {
        return Locator.tagWithClassContaining("div", "lk-body-title").childTag("h3");
    }
    public static Locator.XPathLocator bodyTitle(String title)
    {
        return bodyTitle().withText(title);
    }

    public static Locator footerPanel()
    {
        return Locator.byClass("footer-block");
    }

    public static Locator.XPathLocator pageSignal(String signalName)
    {
        return Locator.id("testSignals").childTag("div").withAttribute("name", signalName);
    }
    public static Locator.XPathLocator pageSignal(String signalName, String value)
    {
        return pageSignal(signalName).withAttribute("value", value);
    }

    public static Locator termsOfUseCheckbox()
    {
        return Locator.id("approvedTermsOfUse");
    }
}
