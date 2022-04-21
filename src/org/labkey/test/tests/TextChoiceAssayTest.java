package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.junit.LabKeyAssert;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Category({Daily.class})
public class TextChoiceAssayTest extends BaseWebDriverTest
{
    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return "TextChoice_Assay_Test";
    }

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        TextChoiceAssayTest init = (TextChoiceAssayTest) getCurrentTest();
        init.doSetup();
    }

    private static final String SAMPLE_TYPE = "TextChoice_Assay_Test_SampleType";
    private static final String SAMPLE_TYPE_PREFIX = "TCA_";

    private static List<String> availableSamples = new ArrayList<>();

    private static final String ASSAY_NAME = "Simple_TC_Assay";

    private static final String BATCH_TC_FIELD = "Batch_TC_Field";
    private static List<String> batchFieldValues = List.of("B1", "B2", "B3");

    private static final String RUN_TC_FIELD = "Run_TC_Field";
    private static List<String> runFieldValues = List.of("RN1", "RN2", "RN3");

    private static final String RESULT_TC_FIELD = "Result_TC_Field";
    private static List<String> resultFieldValues = List.of("RS1", "RS2", "RS3");
    private static final String SAMPLE_FIELD = "Sample";

    private void doSetup() throws IOException, CommandException
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(getProjectName(), null);

        log(String.format("Create a new sample type named '%s' and insert some samples.", SAMPLE_TYPE));
        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(SAMPLE_TYPE);
        sampleTypeDefinition.setNameExpression(String.format("%s${genId}", SAMPLE_TYPE_PREFIX));

        TestDataGenerator dataGenerator = SampleTypeAPIHelper.createEmptySampleType(getCurrentContainerPath(), sampleTypeDefinition);

        for (int index = 0; index < 10; index++)
        {
            String sampleName = String.format("%s%d", SAMPLE_TYPE_PREFIX, index);
            dataGenerator.addCustomRow(Map.of("Name", sampleName));
            availableSamples.add(sampleName);
        }

        dataGenerator.insertRows();

        log(String.format("Create an assay named '%s' and add the TextChoice fields.", ASSAY_NAME));

        goToManageAssays();
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", ASSAY_NAME)
                .setDescription("Testing TextChoice fields.");

        assayDesignerPage.setEditableRuns(true);
        assayDesignerPage.setEditableResults(true);

        DomainFormPanel domainFormPanel = assayDesignerPage.goToBatchFields();
        DomainFieldRow fieldRow = domainFormPanel.addField(BATCH_TC_FIELD);
        fieldRow.setType(FieldDefinition.ColumnType.TextChoice);
        fieldRow.setTextChoiceValues(batchFieldValues);

        // Remove the default fields.
        domainFormPanel.removeField("ParticipantVisitResolver");
        domainFormPanel.removeField("TargetStudy");

        fieldRow = assayDesignerPage.goToRunFields().addField(RUN_TC_FIELD);
        fieldRow.setType(FieldDefinition.ColumnType.TextChoice);
        fieldRow.setTextChoiceValues(runFieldValues);

        domainFormPanel = assayDesignerPage.goToResultsFields();

        fieldRow = domainFormPanel.addField(RESULT_TC_FIELD);
        fieldRow.setType(FieldDefinition.ColumnType.TextChoice);
        fieldRow.setTextChoiceValues(resultFieldValues);

        // Remove the default fields and add a sample field.
        domainFormPanel.removeField("SpecimenID");
        domainFormPanel.removeField("ParticipantID");
        domainFormPanel.removeField("VisitID");
        domainFormPanel.removeField("Date");

        assayDesignerPage.goToResultsFields().addField(new FieldDefinition(SAMPLE_FIELD, FieldDefinition.ColumnType.Sample));

        assayDesignerPage.clickSave();

        log("Add some web parts to make it easier to debug etc...");
        goToProjectHome();
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");
        portalHelper.addWebPart("Assay List");
        portalHelper.exitAdminMode();

    }

    @Before
    public void beforeTest()
    {
        goToProjectHome();
    }

    private String getSelectControlName(String tcFieldName)
    {
        String temp = tcFieldName.toLowerCase();
        return temp.replace("_tc_field", "_TC_Field");
    }

    private void assertSelectOptions(Locator selectLocator, List<String> expectedOptions, String failureMsg)
    {
        WebElement select = selectLocator.findElement(getDriver());

        List<WebElement> optionElements = Locator.tag("option").findElements(select);
        List<String> selectOptions = optionElements.stream().map(el -> el.getAttribute("value")).collect(Collectors.toList());

        // Remove the clear selection/empty option from the list.
        selectOptions.remove("");

        LabKeyAssert.assertEqualsSorted(failureMsg, expectedOptions, selectOptions);

    }

    private void verifyRunResultsTable(Map<String, String> expectedRowData, String expectedBatchValue, String expectedRunValue)
    {
        DataRegionTable dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Data").find();

        // Check the results for each sample.

        for(Map.Entry<String, String> entry : expectedRowData.entrySet())
        {
            Map<String, String> rowData = dataRegionTable.getRowDataAsMap(SAMPLE_FIELD, entry.getKey());

            Map<String, String> expectedData = Map.of(SAMPLE_FIELD, entry.getKey(),
                    RESULT_TC_FIELD, entry.getValue(),
                    String.format("Run/Batch/%s", BATCH_TC_FIELD), expectedBatchValue,
                    String.format("Run/%s", RUN_TC_FIELD), expectedRunValue);

            checker().verifyEquals(String.format("Result row not as expected for sample '%s'.", entry.getKey()), expectedData, rowData);
        }

    }

    private String pickDifferentValue(String currentValue, List<String> possibleValues)
    {
        int index = possibleValues.indexOf(currentValue);
        index = index != possibleValues.size() - 1 ? index + 1 : index - 1;
        return possibleValues.get(index);
    }

    @Test
    public void testEditRunAndResults()
    {

        goToManageAssays();

        clickAndWait(Locator.linkWithText(ASSAY_NAME));

        log("Make sure the 'Edit Runs' and 'Edit Results' options are checked.");
        _assayHelper.clickManageOption(true, "Edit assay design");
        ReactAssayDesignerPage assayDesignerPage = new ReactAssayDesignerPage(getDriver());
        assayDesignerPage.setEditableResults(true);
        assayDesignerPage.setEditableRuns(true);
        assayDesignerPage.clickSave();

        log("Create a run and validate each of the TextChoice fields along the way.");

        DataRegionTable runTable = new DataRegionTable("Runs", getDriver());
        runTable.clickHeaderButtonAndWait("Import Data");

        Locator batchLocator = Locator.name(getSelectControlName(BATCH_TC_FIELD));
        checker().withScreenshot("Assay_Edit_Run_Results_Batch_Field_Error")
                .wrapAssertion(()->assertSelectOptions(batchLocator, batchFieldValues,
                        String.format("Options for the batch field '%s' are not as expected.", BATCH_TC_FIELD)));

        String expectedBatchValue = batchFieldValues.get(0);

        log(String.format("Set the batch field '%s' to '%s'.", BATCH_TC_FIELD, expectedBatchValue));
        WebElement select = batchLocator.findElement(getDriver());
        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(expectedBatchValue));

        clickButton("Next");

        String assayID = "Run_01";

        log(String.format("Set the Assay ID to '%s'.", assayID));

        setFormElement(Locator.tagWithName("input", "name"), assayID);

        Locator runLocator = Locator.name(getSelectControlName(RUN_TC_FIELD));
        checker().withScreenshot("Assay_Edit_Run_Results_Batch_Field_Error")
                .wrapAssertion(()->assertSelectOptions(runLocator, runFieldValues, String.format("Options for the '%s' field not as expected.", RUN_TC_FIELD)));

        String expectedRunValue = runFieldValues.get(0);

        log(String.format("Set the run field '%s' to '%s'.", RUN_TC_FIELD, expectedRunValue));
        select = runLocator.findElement(getDriver());
        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(expectedRunValue));

        Map<String, String> expectedRowData = new HashMap<>();

        StringBuilder resultsPasteText = new StringBuilder();
        resultsPasteText.append(String.format("%s\t%s\n", SAMPLE_FIELD, RESULT_TC_FIELD));

        int tcIndex = 0;
        for (String sample : availableSamples)
        {

            if (tcIndex == resultFieldValues.size())
                tcIndex = 0;

            String resultsValue = resultFieldValues.get(tcIndex++);

            resultsPasteText.append(String.format("%s\t%s\n", sample, resultsValue));

            expectedRowData.put(sample, resultsValue);
        }

        log("Paste in the results and save.");

        setFormElement(Locator.id("TextAreaDataCollector.textArea"), resultsPasteText.toString());

        clickButton("Save and Finish");

        log("Review the saved assay run data");

        DataRegionTable dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        Map<String, String> rowData = dataRegionTable.getRowDataAsMap("Assay ID", assayID);

        checker().verifyEquals(String.format("Value for the TextChoice field '%s' is not as expected.", RUN_TC_FIELD),
                expectedRunValue, rowData.get(RUN_TC_FIELD));

        checker().verifyEquals(String.format("Value for the TextChoice field '%s' is not as expected.", BATCH_TC_FIELD),
                expectedBatchValue, rowData.get(String.format("Batch/%s", BATCH_TC_FIELD)));

        clickAndWait(Locator.linkWithText(assayID));

        verifyRunResultsTable(expectedRowData, expectedBatchValue, expectedRunValue);

        log("Go back the to runs table and edit the TextChoice field for the run result.");

        clickAndWait(Locator.linkWithText("view runs"));

        dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        int rowIndex = dataRegionTable.getRowIndex("Assay ID", assayID);
        dataRegionTable.clickEditRow(rowIndex);

        select = Locator.name(String.format("quf_%s", RUN_TC_FIELD)).findElement(getDriver());

        log(String.format("Run TextChoice value currently is '%s'.", expectedRunValue));
        expectedRunValue = pickDifferentValue(expectedRunValue, runFieldValues);
        log(String.format("Run TextChoice value will be updated to '%s'.", expectedRunValue));

        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(expectedRunValue));

        clickButton("Submit");

        dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        rowData = dataRegionTable.getRowDataAsMap("Assay ID", assayID);

        checker().verifyEquals(String.format("Value for the TextChoice field '%s' is not as expected.", RUN_TC_FIELD),
                expectedRunValue, rowData.get(RUN_TC_FIELD));

        clickAndWait(Locator.linkWithText(assayID));

        String sample = availableSamples.get(1);
        log(String.format("Edit the result for sample '%s'.", sample));

        dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Data").find();
        rowIndex = dataRegionTable.getRowIndex(SAMPLE_FIELD, sample);
        dataRegionTable.clickEditRow(rowIndex);
        select = Locator.name(String.format("quf_%s", RESULT_TC_FIELD)).findElement(getDriver());
        String updatedResultValue = pickDifferentValue(expectedRowData.get(sample), resultFieldValues);
        log(String.format("Result TextChoice value for sample '%s' will be changed from '%s' to '%s'.", sample, expectedRowData.get(sample), updatedResultValue));
        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(updatedResultValue));

        clickAndWait(Locator.lkButton("Submit"));

        expectedRowData.replace(sample, updatedResultValue);

        verifyRunResultsTable(expectedRowData, expectedBatchValue, expectedRunValue);

    }

}
