package org.labkey.test.tests.component;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ui.entities.EntityInsertPanel;
import org.labkey.test.components.ui.entities.EntityUpdateFromFilePanel;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Category({Daily.class})
public class EntityInsertPanelTest extends BaseWebDriverTest
{
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        EntityInsertPanelTest init = (EntityInsertPanelTest) getCurrentTest();

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
    public void testInsertSamplesFromGrid() throws Exception
    {
        String sampleTypeName = "insert_from_edit_grid";
        SampleTypeDefinition props = new SampleTypeDefinition(sampleTypeName).setFields(standardTestSampleFields());
        TestDataGenerator dgen = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props);
        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        EntityInsertPanel testPanel = testPage.getEntityInsertPanel();
        testPanel.targetEntityTypeSelect().select(sampleTypeName);

        String pasteText = "ed\tbrother\tthe quiet one\t\t2\tstringfellow\t11/11/2020\ttrue\n" +
                "jed\tother brother\tthe squinty one\t\t3\tstrongfellow\t11/11/2020\tfalse\n" +
                "ted\tother other brother\tisn't sure about the others\t\t4\tstrangefellow\t11/11/2020\ttrue";

        testPanel.clickAddRows()
                .getEditableGrid().pasteFromCell(0, "Name *", pasteText);
        WebElement success = clickFinishExpectingSuccess("Finish Creating 3 Samples");
        assertThat(success.getText(), is("Created 3 samples in sample type 'insert_from_edit_grid'."));
        List<Map<String, Object>> insertedRows = dgen.getRowsFromServer(createDefaultConnection()).getRows();
        assertThat(insertedRows.size(), is(3));
        Map<String, Object> ed_row = insertedRows.stream().filter(a-> a.get("name").equals("ed")).findFirst().get();
        assertThat(ed_row.get("intColumn"), is(2));
        // eventually, parse the date out of dateColumn
        assertThat(ed_row.get("stringColumn"), is("stringfellow"));
        assertThat(ed_row.get("boolColumn"), is(true));

        Map<String, Object> jed_row = insertedRows.stream().filter(a-> a.get("name").equals("jed")).findFirst().get();
        assertThat(jed_row.get("intColumn"), is(3));
        assertThat(jed_row.get("stringColumn"), is("strongfellow"));
        assertThat(jed_row.get("boolColumn"), is(false));

        Map<String, Object> ted_row = insertedRows.stream().filter(a-> a.get("name").equals("ted")).findFirst().get();
        assertThat(ted_row.get("intColumn"), is(4));
        assertThat(ted_row.get("stringColumn"), is("strangefellow"));
        assertThat(ted_row.get("boolColumn"), is(true));

        dgen.deleteDomain(createDefaultConnection());
    }

    @Test
    public void testFileUpload() throws Exception
    {
        String sampleTypeName = "insert_from_file";
        SampleTypeDefinition props = new SampleTypeDefinition(sampleTypeName).setFields(standardTestSampleFields());
        TestDataGenerator dgen = SampleTypeAPIHelper.createEmptySampleType(getProjectName(), props)
                .withGeneratedRows(10);
        File testFile = dgen.writeData("fileUploadTest.tsv");
        CoreComponentsTestPage testPage = CoreComponentsTestPage.beginAt(this, getProjectName());
        EntityUpdateFromFilePanel testPanel = testPage.getEntityInsertPanelForUpdate();
        testPanel.targetEntityTypeSelect().select(sampleTypeName);
        var previewGrid = testPanel.uploadFileExpectingPreview(testFile, true);

        clickFileImport();  // the file import submit button is different from the grid submit button

        var selectRowsResponse = executeSelectRowCommand("exp.materials", sampleTypeName);
        assertThat(selectRowsResponse.getRows().size(), is(10));
    }

    public WebElement clickFinishExpectingSuccess(String buttonText)
    {
        waitFor(()-> isSubmitEnabled(),
                "the submit button was not enabled", 1500);
        submitButton().click();
        return Locator.tagWithClass("div", "alert-success")
                .waitForElement(getDriver(), 2000);
    }

    private WebElement submitButton()
    {
        return Locator.tagWithClass("button", "test-loc-submit-button").findElement(getDriver());
    }

    private boolean isSubmitEnabled()
    {
        return null == submitButton().getAttribute("disabled");
    }

    public void clickFileImport()
    {
        WebElement button = fileImportSubmitButton();
        shortWait().until(ExpectedConditions.elementToBeClickable(button));
        button.click();
    }

    private WebElement fileImportSubmitButton()
    {
        return Locator.button("Import").findElement(getDriver());
    }

    protected List<FieldDefinition> standardTestSampleFields()
    {
        return Arrays.asList(
                new FieldDefinition("intColumn", FieldDefinition.ColumnType.Integer),
                new FieldDefinition("stringColumn", FieldDefinition.ColumnType.String),
                new FieldDefinition("sampleDate", FieldDefinition.ColumnType.DateAndTime),
                new FieldDefinition("boolColumn", FieldDefinition.ColumnType.Boolean));
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "EntityInsertPanelTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
