/*
 * Copyright (c) 2015 LabKey Corporation
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
import org.labkey.test.util.LogMethod;

import java.util.List;

/**
 * Created by RyanS on 2/23/2015.
 */
@Category({DailyA.class})
public class TourTest extends BaseWebDriverTest
{
    public static final String PROJECT_NAME = "Tour Test Project";
    public static final String SUBFOLDER1 = "test project 1";
    public static final String SUBFOLDER2 = "test project 2";
    public static final String TOUR_NAME = "Test Tour";
    public static final String TOUR_DESC = "Test Tour Description";
    public static final int WAIT = 1000;

    @Nullable
    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
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
        beginAt("/tours/" + getProjectName() + "/" + SUBFOLDER1 + "/begin.view");
        waitForText("Tours");
        waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        waitForText("Tour Builder");
        TourEditor tourEditor = new TourEditor(this);
        tourEditor.setTitle("Test Tour");
        tourEditor.setMode(TourEditor.TourMode.RUNONCE);
        tourEditor.setDescription(TOUR_DESC);
        tourEditor.setSelector(1, "select");
        tourEditor.setStep(1, "{\n" +
                "        placement: \"right\",\n" +
                "        title: \"This is a webpart.\",\n" +
                "        content: \"What can't you do in a webpart!\",\n" +
                "}");
        tourEditor.setSelector(2, "span#adminMenuPopupText");
        tourEditor.setStep(2, "{\n" +
                "        placement: \"left\",\n" +
                "        title: \"This is the admin menu.\",\n" +
                "        content: \"Click here to perform administrative tasks.\",\n" +
                "}");
        tourEditor.setSelector(3, "span#helpMenuPopupText");
        tourEditor.setStep(3, "{\n" +
                "        placement: \"left\",\n" +
                "        title: \"This is the help menu.\",\n" +
                "        content: \"Click here for tutorials and various help tasks.\",\n" +
                "}");
        tourEditor.addStep();
        tourEditor.setSelector(4, "li#folderBar.menu-folders");
        tourEditor.setStep(4, "{\n" +
                "        placement: \"bottom\",\n" +
                "        title: \"This is the folder menu.\",\n" +
                "        content: \"Use the links here to navigate to different folders.\",\n" +
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
        goToProjectHome();
        sleep(WAIT);
        clickFolder(SUBFOLDER1);
        assertBasicTour();
        //tour should only run once
        goToProjectHome();
        sleep(WAIT);
        clickFolder(SUBFOLDER1);
        tourNavigator.assertNoTourBubble();
    }

    public void testTourRoundTrip()
    {
        TourNavigator tourNavigator = new TourNavigator();
        beginAt("/tours/" + getProjectName() + "/" + SUBFOLDER1 + "/begin.view");
        waitForText("Tours");
        clickAndWait(Locator.linkWithText("Edit"));
        TourEditor tourEditor = new TourEditor(this);
        String tourJSON = tourEditor.export();
        beginAt("/tours/" + getProjectName() + "/" + SUBFOLDER2 + "/begin.view");
        waitForText("Tours");
        waitAndClick(Locator.xpath("//span[text()='Insert New']"));
        waitForText("Tour Builder");
        tourEditor = new TourEditor(this);
        tourEditor.importTour(tourJSON);
        tourEditor.setTitle(TOUR_NAME);
        tourEditor.save();
        goToProjectHome();
        sleep(WAIT);
        clickFolder(SUBFOLDER2);
        assertBasicTour();
        //tour should only run once
        goToProjectHome();
        sleep(WAIT);
        clickFolder(SUBFOLDER1);
        tourNavigator.assertNoTourBubble();
    }

    public void testRunAlways()
    {
        beginAt("/tours/" + getProjectName() + "/" + SUBFOLDER1 + "/begin.view");
        waitForText("Tours");
        clickAndWait(Locator.linkWithText("Edit"));
        TourEditor tourEditor = new TourEditor(this);
        tourEditor.setMode(TourEditor.TourMode.RUNALWAYS);
        tourEditor.save();
        goToProjectHome();
        sleep(WAIT);
        clickFolder(SUBFOLDER1);
        assertBasicTour();
        goToProjectHome();
        sleep(WAIT);
        clickFolder(SUBFOLDER1);
        assertBasicTour();
    }

    public void testOffMode()
    {
        TourNavigator tourNavigator = new TourNavigator();
        beginAt("/tours/" + getProjectName() + "/" + SUBFOLDER1 + "/begin.view");
        waitForText("Tours");
        clickAndWait(Locator.linkWithText("Edit"));
        TourEditor tourEditor = new TourEditor(this);
        tourEditor.setMode(TourEditor.TourMode.OFF);
        tourEditor.save();
        goToProjectHome();
        sleep(WAIT);
        clickFolder(SUBFOLDER1);
        tourNavigator.assertNoTourBubble();
    }

    private void assertBasicTour()
    {
        TourNavigator tourNavigator = new TourNavigator();
        tourNavigator.assertTourBubble("1", "This is a webpart.", "What can't you do in a webpart!");
        tourNavigator.nextTourBubble();
        tourNavigator.waitForTourBubble("2");
        tourNavigator.assertTourBubble("2", "This is the admin menu.", "Click here to perform administrative tasks.");
        tourNavigator.nextTourBubble();
        tourNavigator.waitForTourBubble("3");
        tourNavigator.assertTourBubble("3", "This is the help menu.", "Click here for tutorials and various help tasks.");
        tourNavigator.nextTourBubble();
        tourNavigator.waitForTourBubble("4");
        tourNavigator.assertTourBubble("4", "This is the folder menu.", "Use the links here to navigate to different folders.");
        tourNavigator.doneTourBubble();
        sleep(WAIT);
        tourNavigator.assertNoTourBubble();
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
            if(isElementPresent(Locator.divByClassContaining("hopscotch-bubble animated hide"))) return;
            assertElementNotPresent(Locator.divByClassContaining("hopscotch"));
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
        }
    }
}
