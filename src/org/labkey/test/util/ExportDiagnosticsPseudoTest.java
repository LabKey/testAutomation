/*
 * Copyright (c) 2023-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
