/*
 * Copyright (c) 2013-2016 LabKey Corporation
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

import org.openqa.selenium.SearchContext;

public abstract class Locators
{
    public static final Locator.XPathLocator ADMIN_MENU = Locator.xpath("id('adminMenuPopupLink')[@onclick]");
    public static final Locator.XPathLocator UX_ADMIN_MENU_TOGGLE = Locator.xpath("//li[contains(@class,'dropdown dropdown-rollup') and ./a/i[@class='fa fa-cog']]");
    public static final Locator.IdLocator USER_MENU = Locator.id("userMenuPopupLink");
    public static final Locator.XPathLocator UX_USER_MENU = Locator.xpath("//ul[@class='navbar-nav-lk' and ./li/a/i[@class='fa fa-user']]");
    public static final Locator.IdLocator DEVELOPER_MENU = Locator.id("devMenuPopupLink");
    public static final Locator.IdLocator projectBar = Locator.id("projectBar");
    public static final Locator.XPathLocator UX_PROJECT_LIST_CONTAINER = Locator.xpath("//div[contains(@class, 'project-list-container iScroll') and ./p[@class='title' and contains(text(), 'Projects')]]");
    public static final Locator.XPathLocator UX_PROJECT_LIST = Locator.xpath("//div[@class='project-list']");
    public static final Locator.XPathLocator UX_FOLDER_LIST_CONTAINER = Locator.xpath("//div[contains(@class, 'folder-list-container') and ./p[@class='title' and contains(text(), 'Project Folders & Pages')]]");
    public static final Locator.XPathLocator UX_FOLDER_LIST = Locator.xpath("//div[@class='folder-tree_wrap']");
    public static final Locator.IdLocator folderMenu = Locator.id("folderBar");
    public static final Locator.XPathLocator labkeyError = Locator.tagWithClass("*", "labkey-error");
    public static final Locator UX_SIGNIN_LINK = Locator.xpath("//a[contains(text(),'Sign In')]");
    public static final Locator signInButtonOrLink = Locator.tag("a").withText("Sign\u00a0In"); // Will recognize link [BeginAction] or button [LoginAction]
    public static final Locator.XPathLocator folderTab = Locator.tagWithClass("*", "labkey-folder-header").append(Locator.tagWithClass("ul", "tab-nav")).childTag("li");
    public static final Locator.XPathLocator UX_PAGE_NAV = Locator.xpath("//nav[@class='labkey-page-nav']");
    public static final Locator.XPathLocator UX_FOLDER_TAB = Locator.xpath("//li[contains(@class,'hidden') and ./a[@class='dropdown-toggle' and ./i[contains(@class, 'fa-folder-open')]]]");
    public static final Locator.CssLocator labkeyHeader = Locator.css(".labkey-main .header-block");
    public static final Locator.CssLocator labkeyBody = Locator.css(".labkey-main .body-block");

    public static Locator pageSignal(String signalName)
    {
        return Locator.css("#testSignals > div[name=" + signalName + "]");
    }
    public static Locator pageSignal(String signalName, String value)
    {
        return Locator.css("#testSignals > div[name=" + signalName + "][value=" + value + "]");
    }
}
