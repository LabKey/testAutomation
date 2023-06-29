/*
 * Copyright (c) 2013-2019 LabKey Corporation
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
    public static final Locator documentRoot = Locator.css(":root");
    public static final Locator.IdLocator folderMenu = Locator.id("folderBar");
    public static final Locator.XPathLocator labkeyError = Locator.byClass("labkey-error");
    public static final Locator.XPathLocator labkeyErrorHeading = Locator.byClass("labkey-error-heading");
    public static final Locator.XPathLocator labkeyErrorSubHeading = Locator.byClass("labkey-error-subheading");
    public static final Locator.XPathLocator labkeyErrorInstruction = Locator.byClass("labkey-error-instruction");
    public static final Locator.XPathLocator labkeyMessage = Locator.byClass("labkey-message");
    public static final Locator signInLink = Locator.tagWithAttributeContaining("a", "href", "login.view");
    public static final Locator.XPathLocator folderTab = Locator.tagWithClass("div", "lk-nav-tabs-ct").append(Locator.tagWithClass("ul", "lk-nav-tabs")).childTag("li");
    public static final Locator.XPathLocator panelWebpartTitle = Locator.byClass("labkey-wp-title-text");

    public static Locator.XPathLocator headerContainer()
    {
        return Locator.byClass("lk-header-ct");
    }

    public static Locator.XPathLocator floatingHeaderContainer()
    {
        return headerContainer().withClass("box-shadow");
    }

    public static Locator.XPathLocator appFloatingHeader()
    {
        return Locator.tag("div").withClass("app-navigation");
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


    public static Locator brandLink()
    {
        return Locator.byClass("brand-link");
    }

    public static Locator bannerPanel()
    {
        return Locator.byClass("row content-banner");
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
