package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SqlserverOnlyTest;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * User: tgaluhn
 * Date: 12/22/2014
 *
 * Test for core portion of nlp engine integration. Exercises standalone pipeline functionality only.
 * NOTE: The Linux TeamCity agents don't yet have the necessary python support for the nlp engine, so
 * for now this test is SqlserverOnly
 */
@Category({InDevelopment.class})
public class NLPCoreTest extends BaseWebDriverTest implements SqlserverOnlyTest
{
    private static final String SAMPLE_INPUT_FILE_NAME = "sample1.nlp.tsv";
    private static final File SAMPLE_INPUT_FILE = new File(TestFileUtils.getSampleData("nlp"), SAMPLE_INPUT_FILE_NAME);
    protected PortalHelper _portalHelper = new PortalHelper(this);

    @Nullable
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("nlp");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Test
    public void testSteps() throws Exception
    {
        doSetup();

        clickButton("Process and Import Data");
        _fileBrowserHelper.uploadFile(SAMPLE_INPUT_FILE);
        _fileBrowserHelper.importFile(SAMPLE_INPUT_FILE_NAME, "NLP engine invocation and results");
        setFormElement(Locator.id("protocolNameInput"), "dummy");
        clickButton("Analyze");
        int seconds = 0;
        while (isElementPresent(Locator.linkWithText("IMPORT RESULTS RUNNING")) && seconds++ < MAX_WAIT_SECONDS)
        {
            sleep(1000);
        }
        clickAndWait(Locator.linkWithText("All"));
        if (!isElementPresent(Locator.linkWithText("COMPLETE")))
            fail("Import did not complete.");

        goToProjectHome();
        // ensure something of the json result imported, as well as the report txt file.
        assertTextPresentCaseInsensitive("controlInfo", "BOUVIER, SELMA J");
    }

    protected void doSetup()
    {
        doCleanup(false);
        log("Create Project");
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule("NLP");
        _portalHelper.addWebPart("Data Pipeline");
        _portalHelper.addQueryWebPart("NLP Job Runs", "nlp", "JobRun", null);
        _portalHelper.addQueryWebPart("NLP Reports", "nlp", "Report", null);
    }

}
