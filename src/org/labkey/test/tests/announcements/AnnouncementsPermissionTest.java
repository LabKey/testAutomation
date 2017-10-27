/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.tests.announcements;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyC;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.List;

@Category({DailyC.class})
public class AnnouncementsPermissionTest extends BaseWebDriverTest
{
    PortalHelper portalHelper = new PortalHelper(this);
    public static final String NOT_CONTRIBUTOR_ONLY_TITLE = "Not-Contributor-only title";
    public static final String NOT_CONTRIBUTOR_ONLY_MESSAGE = "Not-Contributor-only message";
    private static final String CONTRIBUTOR = "contributor@messages.test";
    private static final String MSG5_TITLE = "test message 5";
    private String TEST_GROUP = "contributorTestGroup";
    private String PERMISSION = "Message Board Contributor";

    @BeforeClass
    public static void setupProject()
    {
        AnnouncementsPermissionTest init = (AnnouncementsPermissionTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();
    }

    @Test
    public void doTestMessageContributorRole()
    {
        clickProject(getProjectName());
        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.createPermissionsGroup(TEST_GROUP, CONTRIBUTOR);
        permissionsHelper.setPermissions(TEST_GROUP, PERMISSION);

        //As other role add a message
        clickProject(getProjectName());
        portalHelper.addWebPart("Messages");
        portalHelper.clickWebpartMenuItem("Messages", true, "New");
        setFormElement(Locator.name("title"), NOT_CONTRIBUTOR_ONLY_TITLE);
        setFormElement(Locator.id("body"), NOT_CONTRIBUTOR_ONLY_MESSAGE);
        clickButton("Submit", longWaitForPage);
        //Confirm message
        impersonate(CONTRIBUTOR);
        portalHelper.clickWebpartMenuItem("Messages", true, "New");
        setFormElement(Locator.name("title"), MSG5_TITLE);
        setFormElement(Locator.id("body"), "Contributor message");
        clickButton("Submit", longWaitForPage);
        assertTextPresent(MSG5_TITLE);
        clickAndWait(Locator.linkWithText("view message or respond"));
        assertElementPresent(Locator.linkWithSpan("Delete Message"));//Confirm here to legitimize not-present assert later.
        clickButton("Delete Message");
        clickButton("Delete");

        //confirm can read other user's message
        clickAndWait(Locator.linkWithText(NOT_CONTRIBUTOR_ONLY_TITLE));

        //confirm cannot delete other user's message
        assertElementNotPresent(Locator.linkWithSpan("Delete Message"));//Confirm here to legitimize not-present assert later.

        //confirm can respond to other user's message
        clickButton("Respond");
        String contributorResponse = "Contributor response";
        setFormElement(Locator.id("body"), contributorResponse);
        clickButton("Submit");
        assertTextPresent(contributorResponse);
    }


    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "AnnouncementsPermissionTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("announcements");
    }
}