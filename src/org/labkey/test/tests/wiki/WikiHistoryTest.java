/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

package org.labkey.test.tests.wiki;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyC;
import org.labkey.test.categories.Wiki;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;

import java.util.Arrays;
import java.util.List;

// Based on WikiTest

@Category({DailyC.class, Wiki.class})
public class WikiHistoryTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "WikiHistoryVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private static final String SUBFOLDER_DESTINATION_NAME = "WikiHistoryVerifyDestinationFolder" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;

    private static final String WIKI_PAGE_ALTTITLE = "PageBBB has HTML";
    private static final String WIKI_PAGE_ALTTITLE_2 = "The third wiki page title";

    private static final String WIKI_PAGE_WEBPART_ID = "qwp999";
    private static final String WIKI_PAGE_TITLE = "_Test Wiki";
    private static final String WIKI_PAGE_CONTENT =
            "<b>Some HTML content</b>\n" +
                    "<b>${labkey.webPart(partName='Query', title='Users', schemaName='core', " +
                    "queryName='Users', allowChooseQuery='true', allowChooseView='true', dataRegionName='" + WIKI_PAGE_WEBPART_ID + "')}</b>\n";
    private static final String WIKI_CHECK_CONTENT = "More HTML content";
    private static final String WIKI_CHECK_CONTENT_2 = "Amazing HTML content here";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("wiki");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testSteps()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        WikiHelper wikiHelper = new WikiHelper(this);

        log("Create Project");
        _containerHelper.createProject(PROJECT_NAME, null);

        goToAdminConsole().clickFullTextSearch();
        if (isTextPresent("pause crawler"))
            clickButton("pause crawler");
        beginAt(getDriver().getCurrentUrl().replace("admin.view", "waitForIdle.view"), 10 * defaultWaitForPage);

        clickProject(PROJECT_NAME);
        portalHelper.addWebPart("Wiki");

        log("test create new html page with a webpart");
        wikiHelper.createNewWikiPage("HTML");

        setFormElement(Locator.name("name"), WIKI_PAGE_TITLE);
        setFormElement(Locator.name("title"), WIKI_PAGE_TITLE);
        wikiHelper.setWikiBody(WIKI_PAGE_CONTENT);
        wikiHelper.saveWikiPage();

        DataRegionTable wikiDataRegion = new DataRegionTable(WIKI_PAGE_WEBPART_ID, getDriver());  // just waiting on the wiki data region
        final Locator.XPathLocator wikiTitleLink = Locator.linkContainingText("_Test Wiki").withAttribute("href");
        assertElementPresent(wikiTitleLink);

        log("test edit wiki");
        clickAndWait(Locator.linkContainingText(WIKI_PAGE_TITLE));
        clickAndWait(Locator.linkWithText("Edit"));
        setFormElement(Locator.name("title"), WIKI_PAGE_ALTTITLE);
        String wikiPageContentEdited =
                "<b>Some HTML content</b><br>\n" +
                "<b>" + WIKI_CHECK_CONTENT + "</b><br>\n";
        wikiHelper.setWikiBody(wikiPageContentEdited);
        wikiHelper.saveWikiPage();
        waitForText(WIKI_CHECK_CONTENT);
        assertTextPresent(WIKI_PAGE_ALTTITLE);

        log("test editing wiki again");
        clickAndWait(Locator.linkContainingText(WIKI_PAGE_ALTTITLE));
        clickAndWait(Locator.linkWithText("Edit"));
        setFormElement(Locator.name("title"), WIKI_PAGE_ALTTITLE_2);
        wikiPageContentEdited =
                "<b>Some HTML content</b><br>\n" +
                        "<b>" + WIKI_CHECK_CONTENT_2 + "</b><br>\n";
        wikiHelper.setWikiBody(wikiPageContentEdited);
        wikiHelper.saveWikiPage();
        waitForText(WIKI_CHECK_CONTENT_2);
        assertTextPresent(WIKI_PAGE_ALTTITLE_2);

        log("create subfolder for wiki copying");
        _containerHelper.createSubfolder(PROJECT_NAME, SUBFOLDER_DESTINATION_NAME);

        log("test copying wiki without history");
        beginAt(WebTestHelper.buildURL("project", PROJECT_NAME, "begin"));
        clickAndWait(Locator.linkContainingText(WIKI_PAGE_ALTTITLE_2));
        wikiHelper.copyAllWikiPages(SUBFOLDER_DESTINATION_NAME, false);
        clickAndWait(Locator.linkWithText("History"));
        assertTextNotPresent(WIKI_PAGE_ALTTITLE);  // can't check first title because it's part of the title of the history page

        log("test copying wiki with history");
        // delete previously copied wiki first though
        clickAndWait(Locator.linkWithText("Return To Page"));
        clickAndWait(Locator.linkWithText("Manage"));
        clickAndWait(Locator.linkWithText("Delete"));
        clickAndWait(Locator.linkWithText("Delete"));
        beginAt(WebTestHelper.buildURL("project", PROJECT_NAME, "begin"));
        clickAndWait(Locator.linkContainingText(WIKI_PAGE_ALTTITLE_2));
        wikiHelper.copyAllWikiPages(SUBFOLDER_DESTINATION_NAME, true);
        clickAndWait(Locator.linkWithText("History"));
        assertTextPresent(WIKI_PAGE_ALTTITLE);
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
