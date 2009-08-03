/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.test.drt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;

/**
 * User: brittp
 * Date: Nov 15, 2005
 * Time: 1:55:56 PM
 */
public class WikiTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "WikiVerifyProject";

    private static final String WIKI_PAGE_ALTTITLE = "PageBBB has HTML";
    private static final String WIKI_PAGE_WEBPART_TEST = "Best Gene Name";
    private static final String WIKI_PAGE_TITLE = "Test Wiki";
    private static final String WIKI_PAGE_CONTENT =
            "<b>Some HTML content</b>\n" +
                    "<b>${labkey.webPart(partName='Query', title='My Proteins', schemaName='ms2', " +
                    "queryName='Sequences', allowChooseQuery='true', allowChooseView='true')}</b>\n";

    public String getAssociatedModuleDirectory()
    {
        return "wiki";
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        log("Create Project");
        createProject(PROJECT_NAME);
        clickLinkWithText("Folder Settings");
        checkCheckbox(Locator.checkboxByTitle("Wiki"));
        submit();

        clickLinkWithText(PROJECT_NAME);
        addWebPart("Wiki");
        addWebPart("Search");

        log("test create new html page with a webpart");
        createNewWikiPage("HTML");

        setFormElement("name", WIKI_PAGE_TITLE);
        setFormElement("title", WIKI_PAGE_TITLE);
        setWikiBody(WIKI_PAGE_CONTENT);

        log("test attachments in wiki");
        if (isFileUploadAvailable())
        {
            File file = new File(getLabKeyRoot() + "/common.properties");
            setFormElement("formFiles[0]", file);
        }
        else
            log("File upload skipped.");
        saveWikiPage();

        if (isFileUploadAvailable())
            assertTextPresent("common.properties");
        assertTextPresent(WIKI_PAGE_WEBPART_TEST);
        assertTextPresent("Some HTML content");

        log("test search wiki");
        searchFor(PROJECT_NAME, "Wiki", 1, WIKI_PAGE_TITLE);

        log("test edit wiki");
        clickLinkWithText("edit");
        setFormElement("title", WIKI_PAGE_ALTTITLE);
        String wikiPageContentEdited =
            "<b>Some HTML content</b><br>\n" +
            "<b>More HTML content</b><br>\n";
        setWikiBody(wikiPageContentEdited);
        saveWikiPage();

        assertTextPresent("More HTML content");
        assertTextPresent(WIKI_PAGE_ALTTITLE);

        log("test delete wiki");
        clickLinkWithText("edit", 0);
        waitAndClickNavButton("Delete Page");
        clickNavButton("Delete");
        assertTextNotPresent(WIKI_PAGE_ALTTITLE);
    }
}
