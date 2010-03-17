package org.labkey.test.bvt;

import org.labkey.test.WebTestHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Mar 9, 2010
 * Time: 3:44:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class RlabkeyTest extends SimpleApiTest
{
    private static final String PROJECT_NAME = "RlabkeyVerifyProject";
    private static final String LIST_NAME = "AllTypes";
    private static final String CREATE_R_MENU = "Views:Create:R View";
    private static final String LIBPATH_OVERRIDE = ".libPaths(\"%s\")\n";

    @Override
    public void runUITests() throws Exception
    {
        log("Create Project");
        createProject(PROJECT_NAME);
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Lists");

        log("Import Lists");
        File listArchive = new File(WebTestHelper.getLabKeyRoot(), "/sampledata/rlabkey/listArchive.zip");

        if (!listArchive.exists())
            fail("Unable to locate the list archive: " + listArchive.getName());

        ListHelper.importListArchive(this, PROJECT_NAME, listArchive);
        createSubfolder(PROJECT_NAME, "test", new String[0]);

        RReportHelper.ensureRConfig(this);

/*
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(LIST_NAME);
        clickMenuButton("Views", CREATE_R_MENU, "Views:Create");

        RReportHelper.installRlabkey(this, true);
        RReportHelper.saveReport(this, "dummy");
*/
    }

    @Override
    public void runApiTests() throws Exception
    {
        File testData = new File(getLabKeyRoot() + "/server/test/data/api/rlabkey-api.xml");
        if (testData.exists())
        {
            // cheating here, to use the api test framework to store rlabkey tests
            List<ApiTestCase> tests = parseTests(testData);

            if (!tests.isEmpty())
            {
                clickLinkWithText(PROJECT_NAME);
                clickLinkWithText(LIST_NAME);
                clickMenuButton("Views", CREATE_R_MENU, "Views:Create");

                // we want to load the Rlabkey package from the override location
                File libPath = new File(getLabKeyRoot() + "/sampledata/rlabkey");
                String pathCmd = String.format(LIBPATH_OVERRIDE, libPath.getAbsolutePath().replaceAll("\\\\", "/"));

                try {
                    for (ApiTestCase test : tests)
                    {
                        String cmd = pathCmd + test.getUrl().trim();
                        String verify = test.getReponse().trim();

                        if (!RReportHelper.executeScript(this, cmd, verify))
                            fail("Failed executing R script for test case: " + test.getName());
                    }
                }
                finally
                {
                    // restore latest version of R from web
//                    RReportHelper.installRlabkey(this, false);
                    RReportHelper.saveReport(this, "dummy2");
                }
            }
        }
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[]{new File(getLabKeyRoot() + "/server/test/data/api/rlabkey-api.xml")};
    }

    @Override
    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }
}
