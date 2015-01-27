/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.BVT;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.WebPart;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.fail;

@Category({BVT.class})
public class PortalTest extends BaseWebDriverTest
{
    private static final String WIKI_WEBPART_TEXT = "The Wiki web part displays a single wiki page.";
    private static final String MESSAGES_WEBPART_TEXT = "all messages";
    private final PortalHelper portalHelper = new PortalHelper(this);

    @Nullable
    @Override
    protected String getProjectName()
    {
        return "PortalTest Project";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        PortalTest initTest = (PortalTest)getCurrentTest();

        initTest._containerHelper.createProject(initTest.getProjectName(), null);
    }

    @Test
    public void doWebpartTest()
    {
        String folderName = "webpartTest";

        createSubfolder(getProjectName(), folderName, new String[] {"Messages", "Wiki", "MS2"});
        portalHelper.addWebPart("Messages");
        assertElementPresent(Locator.linkWithText("Messages"));
        portalHelper.addWebPart("Wiki");
        assertElementPresent(Locator.linkWithText("Wiki"));
        assertElementPresent(Locator.linkWithText("Create a new wiki page"));
        portalHelper.addWebPart("Wiki Table of Contents");

        // move messages below wiki:
        assertTextBefore(MESSAGES_WEBPART_TEXT, WIKI_WEBPART_TEXT);
        portalHelper.moveWebPart("Messages", PortalHelper.Direction.DOWN);
        assertTextBefore(WIKI_WEBPART_TEXT, MESSAGES_WEBPART_TEXT);

        refresh();
        // Verify that the asynchronous save worked by refreshing:
        assertTextBefore(WIKI_WEBPART_TEXT, MESSAGES_WEBPART_TEXT);

        WebPart wikiWebPart = new BodyWebPart(this, "Wiki");
        wikiWebPart.delete();

        refresh();
        // verify that the web part removal was correctly saved:
        assertTextNotPresent(WIKI_WEBPART_TEXT);

        // verify that messages is still present:
        assertElementPresent(Locator.linkWithText("Messages"));
        portalHelper.addWebPart("MS2 Runs");
        assertElementPresent(Locator.linkWithText("MS2 Runs"));

        portalHelper.clickWebpartMenuItem("Messages", "Admin");
        assertTextPresent("Customize");
        clickButton("Cancel");
    }

    @Test
    public void doFolderTypeTest()
    {
        final String folderName = "folderTypeTest";
        final List<String> microarrayRequiredWebparts = Arrays.asList(
                "Data Pipeline",
                "Microarray Summary");
        final List<String> microarrayPreferredWebparts = Arrays.asList(
                "Microarray Runs",
                "Assay Runs",
                "Pending MageML Files",
                "Assay List");
        final List<String> collaborationPreferredWebparts = Arrays.asList(
                "Wiki",
                "Messages",
                "Pages");

        createSubfolder(getProjectName(), getProjectName(), folderName, "Microarray", null);

        List<String> currentPreferredWebparts;

        log("Verify microarray folder webparts");
        assertWebparts(microarrayRequiredWebparts, microarrayPreferredWebparts);

        log("Verify webparts after changing folder type");
        _containerHelper.setFolderType("Collaboration");
        currentPreferredWebparts = new ArrayList<>(microarrayRequiredWebparts);
        currentPreferredWebparts.addAll(microarrayPreferredWebparts);
        currentPreferredWebparts.addAll(collaborationPreferredWebparts);
        assertWebparts(Collections.<String>emptyList(), currentPreferredWebparts);

        for (String webpartTitle : microarrayRequiredWebparts)
        {
            portalHelper.removeWebPart(webpartTitle);
        }
        currentPreferredWebparts.removeAll(microarrayRequiredWebparts);
        assertWebparts(Collections.<String>emptyList(), currentPreferredWebparts);

        log("Verify that required webparts get re-added");
        _containerHelper.setFolderType("Microarray");
        assertWebparts(microarrayRequiredWebparts, currentPreferredWebparts);
    }

    @LogMethod
    public void assertWebparts(List<String> requiredWebparts, List<String> preferredWebparts)
    {
        log(requiredWebparts.size() > 0 ? "Assert that required webparts can't be deleted" : "No required webparts");
        for (String webpartTitle : requiredWebparts)
        {
            log("Check required webpart: " + webpartTitle);
            if (isElementPresent(PortalHelper.Locators.sideWebpartTitle.withText(webpartTitle)))
            {
                click(PortalHelper.Locators.webPartTitleMenu(webpartTitle));
                waitForElement(Ext4Helper.Locators.menuItem("Permissions").notHidden());
                assertElementNotPresent(Ext4Helper.Locators.menuItem("Remove From Page").notHidden());
                click(Locator.id("search-input")); // dismiss menu
            }
            else
                assertElementPresent(Locator.xpath("//tr[th[@title='" + webpartTitle + "']]").withPredicate("not(.//a/img[@title='Remove From Page'])"));
        }

        log(preferredWebparts.size() > 0 ? "Assert that preferred webparts can't be deleted" : "No preferred webparts");
        for (String webpartTitle : preferredWebparts)
        {
            log("Check preferred webpart: " + webpartTitle);
            if (isElementPresent(PortalHelper.Locators.sideWebpartTitle.withText(webpartTitle)))
            {
                click(PortalHelper.Locators.webPartTitleMenu(webpartTitle));
                waitForElement(Ext4Helper.Locators.menuItem("Permissions").notHidden());
                assertElementPresent(Ext4Helper.Locators.menuItem("Remove From Page").notHidden());
                click(Locator.id("search-input")); // dismiss menu
            }
            else
                assertElementPresent(Locator.xpath("//tr[th[@title='" + webpartTitle + "']]//a/img[@title='Remove From Page']"));
        }

        List<WebElement> webparts = PortalHelper.Locators.bodyWebpartTitle.findElements(getDriver());
        webparts.addAll(PortalHelper.Locators.sideWebpartTitle.findElements(getDriver()));

        if (requiredWebparts.size() + preferredWebparts.size() != webparts.size())
        {
            List<String> webpartTitles = new ArrayList<>();

            for (WebElement el : webparts)
            {
                webpartTitles.add(el.getText());
            }

            List<String> allExpectedWebparts = new ArrayList<>(requiredWebparts);
            allExpectedWebparts.addAll(preferredWebparts);

            for (String title : webpartTitles)
            {
                if (!allExpectedWebparts.contains(title))
                    fail("Found unexpected webpart: " + title);
            }

            fail("Should be unreachable. Something went wrong in the test. Make sure you are looking for the correct webparts.");
        }
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
