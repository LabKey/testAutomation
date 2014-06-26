package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.labkey.test.categories.DailyA;

import java.io.File;

@Category({DailyA.class})
public class QueryExportTest extends AbstractExportTest
{
    private static final File LIST_ARCHIVE = new File(getSampledataPath(), "lists/ListDemo.lists.zip");
    private static final String LIST_NAME = "NIMHDemographics";
    private static final String QUERY_NAME = "NIMHQuery";

    @Nullable
    @Override
    protected String getProjectName()
    {
        return "Query Export Test";
    }

    @Override
    protected boolean hasSelectors()
    {
        return false;
    }

    @Override
    protected String getTestColumnTitle()
    {
        return "Name";
    }

    @Override
    protected int getTestColumnIndex()
    {
        return 1;
    }

    @Override
    protected String getExportedTsvTestColumnHeader()
    {
        return "name";
    }

    @Override
    protected String getExportedFilePrefixRegex()
    {
        return QUERY_NAME;
    }

    @Override
    protected String getDataRegionId()
    {
        return "query";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        QueryExportTest initTest = new QueryExportTest();
        initTest.doCleanup(false);

        initTest._containerHelper.createProject(initTest.getProjectName(), null);
        initTest._listHelper.importListArchive(initTest.getProjectName(), LIST_ARCHIVE);

        String sql = "SELECT SubjectId, Name FROM " + LIST_NAME;
        initTest.createQuery(initTest.getProjectName(), QUERY_NAME, "lists", sql, null, false);

        currentTest = initTest;
    }

    @Override
    public void goToDataRegionPage()
    {
        clickProject(getProjectName());
        navigateToQuery("lists", QUERY_NAME);
    }

}
