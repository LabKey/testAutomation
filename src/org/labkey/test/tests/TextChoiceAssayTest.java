package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.junit.LabKeyAssert;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Category({Daily.class})
public class TextChoiceAssayTest extends TextChoiceTest
{

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

    private static String currentBatchValue = BATCH_FIELD_VALUES.get(2);

    private static List<String> unusedRunFieldValues = new ArrayList<>(RUN_FIELD_VALUES);
    private static String currentRunValue;

    private static List<String> unusedResultFiledValues = new ArrayList<>(RESULT_FIELD_VALUES);

    private static Map<String, String> currentResultRowData = new HashMap<>();

    private void doSetup() throws IOException, CommandException
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(getProjectName(), null);

        createDefaultSampleTypeWithTextChoice();

        log(String.format("Create an assay named '%s' and add the TextChoice fields to batch, run and results.", ASSAY_NAME));

        createDefaultAssayDesignWithTextChoice();

        clickAndWait(Locator.linkWithText(ASSAY_NAME));

        // Basically tests the 'happy path' for an assay run. If this doesn't work none of the other test would pass, so
        // it is appropriate to test it during setup.
        createAndValidateAssayRun();

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
        goToManageAssays();
    }

    /**
     * <p>
     *     Helper/test case to create the assay run. This will validate the 'happy path' of setting TextChoice field values
     *     when creating a run.
     * </p>
     * <p>
     *     This will:
     *     <ul>
     *         <li>Validate the expected values in the batch TextChoice list.</li>
     *         <li>Set the batch TextChoice field.</li>
     *         <li>Validate the expected values in the run TextChoice list.</li>
     *         <li>Set the run TextChoice field.</li>
     *         <li>Enter result values for all the samples setting the TextCHoice field.</li>
     *     </ul>
     * </p>
     */
    private void createAndValidateAssayRun()
    {

        DataRegionTable runTable = new DataRegionTable("Runs", getDriver());
        runTable.clickHeaderButtonAndWait("Import Data");

        Locator batchLocator = Locator.name(getSelectControlName(BATCH_TC_FIELD));
        assertSelectOptions(batchLocator, BATCH_FIELD_VALUES,
                String.format("Options for the batch field '%s' are not as expected. Fatal error.", BATCH_TC_FIELD));

        log(String.format("Set the batch field '%s' to '%s'.", BATCH_TC_FIELD, currentBatchValue));
        WebElement select = batchLocator.findElement(getDriver());
        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(currentBatchValue));

        clickButton("Next");

        log(String.format("Set the Assay ID to '%s'.", ASSAY_RUN_ID));

        setFormElement(Locator.tagWithName("input", "name"), ASSAY_RUN_ID);

        Locator runLocator = Locator.name(getSelectControlName(RUN_TC_FIELD));
        assertSelectOptions(runLocator, RUN_FIELD_VALUES,
                String.format("Options for the '%s' field not as expected. Fatal error.", RUN_TC_FIELD));

        currentRunValue = unusedRunFieldValues.get(0);
        unusedRunFieldValues.remove(currentRunValue);

        log(String.format("Set the run field '%s' to '%s'.", RUN_TC_FIELD, currentRunValue));
        select = runLocator.findElement(getDriver());
        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(currentRunValue));

        StringBuilder resultsPasteText = new StringBuilder();
        resultsPasteText.append(String.format("%s\t%s\n", RESULT_SAMPLE_FIELD, RESULT_TC_FIELD));

        int valueIndex = 0;
        int count = 1;
        for (String sample : SAMPLES)
        {

            String resultsValue = RESULT_FIELD_VALUES.get(valueIndex);

            if(count%2 == 0)
                valueIndex++;

            // If it hasn't already happened remove the value from the unused list.
            unusedResultFiledValues.remove(resultsValue);

            resultsPasteText.append(String.format("%s\t%s\n", sample, resultsValue));

            currentResultRowData.put(sample, resultsValue);

            count++;
        }

        log("Paste in the results and save.");

        setFormElement(Locator.id("TextAreaDataCollector.textArea"), resultsPasteText.toString());

        clickButton("Save and Finish");

        log("Validate that the run/results data is as expected.");

        DataRegionTable dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        Map<String, String> rowData = dataRegionTable.getRowDataAsMap("Assay ID", ASSAY_RUN_ID);

        checker().fatal()
                .verifyEquals(String.format("Value for the run TextChoice field '%s' is not as expected.", RUN_TC_FIELD),
                        currentRunValue, rowData.get(RUN_TC_FIELD));

        checker().fatal()
                .verifyEquals(String.format("Value for the batch TextChoice field '%s' is not as expected.", BATCH_TC_FIELD),
                        currentBatchValue, rowData.get(String.format("Batch/%s", BATCH_TC_FIELD)));

        clickAndWait(Locator.linkWithText(ASSAY_RUN_ID));

        verifyRunResultsTable(currentResultRowData, currentBatchValue, currentRunValue);

    }

    /**
     * Helper to set the Edit Runs and Edit Results fields in the assay design.
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
     * For a given TextChoice (UI) field on a run/result assert that the options shown are as expected. This is not the
     * TextChoice field in the assay designer.
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
     * <p>
     *     Validate that TextChoice fields can be set and updated in an assay run/result. This will set editable run and
     *     results to true.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Will validate that the TextChoice fields for the run and results can be updated after the run is saved.</li>
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

        String newRunValue = unusedRunFieldValues.get(0);
        unusedRunFieldValues.remove(0);

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

        String sample = SAMPLES.get(1);
        log(String.format("Edit the result for sample '%s'.", sample));

        dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Data").find();
        rowIndex = dataRegionTable.getRowIndex(RESULT_SAMPLE_FIELD, sample);
        dataRegionTable.clickEditRow(rowIndex);
        select = Locator.name(String.format("quf_%s", RESULT_TC_FIELD)).findElement(getDriver());
        String updatedResultValue = unusedResultFiledValues.get(0);
        log(String.format("Result TextChoice value for sample '%s' will be changed from '%s' to '%s'.", sample, currentResultRowData.get(sample), updatedResultValue));
        new OptionSelect<>(select).selectOption(OptionSelect.SelectOption.textOption(updatedResultValue));

        // Remove the update results value.
        unusedResultFiledValues.remove(updatedResultValue);

        clickAndWait(Locator.lkButton("Submit"));

        currentResultRowData.replace(sample, updatedResultValue);

        verifyRunResultsTable(currentResultRowData, currentBatchValue, currentRunValue);

    }

    /**
     * <p>
     *     In the assay designer update a TextChoice value that has been used in a run/result and validate the new value
     *     is shown in existing runs/results.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Validate that the batch TextChoice field is locked and cannot be updated.</li>
     *         <li>Update the value used in the run TextChoice field and validate the run/result table shows the new value.</li>
     *         <li>Update the value used in the result TextChoice field, for two samples, and validate the new value is shown in the results table.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testUpdateTextChoiceValues()
    {
        clickAndWait(Locator.linkWithText(ASSAY_NAME));

        log("Make sure the 'Edit Runs' and 'Edit Results' options are checked.");
        setEditValues(true);

        log("Edit the assay design.");
        _assayHelper.clickManageOption(true, "Edit assay design");
        ReactAssayDesignerPage assayDesignerPage = new ReactAssayDesignerPage(getDriver());

        log(String.format("Validate field '%s' in the batch properties. It should be locked and cannot be updated.", BATCH_TC_FIELD));
        DomainFieldRow fieldRow = assayDesignerPage.goToBatchFields().getField(BATCH_TC_FIELD);
        fieldRow.expand();
        List<String> actualValues = fieldRow.getLockedTextChoiceValues();

        checker().verifyTrue(String.format("The batch value '%s' is not shown as locked. It should be.", currentBatchValue),
                actualValues.contains(currentBatchValue));

        checker().verifyFalse(String.format("The edit field text box should not be enabled for the batch value '%s'.", currentBatchValue),
                fieldRow.isTextChoiceUpdateFieldEnabled(currentBatchValue));

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
                String.format("1 row with value %s will be updated to %s on save.", currentRunValue, updatedRunValue), updateMsg);

        checker().screenShotIfNewError("Updating_Run_Field_Value_Error");

        String originalResultValue = currentResultRowData.get(SAMPLES.get(2));

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
                String.format("2 rows with value %s will be updated to %s on save.", originalResultValue, updatedResultValue), updateMsg);

        checker().screenShotIfNewError("Updating_Run_Field_Value_Error");

        // If there is some error on save this will terminate the test.
        assayDesignerPage.clickSave();

        // If the save worked update the current/expected values to the new values.
        currentRunValue = updatedRunValue;
        currentResultRowData.replace(SAMPLES.get(2), updatedResultValue);
        currentResultRowData.replace(SAMPLES.get(3), updatedResultValue);

        clickAndWait(Locator.linkWithText("view runs"));

        DataRegionTable dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Runs").find();
        Map<String, String> rowData = dataRegionTable.getRowDataAsMap("Assay ID", ASSAY_RUN_ID);

        checker().withScreenshot("Update_Error")
                .verifyEquals(String.format("Updated value for the TextChoice field '%s' is not as expected in run grid.", RUN_TC_FIELD),
                        updatedRunValue, rowData.get(RUN_TC_FIELD));

        clickAndWait(Locator.linkWithText(ASSAY_RUN_ID));

        verifyRunResultsTable(currentResultRowData, currentBatchValue, updatedRunValue);

    }

    /**
     * <p>
     *     Validate that if the run and results cannot be updated the used TextChoice values also cannot be updated in
     *     the assay design.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Set the run and results to not allow for updates.</li>
     *         <li>Validate that the value used in the run TextChoice field cannot be updated.</li>
     *         <li>Validate that the values used in the results TextChoice field cannot be updated.</li>
     *     </ul>
     * </p>
     */
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
