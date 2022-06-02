package org.labkey.test.tests.list;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.tests.MissingValueIndicatorsTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;

import java.util.Arrays;
import java.util.List;

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


        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[3];

        ListHelper.ListColumn listColumn = new ListHelper.ListColumn("name", "Name", ListHelper.ListColumnType.String, "");
        columns[0] = listColumn;

        listColumn = new ListHelper.ListColumn("age with space", "Age with space", ListHelper.ListColumnType.Integer, "");
        listColumn.setMvEnabled(true);
        columns[1] = listColumn;

        listColumn = new ListHelper.ListColumn("sex", "Sex", ListHelper.ListColumnType.String, "");
        listColumn.setMvEnabled(true);
        columns[2] = listColumn;

        _listHelper.createList(getProjectName(), LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns);

        log("Test upload list data with a combined data and MVI column");
        _listHelper.goToList(LIST_NAME);
        ImportDataPage importDataPage = _listHelper.clickImportData();
        importDataPage.setText(TEST_DATA_SINGLE_COLUMN_LIST_BAD);
        importDataPage.submitExpectingError();

        importDataPage.setText(TEST_DATA_SINGLE_COLUMN_LIST);
        importDataPage.submit();
        validateSingleColumnData();
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
        validateTwoColumnData("query", "name");
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
