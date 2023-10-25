package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONTokener;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.serverapi.reader.Readers;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.IssuesHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Category(Daily.class)
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class FieldEditorRowSelectionActionTest extends BaseWebDriverTest
{
    private final static String PROJECT_NAME = "Field Editor Row Selection Action Test";
    List<String> expectedHeaders = Arrays.asList("Name", "Range URI", "Required", "Lock Type", "Lookup Container", "Lookup Schema", "Lookup Query",
            "Format", "Default Scale", "Concept URI", "Scale", "Description", "Label", "Import Aliases", "Url", "Conditional Formats", "Property Validators",
            "Hidden", "Shown In Update View", "Shown In Insert View", "Shown In Details View", "Default Value Type", "Default Value", "Default Display Value", "Phi",
            "Exclude From Shifting", "Measure", "Dimension", "Recommended Variable", "Mv Enabled");
    List<String> expectedListHeaders = Arrays.asList("Name", "Range URI", "Required", "Is Primary Key", "Lock Type", "Lookup Container", "Lookup Schema", "Lookup Query",
            "Format", "Default Scale", "Concept URI", "Scale", "Description", "Label", "Import Aliases", "Url", "Conditional Formats", "Property Validators",
            "Hidden", "Shown In Update View", "Shown In Insert View", "Shown In Details View", "Default Value Type", "Default Value", "Default Display Value", "Phi",
            "Exclude From Shifting", "Measure", "Dimension", "Recommended Variable", "Mv Enabled");

    @BeforeClass
    public static void setupProject()
    {
        FieldEditorRowSelectionActionTest init = (FieldEditorRowSelectionActionTest) getCurrentTest();
        init.doSetup();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("study");
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), "Study");

        importFolderFromZip(TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip"), false, 1);

        goToProjectHome();
        new PortalHelper(getDriver()).addBodyWebPart("Lists");
        new PortalHelper(getDriver()).addWebPart("Sample Types");
        new PortalHelper(getDriver()).addWebPart("Issue Definitions");
    }

    @Test
    public void testListActions() throws Exception
    {
        String listName = "Technicians";
        goToProjectHome();
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(),
                "lists", listName);
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();

        domainFormPanel.getField("LastName").setSelectRowField(true);
        domainFormPanel.getField("ID").setSelectRowField(true);
        domainFormPanel.clickDeleteFields();

        List uiFields = domainFormPanel.fieldNames();
        checker().verifyEquals("Bulk delete failed for lists", Arrays.asList("Key", "FirstName"), uiFields);

        File downloadedFile = domainFormPanel.clickExportFields();
        ArrayList<String> exportedFields = getFieldsFromExportFile(downloadedFile);

        checker().verifyTrue("Exported Fields are not same as UI", uiFields.equals(exportedFields));

        log("Verifying the toggle between summary view and detail view list domain");
        checker().verifyTrue("Incorrect default display mode", domainFormPanel.isDetailMode());

        domainFormPanel.selectSummaryMode();
        checker().verifyTrue("Did not switch to Summary Mode", domainFormPanel.isSummaryMode());
        checker().wrapAssertion(()-> assertThat(domainFormPanel.getSummaryModeColumns())
                .as("Incorrect header values in summary mode in list domain")
                .containsAll(expectedListHeaders));

        domainFormPanel.clickSummaryGridNameLink("FirstName");
        checker().verifyTrue("Clicking on the name data did not navigate to summary mode", domainFormPanel.isDetailMode());
        checker().verifyTrue("Clicked row is not expanded in summary mode", domainFormPanel.getField("FirstName").isExpanded());
        checker().verifyEquals("Inconsistent number of rows in summary and detail mode",
                domainFormPanel.fieldNames().size(), domainFormPanel.getRowcountInSummaryMode());

        domainDesignerPage.clickCancelWithUnsavedChanges().discardChanges();
    }

    @Test
    public void testDatasetActions() throws Exception
    {
        String datasetName = "Physical Exam";
        goToProjectHome();
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(),
                "study", datasetName);

        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        domainFormPanel.getField("Pulse").setSelectRowField(true);
        domainFormPanel.getField("Respirations").setSelectRowField(true);
        domainFormPanel.getField("Signature").setSelectRowField(true);
        domainFormPanel.clickDeleteFields();

        checker().verifyEquals("Bulk delete failed for datasets ", 6, domainFormPanel.fieldNames().size());

        File downloadedFile = domainFormPanel.clickExportFields();
        ArrayList<String> exportedFields = getFieldsFromExportFile(downloadedFile);
        checker().verifyTrue("Exported Fields are not same as UI", exportedFields.equals(domainFormPanel.fieldNames()));

        log("Verifying the toggle between summary view and detail view");
        checker().verifyTrue("Incorrect default display mode", domainFormPanel.isDetailMode());

        domainFormPanel.selectSummaryMode();
        checker().verifyTrue("Did not switch to Summary Mode", domainFormPanel.isSummaryMode());
        checker().wrapAssertion(()-> assertThat(domainFormPanel.getSummaryModeColumns())
                        .as("Incorrect columns in summary mode")
                        .containsAll(expectedHeaders));

        log("Checking the links in summary mode");
        domainFormPanel.clickSummaryGridNameLink("Language");
        checker().verifyTrue("Clicking on the name data did not navigate to detail mode", domainFormPanel.isDetailMode());
        checker().verifyTrue("Clicked row is not expanded in detail mode", domainFormPanel.getField("Language").isExpanded());
        checker().verifyEquals("Inconsistent number of rows in summary and detail mode",
                domainFormPanel.fieldNames().size(), domainFormPanel.getRowcountInSummaryMode());

        domainDesignerPage.clickCancelWithUnsavedChanges().discardChanges();

    }

    @Test
    public void testUserProperties() throws Exception
    {
        DomainDesignerPage domainDesignerPage = goToSiteUsers().clickChangeUserProperties();
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        domainFormPanel.getField("FirstName").setSelectRowField(true);

        log("Only one fields should be exported");
        File downloadedFile = domainFormPanel.clickExportFields();
        checker().verifyEquals("Non selected fields are imported", Arrays.asList("FirstName"), getFieldsFromExportFile(downloadedFile));

        domainFormPanel.checkSelectAll(true);
        downloadedFile = domainFormPanel.clickExportFields();
        checker().verifyEquals("Exported fields are not same UI fields", getFieldsFromExportFile(downloadedFile), domainFormPanel.fieldNames());

        domainDesignerPage.clickCancel();
    }


    @Test
    public void testAssayAction() throws Exception
    {
        String assayName = "Test Assay";
        goToProjectHome();
        log("Create test assay");
        goToManageAssays();
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", assayName)
                .setDescription("Testing domain actions");
        assayDesignerPage.clickSave();

        File runFile = new File(TestFileUtils.getSampleData("AssayImportExport"), "GenericAssay_Run1.xls");
        log("Importing the Assay run");
        goToManageAssays();
        clickAndWait(Locator.linkWithText(assayName));
        DataRegionTable runTable = new DataRegionTable("Runs", getDriver());
        runTable.clickHeaderButtonAndWait("Import Data");
        clickButton("Next");
        checkRadioButton(Locator.radioButtonById("Fileupload"));
        setFormElement(Locator.input("__primaryFile__"), runFile);
        clickButton("Save and Finish");

        goToManageAssays();
        clickAndWait(Locator.linkWithText(assayName));
        assayDesignerPage = _assayHelper.clickEditAssayDesign(false);
        DomainFormPanel domainFormPanel = assayDesignerPage.goToResultsFields();

        domainFormPanel.getField("VisitID").setSelectRowField(true);
        domainFormPanel.getField("ParticipantID").setSelectRowField(true);
        domainFormPanel.clickDeleteFields();

        checker().verifyEquals("Bulk delete failed in user properties ", 2, domainFormPanel.fieldNames().size());

        File downloadedFile = domainFormPanel.clickExportFields();
        checker().verifyTrue("The exported fields do not match the UI fields",
                domainFormPanel.fieldNames().equals(getFieldsFromExportFile(downloadedFile)));

        log("Verifying the toggle between summary view and detail view for Results domain");
        checker().verifyTrue("Incorrect default display mode for result domain", domainFormPanel.isDetailMode());

        domainFormPanel.selectSummaryMode();
        checker().verifyTrue("Did not switch to Summary Mode", domainFormPanel.isSummaryMode());
        List<String> actualHeaders = domainFormPanel.getSummaryModeColumns();
        checker().wrapAssertion(()-> assertThat(actualHeaders)
                .as("Incorrect header values in detail mode in result domain")
                .containsAll(expectedHeaders));

        domainFormPanel.clickSummaryGridNameLink("Date");
        checker().verifyTrue("Clicking on the name data did not navigate to detail mode", domainFormPanel.isDetailMode());
        checker().verifyTrue("Clicked row is not expanded in detail mode", domainFormPanel.getField("Date").isExpanded());
        checker().verifyEquals("Inconsistent number of rows in summary and detail mode",
                domainFormPanel.fieldNames().size(), domainFormPanel.getRowcountInSummaryMode());

        assayDesignerPage.clickSave();
    }

    @Test
    public void testIssueDefinitionsAction() throws Exception
    {
        String issueName = "Test Issues";
        goToProjectHome();
        IssuesHelper issuesHelper = new IssuesHelper(this);
        issuesHelper.createNewIssuesList(issueName, getContainerHelper());

        goToProjectHome();
        clickAndWait(Locator.linkWithText(issueName));
        DataRegionTable table = new DataRegionTable("issues-testissues", getDriver());
        table.clickHeaderButtonAndWait("Admin");

        DomainDesignerPage domainDesignerPage = new DomainDesignerPage(getDriver());
        DomainFormPanel domainFormPanel = domainDesignerPage.expandFieldsPanel("Fields");
        domainFormPanel.addField("Test1").setSelectRowField(true);
        domainFormPanel.addField("Test2").setSelectRowField(true);
        domainFormPanel.addField("Test3").setSelectRowField(true);
        domainFormPanel.clickDeleteFields();

        checker().verifyEquals("Bulk delete failed for datasets ", 8, domainFormPanel.fieldNames().size());

        domainDesignerPage.clickSave();
    }

    private ArrayList<String> getFieldsFromExportFile(File exportFile) throws Exception
    {
        try (Reader reader = Readers.getReader(exportFile))
        {
            JSONArray jsonArray = new JSONArray(new JSONTokener(reader));

            ArrayList<String> exportFields = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++)
            {
                PropertyDescriptor field = new PropertyDescriptor(jsonArray.getJSONObject(i));
                exportFields.add(field.getName());
            }
            return exportFields;
        }
    }
}

