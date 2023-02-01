package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class StudySurveyTest extends BaseWebDriverTest
{
    PortalHelper portalHelper = new PortalHelper(this);
    private String datasetName = "SampleDataset";
    private String surveyDesignName = "Dataset Survey Design";

    @BeforeClass
    public static void doSetup()
    {
        StudySurveyTest initTest = (StudySurveyTest) getCurrentTest();
        initTest.setupProject();
    }

    public static String getDate(int incrementDays)
    {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime date = LocalDateTime.now().plusDays(incrementDays);
        return dtf.format(date);
    }

    @Override
    protected String getProjectName()
    {
        return "Study Survey Test Project";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    private void setupProject()
    {
        _containerHelper.createProject(getProjectName(), "Study");
        clickButton("Create Study");
        click(Locator.radioButtonById("continuousTimepointType"));
        clickButton("Create Study");
        _containerHelper.enableModule("Survey");

        goToProjectHome();
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Datasets");
        portalHelper.addWebPart("Survey Designs");
        portalHelper.exitAdminMode();
    }

    @Test
    public void testUpdateSurveyDatasetDate()
    {
        goToProjectHome();
        String surveyLabel = "Study Survey";

        log("Creating a new dataset");
        _studyHelper.goToManageDatasets()
                .clickCreateNewDataset()
                .setName(datasetName)
                .clickSave();

        gotoDataset(datasetName);
        DataRegionTable table = new DataRegionTable("Dataset", getDriver());
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_ParticipantId"), "1");
        setFormElement(Locator.name("quf_date"), getDate(-1));
        clickButton("Submit");

        goToProjectHome();
        createSurveyDesign(surveyDesignName, null, "study", datasetName, null);

        addSurveyWebpart(surveyDesignName);
        clickButton("Create Survey", WAIT_FOR_JAVASCRIPT);
        waitForText("Survey Label*");
        setFormElement(Locator.name("_surveyLabel_"), surveyLabel);
        setFormElement(Locator.name("participantid"), "1");
        setFormElement(Locator.name("date"), getDate(0));
        doAndWaitForPageToLoad(() ->
        {
            final WebElement submitButton = Ext4Helper.Locators.ext4Button("Submit completed form").findElement(getDriver());
            shortWait().withMessage("Submit button not enabled").until(wd -> !submitButton.getAttribute("class").contains("disabled"));
            submitButton.click();
            _extHelper.waitForExtDialog("Success");
        });

        String newDate = getDate(2);
        goToProjectHome();
        clickEditForLabel("Surveys: " + surveyDesignName, surveyLabel);
        setFormElement(Locator.name("date"), newDate);
        waitAndClick(Ext4Helper.Locators.ext4ButtonEnabled("Save"));
        _extHelper.waitForExtDialog("Success");
        _extHelper.waitForExtDialogToDisappear("Success");

        goToProjectHome();
        clickEditForLabel("Surveys: " + surveyDesignName, surveyLabel);

        //Test is verifying that edited date is not messing up the survey.
        //https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=39291

        WebElement dateField = Locator.name("date").findWhenNeeded(getDriver());
        waitFor(dateField::isDisplayed, "Date field never showed up.", 1_000);
        checker().verifyEquals("Edited date is incorrect", newDate, getFormElement(dateField));
    }

    private void gotoDataset(String datasetName)
    {
        goToProjectHome();
        clickAndWait(Locator.linkContainingText("dataset"));
        clickAndWait(Locator.linkWithText(datasetName));
    }

    private void addSurveyWebpart(String surveyDesignName)
    {
        log("Configure Surveys webpart");
        portalHelper.addWebPart("Surveys");
        waitForElement(Locator.css(".survey-designs-loaded-marker"));
        _ext4Helper.selectComboBoxItem("Survey Design:", surveyDesignName);
        clickButton("Submit");
        waitForText("Surveys: " + surveyDesignName);
    }

    private void clickEditForLabel(String webPartTitle, String label)
    {
        waitForText(label);
        DataRegionTable dt = DataRegionTable.findDataRegionWithinWebpart(this, webPartTitle);
        dt.clickEditRow(dt.getRowIndex("Label", label));
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }

}
