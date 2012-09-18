package org.labkey.test;

import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.util.APIAssayHelper;
import org.labkey.test.util.AbstractAssayHelper;
import org.labkey.test.util.UIAssayHelper;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 9/14/12
 * Time: 2:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class AssayAPITest extends BaseSeleniumWebTest
{
    @Override
    protected String getProjectName()
    {
        return "Assay API TEST";
    }

    protected boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Assay");
        goToProjectHome();
        int pipelineCount = 0;
        String runName = "trial01.xls";
        importAssayAndRun(getSampledataPath() + "\\AssayAPI\\XLS Assay.xar.xml", ++pipelineCount, "XLS Assay",
                getSampledataPath() + "\\GPAT\\" + runName,  runName, new String[] {"1 - 100 of 201", "K770K3VY-19"});
//
//        goToProjectHome();

        //Issue 16073
//        importAssayAndRun(getSampledataPath() + "\\AssayAPI\\BatchPropRequired.xar", ++pipelineCount, "BatchPropRequired",
//                getSampledataPath() + "\\GPAT\\" + runName,  runName, new String[] {"1 - 100 of 201", "K770K3VY-19"});
//        assayHelper.getCurrentAssayNumber();
    }

    protected void  importAssayAndRun(String assayPath, int pipelineCount, String assayName, String runPath,
                                      String runName, String[] textToCheck)
    {
        AbstractAssayHelper assayHelper = new APIAssayHelper(this);
        assayHelper.uploadXarFileAsAssayDesign(assayPath, pipelineCount, assayName);
        try
        {
            assayHelper.importAssay(assayName, runPath, getProjectName());
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }

        log("verify import worked");
        goToProjectHome();
        clickLinkContainingText(assayName);
        clickLinkContainingText(runName);
        assertTextPresent(textToCheck);
    }

    @Override
    protected void doCleanup() throws Exception
    {
        _containerHelper.deleteProject(getProjectName());
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
