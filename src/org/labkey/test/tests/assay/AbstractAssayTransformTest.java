package org.labkey.test.tests.assay;

import org.junit.BeforeClass;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.util.RReportHelper;

import java.util.Collections;
import java.util.List;

/**
 * Shared setup/cleanup helpers for assay transform-related WebDriver tests.
 * Consolidates common project creation, R configuration, and project cleanup.
 */
public abstract class AbstractAssayTransformTest extends BaseWebDriverTest
{
    @BeforeClass
    public static void setupProject()
    {
        AbstractAssayTransformTest init = getCurrentTest();
        init.doSetup();
    }

    protected void doSetup()
    {
        new RReportHelper(this).ensureRConfig();
        _containerHelper.createProject(getProjectName(), "Assay");
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.emptyList();
    }
}
