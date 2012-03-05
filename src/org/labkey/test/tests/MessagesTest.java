/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

package org.labkey.test.tests;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;


public class MessagesTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "MessagesVerifyProject";

    private static final String EXPIRES = "2108-07-19";
    private static final String MSG1_TITLE = "test message 1";
    private static final String MSG1_BODY_FIRST = "this is a test message";
    private static final String MSG1_BODY = "this is a test message to Banana";
    private static final String RESP1_TITLE = "test response 1";
    private static final String RESP1_BODY = "this is another test, thanks";

    String user = "message_user@gmail.com";
    String group = "Message group";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/announcements";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doCleanup()
    {
        deleteProject(PROJECT_NAME, false);
        deleteUser(user, false);
        deleteGroup(group);
    }

    protected void doTestSteps()
    {
        log("Create Project");
        createProject(PROJECT_NAME);
        clickAdminMenuItem("Project", "Management");
        clickLinkContainingText("Folder Settings");
        checkRadioButton(Locator.radioButtonByNameAndValue("folderType", "Collaboration"));
        submit();
        addWebPart("Search");
        createUser(user, null);
        goToHome();
        goToProjectHome();
        createPermissionsGroup(group);
        setPermissions(group, "Editor");
//        add
        addUserToProjGroup(user, getProjectName(), group);
        goToProjectHome();


        enableEmailRecorder();

        log("Check that Plain Text message works and is added everywhere");
        clickLinkWithText(PROJECT_NAME);
        clickNavButton("New");

        // Check defaults for uncustomized message board
        assertTextNotPresent("Status");
        assertTextNotPresent("Assigned To");
        assertTextNotPresent("Members");
        assertTextNotPresent("Expires");

        setFormElement("title", MSG1_TITLE);
        setFormElement("body", MSG1_BODY_FIRST);
        selectOptionByText("rendererType", "Plain Text");

        log("test attachments too");
        if (isFileUploadAvailable())
        {
            clickLinkWithText("Attach a file", false);
            File file = new File(getLabKeyRoot() + "/common.properties");
            setFormElement("formFiles[00]", file);
        }
        else
            log("File upload skipped.");
        submit();
        if (isFileUploadAvailable())
            assertTextPresent("common.properties");
        assertTextPresent(MSG1_BODY_FIRST);
        clickLinkWithText("view message or respond");
        clickLinkWithText("view list");
        assertTextPresent(MSG1_TITLE);
        goToModule("Messages");
        clickLinkWithText("Messages");
        clickLinkWithText(MSG1_TITLE);

        log("test edit messages");
        clickLinkWithText("edit");
        setFormElement("body", MSG1_BODY);
        if (isFileUploadAvailable())
        {
            assertTextPresent("remove");
            clickLinkWithText("remove", false);
            waitForText("This cannot be undone");
            clickButton("OK", 0);
            waitForTextToDisappear("remove");
            assertTextNotPresent("common.properties");
        }
        submit();
        assertTextPresent(MSG1_BODY);


        log("verify a user can subscribe to a thread");
        impersonate(user);
        goToProjectHome();
        clickLinkContainingText("view message");
        Locator subscribeButton = Locator.tagWithText("span", "subscribe");
        assertElementPresent(subscribeButton);
        click(subscribeButton);
        click(Locator.tagWithText("span", "thread"));
        waitForPageToLoad();
//        clickButton("Update");
//        clickButton("Done");
        clickLinkWithText("unsubscribe");
        assertElementPresent(subscribeButton);

        click(subscribeButton);
        click(Locator.tagWithText("span", "forum"));
        waitForPageToLoad();
        clickButton("Update");
        clickButton("Done");


        stopImpersonating();
        goToProjectHome();


        log("test customize");
        clickLinkWithText("Messages");
        clickLinkWithText("Customize");
        checkCheckbox("expires");
        clickNavButton("Save");

        log("test add response");
        clickLinkWithText("view message or respond");
        clickNavButton("Respond");
        setFormElement("expires", EXPIRES);
        setFormElement("title", RESP1_TITLE);
        setFormElement("body", RESP1_BODY);
        submit();

        log("Make sure response was entered correctly");
        assertTextPresent(RESP1_TITLE);
        assertTextPresent(EXPIRES);
        assertTextPresent(RESP1_BODY);


        log("test the search module on messages");
        clickLinkWithText(PROJECT_NAME);
        searchFor(PROJECT_NAME, "Banana", 1, MSG1_TITLE);
        
        schemaTest();

        log("test delete message works and is recognized");
        clickNavButton("Delete Message");
        clickNavButton("Delete");
        assertTextNotPresent(MSG1_TITLE);
        assertTextNotPresent(RESP1_TITLE);
    }

    private void schemaTest()
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        SelectRowsCommand selectCmd = new SelectRowsCommand("announcement", "ForumSubscription");
        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(ContainerFilter.AllFolders);
        selectCmd.setColumns(Arrays.asList("*"));
        SelectRowsResponse selectResp = null;

        String[] queries = {"Announcement", "AnnouncementSubscription", "EmailFormat", "EmailOption", "ForumSubscription"};
        int[] counts = {2, 0, 2, 5, 1};

        for(int i = 0; i<queries.length; i++)
        {
            selectCmd.setQueryName(queries[i]);
            try
            {
                selectResp = selectCmd.execute(cn, "/" + getProjectName());
            }
            catch (Exception e)
            {
               fail(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
            }
            assertEquals(counts[i],selectResp.getRowCount().intValue());
        }
    }
}
