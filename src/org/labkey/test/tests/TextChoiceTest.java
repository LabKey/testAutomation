package org.labkey.test.tests;

import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for the TextChoice fields. Contains some shared helper functions.
 */
public abstract class TextChoiceTest extends BaseWebDriverTest
{

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment", "issues");
    }

    // Sample Type field names, values etc...
    protected static final String ST_NAME = "Simple_TC_SampleType";
    protected static final String ST_TC_FIELD = "TC_Field";
    protected static final String ST_TEXT_FIELD = "Str";
    protected static final List<String> ST_VALUES = Arrays.asList("ST1", "ST2", "ST3");

    protected static final List<String> SAMPLES = Arrays.asList("S_1", "S_2", "S_3", "S_4", "S_5", "S_6", "S_7", "S_8", "S_9", "S_10");

    protected static List<Map<String, String>> ST_DATA = new ArrayList<>();

    // Assay field names, values etc...
    protected static final String ASSAY_NAME = "Simple_TC_Assay";
    protected static final String ASSAY_RUN_ID = "The_One_And_Only_Run";

    protected static final String BATCH_TC_FIELD = "Batch_TC_Field";
    protected static final List<String> BATCH_FIELD_VALUES = List.of("B1", "B2", "B3");
    protected static final String BATCH_VALUE = BATCH_FIELD_VALUES.get(1);

    protected static final String RUN_TC_FIELD = "Run_TC_Field";
    protected static final List<String> RUN_FIELD_VALUES = List.of("RN1", "RN2", "RN3", "RN4", "RN5");
    protected static final String RUN_VALUE = RUN_FIELD_VALUES.get(1);

    protected static final String RESULT_TC_FIELD = "Result_TC_Field";
    protected static final List<String> RESULT_FIELD_VALUES = List.of("RS1", "RS2", "RS3", "RS4", "RS5", "RS6", "RS7", "RS8", "RS9", "RS10");

    protected static final String RESULT_SAMPLE_FIELD = "Sample";

    /**
     * Create a sample type using the 'default values'. Does no validation of the design.
     *
     * @throws IOException Can be thrown by the create command.
     * @throws CommandException Can be thrown by the create command.
     */
    protected void createDefaultSampleTypeWithTextChoice() throws IOException, CommandException
    {
        log(String.format("Create a new sample type named '%s'.", ST_NAME));
        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(ST_NAME);
        sampleTypeDefinition.setNameExpression("S_${genId}");

        log(String.format("Create a TextChoice field '%s' in the sample type.", ST_TC_FIELD));
        FieldDefinition fieldDefinition = new FieldDefinition(ST_TC_FIELD, FieldDefinition.ColumnType.TextChoice);
        fieldDefinition.setTextChoiceValues(ST_VALUES);
        sampleTypeDefinition.addField(fieldDefinition);

        log(String.format("Create a String field '%s' in the sample type.", ST_TEXT_FIELD));
        fieldDefinition = new FieldDefinition(ST_TEXT_FIELD, FieldDefinition.ColumnType.String);
        sampleTypeDefinition.addField(fieldDefinition);

        TestDataGenerator dataGenerator = SampleTypeAPIHelper.createEmptySampleType(getCurrentContainerPath(), sampleTypeDefinition);

        ST_DATA = new ArrayList<>();

        int valueIndex = 0;
        for (String sample : SAMPLES)
        {
            Map<String, Object> rowMap = new HashMap<>();

            String txtField = String.format("String field for sample %s", sample);

            // This will leave one of the values unused/unlocked.
            if (valueIndex == 2)
                valueIndex = 0;

            String tcValue = ST_VALUES.get(valueIndex++);

            // Create the Map<String, Object> to use for the data import.
            rowMap.put("Name", sample);
            rowMap.put(ST_TEXT_FIELD, txtField);
            rowMap.put(ST_TC_FIELD, tcValue);

            dataGenerator.addCustomRow(rowMap);

            // Add the row to the expected list of Map<String, String>
            ST_DATA.add(Map.of("Name", sample, ST_TEXT_FIELD, txtField, ST_TC_FIELD, tcValue));
        }

        dataGenerator.insertRows();

    }

    /**
     * Helper to create an assay design using the 'default values'. It does no validation of the design.
     */
    protected void createDefaultAssayDesignWithTextChoice()
    {
        goToManageAssays();
        ReactAssayDesignerPage assayDesignerPage = _assayHelper.createAssayDesign("General", ASSAY_NAME)
                .setDescription("Testing TextChoice fields.");

        log(String.format("Add a TextChoice field named '%s' to the batch properties.", BATCH_TC_FIELD));
        DomainFormPanel domainFormPanel = assayDesignerPage.goToBatchFields();
        DomainFieldRow fieldRow = domainFormPanel.addField(BATCH_TC_FIELD);
        fieldRow.setType(FieldDefinition.ColumnType.TextChoice);
        fieldRow.setTextChoiceValues(BATCH_FIELD_VALUES);

        log("Remove the default batch fields.");
        domainFormPanel.removeField("ParticipantVisitResolver");
        domainFormPanel.removeField("TargetStudy");

        log(String.format("Add a TextChoice field named '%s' to the run properties.", RUN_TC_FIELD));
        fieldRow = assayDesignerPage.goToRunFields().addField(RUN_TC_FIELD);
        fieldRow.setType(FieldDefinition.ColumnType.TextChoice);
        fieldRow.setTextChoiceValues(RUN_FIELD_VALUES);

        domainFormPanel = assayDesignerPage.goToResultsFields();

        log(String.format("Add a TextChoice field named '%s' to the results.", RESULT_TC_FIELD));
        fieldRow = domainFormPanel.addField(RESULT_TC_FIELD);
        fieldRow.setType(FieldDefinition.ColumnType.TextChoice);
        fieldRow.setTextChoiceValues(RESULT_FIELD_VALUES);

        log("Remove the default result fields.");
        domainFormPanel.removeField("SpecimenID");
        domainFormPanel.removeField("ParticipantID");
        domainFormPanel.removeField("VisitID");
        domainFormPanel.removeField("Date");

        log(String.format("Add a sample field name '%s'.", RESULT_SAMPLE_FIELD));
        domainFormPanel.addField(new FieldDefinition(RESULT_SAMPLE_FIELD, FieldDefinition.ColumnType.Sample));

        assayDesignerPage.clickSave();

    }

    /**
     * Simple helper to identify the name of the control on a page based on the field name. The name of the
     * control is the field but the first letter is lower case. This lets the test not worry about that.
     *
     * @param tcFieldName The TextChoice field name.
     * @return The field name with the first letter lower case.
     */
    protected String getSelectControlName(String tcFieldName)
    {
        return Character.toLowerCase(tcFieldName.charAt(0)) + tcFieldName.substring(1);
    }

    /**
     * For an assays results table verify the rows. The values for the batch and run TextChoice fields should be the same
     * for all rows but the value for the result field can differ from row to row.
     *
     * @param expectedRowData A map with the sample id and the expected TextChoice value for the result.
     * @param expectedRunValue Expected TextChoice value for the run field.
     */
    protected void verifyRunResultsTable(Map<String, String> expectedRowData, String expectedBatchValue, String expectedRunValue)
    {
        DataRegionTable dataRegionTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("Data").find();

        // Check the results for each sample.

        for(Map.Entry<String, String> entry : expectedRowData.entrySet())
        {
            Map<String, String> rowData = dataRegionTable.getRowDataAsMap(RESULT_SAMPLE_FIELD, entry.getKey());

            Map<String, String> expectedData = Map.of(RESULT_SAMPLE_FIELD, entry.getKey(),
                    RESULT_TC_FIELD, entry.getValue(),
                    String.format("Run/Batch/%s", BATCH_TC_FIELD), expectedBatchValue,
                    String.format("Run/%s", RUN_TC_FIELD), expectedRunValue);

            checker().verifyEquals(String.format("Result row not as expected for sample '%s'.", entry.getKey()), expectedData, rowData);
        }

    }

}
