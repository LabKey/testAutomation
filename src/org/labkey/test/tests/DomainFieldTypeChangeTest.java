package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class DomainFieldTypeChangeTest extends BaseWebDriverTest
{
    @BeforeClass
    public static void setupProject()
    {
        DomainFieldTypeChangeTest init = (DomainFieldTypeChangeTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        new PortalHelper(getDriver()).addBodyWebPart("Lists");
    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "Domain Field Type Change Test Project";
    }

    @Test
    public void testProvisionedDomainFieldChanges() throws IOException, CommandException
    {
        String listName = "SampleListWithAllDataTypes";

        log("Creating list with variety of data fields");
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("testInteger", FieldDefinition.ColumnType.Integer),
                        new FieldDefinition("testDecimal", FieldDefinition.ColumnType.Decimal),
                        new FieldDefinition("testDate", FieldDefinition.ColumnType.DateAndTime),
                        new FieldDefinition("testBoolean", FieldDefinition.ColumnType.Boolean)));
        dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));

        log("Inserting sample rows in the list");
        dgen.addCustomRow(Map.of("name", "first", "testInteger", "1",
                "testDecimal", "1.10", "testDate", "01-01-2022",
                "testBoolean", "true"));
        dgen.addCustomRow(Map.of("name", "Second", "testInteger", "2",
                "testDecimal", "2.20", "testDate", "01-02-2022",
                "testBoolean", "false"));
        dgen.addCustomRow(Map.of("name", "Third", "testInteger", "3",
                "testDecimal", "3.30", "testDate", "01-03-2022",
                "testBoolean", "true"));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        log("Verifying Integer to Decimal change");
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        domainFormPanel.getField("testInteger").setType(FieldDefinition.ColumnType.Decimal, true);
        domainFormPanel.getField("testBoolean").setNumberFormat("yes;no");
        domainDesignerPage.clickFinish();

        clickAndWait(Locator.linkWithText(listName));
        DataRegionTable table = new DataRegionTable("query", getDriver());
        checker().verifyEquals("Incorrect values after changing integer to decimal", Arrays.asList("1.0", "2.0", "3.0"),
                table.getColumnDataAsText("testInteger"));

        log("Verifying changing data fields to string");
        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        domainFormPanel = domainDesignerPage.fieldsPanel();
        domainFormPanel.getField("testInteger").setType(FieldDefinition.ColumnType.String, true);
        domainFormPanel.getField("testDecimal").setType(FieldDefinition.ColumnType.String, true);
        domainFormPanel.getField("testDate").setType(FieldDefinition.ColumnType.String, true);
        domainFormPanel.getField("testBoolean").setType(FieldDefinition.ColumnType.String, true);
        domainDesignerPage.clickFinish();

        clickAndWait(Locator.linkWithText(listName));
        table = new DataRegionTable("query", getDriver());
        log("Verifying inserting string values");
        table.clickInsertNewRow();
        setFormElement(Locator.name("quf_name"), "Fourth");
        setFormElement(Locator.name("quf_testInteger"), "New1");
        setFormElement(Locator.name("quf_testDecimal"), "New1.1");
        setFormElement(Locator.name("quf_testDate"), "New01-02-2022");
        setFormElement(Locator.name("quf_testBoolean"), "NewTrue");
        clickButton("Submit");
        table.clickEditRow(0);
        setFormElement(Locator.name("quf_testInteger"), "Edited1");
        clickButton("Submit");
        checker().verifyEquals("Incorrect values after changing integer to string", Arrays.asList("Edited1", "2", "3", "New1"),
                table.getColumnDataAsText("testInteger"));
        checker().verifyEquals("Incorrect values after changing decimal to string", Arrays.asList("1.1", "2.2", "3.3", "New1.1"),
                table.getColumnDataAsText("testDecimal"));
        checker().verifyEquals("Incorrect values after changing boolean to string", Arrays.asList("yes", "no", "yes", "NewTrue"),
                table.getColumnDataAsText("testBoolean"));
        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.MicrosoftSQLServer)
            checker().verifyEquals("Incorrect values after changing date to string", Arrays.asList("Jan 1 2022 12:00AM", "Jan 2 2022 12:00AM", "Jan 3 2022 12:00AM", "New01-02-2022"),
                    table.getColumnDataAsText("testDate"));
        else
            checker().verifyEquals("Incorrect values after changing date to string", Arrays.asList("2022-01-01 00:00:00", "2022-01-02 00:00:00", "2022-01-03 00:00:00", "New01-02-2022"),
                    table.getColumnDataAsText("testDate"));
    }

    @Test
    public void testNonProvisionedDomainFieldChanges()
    {
        String assayName = "Assay1";
        String runName = "Run1";
        File runFile = new File(TestFileUtils.getSampleData("AssayImportExport"), "GenericAssay_Run1.xlsx");
        goToManageAssays();
        APIAssayHelper assayHelper = new APIAssayHelper(this);
        ReactAssayDesignerPage assayDesignerPage = assayHelper.createAssayDesign("General", assayName);

        DomainFormPanel runFields = assayDesignerPage.goToRunFields();
        runFields.addField("runTestInteger").setType(FieldDefinition.ColumnType.Integer);
        runFields.addField("runTestDecimal").setType(FieldDefinition.ColumnType.Decimal);
        runFields.addField("runTestDate").setType(FieldDefinition.ColumnType.DateAndTime);
        runFields.addField("runTestBoolean").setType(FieldDefinition.ColumnType.Boolean);

        DomainFormPanel batchFields = assayDesignerPage.goToBatchFields();
        batchFields.addField("batchTestInteger").setType(FieldDefinition.ColumnType.Integer);
        batchFields.addField("batchTestDecimal").setType(FieldDefinition.ColumnType.Decimal);
        batchFields.addField("batchTestDate").setType(FieldDefinition.ColumnType.DateAndTime);
        batchFields.addField("batchTestBoolean").setType(FieldDefinition.ColumnType.Boolean);
        assayDesignerPage.clickFinish();

        goToManageAssays();
        clickAndWait(Locator.linkWithText(assayName));
        DataRegionTable table = new DataRegionTable("Runs", getDriver());
        table.clickHeaderButton("Import Data");
        setFormElement(Locator.name("batchTestInteger"), "1");
        setFormElement(Locator.name("batchTestDecimal"), "1.1");
        setFormElement(Locator.name("batchTestDate"), "01-01-2022");
        checkCheckbox(Locator.name("batchTestBoolean"));
        clickButton("Next");

        setFormElement(Locator.name("name"), runName);
        setFormElement(Locator.name("runTestInteger"), "12");
        setFormElement(Locator.name("runTestDecimal"), "1.12");
        setFormElement(Locator.name("runTestDate"), "01-03-2022");
        checkRadioButton(Locator.radioButtonById("Fileupload"));
        setFormElement(Locator.input("__primaryFile__"), runFile);
        clickButton("Save and Finish");

        goToManageAssays();
        clickAndWait(Locator.linkWithText(assayName));
        waitForElement(Locator.linkWithText(runName));
        click(Locator.linkWithText("Manage assay design"));
        assayDesignerPage = _assayHelper.clickEditAssayDesign();
        runFields = assayDesignerPage.goToRunFields();
        runFields.getField("runTestInteger").setType(FieldDefinition.ColumnType.Decimal, true);
        runFields.getField("runTestBoolean").setNumberFormat("yes;no");

        batchFields = assayDesignerPage.goToBatchFields();
        batchFields.getField("batchTestDate").setType(FieldDefinition.ColumnType.String, true);
        batchFields.getField("batchTestDecimal").setType(FieldDefinition.ColumnType.String, true);
        assayDesignerPage.clickFinish();

        assayDesignerPage = _assayHelper.clickEditAssayDesign();
        runFields = assayDesignerPage.goToRunFields();
        runFields.getField("runTestBoolean").setType(FieldDefinition.ColumnType.String, true);
        assayDesignerPage.clickFinish();

        goToManageAssays();
        clickAndWait(Locator.linkWithText(assayName));
        table = new DataRegionTable("Runs", getDriver());
        checker().verifyEquals("Run fields : Incorrect value after changing Integer to decimal", Arrays.asList("12.0"),
                table.getColumnDataAsText("runTestInteger"));
        checker().verifyEquals("Run fields : Incorrect value after changing Boolean to string", Arrays.asList("no"),
                table.getColumnDataAsText("runTestBoolean"));
        checker().verifyEquals("Batch fields : Incorrect value after changing Decimal to string", Arrays.asList("1.1"),
                table.getColumnDataAsText("Batch/batchTestDecimal"));
        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.MicrosoftSQLServer)
            checker().verifyEquals("Batch fields : Incorrect value after changing Date to string", Arrays.asList("Jan 1 2022 12:00AM"),
                    table.getColumnDataAsText("Batch/batchTestDate"));
        else
            checker().verifyEquals("Batch fields : Incorrect value after changing Date to string", Arrays.asList("2022-01-01 00:00:00"),
                    table.getColumnDataAsText("Batch/batchTestDate"));


        checker().screenShotIfNewError("AfterRunAndBatchChanges");
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
