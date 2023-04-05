package org.labkey.test.util;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({})
@Prioritized(priority = -100) // Export diagnostics last
public class ExportDiagnosticsPseudoTest extends BaseWebDriverTest
{
    @Test
    public void exportDiagnostics() throws Exception
    {
        SimpleHttpRequest req = new SimpleHttpRequest(WebTestHelper.buildURL("diagnostics", "exportDiagnostics"), "POST");
        req.copySession(getDriver());
        File diagnosticsFile = req.getResponseAsFile();
        getArtifactCollector().publishArtifact(diagnosticsFile);
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
