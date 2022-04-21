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
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.pages.query.UpdateQueryRowPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Category({Daily.class})
public class TextChoiceSampleTypeTest extends BaseWebDriverTest
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
        return "TextChoice_SampleType_Test";
    }

    @BeforeClass
    public static void setupProject()
    {
        TextChoiceSampleTypeTest init = (TextChoiceSampleTypeTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        _containerHelper.createProject(getProjectName(), null);
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");
        portalHelper.exitAdminMode();
    }

    @Before
    public void beforeTest()
    {
        goToProjectHome();
    }

    /**
     * <p>
     *     Test the TextChoice field in the sample type designer. Focus on creating the field and providing values.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Use the designer to add a TextChoice field.</li>
     *         <li>Add values to the TextChoice field using the dialog.</li>
     *         <li>Verify that the values are displayed in a sorted order.</li>
     *         <li>Adding a duplicate value will be ignored.</li>
     *         <li>Values that have been assigned are locked.</li>
     *         <li>Searching for a value works as expected.</li>
     *         <li>Convert a text field to a TextChoice field.</li>
     *         <li>Validate that the converted field uses the existing text values as TextChoice values.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testTextChoiceInSampleTypeDesigner()
    {
        String sampleTypeName = "Test_TC_In_Designer";
        String textChoiceFieldName = "TextChoice_Field_1";

        // The list is intentionally out of alphabetical order. This will help validate the list is ordered after saving.
        List<String> expectedValues = new ArrayList<>();
        expectedValues.add("C");
        expectedValues.add("A");
        expectedValues.add("\u00DC");
        expectedValues.add("XYZ");
        expectedValues.add("A string with spaces.");
        expectedValues.add("B");

        // Identify a couple of TextChoice values that will be used in samples.
        List<String> valuesUsed = new ArrayList<>();
        valuesUsed.add(expectedValues.get(2));
        valuesUsed.add(expectedValues.get(4));

        String duplicateValue = expectedValues.get(3);

        String searchValue = "A";
        List<String> searchValuesExpected = new ArrayList<>();
        searchValuesExpected.add(expectedValues.get(1));
        searchValuesExpected.add(expectedValues.get(4));

        log(String.format("Create a new sample type named '%s'.", sampleTypeName));
        goToProjectHome();
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName);
        sampleTypeDefinition.setNameExpression("TCD_${genId}");

        log("Add a TextChoice field. This will validate adding a new TextChoice field.");
        FieldDefinition textChoiceField = new FieldDefinition(textChoiceFieldName, ColumnType.TextChoice);
        textChoiceField.setTextChoiceValues(expectedValues);

        sampleTypeDefinition.addField(textChoiceField);

        final String textField = "txtField";
        log(String.format("Add a text field '%s' that will be converted to a TextChoice field.", textField));
        sampleTypeDefinition.addField(new FieldDefinition(textField, ColumnType.String));

        sampleTypeHelper.createSampleType(sampleTypeDefinition);

        log("Edit the sample type and validate that the expected values are shown. They should be in alphabetical order.");

        UpdateSampleTypePage updatePage = sampleTypeHelper.goToEditSampleType(sampleTypeName);
        DomainFieldRow fieldRow = updatePage.getFieldsPanel().getField(textChoiceFieldName);
        fieldRow = fieldRow.expand();

        List<String> actualValues = fieldRow.getTextChoiceValues();

        // Sort only the original list, not the list returned from the UI.
        Collections.sort(expectedValues);

        checker().verifyEquals("Values for field not as expected.", expectedValues, actualValues);

        log("Validate that none of the values are shown as locked.");
        actualValues = fieldRow.getLockedTextChoiceValues();

        checker().verifyTrue(String.format("Fields '%s' are locked, they should not be.", String.join(",", actualValues)),
                actualValues.isEmpty());

        checker().screenShotIfNewError("ST_Designer_Initial_Values_Not_Correct");

        List<String> expectedConvertedValues = List.of("Apple", "Banana");
        log("Add some samples to the sample type and set the TextChoice field for some of the samples.");
        sampleTypeHelper.goToSampleType(sampleTypeName);
        List<Map<String, String>> samples = new ArrayList<>();

        samples.add(Map.of(textChoiceFieldName, valuesUsed.get(0), textField, ""));
        samples.add(Map.of(textChoiceFieldName, "", textField, expectedConvertedValues.get(0)));
        samples.add(Map.of(textChoiceFieldName, valuesUsed.get(1), textField, ""));
        samples.add(Map.of(textChoiceFieldName, valuesUsed.get(1), textField, ""));
        samples.add(Map.of(textChoiceFieldName, "", textField, expectedConvertedValues.get(1)));

        sampleTypeHelper.bulkImport(samples);

        log(String.format("Edit the sample type again and validate that TextChoice values '%s' are locked.", String.join(", ", valuesUsed)));

        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        updatePage = new UpdateSampleTypePage(getDriver());

        fieldRow = updatePage.getFieldsPanel().getField(textChoiceFieldName);
        fieldRow = fieldRow.expand();

        List<String> lockedValues = fieldRow.getLockedTextChoiceValues();

        // Sort the result from the UI, at this point more concerned about the values and not the order.
        Collections.sort(lockedValues);
        Collections.sort(valuesUsed);

        checker()
                .withScreenshot("ST_Designer_Locked_Values_Error")
                .verifyEquals("Locked values not as expected.", valuesUsed, lockedValues);

        log(String.format("Add some more TextChoice values. Including the already existing value '%s'.", duplicateValue));
        List<String> newValues = new ArrayList<>();
        newValues.add("Q");
        newValues.add("R");
        newValues.add("S");
        newValues.add(duplicateValue);

        fieldRow.setTextChoiceValues(newValues);

        // Remove the duplicate value from the list.
        newValues.remove(duplicateValue);

        expectedValues.addAll(newValues);

        log(String.format("Validate the list of values is updated and that '%s' is only in the list once.", duplicateValue));
        actualValues = fieldRow.getTextChoiceValues();

        Collections.sort(expectedValues);
        Collections.sort(actualValues);

        checker()
                .withScreenshot("ST_Designer_Duplicate_Error")
                .verifyEquals("List not as expected after adding new values.", expectedValues, actualValues);

        log("Save the change, and the edit the sample type again to make sure the values are still as expected.");
        updatePage.clickSave();

        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        updatePage = new UpdateSampleTypePage(getDriver());

        fieldRow = updatePage.getFieldsPanel().getField(textChoiceFieldName);
        fieldRow = fieldRow.expand();

        actualValues = fieldRow.getTextChoiceValues();

        checker()
                .withScreenshot("ST_Designer_Duplicate_After_Save_Error")
                .verifyEquals("List not as expected after adding new values and saving.", expectedValues, actualValues);

        log(String.format("Validate that searching for '%s' returns the expected values.", searchValue));
        actualValues = fieldRow.searchForTextChoiceValue(searchValue);

        Collections.sort(searchValuesExpected);

        checker()
                .withScreenshot("ST_Designer_Search_Error")
                .verifyEquals(String.format("Values found while searching for '%s' not as expected.", searchValue), searchValuesExpected, actualValues);

        log(String.format("Change the text field '%s' to a TextChoice field.", textField));

        fieldRow = updatePage.getFieldsPanel().getField(textField);
        fieldRow = fieldRow.expand();

        fieldRow.setType(ColumnType.TextChoice);

        log("Validate that the field now has a TextChoice list and has the expected values, and they are locked.");

        actualValues = fieldRow.getTextChoiceValues();

        LabKeyAssert.assertEqualsSorted("Converting a text field to a TextChoice field did not populate the values list as expected.",
                expectedConvertedValues, actualValues);

        actualValues = fieldRow.getLockedTextChoiceValues();

        LabKeyAssert.assertEqualsSorted("All of the converted values should show as locked, they do not.",
                expectedConvertedValues, actualValues);

        updatePage.clickSave();

        log("Updating field should have worked.");

    }

    final String STR_FIELD_NAME = "Str";

    private TestDataGenerator createSampleType(String sampleTypeName, String sampleNamePrefix, String textChoiceFieldName, List<String> textChoiceValues)
    {
        log(String.format("Create a new sample type named '%s'.", sampleTypeName));
        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(sampleTypeName);
        sampleTypeDefinition.setNameExpression(String.format("%s${genId}", sampleNamePrefix));

        log(String.format("Add a TextChoice field named '%s'.", textChoiceFieldName));
        FieldDefinition textChoiceField = new FieldDefinition(textChoiceFieldName, ColumnType.TextChoice);
        textChoiceField.setTextChoiceValues(textChoiceValues);

        sampleTypeDefinition.addField(textChoiceField);

        log(String.format("Add a simple string field '%s' just because.", STR_FIELD_NAME));
        FieldDefinition strField = new FieldDefinition(STR_FIELD_NAME, ColumnType.String);
        sampleTypeDefinition.addField(strField);

        return SampleTypeAPIHelper.createEmptySampleType(getCurrentContainerPath(), sampleTypeDefinition);

    }

    /**
     * <p>
     *     Test the update and deleting of TextChoice values.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Validate assigned values are locked.</li>
     *         <li>The delete button is disabled for locked values.</li>
     *         <li>Delete an unlocked value.</li>
     *         <li>Update a locked value and validate the update message</li>
     *         <li>Validate that the updated value is shown on the samples overview page.</li>
     *         <li>Update an unlocked value.</li>
     *         <li>Updating a value to an existing value causes an error.</li>
     *         <li>Updating/saving a changed sample type design does not update the TextChoice value if the 'Apply' button wasn't clicked.</li>
     *     </ul>
     * </p>
     * @throws IOException Can be thrown by the create API call.
     * @throws CommandException Can be thrown by the create API call.
     */
    @Test
    public void testUpdatingAndDeletingValuesInSampleType() throws IOException, CommandException
    {
        final String sampleTypeName = "TC_Value_Updates";
        final String textChoiceFieldName = "TextChoice_Field_1";
        final String namePrefix = "TCE_";

        // Some TextChoice values.
        List<String> expectedUnLockedValues = new ArrayList<>();
        expectedUnLockedValues.add("ÅÅ");
        expectedUnLockedValues.add("BB");
        expectedUnLockedValues.add("CC");
        expectedUnLockedValues.add("DD");
        expectedUnLockedValues.add("E E E");
        expectedUnLockedValues.add("ƒƒ");
        expectedUnLockedValues.add("GG");
        expectedUnLockedValues.add("H");

        TestDataGenerator dataGenerator = createSampleType(sampleTypeName, namePrefix, textChoiceFieldName, expectedUnLockedValues);

        log("Create some samples that have TextChoice values set.");

        // Only assign a few of the values to samples (i.e. lock them).
        List<String> expectedLockedValues = new ArrayList<>();
        expectedLockedValues.add(expectedUnLockedValues.get(0));
        expectedLockedValues.add(expectedUnLockedValues.get(1));
        expectedLockedValues.add(expectedUnLockedValues.get(2));

        expectedUnLockedValues.removeAll(expectedLockedValues);

        Map<String, String> samplesWithTC = new HashMap<>();

        int index = 0;
        for(int i = 1; i <= 20; i++)
        {

            String sampleName = String.format("%s%d", namePrefix, i);

            Map<String, Object> sample = new HashMap<>();

            String strFieldValue;

            // Give a TextChoice value to every other sample.
            if(i%2 == 0)
            {

                if(index >= expectedLockedValues.size())
                    index = 0;

                String tcValue = expectedLockedValues.get(index);
                index++;

                sample.put(textChoiceFieldName, tcValue);
                strFieldValue = String.format("This sample has a TextChoice value of '%s'.", tcValue);

                samplesWithTC.put(sampleName, tcValue);
            }
            else
            {
                strFieldValue = "This sample does not have a TextChoice value.";
            }

            sample.put("Name", sampleName);
            sample.put(STR_FIELD_NAME, strFieldValue);

            dataGenerator.addCustomRow(sample);
        }

        dataGenerator.insertRows();

        log("Edit the sample type and validate that the locked values are as expected.");

        goToProjectHome();
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        UpdateSampleTypePage updatePage = sampleTypeHelper.goToEditSampleType(sampleTypeName);
        DomainFieldRow fieldRow = updatePage.getFieldsPanel().getField(textChoiceFieldName);
        fieldRow = fieldRow.expand();

        List<String> actualValues = fieldRow.getLockedTextChoiceValues();

        Collections.sort(expectedLockedValues);
        Collections.sort(actualValues);

        checker().verifyEquals("Locked values not as expected, cannot continue.",
                expectedLockedValues, actualValues);

        log("Validate that the delete button is disabled for a locked sample.");
        String value = expectedLockedValues.get(1);
        fieldRow.selectTextChoiceValue(value);
        checker().verifyFalse(String.format("Delete button is enabled for value '%s', it should not be.", value),
                fieldRow.isTextChoiceDeleteButtonEnabled());

        log("Validate that unused values are not locked and can be deleted.");
        value = expectedUnLockedValues.get(0);
        fieldRow.selectTextChoiceValue(value);

        if(checker().verifyTrue(String.format("Delete button is not enabled for value '%s', it should be.", value),
                fieldRow.isTextChoiceDeleteButtonEnabled()))
        {
            fieldRow.deleteTextChoiceValue(value);
            expectedUnLockedValues.remove(value);

            actualValues = fieldRow.getTextChoiceValues();

            checker().verifyFalse(String.format("TextChoice value '%s' should not be in the list of values.", value),
                    actualValues.contains(value));
        }
        else
        {
            log("Delete button was not as expected for unlocked value, not going to test deleting a TextChoice value.");
        }

        checker().screenShotIfNewError("Edit_Values_Locked_Unlocked_Deleted_Error");

        log("Update a locked value and validate the status message.");
        String valueToUpdate = expectedLockedValues.get(1);
        String updatedValue = String.format("%s and here is an update", valueToUpdate);

        String actualMsg = fieldRow.updateLockedTextChoiceValue(valueToUpdate, updatedValue);
        String expectedMsg = String.format("3 rows with value %s will be updated to %s on save.", valueToUpdate, updatedValue);

        checker().withScreenshot("Edit_Values_Update_Message_Error")
                .verifyEquals("Value update message not as expected.",
                        expectedMsg, actualMsg);

        log("Update an unlocked value and verify that no message is shown.");
        String unLockedToUpdate = expectedUnLockedValues.get(1);
        String unLockedUpdated = String.format("%s updated", unLockedToUpdate);
        fieldRow.updateTextChoiceValue(unLockedToUpdate, unLockedUpdated);

        // Replace the value in the list of expected sample properties.
        expectedUnLockedValues.remove(unLockedToUpdate);
        expectedUnLockedValues.add(unLockedUpdated);

        log("Save the changes and verify the TextChoice values on the various samples.");

        updatePage.clickSave();

        // Construct a list of samples that have TextChoice set and what they are expected to be.
        List<Map<String, String>> expectedSamples = new ArrayList<>();

        for(Map.Entry<String, String> entry : samplesWithTC.entrySet())
        {
            String sampleId = entry.getKey();

            // Need to special case for the TC value that was just updated.
            String sampleValue;
            if(entry.getValue().equals(valueToUpdate))
            {
                sampleValue = updatedValue;
            }
            else
            {
                sampleValue = entry.getValue();
            }

            expectedSamples.add(Map.of("Name", sampleId, textChoiceFieldName, sampleValue));
        }

        // Let the helper verify the values. If a TextChoice value is not as expected this will assert and stop the test.
        sampleTypeHelper.verifyDataValues(expectedSamples);

        log("Now, edit the sample type again and update a value to an already existing value.");

        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        updatePage = new UpdateSampleTypePage(getDriver());

        fieldRow = updatePage.getFieldsPanel().getField(textChoiceFieldName);
        fieldRow = fieldRow.expand();

        valueToUpdate = expectedUnLockedValues.get(0);
        updatedValue = expectedLockedValues.get(0);

        actualMsg = fieldRow.updateTextChoiceValueExpectError(valueToUpdate, updatedValue);
        expectedMsg = String.format("\"%s\" already exists in the list of values.", updatedValue);

        checker().verifyEquals("Error message for duplicate value not as expected.",
                        expectedMsg, actualMsg);

        checker().verifyFalse("Apply button should not be enabled.",
                fieldRow.isTextChoiceApplyButtonEnabled());

        checker().screenShotIfNewError("Edit_Values_Error_Message_Error");

        log("Click save, there should be no errors.");
        updatePage.clickSave();

        log("Finally update a value and click the save button for the sample type without clicking 'Apply'.");

        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        updatePage = new UpdateSampleTypePage(getDriver());

        fieldRow = updatePage.getFieldsPanel().getField(textChoiceFieldName);
        fieldRow = fieldRow.expand();

        valueToUpdate = expectedUnLockedValues.get(2);
        updatedValue = String.format("%s no change", valueToUpdate);

        fieldRow.selectTextChoiceValue(valueToUpdate);
        fieldRow.setUpdateTextChoiceValue(updatedValue);

        updatePage.clickSave();

        log("Check that the TextChoice value was not updated.");

        waitAndClickAndWait(Locator.lkButton("Edit Type"));
        updatePage = new UpdateSampleTypePage(getDriver());

        fieldRow = updatePage.getFieldsPanel().getField(textChoiceFieldName);
        fieldRow = fieldRow.expand();

        actualValues = fieldRow.getTextChoiceValues();

        checker().verifyTrue(String.format("Expected value '%s' is not in the list of values.", valueToUpdate),
                actualValues.contains(valueToUpdate));

    }

    /**
     * <p>
     *     Validate the TextChoice values when editing/create a sample.
     * </p>
     * <p>
     *     This test will:
     *     <ul>
     *         <li>Use the UI to set the TextChoice value for a sample.</li>
     *         <li>Verify that the options shown for a TextChoice field are as expected.</li>
     *         <li>Use import to update a sample and set its TextChoice field.</li>
     *         <li>Validate that if an invalid value is used in the import an error message is shown.</li>
     *     </ul>
     * </p>
     * @throws IOException Can be thrown by the create API call.
     * @throws CommandException Can be thrown by the create API call.
     */
    @Test
    public void testSetTextChoiceValueForSample() throws IOException, CommandException
    {
        final String sampleTypeName = "TC_Sample_Edit";
        final String textChoiceFieldName = "TextChoice_Field";
        final String textChoiceFieldCaption = "Text Choice Field";

        final String namePrefix = "TCSM_";

        // Some TextChoice values.
        List<String> tcValues = new ArrayList<>();
        tcValues.add("ÅÅ");
        tcValues.add("BB");
        tcValues.add("CC");

        TestDataGenerator dataGenerator = createSampleType(sampleTypeName, namePrefix, textChoiceFieldName, tcValues);

        log("Create some samples int the sample type. None of the samples will have a TextChoice value.");

        List<String> availableSamples = new ArrayList<>();

        for(int i = 1; i <= 5; i++)
        {
            Map<String, Object> sample = new HashMap<>();
            String sampleName = String.format("%s%d", namePrefix, i);
            sample.put("Name", sampleName);
            sample.put(STR_FIELD_NAME, "This sample does not have a TextChoice value.");
            dataGenerator.addCustomRow(sample);
            availableSamples.add(sampleName);
        }

        dataGenerator.insertRows();

        refresh();

        String sample = availableSamples.get(0);
        availableSamples.remove(0);

        log(String.format("Edit the sample '%s' and validate TextChoice values are shown in the select for the field.", sample));

        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        DataRegionTable samplesTable = sampleTypeHelper.goToSampleType(sampleTypeName)
                .getSamplesDataRegionTable();

        UpdateQueryRowPage updateSamplePage = samplesTable.clickEditRow(samplesTable.getRowIndex("Name", sample));

        WebElement select = Locator.name(String.format("quf_%s", textChoiceFieldName)).findElement(getDriver());

        List<WebElement> optionElements = Locator.tag("option").findElements(select);
        List<String> options = optionElements.stream().map(el -> el.getAttribute("value")).collect(Collectors.toList());

        // Remove the clear selection/empty option from the list.
        options.remove("");

        LabKeyAssert.assertEqualsSorted(String.format("Options for the '%s' field not as expected. Fatal error.", textChoiceFieldName),
                tcValues, options);

        String expectedValue = tcValues.get(0);
        updateSamplePage.setField(textChoiceFieldName, OptionSelect.SelectOption.textOption(expectedValue));

        updateSamplePage.submit();

        samplesTable = sampleTypeHelper.getSamplesDataRegionTable();
        String actualValue = samplesTable.getRowDataAsMap("Name", sample).get(textChoiceFieldName);

        checker().verifyEquals(String.format("Doesn't look like the text choice value of '%s' was set for sample '%s'.", expectedValue, sample),
                expectedValue, actualValue);

        log("Use import to update/set a TextChoice value.");
        sample = availableSamples.get(0);
        availableSamples.remove(0);

        expectedValue = tcValues.get(1);
        List<Map<String, String>> sampleData = new ArrayList<>();
        sampleData.add(Map.of("Name", sample, textChoiceFieldName, expectedValue));

        sampleTypeHelper.mergeImport(sampleData);

        samplesTable = sampleTypeHelper.getSamplesDataRegionTable();
        actualValue = samplesTable.getRowDataAsMap("Name", sample).get(textChoiceFieldName);

        checker().verifyEquals(String.format("Doesn't look like the text choice value of '%s' was set for sample '%s' after import/update.", expectedValue, sample),
                expectedValue, actualValue);

        log("Finally validate that an invalid TextChoice value used during sample import/update fails.");

        sample = availableSamples.get(0);
        availableSamples.remove(0);

        String invalidValue = "ZZZZ";

        sampleData = new ArrayList<>();
        sampleData.add(Map.of("Name", sample, textChoiceFieldName, invalidValue));

        sampleTypeHelper.mergeImportExpectingError(sampleData);

        WebElement errorMsg = Locator.tagWithClass("div", "labkey-error").findWhenNeeded(getDriver());
        waitFor(errorMsg::isDisplayed, "Error message was not shown.", 1_000);

        checker().verifyEquals("Error message not as expected.",
                String.format("Value '%s' for field '%s' is invalid.", invalidValue, textChoiceFieldCaption), errorMsg.getText());
    }

}
