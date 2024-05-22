package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout (minutes = 2)
public class FiltersOnMultipleGridsTest extends BaseWebDriverTest
{
    private final String LIST_NAME = "Test list for " + getProjectName();
    private final String LIST_WEBPART_TITLE = "Single list " + LIST_NAME;
    private final String QUERY_WEBPART_TITLE = "Query 1";


    @BeforeClass
    public static void setupProject()
    {
        FiltersOnMultipleGridsTest init = (FiltersOnMultipleGridsTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName());
        log("Creating test list");
        _listHelper.createList(getProjectName(), LIST_NAME, "RowId",
                new FieldDefinition("FirstName", ColumnType.String),
                new FieldDefinition("LastName", ColumnType.String),
                new FieldDefinition("Age", ColumnType.Integer));

        log("Adding Single list webpart");
        goToProjectHome();
        PortalHelper _portalHelper = new PortalHelper(getDriver());
        _portalHelper.addBodyWebPart("List - Single");
        setFormElement(Locator.name("title"), LIST_WEBPART_TITLE);
        _ext4Helper.selectComboBoxItem("List:", LIST_NAME);
        clickButton("Submit");

        log("Adding Query webpart");
        _portalHelper.addBodyWebPart("Query");
        setFormElement(Locator.name("title"), QUERY_WEBPART_TITLE);
        _ext4Helper.selectComboBoxItem("Schema:", "core");
        Ext4Helper.Locators.radiobutton(this, "Show the contents of a specific query and view.").findElement(getDriver()).click();
        _ext4Helper.selectComboBoxItem("Query:", "Groups");
        clickButton("Submit");
    }


    @Test
    public void testFiltersOnMultipleGrid()
    {
        goToProjectHome();
        log("Inserting list data and setting filters");
        DataRegionTable listTable = DataRegionTable.findDataRegionWithinWebpart(this, LIST_WEBPART_TITLE);
        insertListData("Joe", "Biden", "70");
        insertListData("Donald", "Trump", "65");
        listTable.setFilter("FirstName", "Equals", "Joe");
        listTable.setFilter("Age", "Is Greater Than", "60");

        log("Custom view for query");
        DataRegionTable queryTable = DataRegionTable.findDataRegionWithinWebpart(this, QUERY_WEBPART_TITLE);
        CustomizeView customizeView = queryTable.openCustomizeGrid();
        customizeView.addColumn("UserId");
        customizeView.clickViewGrid();

        queryTable = DataRegionTable.findDataRegionWithinWebpart(this, QUERY_WEBPART_TITLE);
        checker().verifyEquals("Incorrect columns on query webparts", Arrays.asList("Name", "Type", "Container", "UserId"),
                queryTable.getColumnNames());
        checker().verifyEquals("Incorrect filter on list webparts", Arrays.asList("First Name = Joe", "Age > 60"),
                getTexts(DataRegionTable.Locators.filterContextAction().findElements(getDriver())));
    }

    private void insertListData(String firstName, String lastName, String age)
    {
        DataRegionTable listTable = DataRegionTable.findDataRegionWithinWebpart(this, LIST_WEBPART_TITLE);
        listTable.clickInsertNewRow()
                .setField("FirstName", firstName)
                .setField("LastName", lastName)
                .setField("Age", age)
                .submit();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
