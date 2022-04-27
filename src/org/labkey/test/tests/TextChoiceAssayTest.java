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

    private static final List<String> samples = Arrays.asList("S_1", "S_2", "S_3", "S_4", "S_5", "S_6", "S_7", "S_8", "S_9", "S_10");

    private static final String ASSAY_NAME = "Simple_TC_Assay";
    private static final String ASSAY_RUN_ID = "The_One_And_Only_Run";

    private static final String BATCH_TC_FIELD = "Batch_TC_Field";
    private static final List<String> batchFieldValues = List.of("B1", "B2", "B3");
    private static final String BATCH_VALUE = "B2";

    private static final String RUN_TC_FIELD = "Run_TC_Field";
    private static final List<String> runFieldValues = List.of("RN1", "RN2", "RN3", "RN4", "RN5");
    private static List<String> unlockedRunFieldValues = new ArrayList<>(runFieldValues);
    private static String currentRunValue;

    private static final String RESULT_TC_FIELD = "Result_TC_Field";
    private static final List<String> resultFieldValues = List.of("RS1", "RS2", "RS3", "RS4", "RS5", "RS6", "RS7", "RS8", "RS9", "RS10");
    private static List<String> unlockedResultFieldValues = new ArrayList<>(resultFieldValues);

    private static Map<String, String> currentResultRowData = new HashMap<>();

    private static final String SAMPLE_FIELD = "Sample";

    private void doSetup() throws IOException, CommandException
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(getProjectName(), null);

        log(String.format("Create a new sample type named '%s' and insert some samples.", SAMPLE_TYPE));
        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(SAMPLE_TYPE);
        sampleTypeDefinition.setNameExpression("S_${genId}");

        TestDataGenerator dataGenerator = SampleTypeAPIHelper.createEmptySampleType(getCurrentContainerPath(), sampleTypeDefinition);

        for (String sample : samples)
        {
            dataGenerator.addCustomRow(Map.of("Name", sample));
        }

        dataGenerator.insertRows();

        log(String.format("Create an assay named '%s' and add the TextChoice fields to batch, run and results.", ASSAY_NAME));

        createAssayDesign();

        clickAndWait(Locator.linkWithText(ASSAY_NAME));

        createAssayRun();

        log("Add some web parts to make it easier to debug etc...");
        goToProjectHome();
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");
        portalHelper.addWebPart("Assay List");
        portalHelper.exitAdminMode();

    }

    // Rather than create a separate test to validate that a TextChoice field can be added to an assay design through
    // the UI this test class will create a shared assay design and use the UI to add the TextChoice fields here in the
    // setup. The TextChoiceSampleTypeTest.testTextChoiceInSampleTypeDesigner does more validation of creating a
    // TextChoice field using the UI and there is no need to repeat that here.
    private void createAssayDesign()
    {
        goToManageAssays();
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", ASSAY_NAME)
                .setDescription("Testing TextChoice fields.");

        assayDesignerPage.setEditableRuns(true);
        assayDesignerPage.setEditableResults(true);

        log(String.format("Add a TextChoice field named '%s' to the batch properties.", BATCH_TC_FIELD));
        DomainFormPanel domainFormPanel = assayDesignerPage.goToBatchFields();
        DomainFieldRow fieldRow = domainFormPanel.addField(BATCH_TC_FIELD);
        fieldRow.setType(FieldDefinition.ColumnType.TextChoice);
        fieldRow.setTextChoiceValues(batchFieldValues);

        // Remove the default fields.
        domainFormPanel.removeField("ParticipantVisitResolver");
        domainFormPanel.removeField("TargetStudy");

        log(String.format("Add a TextChoice field named '%s' to the run properties.", RUN_TC_FIELD));
        fieldRow = assayDesignerPage.goToRunFields().addField(RUN_TC_FIELD);
        fieldRow.setType(FieldDefinition.ColumnType.TextChoice);
        fieldRow.setTextChoiceValues(runFieldValues);

        domainFormPanel = assayDesignerPage.goToResultsFields();

        log(String.format("Add a TextChoice field named '%s' to the results.", RESULT_TC_FIELD));
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

    }

    private void createAssayRun()
    {

        DataRegionTable runTable = new DataRegionTable("Runs", getDriver());
        runTable.clickHeaderButtonAndWait("Import Data");

        Locator batchLocator = Locator.name(getSelectControlName(BATCH_TC_FIELD));
        checker().withScreenshot("Assay_Edit_Run_Results_Batch_Field_Error")
                .wrapAssertion(()->assertSelectOptions(batchLocator, batchFieldValues,
                        String.format("Options for the batch field '%s' are not as expected.", BATCH_TC_FIELD)));

        log(String.format("Set the batch field '%s' to '%s'.", BATCH_TC_FIELD, BATCH_VALUE));
        WebElement select = batchLocator.findElement(getDriver());
        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(BATCH_VALUE));

        clickButton("Next");

        log(String.format("Set the Assay ID to '%s'.", ASSAY_RUN_ID));

        setFormElement(Locator.tagWithName("input", "name"), ASSAY_RUN_ID);

        Locator runLocator = Locator.name(getSelectControlName(RUN_TC_FIELD));
        checker().withScreenshot("Assay_Edit_Run_Results_Batch_Field_Error")
                .wrapAssertion(()->assertSelectOptions(runLocator, runFieldValues, String.format("Options for the '%s' field not as expected.", RUN_TC_FIELD)));

        currentRunValue = unlockedRunFieldValues.get(0);
        unlockedRunFieldValues.remove(currentRunValue);

        log(String.format("Set the run field '%s' to '%s'.", RUN_TC_FIELD, currentRunValue));
        select = runLocator.findElement(getDriver());
        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(currentRunValue));

        StringBuilder resultsPasteText = new StringBuilder();
        resultsPasteText.append(String.format("%s\t%s\n", SAMPLE_FIELD, RESULT_TC_FIELD));

        int valueIndex = 0;
        int count = 1;
        for (String sample : samples)
        {

            String resultsValue = resultFieldValues.get(valueIndex);

            if(count%2 == 0)
                valueIndex++;

            // If it hasn't already happened remove the value from the unlocked list.
            unlockedResultFieldValues.remove(resultsValue);

            resultsPasteText.append(String.format("%s\t%s\n", sample, resultsValue));

            currentResultRowData.put(sample, resultsValue);

            count++;
        }

        log("Paste in the results and save.");

        setFormElement(Locator.id("TextAreaDataCollector.textArea"), resultsPasteText.toString());

        clickButton("Save and Finish");

    }

    @Before
    public void beforeTest()
    {
        goToProjectHome();
        goToManageAssays();
    }

    /**
     * Helper to set the Edit Runs and Edit Results fields.
     *
     * @param canEdit Set to true to make them editable, false otherwise.
     */
    private void setEditValues(boolean canEdit)
    {
        log(String.format("Setting the 'Edit Results' and 'Edit Runs' fields to %s.", canEdit));

        _assayHelper.clickManageOption(true, "Edit assay design");
        ReactAssayDesignerPage assayDesignerPage = new ReactAssayDesignerPage(getDriver());
        assayDesignerPage.setEditableResults(canEdit);
        assayDesignerPage.setEditableRuns(canEdit);
        assayDesignerPage.clickSave();
    }

    /**
     * Simple helper to identify the name of the select control on the page based on the field name. Some code some
     * place is changing the first letter of the field name to lower case when naming the control.
     *
     * @param tcFieldName The TextChoice field name.
     * @return The field name with the first letter lower case.
     */
    private String getSelectControlName(String tcFieldName)
    {
        return Character.toLowerCase(tcFieldName.charAt(0)) + tcFieldName.substring(1);
    }

    /**
     * For a given TextChoice (UI) field assert that the options shown are as expected.
     *
     * @param selectLocator The locator for the selector.
     * @param expectedOptions The expected list of options.
     * @param failureMsg If they don't match use this as the failure message.
     */
    private void assertSelectOptions(Locator selectLocator, List<String> expectedOptions, String failureMsg)
    {
        WebElement select = selectLocator.findElement(getDriver());

        List<WebElement> optionElements = Locator.tag("option").findElements(select);
        List<String> selectOptions = optionElements.stream().map(el -> el.getAttribute("value")).collect(Collectors.toList());

        // Remove the clear selection/empty option from the list.
        selectOptions.remove("");

        LabKeyAssert.assertEqualsSorted(failureMsg, expectedOptions, selectOptions);

    }

    /**
     * For an assays results table verify the rows. The values for the batch and run TextChoice fields should be the same
     * for all but the value for the result field will differ from row to row.
     *
     * @param expectedRowData A map with the sample id and the expected TextChoice value for the result.
     * @param expectedRunValue Expected TextChoice value for the run field.
     */
    private void verifyRunResultsTable(Map<String, String> expectedRowData, String expectedRunValue)
    {
        DataRegionTable dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Data").find();

        // Check the results for each sample.

        for(Map.Entry<String, String> entry : expectedRowData.entrySet())
        {
            Map<String, String> rowData = dataRegionTable.getRowDataAsMap(SAMPLE_FIELD, entry.getKey());

            Map<String, String> expectedData = Map.of(SAMPLE_FIELD, entry.getKey(),
                    RESULT_TC_FIELD, entry.getValue(),
                    String.format("Run/Batch/%s", BATCH_TC_FIELD), BATCH_VALUE,
                    String.format("Run/%s", RUN_TC_FIELD), expectedRunValue);

            checker().verifyEquals(String.format("Result row not as expected for sample '%s'.", entry.getKey()), expectedData, rowData);
        }

    }

    /**
     * <p>
     *     Validate that TextChoice fields can be set and updated in an assay run/result.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Test 'happy path' and set the TextChoice fields for an assay batch, run and results.</li>
     *         <li>Will validate that the TextChoice fields for the run and results can be updated after the fact.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testEditRunAndResults()
    {

        clickAndWait(Locator.linkWithText(ASSAY_NAME));

        log("Make sure the 'Edit Runs' and 'Edit Results' options are checked.");
        setEditValues(true);

        DataRegionTable dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        int rowIndex = dataRegionTable.getRowIndex("Assay ID", ASSAY_RUN_ID);
        dataRegionTable.clickEditRow(rowIndex);

        WebElement select = Locator.name(String.format("quf_%s", RUN_TC_FIELD)).findElement(getDriver());

        String newRunValue = unlockedRunFieldValues.get(0);
        unlockedRunFieldValues.remove(0);

        log(String.format("Update the Run TextChoice field from '%s' to '%s'.", currentRunValue, newRunValue));

        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(newRunValue));
        clickButton("Submit");

        dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        Map<String, String> rowData = dataRegionTable.getRowDataAsMap("Assay ID", ASSAY_RUN_ID);

        if(checker().withScreenshot("Run_Filed_Not_Updated")
                .verifyEquals(String.format("Value for the TextChoice field '%s' is not as expected.", RUN_TC_FIELD),
                        newRunValue, rowData.get(RUN_TC_FIELD)))
        {
            // If the run value was updated as expected save the new value as the current value.
            currentRunValue = newRunValue;
        }

        clickAndWait(Locator.linkWithText(ASSAY_RUN_ID));

        String sample = samples.get(1);
        log(String.format("Edit the result for sample '%s'.", sample));

        dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Data").find();
        rowIndex = dataRegionTable.getRowIndex(SAMPLE_FIELD, sample);
        dataRegionTable.clickEditRow(rowIndex);
        select = Locator.name(String.format("quf_%s", RESULT_TC_FIELD)).findElement(getDriver());
        String updatedResultValue = unlockedResultFieldValues.get(0);
        log(String.format("Result TextChoice value for sample '%s' will be changed from '%s' to '%s'.", sample, currentResultRowData.get(sample), updatedResultValue));
        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(updatedResultValue));

        // Remove the update results value.
        unlockedResultFieldValues.remove(updatedResultValue);

        clickAndWait(Locator.lkButton("Submit"));

        currentResultRowData.replace(sample, updatedResultValue);

        verifyRunResultsTable(currentResultRowData, currentRunValue);

    }

    @Test
    public void testUpdateTextChoiceValues()
    {
        clickAndWait(Locator.linkWithText(ASSAY_NAME));

        log("Make sure the 'Edit Runs' and 'Edit Results' options are checked.");
        setEditValues(true);

        log("Edit the assay design and validate that the used TextChoice fields are locked but can be updated.");
        _assayHelper.clickManageOption(true, "Edit assay design");
        ReactAssayDesignerPage assayDesignerPage = new ReactAssayDesignerPage(getDriver());

        log(String.format("Validate field '%s' in the batch properties.", BATCH_TC_FIELD));
        DomainFieldRow fieldRow = assayDesignerPage.goToBatchFields().getField(BATCH_TC_FIELD);
        fieldRow.expand();
        List<String> actualValues = fieldRow.getLockedTextChoiceValues();

        checker().verifyTrue(String.format("The batch value '%s' is not shown as locked. It should be.", BATCH_VALUE),
                actualValues.contains(BATCH_VALUE));

        checker().verifyFalse(String.format("The edit field text box should not be enabled for the batch value '%s'.", BATCH_VALUE),
                fieldRow.isTextChoiceUpdateFieldEnabled(BATCH_VALUE));

        checker().screenShotIfNewError("Batch_Field_Value_Error");

        log(String.format("Update the value for field '%s' in the run properties.", RUN_TC_FIELD));
        fieldRow = assayDesignerPage.goToRunFields().getField(RUN_TC_FIELD);
        fieldRow.expand();
        actualValues = fieldRow.getLockedTextChoiceValues();

        checker().verifyTrue(String.format("The run value '%s' is not shown as locked. It should be.", currentRunValue),
                actualValues.contains(currentRunValue));
        checker().fatal()
                .verifyTrue(String.format("The edit field text box should be enabled for the run value '%s', it is not. Fatal error.", currentRunValue),
                        fieldRow.isTextChoiceUpdateFieldEnabled(currentRunValue));

        String updatedRunValue = String.format("%s_updated", currentRunValue);

        log(String.format("Update the TextChoice value '%s' to be '%s'.", currentRunValue, updatedRunValue));
        String updateMsg = fieldRow.updateLockedTextChoiceValue(currentRunValue, updatedRunValue);

        checker().verifyEquals("Update message not as expected.",
                "1 row with value RN1 will be updated to RN1_updated on save.", updateMsg);

        checker().screenShotIfNewError("Updating_Run_Field_Value_Error");

        String originalResultValue = currentResultRowData.get(samples.get(2));

        log(String.format("Update the value '%s' in the field '%s' in the results properties.", originalResultValue, RESULT_TC_FIELD));
        fieldRow = assayDesignerPage.goToResultsFields().getField(RESULT_TC_FIELD);
        fieldRow.expand();
        actualValues = fieldRow.getLockedTextChoiceValues();

        checker().verifyTrue(String.format("The result value '%s' is not shown as locked. It should be.", originalResultValue),
                actualValues.contains(originalResultValue));
        checker().fatal()
                .verifyTrue(String.format("The edit field text box should be enabled for the result value '%s'. Fatal error.", originalResultValue),
                        fieldRow.isTextChoiceUpdateFieldEnabled(originalResultValue));

        String updatedResultValue = String.format("%s_updated", originalResultValue);

        log(String.format("Update the TextChoice value '%s' to be '%s'.", originalResultValue, updatedResultValue));
        updateMsg = fieldRow.updateLockedTextChoiceValue(originalResultValue, updatedResultValue);

        checker().verifyEquals("Update message for result values not as expected.",
                "2 rows with value RS2 will be updated to RS2_updated on save.", updateMsg);

        checker().screenShotIfNewError("Updating_Run_Field_Value_Error");

        assayDesignerPage.clickSave();

        // If the save worked update the current values to the new values.
        currentRunValue = updatedRunValue;
        currentResultRowData.replace(samples.get(2), updatedResultValue);
        currentResultRowData.replace(samples.get(3), updatedResultValue);

        clickAndWait(Locator.linkWithText("view runs"));

        DataRegionTable dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        Map<String, String> rowData = dataRegionTable.getRowDataAsMap("Assay ID", ASSAY_RUN_ID);

        checker().withScreenshot("Update_Error")
                .verifyEquals(String.format("Value for the TextChoice field '%s' is not as expected.", RUN_TC_FIELD),
                        updatedRunValue, rowData.get(RUN_TC_FIELD));

        clickAndWait(Locator.linkWithText(ASSAY_RUN_ID));

        verifyRunResultsTable(currentResultRowData, updatedRunValue);

    }

    @Test
    public void testLockedRunAndResults()
    {

        clickAndWait(Locator.linkWithText(ASSAY_NAME));

        log("Make sure the 'Edit Runs' and 'Edit Results' options are unchecked.");
        setEditValues(false);

        log("Edit the assay design and validate that the used TextChoice fields are locked but can not be updated.");
        _assayHelper.clickManageOption(true, "Edit assay design");
        ReactAssayDesignerPage assayDesignerPage = new ReactAssayDesignerPage(getDriver());

        log(String.format("Check the '%s' field in the run properties.", RUN_TC_FIELD));
        DomainFieldRow fieldRow = assayDesignerPage.goToRunFields().getField(RUN_TC_FIELD);
        fieldRow.expand();

        checker().withScreenshot("Run_Field_Edit_Enabled_Error")
                .verifyFalse(String.format("The edit field text box should not be enabled for the run value '%s'.", currentRunValue),
                        fieldRow.isTextChoiceUpdateFieldEnabled(currentRunValue));

        fieldRow = assayDesignerPage.goToResultsFields().getField(RESULT_TC_FIELD);
        fieldRow.expand();

        for(String assignedResultValue : currentResultRowData.values())
        {
            checker().verifyFalse(String.format("The edit field text box should not be enabled for the result value '%s'.", assignedResultValue),
                    fieldRow.isTextChoiceUpdateFieldEnabled(assignedResultValue));
        }

        checker().screenShotIfNewError("Result_Field_Edit_Enabled_Error");

        assayDesignerPage.clickCancel();
    }

}
