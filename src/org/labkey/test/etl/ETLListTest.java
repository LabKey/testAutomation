package org.labkey.test.etl;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({InDevelopment.class})
public class ETLListTest extends BaseWebDriverTest
{
    ETLHelper _etlHelper = new ETLHelper(this, getProjectName());
    private static final String ETL_LIST_MERGE = "{ETLtest}/ListAToListB";
    private static final File ETL_LIST_ARCHIVE = TestFileUtils.getSampleData("lists/ETL_ListAListB.lists.zip");
    private static final String SRC_LIST = "ListA";
    private static final String DEST_LIST = "ListB";


    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        ETLListTest init = (ETLListTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModules(Arrays.asList("DataIntegration", "ETLtest"));
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
        _listHelper.importListArchive(ETL_LIST_ARCHIVE);
    }

    @Test
    public void testMergeEtl()
    {
        List<String> expectedKeys = new ArrayList<>(Arrays.asList("K1", "K3"));
        _etlHelper.runETL(ETL_LIST_MERGE);

        clickAndWait(Locator.linkWithText(DEST_LIST));
        DataRegionTable dest = new DataRegionTable("query", this);
        List<String> actualKeys = dest.getColumnDataAsText("Key");
        assertEquals("Initial list copy failed", expectedKeys, actualKeys);

        goBack();
        clickAndWait(Locator.linkWithText(SRC_LIST));
        _listHelper.insertNewRow(Maps.of("Key", "K4", "Field1", "new"));
        _etlHelper.runETL(ETL_LIST_MERGE);
        expectedKeys.add("K4");

        clickAndWait(Locator.linkWithText(DEST_LIST));
        dest = new DataRegionTable("query", this);
        actualKeys = dest.getColumnDataAsText("Key");
        assertEquals("Initial list copy failed", expectedKeys, actualKeys);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "ETLListTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("DataIntegration");
    }
}