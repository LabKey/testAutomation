package org.labkey.test.tests;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.BaseWebDriverTest;

/**
 * User: jeckels
 * Date: 10/17/12
 */
public class SchemaTest extends BaseWebDriverTest
{
    @Override
    protected String getProjectName()
    {
        return "TestDontDelete";
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        beginAt("/query/Assay/begin.view");
        selectSchema("assay.General.AssayId");
    }

    @Override
    protected void doCleanup() throws Exception
    {
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "luminex";
    }
}
