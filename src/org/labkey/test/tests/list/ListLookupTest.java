package org.labkey.test.tests.list;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
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
import org.labkey.test.params.ContainerInfo;
import org.labkey.test.params.FieldInfo;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.data.TestDataUtils;
import org.labkey.test.util.query.QueryApiHelper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.labkey.test.params.FieldDefinition.ColumnType;
import static org.labkey.test.params.FieldDefinition.IntLookup;
import static org.labkey.test.params.FieldDefinition.labelFromName;
import static org.labkey.test.util.DomainUtils.DomainKind.IntList;
import static org.labkey.test.util.TextUtils.normalizeSpace;

// Issue 52098, Issue 49422
@Category({Daily.class, Data.class, Hosting.class})
public class ListLookupTest extends BaseWebDriverTest
{
    private static final ContainerInfo PROJECT = ContainerInfo.project("ListLookupTest");

    private static final String lookToListName = IntList.randomName("lookToList");
    private static final FieldInfo lookToKeyField = IntList.randomField("lookToKeyField", ColumnType.Integer);
    private static final String lookToKeyFieldKey = lookToKeyField.toString();
    private static final FieldInfo lookToField = IntList.randomField("lookToField", ColumnType.String);
    private static final String lookToFieldFieldKey = lookToField.toString();
    private static List<Map<String, String>> lookToListValues;
    private static String lookupKeyAsNameNumber;
    private static String lookupKeyAsNameFieldValue;
    private static final String lookFromListName = IntList.randomName("lookFromList");
    private static final FieldInfo lookFromKeyField = IntList.randomField("Look From Key Field", ColumnType.Integer);
    private static final FieldInfo lookFromLookupField = IntList.randomField("Look From Lookup Field", new IntLookup(ListHelper.LIST_SCHEMA, lookToListName));
    private static final String lookFromLookupFieldKey = lookFromLookupField.toString();

    @BeforeClass
    public static void setupProject()
    {
        ListLookupTest init = getCurrentTest();
        init.doSetup();
    }

    @Before
    public void beforeTest() throws Exception
    {
        resetList();
    }

    private void doSetup()
    {
        log("Setup project and list module");
        _containerHelper.createProject(getProjectName(), null);

        log("Create a list to use as a lookup table with some number-like names.");
        _listHelper.createList(getProjectName(), lookToListName, lookToKeyField.getName(), lookToField.getFieldDefinition());
        String bulkData = tsvFromColumn(List.of(
            lookToField.getName(),
            "1E2",
            "102",
            "Lookup",
            ".123"));
        _listHelper.bulkImportData(bulkData);

        DataRegionTable dataRegionTable = new DataRegionTable("query", getDriver());
        CustomizeView customizer = dataRegionTable.openCustomizeGrid();
        customizer.showHiddenItems();
        customizer.addColumn(lookToKeyFieldKey);
        customizer.clickViewGrid();
        lookToListValues = dataRegionTable.getTableData();
        lookupKeyAsNameNumber = lookToListValues.getFirst().get(lookToKeyFieldKey);
        lookupKeyAsNameFieldValue = lookToListValues.getFirst().get(lookToFieldFieldKey);
        _listHelper.insertNewRow(Map.of(lookToField.getName(), lookupKeyAsNameNumber));

        log("Create a second list that looks up to the first list.");
        _listHelper.createList(getProjectName(), lookFromListName, lookFromKeyField.getName());
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(lookFromListName);
        listDefinitionPage.getFieldsPanel()
                .addField(lookFromLookupField.getFieldDefinition());
        listDefinitionPage.clickSave();
    }

