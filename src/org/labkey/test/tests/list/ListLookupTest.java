package org.labkey.test.tests.list;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.data.TestDataUtils;
import org.labkey.test.util.query.QueryApiHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.params.FieldDefinition.labelFromName;
import static org.labkey.test.util.TextUtils.normalizeSpace;
import static org.labkey.test.util.TestDataGenerator.ALL_CHARS_PLACEHOLDER;
import static org.labkey.test.util.TestDataGenerator.REPEAT_PLACEHOLDER;

// Issue 52098, Issue 49422
@Category({Daily.class, Data.class, Hosting.class})
public class ListLookupTest extends BaseWebDriverTest
{
    private static final String lookToListName = TestDataGenerator.randomDomainName("lookToList", DomainUtils.DomainKind.IntList);
    private static final String lookToKeyFieldName = TestDataGenerator.randomFieldName("lookToKeyField", 5, 5, "" + REPEAT_PLACEHOLDER + ALL_CHARS_PLACEHOLDER, DomainUtils.DomainKind.IntList);
    private static final String lookToFieldName = TestDataGenerator.randomFieldName("lookToField", null, DomainUtils.DomainKind.IntList);
    private static List<Map<String, String>> lookToListValues;
    private static String lookupKeyAsNameNumber;
    private static String lookupKeyAsNameFieldValue;
    private static final String lookFromListName = TestDataGenerator.randomDomainName("lookFromList", DomainUtils.DomainKind.IntList);
    private static final String lookFromKeyFieldName = TestDataGenerator.randomFieldName("Look From Key Field", 5, 5, "" + REPEAT_PLACEHOLDER + ALL_CHARS_PLACEHOLDER, DomainUtils.DomainKind.IntList);
    private static final String lookFromLookupFieldName = TestDataGenerator.randomFieldName("Look From Lookup Field", null, DomainUtils.DomainKind.IntList);
    private static final String lookFromLookupFieldKey = EscapeUtil.fieldKeyEncodePart(lookFromLookupFieldName);

    @BeforeClass
    public static void setupProject()
    {
        ListLookupTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        log("Setup project and list module");
        _containerHelper.createProject(getProjectName(), null);

        log("Create a list to use as a lookup table with some number-like names.");
        _listHelper.createList(getProjectName(), lookToListName, lookToKeyFieldName,
                new FieldDefinition(lookToFieldName, FieldDefinition.ColumnType.String));
        String bulkData = tsvFromColumn(List.of(
            lookToFieldName,
            "1E2",
            "102",
            "Lookup",
            ".123"));
        _listHelper.bulkImportData(bulkData);

        DataRegionTable dataRegionTable = new DataRegionTable("query", getDriver());
        CustomizeView customizer = dataRegionTable.openCustomizeGrid();
        customizer.showHiddenItems();
        customizer.addColumn(EscapeUtil.fieldKeyEncodePart(lookToKeyFieldName));
        customizer.clickViewGrid();
        lookToListValues = dataRegionTable.getTableData();
        lookupKeyAsNameNumber = lookToListValues.get(0).get(EscapeUtil.fieldKeyEncodePart(lookToKeyFieldName));
        lookupKeyAsNameFieldValue = lookToListValues.get(0).get(EscapeUtil.fieldKeyEncodePart(lookToFieldName));
        _listHelper.insertNewRow(Map.of(lookToFieldName, lookupKeyAsNameNumber));

        log("Create a second list that looks up to the first list.");
        _listHelper.createList(getProjectName(), lookFromListName, lookFromKeyFieldName);
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(lookFromListName);
        listDefinitionPage.getFieldsPanel()
                .addField(lookFromLookupFieldName)
                .setLookup(new FieldDefinition.IntLookup("lists", lookToListName));
        listDefinitionPage.clickSave();
    }


