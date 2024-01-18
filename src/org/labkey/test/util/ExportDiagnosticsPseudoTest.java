package org.labkey.test.util;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({})
@Order(Double.MAX_VALUE) // Export diagnostics last
public class ExportDiagnosticsPseudoTest extends BaseWebDriverTest
{
    @Test
    public void exportDiagnostics() throws Exception
    {
        File diagnosticsDir = new File(TestFileUtils.getTestTempDir(), "diagnostics");
        if (diagnosticsDir.exists())
        {
            FileUtils.forceDelete(diagnosticsDir);
        }
        FileUtils.forceMkdir(diagnosticsDir);

        if (_containerHelper.getAllModules().contains("CloudServices"))
        {
            SimpleHttpRequest req = new SimpleHttpRequest(WebTestHelper.buildURL("diagnostics", "exportDiagnostics"), "POST");
            req.copySession(getDriver());
            File diagnosticsFile = req.getResponseAsFile(diagnosticsDir);
            getArtifactCollector().publishArtifact(diagnosticsFile);
        }
        else
        {

            TestLogger.info("CloudServices module not available, dumping logs manually");
            SimpleHttpRequest req = new SimpleHttpRequest(WebTestHelper.buildURL("admin", "showPrimaryLog"));
            req.copySession(getDriver());
            req.getResponseAsFile(new File(diagnosticsDir, "labkey.log"));
            req = new SimpleHttpRequest(WebTestHelper.buildURL("admin", "showAllErrors"));
            req.copySession(getDriver());
            req.getResponseAsFile(new File(diagnosticsDir, "labkey-errors.log"));
            req = new SimpleHttpRequest(WebTestHelper.buildURL("admin", "exportQueries"), "POST");
            req.copySession(getDriver());
            req.getResponseAsFile(diagnosticsDir);
            getArtifactCollector().publishArtifact(diagnosticsDir);
        }
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
