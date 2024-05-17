package org.labkey.test.tests.list;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.tests.MissingValueIndicatorsTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.util.DataRegionTable.DataRegion;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class ListMissingValuesTest extends MissingValueIndicatorsTest
{
    @BeforeClass
    public static void beforeTestClass()
    {
        ListMissingValuesTest init = (ListMissingValuesTest)getCurrentTest();

        init.setupProject();
    }

    @LogMethod
    private void setupProject()
    {
        _containerHelper.createProject(getProjectName(), null);
        setupMVIndicators();
    }

    @Test
    public void testListMV()
    {
        final String LIST_NAME = "MVList";
        final String TEST_DATA_SINGLE_COLUMN_LIST =
                "Name\tAge with space\tSex\n" +
                        "Ted\tN\tmale\n" +
                        "Alice\t17\tfemale\n" +
                        "Bob\tQ\tN";
        final String TEST_DATA_TWO_COLUMN_LIST =
                "Name\tAge with space\tAge with spaceMVIndicator\tSex\tSexMVIndicator\n" +
                        "Franny\t\tN\tmale\t\n" +
                        "Zoe\t25\tQ\tfemale\t\n" +
                        "J.D.\t50\t\tmale\tQ";
        final String TEST_DATA_SINGLE_COLUMN_LIST_BAD =
                "Name\tAge with space\tSex\n" +
                        "Ted\t.N\tmale\n" +
                        "Alice\t17\tfemale\n" +
                        "Bob\tQ\tN";
        final String TEST_DATA_TWO_COLUMN_LIST_BAD =
                "Name\tAge with space\tAge with spaceMVIndicator\tSex\tSexMVIndicator\n" +
                        "Franny\t\tN\tmale\t\n" +
                        "Zoe\t25\tQ\tfemale\t\n" +
                        "J.D.\t50\t\tmale\t.Q";


        FieldDefinition[] columns = new FieldDefinition[3];

        FieldDefinition listColumn = new FieldDefinition("name", ColumnType.String).setLabel("Name");
        columns[0] = listColumn;

        listColumn = new FieldDefinition("age with space", ColumnType.Integer).setLabel("Age with space");
        listColumn.setMvEnabled(true);
        columns[1] = listColumn;

        listColumn = new FieldDefinition("sex", ColumnType.String).setLabel("Sex");
        listColumn.setMvEnabled(true);
        columns[2] = listColumn;

        String containerPath = getProjectName();
        _listHelper.createList(containerPath, LIST_NAME, "Key", columns);

        log("Test upload list data with a combined data and MVI column");
        _listHelper.goToList(LIST_NAME);
        ImportDataPage importDataPage = _listHelper.clickImportData();
        importDataPage.setText(TEST_DATA_SINGLE_COLUMN_LIST_BAD);
        importDataPage.submitExpectingError();

        importDataPage.setText(TEST_DATA_SINGLE_COLUMN_LIST);
        importDataPage.submit();
        assertNoLabKeyErrors();

        DataRegionTable dataRegion = new DataRegionTable("query", this);

        Map<String, List<String>> expectedData = new HashMap<>();
        expectedData.put("Name", List.of("Ted", "Alice", "Bob"));
        expectedData.put("Age with space", List.of("N", "17", "Q"));
        expectedData.put("Sex", List.of("male", "female", "N"));

        Map<String, List<Integer>> expectedMVIndicators = new HashMap<>();
        expectedMVIndicators.put("Age with space", List.of(0, 2));
        expectedMVIndicators.put("Sex", List.of(2));

        checkDataregionData(dataRegion, expectedData);
        checkMvIndicatorPresent(dataRegion, expectedMVIndicators);

        testMvFiltering(List.of("age with space", "sex"));

        deleteListData();

        log("Test inserting a single new row");
        DataRegion(getDriver()).find().clickInsertNewRow();
        setFormElement(Locator.name("quf_name"), "Sid");
        setFormElement(Locator.name("quf_sex"), "male");
        selectOptionByValue(Locator.name("quf_age with spaceMVIndicator"), "Z");
        clickButton("Submit");
        assertNoLabKeyErrors();
        assertTextPresent("Sid", "male", "N");

        deleteListData();

        log("Test separate MVIndicator column");
        importDataPage = DataRegion(getDriver()).find().clickImportBulkData();
        importDataPage.setText(TEST_DATA_TWO_COLUMN_LIST_BAD);
        importDataPage.submitExpectingError();

        importDataPage.setText(TEST_DATA_TWO_COLUMN_LIST);
        importDataPage.submit();
        assertNoLabKeyErrors();

        dataRegion = new DataRegionTable("query", this);

        expectedData = new HashMap<>();
        expectedData.put("Name", List.of("Franny", "Zoe", "J.D."));
        expectedData.put("Age with space", List.of("N", "Q", "50"));
        expectedData.put("Sex", List.of("male", "female", "Q"));

        expectedMVIndicators = new HashMap<>();
        expectedMVIndicators.put("Age with space", List.of(0, 1));
        expectedMVIndicators.put("Sex", List.of(2));

        checkDataregionData(dataRegion, expectedData);
        checkMvIndicatorPresent(dataRegion, expectedMVIndicators);
        checkOriginalValuePopup(dataRegion, "sex", 2, "male");

        testMvFiltering(List.of("age with space", "sex"));
    }

    private void deleteListData()
    {
        DataRegionTable dt = new DataRegionTable("query", getDriver());
        dt.checkAllOnPage();
        dt.deleteSelectedRows();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment", "list");
    }
}
