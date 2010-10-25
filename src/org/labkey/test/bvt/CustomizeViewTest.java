package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ListHelper;

/**
 * Created by IntelliJ IDEA.
 * User: kevink
 * Date: Oct 14, 2010
 * Time: 11:37:20 AM
 */
public class CustomizeViewTest extends BaseSeleniumWebTest
{
    public static final String PROJECT_NAME = "CustomizeViewTest";
    public static final String LIST_NAME = "People";
    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.AutoInteger;
    private final static String LIST_KEY_NAME = "Key";
    protected static final String TEST_ASSAY = "TestAssay1";
    protected static final String TEST_ASSAY_DESC = "Description for assay 1";

    private final static ListHelper.ListColumn[] LIST_COLUMNS = new ListHelper.ListColumn[]
            {
                    new ListHelper.ListColumn("FirstName", "First Name", ListHelper.ListColumnType.String, "The first name"),
                    new ListHelper.ListColumn("LastName", "Last Name", ListHelper.ListColumnType.String, "The last name"),
                    new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "The age")
            };
    static
    {
        LIST_COLUMNS[0].setRequired(true);
        LIST_COLUMNS[1].setRequired(true);
    }

    private final static String[][] TEST_DATA =
            {
                    { "1", "Bill", "Billson", "34" },
                    { "2", "Jane", "Janeson", "42" },
                    { "3", "John", "Johnson", "17" },
                    { "4", "Mandy", "Mandyson", "32" },
                    { "5", "Norbert", "Norbertson", "28" },
                    { "6", "Penny", "Pennyson", "38" },
                    { "7", "Yak", "Yakson", "88" },
            };


    @Override
    protected void doCleanup() throws Exception
    {
        try { deleteProject(PROJECT_NAME) ; } catch (Throwable t) { }
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        createProject(PROJECT_NAME);
        createList();

        log("** Show only LastName and Age");
        setColumns("LastName", "Age");
        assertTextPresent("Norbertson");
        assertTextNotPresent("First Name");

        log("** Add filter: FirstName starts with 'J'");
        addFilter("LastName", "Starts With", "J");
        assertTextNotPresent("Norbertson");
        assertTextPresent("Janeson");
        assertTextPresent("Johnson");

        log("** Add another filter: FirstName != 'Johnson'");
        addFilter("LastName", "Does Not Equal", "Johnson");
        assertTextPresent("Janeson");
        assertTextNotPresent("Johnson");

        log("** Remove filter");
        removeFilter("LastName");
        assertTextPresent("Johnson");
        assertTextPresent("Norbertson");

        log("** Add sort by Age");
        assertTextBefore("Billson", "Johnson");
        addSort("Age", "Ascending");
        assertTextBefore("Johnson", "Billson");

        log("** Remove sort");
        removeSort("Age");
        assertTextBefore("Billson", "Johnson");

        // TODO: pin, unpin, setting column title, move columns/filters/sort, remove single filter clause, save named view, revert, click "Revert|Edit|Save" links, 
    }

    private void createList()
    {
        ListHelper.createList(this, PROJECT_NAME, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, LIST_COLUMNS);

        StringBuilder data = new StringBuilder();
        data.append(LIST_KEY_NAME).append("\t");
        for (int i = 0; i < LIST_COLUMNS.length; i++)
        {
            data.append(LIST_COLUMNS[i].getName());
            data.append(i < LIST_COLUMNS.length - 1 ? "\t" : "\n");
        }
        for (String[] rowData : TEST_DATA)
        {
            for (int col = 0; col < rowData.length; col++)
            {
                data.append(rowData[col]);
                data.append(col < rowData.length - 1 ? "\t" : "\n");
            }
        }

        ListHelper.clickImportData(this);
        setFormElement("ff_data", data.toString());
        submit();
        for (String[] rowData : TEST_DATA)
        {
            // check that all the data is in the grid (skipping the key column at index 0)
            for (int col = 1; col < rowData.length; col++)
            {
                assertTextPresent(rowData[col]);
            }
        }
    }

    void setColumns(String... fieldKeys)
    {
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.clearCustomizeViewColumns(this);
        for (String fieldKey : fieldKeys)
            CustomizeViewsHelper.addCustomizeViewColumn(this, fieldKey);
        clickNavButton("Apply", longWaitForPage);
    }

    void addFilter(String fieldKey, String op, String value)
    {
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewFilter(this, fieldKey, op, value);
        clickNavButton("Apply", longWaitForPage);
    }

    void addSort(String fieldKey, String order)
    {
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewSort(this, fieldKey, order);
        clickNavButton("Apply", longWaitForPage);
    }

    void removeFilter(String fieldKey)
    {
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.removeCustomizeViewFilter(this, fieldKey);
        clickNavButton("Apply", longWaitForPage);
    }

    void removeSort(String fieldKey)
    {
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.removeCustomizeViewSort(this, fieldKey);
        clickNavButton("Apply", longWaitForPage);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