    @Test
    public void testWithoutValidatorOrAlternateKeys() throws IOException, CommandException
    {
        goToProjectHome();
        setLookupValidatorEnabled(false);

        log("Import data into the second list without alternate keys.");
        String bulkData = tsvFromColumn(List.of(lookFromLookupField.getName(), lookupKeyAsNameNumber));
        _listHelper.clickImportData()
                .setText(bulkData)
                .submit();

        log("Verify the import succeeds and resolves by primary key when not expecting alternate keys.");
        List<Map<String, String>> expectedData = List.of(Map.of(lookFromLookupFieldKey, lookupKeyAsNameFieldValue));
        validateListValues(expectedData);

        log("Clean out list before next import.");
        resetList();

        log("Import data into second list without alternate keys supplying invalid primary key");
        bulkData = tsvFromColumn(List.of(lookFromLookupField.getName(), "1000"));
        _listHelper.clickImportData()
                .setText(bulkData)
                .submit();

        log("Verify the import succeeds but invalid primary key is left unresolved.");
        expectedData = List.of(Map.of(lookFromLookupFieldKey, "<1000>"));
        validateListValues(expectedData);

        log("Check for error if not using alternate key and type does not match");
        bulkData = tsvFromColumn(List.of(lookFromLookupField.getName(), "noneSuch"));
        String error = _listHelper.clickImportData()
                .setText(bulkData)
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message for invalid primary key not as expected",
                "Could not convert value 'noneSuch' (String) for Integer field '" + normalizeSpace(lookFromLookupField.getName()) + "'", error);
    }

    @Test
    public void testWithoutValidatorWithAlternateKeys()
    {
        goToProjectHome();
        setLookupValidatorEnabled(false);

        log("Import data into the second list using number-like lookup values expecting alternate keys but also accepting primary keys.");
        String bulkData = tsvFromColumn(List.of(
            lookFromLookupField.getName(),
            "1E2", // valid alternate key looking like a number
            lookupKeyAsNameNumber, // valid alternate key same value as a primary key
            ".123", // valid alternate key looking like a float
            "Lookup", // valid alternate key that is a string
            lookupKeyAsNameNumber, // another copy
            "102", // valid number-like alternate key
            lookToListValues.get(1).get(lookToKeyFieldKey), // primary key value not matching an alternate key
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
                Map.of(lookFromLookupFieldKey, lookToListValues.get(1).get(lookToFieldFieldKey)),
                Map.of(lookFromLookupFieldKey, "<1000>")
        );
        validateListValues(expectedData);

        log("Check for error if providing non-matching string value that is not a number");
        bulkData = tsvFromColumn(List.of(lookFromLookupField.getName(), "NotAValue"));
        ImportDataPage importDataPage = _listHelper.clickImportData();
        String error = importDataPage
                .setText(bulkData)
                .setImportLookupByAlternateKey(true)
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message after supplying invalid alternate key not as expected",
                "Value 'NotAValue' not found for field " + normalizeSpace(lookFromLookupField.getName()) + " in the current context.", error);
    }

    @Test
    public void testWithLookupValidatorWithoutAlternateKeys()
    {
        goToProjectHome();
        setLookupValidatorEnabled(true);

        //   without alternate keys
        log("With lookup validation on, import data into the second list without alternate keys.");
        String bulkData = tsvFromColumn(List.of(lookFromLookupField.getName(), lookupKeyAsNameNumber));
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
                .setText(tsvFromColumn(List.of(lookFromLookupField.getName(), "1000")))
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message for invalid primary key value not as expected",
                "Value '1000' was not present in lookup target 'lists." + normalizeSpace(lookToListName)
                        + "' for field '" + normalizeSpace(labelFromName(lookFromLookupField.getName())) + "'", error);

        log("With lookup validation on, import data and provide an invalid primary key of type string.");
        error = importDataPage
                .setText(tsvFromColumn(List.of(lookFromLookupField.getName(), "Look")))
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message for invalid primary key type not as expected",
                "Could not convert value 'Look' (String) for Integer field '" + normalizeSpace(lookFromLookupField.getName()) + "'", error);
    }

    @Test
    public void testWithLookupValidatorAndAlternateKeys()
    {
        goToProjectHome();
        setLookupValidatorEnabled(true);

        log("With lookup validation on, import data into the second list using number-like lookup values expecting alternate keys but also accepting primary keys.");
        String bulkData = tsvFromColumn(List.of(
            lookFromLookupField.getName(),
            "1E2", // valid alternate key looking like a number
            lookupKeyAsNameNumber, // valid alternate key same value as a primary key
            ".123", // valid alternate key looking like a float
            "Lookup", // valid alternate key that is a string
            lookupKeyAsNameNumber, // another copy
            "102", // valid number-like alternate key
            lookToListValues.get(1).get(lookToKeyFieldKey) // primary key value not matching an alternate key
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
                Map.of(lookFromLookupFieldKey, lookToListValues.get(1).get(lookToFieldFieldKey))
        );
        validateListValues(expectedData);

        bulkData = tsvFromColumn(List.of(lookFromLookupField.getName(), "Invalid"));
        ImportDataPage importDataPage = _listHelper.clickImportData();
        String error = importDataPage
                .setText(bulkData)
                .setImportLookupByAlternateKey(true)
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message for invalid string alternate key not as expected",
                "Value 'Invalid' not found for field " + normalizeSpace(lookFromLookupField.getName()) + " in the current context.", error);

        bulkData = tsvFromColumn(List.of(lookFromLookupField.getName(), "1234"));
        error = importDataPage
                .setText(bulkData)
                .setImportLookupByAlternateKey(true)
                .submitExpectingError();
        checker().withScreenshot().verifyEquals("Error message for invalid number-like alternate key not as expected",
                "Value '1234' was not present in lookup target 'lists." + normalizeSpace(lookToListName)
                        + "' for field '" + normalizeSpace(labelFromName(lookFromLookupField.getName())) + "'", error);
    }

    private void setLookupValidatorEnabled(boolean enabled)
    {
        log("Setting lookup validator to " + enabled + " on list " + lookFromListName);
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(lookFromListName);
        listDefinitionPage.getFieldsPanel()
                .getField(lookFromLookupField.getName())
                .expand()
                .setLookupValidatorEnabled(enabled);
        listDefinitionPage.clickSave();
    }

    private void resetList() throws IOException, CommandException
    {
        new QueryApiHelper(createDefaultConnection(), getProjectName(), ListHelper.LIST_SCHEMA, lookFromListName).truncateTable();
    }

    private void validateListValues(List<Map<String, String>> expectedValue)
    {
        List<Map<String, String>> actualValue = new DataRegionTable("query", getDriver())
                .getTableData();

        checker().withScreenshot().verifyEquals("List data not as expected after action.", expectedValue, actualValue);
    }

    private String tsvFromColumn(List<String> column)
    {
        List<List<String>> rows = column.stream().map(Collections::singletonList).toList();
        return TestDataUtils.stringFromRows(rows);
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return PROJECT.getName();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("list");
    }
}
