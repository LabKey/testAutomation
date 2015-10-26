package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.RReportHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({InDevelopment.class})
public class SharedReportTest extends BaseWebDriverTest
{
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        SharedReportTest init = (SharedReportTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        RReportHelper rHelper = new RReportHelper(this);
        rHelper.ensureRConfig();
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule("simpletest");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testSharedReport()
    {

    }

    @Test
    public void testSharedModuleReport()
    {

    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "SharedReportTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}