    @Test
    public void testWithoutValidatorOrAlternateKeys() throws IOException, CommandException
    {
        goToProjectHome();
        setLookupValidatorEnabled(false);
        resetList();

        log("Import data into the second list without alternate keys.");
        String bulkData = tsvFromColumn(List.of(lookFromLookupFieldName, lookupKeyAsNameNumber));
        _listHelper.clickImportData()
                .setText(bulkData)
                .submit();
        log("Verify the import succeeds and resolves by primary key when not expecting alternate keys.");
        List<Map<String, String>> expectedData = List.of(
                Map.of(lookFromLookupFieldKey, lookupKeyAsNameFieldValue)
        );
        validateListValues(expectedData);

        log("Clean out list before next import.");
        resetList();
        log("Import data into second list without alternate keys supplying invalid primary key");
        bulkData = tsvFromColumn(List.of(lookFromLookupFieldName, "1000"));
        _listHelper.clickImportData()
                .setText(bulkData)
                .submit();
        log("Verify the import succeeds but invalid primary key is left unresolved.");
        expectedData = List.of(
                Map.of(lookFromLookupFieldKey, "<1000>")
        );
        validateListValues(expectedData);

        log("Check for error if not using alternate key and type does not match");
        bulkData = tsvFromColumn(List.of(lookFromLookupFieldName, "noneSuch"));
        ImportDataPage importDataPage = _listHelper.clickImportData();
        String error = importDataPage
                .setText(bulkData)
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message for invalid primary key not as expected",
                "Could not convert value 'noneSuch' (String) for Integer field '" + normalizeSpace(lookFromLookupFieldName) + "'", error);
    }

    @Test
    public void testWithoutValidatorWithAlternateKeys() throws IOException, CommandException
    {
        goToProjectHome();
        setLookupValidatorEnabled(false);
        log("Clean out list before next import.");
        resetList();
        log("Import data into the second list using number-like lookup values expecting alternate keys but also accepting primary keys.");
        String bulkData = tsvFromColumn(List.of(
            lookFromLookupFieldName,
            "1E2", // valid alternate key looking like a number
            lookupKeyAsNameNumber, // valid alternate key same value as a primary key
            ".123", // valid alternate key looking like a float
            "Lookup", // valid alternate key that is a string
            lookupKeyAsNameNumber, // another copy
            "102", // valid number-like alternate key
            lookToListValues.get(1).get(EscapeUtil.fieldKeyEncodePart(lookToKeyFieldName)), // primary key value not matching an alternate key
            "1000" // primary key-type value that doesn't match
        ));
        _listHelper.clickImportData()
                .setText(bulkData)
                .setImportLookupByAlternateKey(true)
                .submit();
        log("Verify the import succeeds and resolves the lookups appropriately.");
        List<Map<String, String>> expectedData = List.of(
                Map.of(lookFromLookupFieldKey, "1E2"),
                Map.of(lookFromLookupFieldKey, lookupKeyAsNameNumber),
                Map.of(lookFromLookupFieldKey, ".123"),
                Map.of(lookFromLookupFieldKey, "Lookup"),
                Map.of(lookFromLookupFieldKey, lookupKeyAsNameNumber),
                Map.of(lookFromLookupFieldKey, "102"),
                Map.of(lookFromLookupFieldKey, lookToListValues.get(1).get(EscapeUtil.fieldKeyEncodePart(lookToFieldName))),
                Map.of(lookFromLookupFieldKey, "<1000>")
        );
        validateListValues(expectedData);

        log("Check for error if providing non-matching string value that is not a number");
        bulkData = tsvFromColumn(List.of(lookFromLookupFieldName, "NotAValue"));
        ImportDataPage importDataPage = _listHelper.clickImportData();
        String error = importDataPage
                .setText(bulkData)
                .setImportLookupByAlternateKey(true)
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message after supplying invalid alternate key not as expected",
                "Value 'NotAValue' not found for field " + normalizeSpace(lookFromLookupFieldName) + " in the current context.", error);
    }

    @Test
    public void testWithLookupValidatorWithoutAlternateKeys() throws IOException, CommandException
    {
        goToProjectHome();
        setLookupValidatorEnabled(true);
        log("Clean out list before next import.");
        resetList();

        //   without alternate keys
        log("With lookup validation on, import data into the second list without alternate keys.");
        String bulkData = tsvFromColumn(List.of(lookFromLookupFieldName, lookupKeyAsNameNumber));
        _listHelper.clickImportData()
                .setText(bulkData)
                .submit();
        log("Verify the import succeeds and resolves by primary key when not expecting alternate keys.");
        List<Map<String, String>> expectedData = List.of(
                Map.of(lookFromLookupFieldKey, lookupKeyAsNameFieldValue)
        );
        validateListValues(expectedData);

        log("With lookup validation on, import data and provide an invalid primary key.");
        ImportDataPage importDataPage = _listHelper.clickImportData();
        String error = importDataPage
                .setText(tsvFromColumn(List.of(lookFromLookupFieldName, "1000")))
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message for invalid primary key value not as expected",
                "Value '1000' was not present in lookup target 'lists." + normalizeSpace(lookToListName)
                        + "' for field '" + normalizeSpace(labelFromName(lookFromLookupFieldName)) + "'", error);

        log("With lookup validation on, import data and provide an invalid primary key of type string.");
        error = importDataPage
                .setText(tsvFromColumn(List.of(lookFromLookupFieldName, "Look")))
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message for invalid primary key type not as expected",
                "Could not convert value 'Look' (String) for Integer field '" + normalizeSpace(lookFromLookupFieldName) + "'", error);
    }

    @Test
    public void testWithLookupValidatorAndAlternateKeys() throws IOException, CommandException
    {
        goToProjectHome();
        setLookupValidatorEnabled(true);
        log("Clean out list before next import.");
        resetList();

        log("With lookup validation on, import data into the second list using number-like lookup values expecting alternate keys but also accepting primary keys.");
        String bulkData = tsvFromColumn(List.of(
            lookFromLookupFieldName,
            "1E2", // valid alternate key looking like a number
            lookupKeyAsNameNumber, // valid alternate key same value as a primary key
            ".123", // valid alternate key looking like a float
            "Lookup", // valid alternate key that is a string
            lookupKeyAsNameNumber, // another copy
            "102", // valid number-like alternate key
            lookToListValues.get(1).get(EscapeUtil.fieldKeyEncodePart(lookToKeyFieldName)) // primary key value not matching an alternate key
        ));
        _listHelper.clickImportData()
                .setText(bulkData)
                .setImportLookupByAlternateKey(true)
                .submit();
        log("Verify the import succeeds and resolves the lookups appropriately.");
        List<Map<String, String>> expectedData = List.of(
                Map.of(lookFromLookupFieldKey, "1E2"),
                Map.of(lookFromLookupFieldKey, lookupKeyAsNameNumber),
                Map.of(lookFromLookupFieldKey, ".123"),
                Map.of(lookFromLookupFieldKey, "Lookup"),
                Map.of(lookFromLookupFieldKey, lookupKeyAsNameNumber),
                Map.of(lookFromLookupFieldKey, "102"),
                Map.of(lookFromLookupFieldKey, lookToListValues.get(1).get(EscapeUtil.fieldKeyEncodePart(lookToFieldName)))
        );
        validateListValues(expectedData);

        bulkData = tsvFromColumn(List.of(lookFromLookupFieldName, "Invalid"));
        ImportDataPage importDataPage = _listHelper.clickImportData();
        String error = importDataPage
                .setText(bulkData)
                .setImportLookupByAlternateKey(true)
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message for invalid string alternate key not as expected",
                "Value 'Invalid' not found for field " + normalizeSpace(lookFromLookupFieldName) + " in the current context.", error);

        bulkData = tsvFromColumn(List.of(lookFromLookupFieldName, "1234"));
        error = importDataPage
                .setText(bulkData)
                .setImportLookupByAlternateKey(true)
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message for invalid number-like alternate key not as expected",
                "Value '1234' was not present in lookup target 'lists." + normalizeSpace(lookToListName)
                        + "' for field '" + normalizeSpace(labelFromName(lookFromLookupFieldName)) + "'", error);

    }

    private void setLookupValidatorEnabled(boolean enabled)
    {
        log("Setting lookup validator to " + enabled + " on list " + lookFromListName);
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(lookFromListName);
        listDefinitionPage.getFieldsPanel()
                .getField(lookFromLookupFieldName)
                .expand()
                .setLookupValidatorEnabled(enabled);
        listDefinitionPage.clickSave();
    }

    private void resetList() throws IOException, CommandException
    {
        new QueryApiHelper(createDefaultConnection(), getProjectName(), "lists", lookFromListName).truncateTable();
    }

    private void validateListValues(List<Map<String, String>> expectedValue)
    {
        DataRegionTable dataRegionTable = new DataRegionTable("query", getDriver());
        List<Map<String, String>> actualValue = dataRegionTable.getTableData();

        assertEquals("List data not as expected after action.",
                expectedValue, actualValue);
    }

    private String tsvFromColumn(List<String> column)
    {
        List<List<String>> rows = column.stream().map(Collections::singletonList).toList();
        return TestDataUtils.stringFromRows(rows);
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "List Lookup Test";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }

}
