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
import org.labkey.test.components.assay.AssayConstants;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.query.UpdateQueryRowPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldInfo;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DomainUtils;
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
        DomainFieldTypeChangeTest init = getCurrentTest();
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
        log("Creating list with variety of data fields");
        String listName = TestDataGenerator.randomDomainName("SampleListWithAllDataTypes");
        FieldInfo stringField = FieldInfo.random("name", FieldDefinition.ColumnType.String, DomainUtils.DomainKind.IntList);
        FieldInfo integerField = FieldInfo.random("Test/Integer", FieldDefinition.ColumnType.Integer, DomainUtils.DomainKind.IntList);
        FieldInfo decimalField = FieldInfo.random("Test/Decimal", FieldDefinition.ColumnType.Decimal, DomainUtils.DomainKind.IntList);
        FieldInfo dateField = FieldInfo.random("Test/Date", FieldDefinition.ColumnType.DateAndTime, DomainUtils.DomainKind.IntList);
        FieldInfo booleanField = FieldInfo.random("Test'/\"Boolean", FieldDefinition.ColumnType.Boolean, DomainUtils.DomainKind.IntList); // GitHub Issue #647
        TestDataGenerator dgen = new TestDataGenerator("lists", listName, getProjectName())
                .withColumns(List.of(
                        stringField.getFieldDefinition(),
                        integerField.getFieldDefinition(),
                        decimalField.getFieldDefinition(),
                        dateField.getFieldDefinition(),
                        booleanField.getFieldDefinition()));
        dgen.createDomain(createDefaultConnection(), DomainUtils.DomainKind.IntList.name(), Map.of("keyName", "id"));

        log("Inserting sample rows in the list");
        dgen.addCustomRow(Map.of(
                stringField.getName(), "first",
                integerField.getName(), "1",
                decimalField.getName(), "1.10",
                dateField.getName(), "01-01-2022",
                booleanField.getName(), "true"));
        dgen.addCustomRow(Map.of(
                stringField.getName(), "Second",
                integerField.getName(), "2",
                decimalField.getName(), "2.20",
                dateField.getName(), "01-02-2022",
                booleanField.getName(), "false"));
        dgen.addCustomRow(Map.of(
                stringField.getName(), "Third",
                integerField.getName(), "3",
                decimalField.getName(), "3.30",
                dateField.getName(), "01-03-2022",
                booleanField.getName(), "true"));
        dgen.insertRows(createDefaultConnection(), dgen.getRows());

        log("Verifying Integer to Decimal change");
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        domainFormPanel.getField(integerField.getName()).setType(FieldDefinition.ColumnType.Decimal, true);
        domainFormPanel.getField(booleanField.getName()).setNumberFormat("yes;no");
        domainDesignerPage.clickFinish();

        clickAndWait(Locator.linkWithText(listName));
        DataRegionTable table = new DataRegionTable("query", getDriver());
        checker().verifyEquals("Incorrect values after changing integer to decimal", Arrays.asList("1.0", "2.0", "3.0"),
                table.getColumnDataAsText(integerField.getLabel()));

        log("Verifying changing data fields to string");
        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        domainFormPanel = domainDesignerPage.fieldsPanel();
        domainFormPanel.getField(integerField.getName()).setType(FieldDefinition.ColumnType.String, true);
        domainFormPanel.getField(decimalField.getName()).setType(FieldDefinition.ColumnType.String, true);
        domainFormPanel.getField(dateField.getName()).setType(FieldDefinition.ColumnType.String, true);
        domainFormPanel.getField(booleanField.getName()).setType(FieldDefinition.ColumnType.String, true); // GitHub Issue #647
        domainDesignerPage.clickFinish();

        clickAndWait(Locator.linkWithText(listName));
        table = new DataRegionTable("query", getDriver());
        log("Verifying inserting string values");
        UpdateQueryRowPage updateQueryRowPage = table.clickInsertNewRow();
        updateQueryRowPage.setField(stringField.getName(), "Fourth");
        updateQueryRowPage.setField(integerField.getName(), "New1");
        updateQueryRowPage.setField(decimalField.getName(), "New1.1");
        updateQueryRowPage.setField(dateField.getName(), "New01-02-2022");
        updateQueryRowPage.setField(booleanField.getName(), "NewTrue");
        clickButton("Submit");
        updateQueryRowPage = table.clickEditRow(0);
        updateQueryRowPage.setField(integerField.getName(), "Edited1");
        clickButton("Submit");
        checker().verifyEquals("Incorrect values after changing integer to string", Arrays.asList("Edited1", "2", "3", "New1"),
                table.getColumnDataAsText(integerField.getName()));
        checker().verifyEquals("Incorrect values after changing decimal to string", Arrays.asList("1.1", "2.2", "3.3", "New1.1"),
                table.getColumnDataAsText(decimalField.getName()));
        checker().verifyEquals("Incorrect values after changing boolean to string", Arrays.asList("yes", "no", "yes", "NewTrue"),
                table.getColumnDataAsText(booleanField.getName()));
        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.MicrosoftSQLServer)
            checker().verifyEquals("Incorrect values after changing date to string", Arrays.asList("Jan 1 2022 12:00AM", "Jan 2 2022 12:00AM", "Jan 3 2022 12:00AM", "New01-02-2022"),
                    table.getColumnDataAsText(dateField.getName()));
        else
            checker().verifyEquals("Incorrect values after changing date to string", Arrays.asList("2022-01-01 00:00:00", "2022-01-02 00:00:00", "2022-01-03 00:00:00", "New01-02-2022"),
                    table.getColumnDataAsText(dateField.getName()));
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

        setFormElement(AssayConstants.ASSAY_NAME_FIELD_LOCATOR, runName);
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
