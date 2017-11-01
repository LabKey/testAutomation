/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.TourEditor;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.util.List;

@Category({DailyA.class})
public class TourTest extends BaseWebDriverTest
{
    public static final String SUBFOLDER1 = "test project 1";
    public static final String SUBFOLDER2 = "test project 2";
    public static final String TOUR_NAME = "Test Tour";
    public static final String TOUR_DESC = "Test Tour Description";
    public static final int WAIT = 1000;

    @Nullable
    @Override
    protected String getProjectName()
    {
        return "Tour Test Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    @LogMethod
    public static void setup() throws Exception
    {
        TourTest initTest = (TourTest)getCurrentTest();
        initTest._containerHelper.createProject(initTest.getProjectName(), "Collaboration");
        initTest._containerHelper.createSubfolder(initTest.getProjectName(), SUBFOLDER1);
        initTest._containerHelper.createSubfolder(initTest.getProjectName(), SUBFOLDER2);
        initTest.setupBasicTour();
    }

    @LogMethod
    public void setupBasicTour()
    {
        TourEditor tourEditor = navigateToInsertTour(SUBFOLDER1);
        tourEditor.setTitle(TOUR_NAME);
        tourEditor.setMode(TourEditor.TourMode.RUNONCE);
        tourEditor.setDescription(TOUR_DESC);
        tourEditor.setSelector(1, ".navbar-nav-lk .fa-search");
        tourEditor.setStep(1, "{\n" +
                "        placement: \"right\",\n" +
                "        title: \"This is global search.\",\n" +
                "        content: \"Search for it all!\",\n" +
                "}");
        tourEditor.setSelector(2, ".navbar-nav-lk .fa-cog");
        tourEditor.setStep(2, "{\n" +
                "        placement: \"left\",\n" +
                "        title: \"This is the admin menu.\",\n" +
                "        content: \"Click here to perform administrative tasks.\",\n" +
                "}");
        tourEditor.setSelector(3, ".navbar-nav-lk .fa-user");
        tourEditor.setStep(3, "{\n" +
                "        placement: \"left\",\n" +
                "        title: \"This is the help menu.\",\n" +
                "        content: \"Click here for tutorials and various help tasks.\",\n" +
                "}");
        tourEditor.addStep();
        tourEditor.setSelector(4, ".lk-body-title h3");
        tourEditor.setStep(4, "{\n" +
                "        placement: \"bottom\",\n" +
                "        title: \"This is the page title.\",\n" +
                "        content: \"Displays the title of the page you are on.\",\n" +
                "}");
        sleep(WAIT);
        tourEditor.save();
    }

    @Test
    public void runTests()
    {
        getDriver().manage().window().maximize();
        testBasicTour();
        testTourRoundTrip();
        testRunAlways();
        testOffMode();
    }

    public void testBasicTour()
    {
        TourNavigator tourNavigator = new TourNavigator();
        navigateToFolder(SUBFOLDER1);
        assertBasicTour();
        //tour should only run once
        navigateToFolder(SUBFOLDER1);
        tourNavigator.assertNoTourBubble();
    }

    public void testTourRoundTrip()
    {
        TourNavigator tourNavigator = new TourNavigator();
        TourEditor tourEditor = navigateToEditTour(SUBFOLDER1, TOUR_NAME);
        final String tourJSON = tourEditor.export();
        tourEditor = navigateToInsertTour(SUBFOLDER2);
        tourEditor.importTour(tourJSON);
        tourEditor.setTitle(TOUR_NAME);
        tourEditor.save();
        navigateToFolder(SUBFOLDER2);
        assertBasicTour();
        //tour should only run once
        navigateToFolder(SUBFOLDER1);
        tourNavigator.assertNoTourBubble();
    }

    public void testRunAlways()
    {
        TourEditor tourEditor = navigateToEditTour(SUBFOLDER1, TOUR_NAME);
        tourEditor.setMode(TourEditor.TourMode.RUNALWAYS);
        tourEditor.save();
        navigateToFolder(SUBFOLDER1);
        assertBasicTour();
        navigateToFolder(SUBFOLDER1);
        assertBasicTour();
    }

    public void testOffMode()
    {
        TourNavigator tourNavigator = new TourNavigator();
        TourEditor tourEditor = navigateToEditTour(SUBFOLDER1, TOUR_NAME);
        tourEditor.setMode(TourEditor.TourMode.OFF);
        tourEditor.save();
        navigateToFolder(SUBFOLDER1);
        tourNavigator.assertNoTourBubble();
    }

    private void assertBasicTour()
    {
        TourNavigator tourNavigator = new TourNavigator();
        tourNavigator.waitForTourBubble("1");
        tourNavigator.assertTourBubble("1", "This is global search.", "Search for it all!");
        tourNavigator.nextTourBubble();
        tourNavigator.waitForTourBubble("2");
        tourNavigator.assertTourBubble("2", "This is the admin menu.", "Click here to perform administrative tasks.");
        tourNavigator.nextTourBubble();
        tourNavigator.waitForTourBubble("3");
        tourNavigator.assertTourBubble("3", "This is the help menu.", "Click here for tutorials and various help tasks.");
        tourNavigator.nextTourBubble();
        tourNavigator.waitForTourBubble("4");
        tourNavigator.assertTourBubble("4", "This is the page title.", "Displays the title of the page you are on.");
        tourNavigator.doneTourBubble();
        sleep(WAIT);
        tourNavigator.assertNoTourBubble();
    }

    private void navigateToFolder(String folder)
    {
        navigateToFolder(getProjectName(), folder);
    }

    private TourEditor navigateToEditTour(String folder, String tourTitle)
    {
        DataRegionTable table = navigateToTours(folder);
        table.clickEditRow(table.getRowIndex("Title", tourTitle));
        waitForText("Tour Builder");
        return new TourEditor(getDriver());
    }

    private TourEditor navigateToInsertTour(String folder)
    {
        DataRegionTable table = navigateToTours(folder);
        table.clickInsertNewRow();
        waitForText("Tour Builder");
        return new TourEditor(getDriver());
    }

    private DataRegionTable navigateToTours(String folder)
    {
        beginAt("/tours/" + getProjectName() + "/" + folder + "/begin.view");
        return new DataRegionTable("query", getDriver());
    }

    private class TourNavigator
    {
        public void nextTourBubble()
        {
            click(Locator.tagWithClass("button", "hopscotch-nav-button next hopscotch-next").withText("Next"));
        }

        public void doneTourBubble()
        {
            click(Locator.tagWithClass("button", "hopscotch-nav-button next hopscotch-next").withText("Done"));
        }

        public void assertNoTourBubble()
        {
            if (isElementPresent(Locator.tag("div").withAttributeContaining("class", "hopscotch-bubble animated hide")))
                return;
            assertElementNotPresent(Locator.tag("div").withAttributeContaining("class", "hopscotch"));
        }

        private void assertTourBubble(String number, String title, String content)
        {
            waitForTourBubble(number);
            Assert.assertEquals(number, getText(Locator.tagWithClass("span", "hopscotch-bubble-number")));
            Assert.assertEquals(title, getText(Locator.tagWithClass("h3", "hopscotch-title")));
            Assert.assertEquals(content, getText(Locator.tagWithClass("div", "hopscotch-content")));
        }

        private void waitForTourBubble(String number)
        {
            waitForElement(Locator.tagWithClass("span", "hopscotch-bubble-number").withText(number));
            sleep(750); // time for animation to complete
        }
    }
}
