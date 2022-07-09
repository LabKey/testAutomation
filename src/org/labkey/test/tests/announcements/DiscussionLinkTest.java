/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.test.categories.Daily;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.core.admin.ProjectSettingsPage;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class DiscussionLinkTest extends BaseWebDriverTest
{
    public static final String WIKI_NAME = "Link test";

    @BeforeClass
    public static void setupProject()
    {
        DiscussionLinkTest init = (DiscussionLinkTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName());
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testDiscussionLink()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Wiki");
        //Create wiki using WikiHelper
        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createNewWikiPage();
        setFormElement(Locator.name("name"), WIKI_NAME);
        wikiHelper.saveWikiPage();
        //Confirm link present using AssertElementPresent by text
        clickAndWait(Locator.linkContainingText(WIKI_NAME));
        waitForElement(Locator.linkContainingText("Discussion"));
        //goto l and feel
        ProjectSettingsPage projectSettingsPage = goToProjectSettings();
        //confirm Enable discussion enabled checked
        Checkbox discussionEnabledCheckbox = projectSettingsPage.getDiscussionEnabledCheckbox();
        assertEquals("Enable Discussion should be checked.", true, discussionEnabledCheckbox.isChecked());

        //un-check Enabled
        discussionEnabledCheckbox.uncheck();
        projectSettingsPage.save();

        //Confirm Discussion link is not present
        goToProjectHome();
        click(Locator.linkContainingText(WIKI_NAME));
        assertElementNotPresent(Locator.linkContainingText("discussion"));
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "DiscussionLinkTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("announcements");
    }
}