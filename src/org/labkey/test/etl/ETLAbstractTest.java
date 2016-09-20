/*
 * Copyright (c) 2013-2015 LabKey Corporation
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
package org.labkey.test.etl;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.labkey.api.util.Pair;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.tests.SimpleModuleTest;
import org.labkey.test.util.DataIntegrationHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public abstract class ETLAbstractTest extends BaseWebDriverTest
{
    public static final String ETL_OUT = "etlOut";
    protected static final String DATA_INTEGRATION_TAB = "DataIntegration";
    static final String TRANSFORM_APPEND_DESC = "Append Test";
    static final String TRANSFORM_TRUNCATE_DESC = "Truncate Test";
    static final String TRANSFORM_BYRUNID = "{simpletest}/appendIdByRun";
    static final String APPEND_WITH_ROWVERSION = "{simpletest}/appendWithRowversion";
    static final String APPEND = "{simpletest}/append";
    static final String APPEND_SELECT_ALL = "{simpletest}/appendSelectAll";
    static final String TRANSFORM_BAD_THROW_ERROR_SP = "{simpletest}/SProcBadThrowError";
    protected static final String TRANSFORM_APPEND = "{simpletest}/append";
    protected static final String TRANSFORM_TRUNCATE = "{simpletest}/truncate";
    public ETLHelper _etlHelper = new ETLHelper(this, getProjectName());
    protected DataIntegrationHelper _diHelper = _etlHelper.getDiHelper();

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("dataintegration");
    }

    @Override
    public void checkQueries()
    {
        log("Skipping query check. Some tables used by queries in simpletest module are not created in this test");
        log("Query check from " + SimpleModuleTest.class.getSimpleName() + " should cover anything this would check");
    }

    protected void doSetup()
    {
        _etlHelper.doSetup();
    }

    protected boolean isResetInPreTest()
    {
        return false;
    }

    @Before
    public void preTest() throws Exception
    {
        if (isResetInPreTest())
        {
            _etlHelper.resetCounts();
            resetErrors();
            _etlHelper.cleanupTestTables();
            goToProjectHome();
        }
    }

    protected boolean isCheckErrorsInPostTest()
    {
        return false;
    }

    @After
    public void postTest()
    {
        if (isCheckErrorsInPostTest() && !_testFailed)
            checkExpectedErrors(_etlHelper.getExpectedErrorCount());
    }

    @NotNull
    protected File setupPipelineFileAnalysis(String outputSubDir) throws IOException
    {
        //file ETL output and external pipeline test
        File dir = TestFileUtils.getTestTempDir();
        FileUtils.deleteDirectory(dir);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        setPipelineRoot(dir.getAbsolutePath());
        if (null != outputSubDir)
        {
            dir = new File(dir, outputSubDir);
        }
        return dir;
    }

    protected void validateFileRow(List<String[]> rows, int index, String name)
    {
        // The 6th field in the tsv is the name column; verify it's what we expect
        assertEquals("Row " + index + " was not for name '" + name +"'", name, rows.get(index)[5]);
    }

    protected void validatePipelineFileAnalysis(File dir, String jobId, int expectedOutputRows) throws IOException, CommandException
    {
        Pair<List<String[]>, List<String[]>> fileRows = readFile(dir, jobId, null, true);
        String[] expected = WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL ?
                "rowid,container,created,modified,id,name,transformrun,rowversion,diTransformRunId,diModified".split(",")
                : "rowid,container,created,modified,id,name,transformrun,diTransformRunId,diModified".split(",");
        // Validate the initially written tsv file
        assertArrayEquals("ETL query output file did not contain header", expected, fileRows.first.get(0));
        validateFileRow(fileRows.first, 1, "Subject 2");
        validateFileRow(fileRows.first, 2, "Subject 3");

        // Validate the file output from the pipeline job
        assertEquals("ETL pipeline output file did not have expected number of rows.", expectedOutputRows, fileRows.second.size());
        validateFileRow(fileRows.second, expectedOutputRows - 2, "Subject 2");
        validateFileRow(fileRows.second, expectedOutputRows - 1, "Subject 3");
    }

    protected Pair<List<String[]>, List<String[]>> readFile(File dir, String jobId, @Nullable Integer batchNum, boolean expectOutFile) throws IOException, CommandException
    {
        Pair<List<String[]>, List<String[]>> results = new Pair<>(new ArrayList<>(), new ArrayList<>());

        String baseName = "report-" + _diHelper.getTransformRunFieldByJobId(jobId, "transformRunId");
        if (null != batchNum && batchNum > 0)
            baseName = baseName + "-" + batchNum;
        File etlFile = new File(dir, baseName + ".testIn.tsv");
        String fileContents = TestFileUtils.getFileContents(etlFile);

        List<String> rows = Arrays.asList(fileContents.split("[\\n\\r]+"));
        results.first.addAll(rows.stream().map(row -> row.split(",")).collect(Collectors.toList()));

        if (expectOutFile)
        {
            //file created by external pipeline or a destination with targetType="file"
            File etlFile2 = new File(dir, baseName + ".testOut.tsv");
            waitFor(etlFile2::exists, "Output file wasn't written", 5000);
            fileContents = TestFileUtils.getFileContents(etlFile2);
            rows = Arrays.asList(fileContents.split("[\\n\\r]+"));
            results.second.addAll(rows.stream().map(row -> row.split(",")).collect(Collectors.toList()));
        }
        return results;
    }
}
