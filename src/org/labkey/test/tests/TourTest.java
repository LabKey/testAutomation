package org.labkey.test.tests;

import junit.framework.Assert;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.TourEditor;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.NoSuchElementException;

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
        initTest._containerHelper.createSubfolder(initTest.getProjectName(), SUBFOLDER1, null);
        initTest._containerHelper.createSubfolder(initTest.getProjectName(), SUBFOLDER2, null);
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
        tourEditor.setSelector(1, "span#adminMenuPopupText");
        tourEditor.setStep(1, "{\n" +
                "        placement: \"left\",\n" +
                "        title: \"This is the admin menu.\",\n" +
                "        content: \"Click here to perform administrative tasks.\",\n" +
                "}");
        tourEditor.setSelector(2, "select");
        tourEditor.setStep(2, "{\n" +
                "        placement: \"right\",\n" +
                "        title: \"This is a webpart.\",\n" +
                "        content: \"What can't you do in a webpart!\",\n" +
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
        sleep(WAIT_FOR_JAVASCRIPT);
        tourEditor.save();
    }

    @Test
    public void runTests()
    {
        testBasicTour();
        testTourRoundTrip();
    }

    public void testBasicTour()
    {
        goToProjectHome();
        clickFolder(SUBFOLDER1);
        assertTourBubble("1", "This is the admin menu.", "Click here to perform administrative tasks.");
        dismissTourBubble();
        sleep(WAIT);
        assertTourBubble("2", "This is a webpart.", "What can't you do in a webpart!");
        dismissTourBubble();
        sleep(WAIT);
        assertTourBubble("3", "This is the help menu.", "Click here for tutorials and various help tasks.");
        dismissTourBubble();
        sleep(WAIT);
        assertTourBubble("4", "This is the folder menu.", "Use the links here to navigate to different folders.");
        dismissTourBubble();
        sleep(WAIT);
        assertNoTourBubble();
        //tour should only run once
        goToProjectHome();
        clickFolder(SUBFOLDER1);
        assertNoTourBubble();
    }

    public void testTourRoundTrip()
    {
        beginAt("/tours/" + getProjectName() + "/" + SUBFOLDER1 + "/begin.view");
        waitForText("Tours");
        click(Locator.linkWithText("Edit"));
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
        clickFolder(SUBFOLDER2);
        assertTourBubble("1", "This is the admin menu.", "Click here to perform administrative tasks.");
        dismissTourBubble();
        sleep(WAIT);
        assertTourBubble("2", "This is a webpart.", "What can't you do in a webpart!");
        dismissTourBubble();
        sleep(WAIT);
        assertTourBubble("3", "This is the help menu.", "Click here for tutorials and various help tasks.");
        dismissTourBubble();
        sleep(WAIT);
        assertTourBubble("4", "This is the folder menu.", "Use the links here to navigate to different folders.");
        dismissTourBubble();
        sleep(WAIT);
        assertNoTourBubble();
        //tour should only run once
        goToProjectHome();
        clickFolder(SUBFOLDER1);
        assertNoTourBubble();
    }

    private void assertTourBubble(String number, String title, String content)
    {
        Assert.assertEquals(getText(Locator.tagWithClass("span", "hopscotch-bubble-number")), number);
        Assert.assertEquals(getText(Locator.tagWithClass("h3", "hopscotch-title")), title);
        Assert.assertEquals(getText(Locator.tagWithClass("div", "hopscotch-content")), content);
    }

    private void dismissTourBubble()
    {
        try
        {
            click(Locator.button("Next"));
        }
        catch (NoSuchElementException ignore)
        {
            click(Locator.button("Done"));
        }
    }

    private void assertNoTourBubble()
    {
        if(isElementPresent(Locator.divByClassContaining("hopscotch-bubble animated hide"))) return;
        assertElementNotPresent(Locator.divByClassContaining("hopscotch"));
    }
}
