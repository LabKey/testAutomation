/*
 * Copyright (c) 2007-2008 LabKey Corporation
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


public class MessagesTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "MessagesVerifyProject";

    private static final String EXPIRES = "2108-07-19";
    private static final String MSG1_TITLE = "test message 1";
    private static final String MSG1_BODY_FIRST = "this is a test message";
    private static final String MSG1_BODY = "this is a test message to Banana";
    private static final String RESP1_TITLE = "test response 1";
    private static final String RESP1_BODY = "this is another test, thanks";

    public String getAssociatedModuleDirectory()
    {
        return "announcements";
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
        clickLinkWithText("Customize Folder");
        checkCheckbox(Locator.checkboxByNameAndValue("folderType", "Collaboration", true));
        submit();
        addWebPart("Search");

        log("Check that Plain Text message works and is added everywhere");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("new message");
        setFormElement("title", MSG1_TITLE);
        setFormElement("body", MSG1_BODY_FIRST);
        selectOptionByText("rendererType", "Plain Text");

        log("test attachments too");
        if (isFileUploadAvailable())
        {
            clickLinkWithText("Attach a file", false);
            File file = new File(getLabKeyRoot() + "/common.properties");
            setFormElement("formFiles[0]", file);
        }
        else
            log("File upload skipped.");
        submit();
        if (isFileUploadAvailable())
            assertTextPresent("common.properties");
        assertTextPresent(MSG1_BODY_FIRST);
        clickLinkWithText("view list");
        assertTextPresent(MSG1_TITLE);
        clickLinkWithText("Messages");
        clickLinkWithText(MSG1_TITLE);

        log("test edit messages");
        clickLinkWithText("edit");
        setFormElement("body", MSG1_BODY);
        if (isFileUploadAvailable())
            assertTextPresent("Delete");
        submit();
        assertTextPresent(MSG1_BODY);

        log("test add response");
        clickNavButton("Post Response");
        setFormElement("title", RESP1_TITLE);
        setFormElement("expires", EXPIRES);
        setFormElement("body", RESP1_BODY);
        submit();

        log("Make sure response was entered correctly");
        assertTextPresent(RESP1_TITLE);
        assertTextPresent(EXPIRES);
        assertTextPresent(RESP1_BODY);


        log("test the search module on messages");
        clickLinkWithText(PROJECT_NAME);
        searchFor(PROJECT_NAME, "Banana", 1, RESP1_TITLE);

        log("test delete message works and is recognized");
        clickNavButton("Delete Message");
        clickNavButton("Delete");
        assertTextNotPresent(MSG1_TITLE);
        assertTextNotPresent(RESP1_TITLE);
    }
}
