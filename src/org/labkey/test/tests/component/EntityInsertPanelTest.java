package org.labkey.test.tests.component;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.components.ui.EntityInsertPanel;
import org.labkey.test.pages.test.CoreComponentsTestPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Category({DailyB.class})
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
        testPanel.getEntityTypeSelect("Sample Type").select(sampleTypeName);

        String pasteText = "ed\tbrother\tthe quiet one\t\t2\tstringfellow\t11/11/2020\ttrue\n" +
                "jed\tother brother\tthe squinty one\t\t3\tstrongfellow\t11/11/2020\tfalse\n" +
                "ted\tother other brother\tisn't sure about the others\t\t4\tstrangefellow\t11/11/2020\ttrue";

        testPanel.clickAddRows()
                .getEditableGrid().pasteFromCell(0, "Name *", pasteText);
        WebElement success = clickFinishExpectingSuccess("Finish Creating 3 Samples");
        assertThat(success.getText(), is("Created 3 samples in sample type 'insert_from_edit_grid'."));
        List<Map<String, Object>> insertedRows = dgen.getRowsFromServer(createDefaultConnection()).getRows();
        assertThat(insertedRows.size(), is(3));

        dgen.deleteDomain(createDefaultConnection());
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
