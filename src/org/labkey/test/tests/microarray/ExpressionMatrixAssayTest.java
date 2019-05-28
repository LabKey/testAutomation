/*
 * Copyright (c) 2014-2018 LabKey Corporation
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
package org.labkey.test.tests.microarray;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PipelineAnalysisHelper;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class, Assays.class})
public class ExpressionMatrixAssayTest extends BaseExpressionMatrixTest
{
    private final PipelineAnalysisHelper pipelineAnalysis = new PipelineAnalysisHelper(this);
    private static int expectedPipelineJobCount;

    @BeforeClass
    public static void initJobCounter()
    {
        expectedPipelineJobCount = 0;
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testExpressionMatrixAssay()
    {
        final String importAction = "Use R to generate a dummy matrix tsv output file with two samples and two features.";
        final String protocolName = "CreateMatrix";
        final String description = "Execute create-matrix R pipeline, import values. Also protocolDescription comment field.";
        final String[] targetFiles = {CEL_FILE1.getName(), CEL_FILE2.getName()};
        final String runName = "myrun";
        final String parameterXml = getParameterXml(runName, protocolName, null, String.valueOf(getFeatureSetId()), true, null);
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName,
                "protocolDescription", description,
                "xmlParameters", parameterXml,
                "saveProtocol", "false");
        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);

        final String pipelineName = PIPELINE_NAME;
        final File fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
        final Map<String, Set<String>> outputFiles = Maps.of(
                pipelineName + ".xml", Collections.emptySet(),
                protocolName + "-taskInfo.tsv", Collections.emptySet(),
                protocolName + ".log", Collections.emptySet(),
                protocolName + ".tsv", Collections.emptySet());
        PipelineAnalysisHelper.setExpectedJobCount(++expectedPipelineJobCount);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, runName, description, fileRoot, outputFiles);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        assertElementPresent(Locator.linkWithText(getFeatureSetName()));
        clickAndWait(Locator.linkWithText(runName));

        DataRegionTable resultTable = new DataRegionTable("Data", this);
        resultTable.setSort("Value", SortDirection.ASC);
        assertEquals(Arrays.asList("10.000000", "20.000000", "30.000000", "40.000000"),
                resultTable.getColumnDataAsText("Value"));
        assertEquals(Arrays.asList("78495_at", "78383_at", "78495_at", "78383_at"),
                resultTable.getColumnDataAsText("Probe Id"));
        assertEquals(Arrays.asList("SampleA", "SampleA", "SampleB", "SampleB"),
                resultTable.getColumnDataAsText("Sample Id"));
        assertEquals(Arrays.asList(runName, runName, runName, runName),
                resultTable.getColumnDataAsText("Run"));

        goToSchemaBrowser();
        selectQuery("assay.ExpressionMatrix." + ASSAY_NAME, "FeatureDataBySample");
    }

    @Test
    public void testExpressionMatrixAssayNoValues()
    {
        final String importAction = "Use R to generate a dummy matrix tsv output file with two samples and two features.";
        final String protocolName = "CreateMatrixNoValues";
        final String description = "Execute create-matrix R pipeline, import values. Also 'assay comment' xml property.";
        final String runName = "awesome";
        final String[] targetFiles = {CEL_FILE1.getName(), CEL_FILE2.getName()};
        final String parameterXml = getParameterXml(runName, protocolName, description, String.valueOf(getFeatureSetId()), false, null);
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName,
                "xmlParameters", parameterXml,
                "saveProtocol", "false");
        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);

        final String pipelineName = PIPELINE_NAME;
        final File fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
        final Map<String, Set<String>> outputFiles = Maps.of(
                pipelineName + ".xml", Collections.emptySet(),
                protocolName + "-taskInfo.tsv", Collections.emptySet(),
                protocolName + ".log", Collections.emptySet(),
                protocolName + ".tsv", Collections.emptySet());
        PipelineAnalysisHelper.setExpectedJobCount(++expectedPipelineJobCount);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, runName, description, fileRoot, outputFiles);

        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        assertElementPresent(Locator.linkWithText(getFeatureSetName()));
        clickAndWait(Locator.linkWithText(runName));

        DataRegionTable resultTable = new DataRegionTable("Data", this);
        assertEquals(0, resultTable.getDataRowCount());

        goToSchemaBrowser();
        selectQuery("assay.ExpressionMatrix." + ASSAY_NAME, "FeatureDataBySample");
    }

    @Test
    public void testReferenceFeatureAnnotationSetByName()
    {
        final String importAction = "Use R to generate a dummy matrix tsv output file with two samples and two features.";
        final String protocolName = "ReferenceFeatureAnnotationSetByName";
        final String description = "Reference desired feature annotation set by name";
        final String runName = "wakka-wakka";
        final String[] targetFiles = {CEL_FILE1.getName(), CEL_FILE2.getName()};
        final String parameterXml = getParameterXml(runName, protocolName, description, getFeatureSetName(), false, null);
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName,
                "xmlParameters", parameterXml,
                "saveProtocol", "false");
        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);

        final String pipelineName = PIPELINE_NAME;
        final File fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
        final Map<String, Set<String>> outputFiles = Maps.of(
                pipelineName + ".xml", Collections.emptySet(),
                protocolName + "-taskInfo.tsv", Collections.emptySet(),
                protocolName + ".log", Collections.emptySet(),
                protocolName + ".tsv", Collections.emptySet());
        PipelineAnalysisHelper.setExpectedJobCount(++expectedPipelineJobCount);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, runName, description, fileRoot, outputFiles);
    }

    @Test
    public void testFeatureAnnotationSetOutputProperty()
    {
        final String importAction = "Use R to generate a dummy matrix tsv output file with two samples and two features.";
        final String protocolName = "FeatureAnnotationSetOutputProperty";
        final String description = "Use a task output property to reference desired feature annotation set by name";
        final String runName = "everything is awesome";
        final String[] targetFiles = {CEL_FILE1.getName(), CEL_FILE2.getName()};
        final String parameterXml = getParameterXml(runName, protocolName, null, null, false, Collections.singletonMap("myFeatureSet", getFeatureSetName()));
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName,
                "protocolDescription", description,
                "xmlParameters", parameterXml,
                "saveProtocol", "false");
        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);

        final String pipelineName = PIPELINE_NAME;
        final File fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
        final Map<String, Set<String>> outputFiles = Maps.of(
                pipelineName + ".xml", Collections.emptySet(),
                protocolName + "-taskInfo.tsv", Collections.emptySet(),
                protocolName + ".log", Collections.emptySet(),
                protocolName + ".tsv", Collections.emptySet());
        PipelineAnalysisHelper.setExpectedJobCount(++expectedPipelineJobCount);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, runName, description, fileRoot, outputFiles);
    }

    @Test
    public void testImportNewFeatureAnnotationSet()
    {
        // Upload new feature annotation set to the pipeline
        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(FEATURE_SET_DATA);

        final String importAction = "Use R to generate a dummy matrix tsv output file with two samples and two features.";
        final String protocolName = "ImportNewFeatureAnnotationSet";
        final String description = "Use a task output property to import new feature annotation set";
        final String runName = "now with calcium!";
        final String[] targetFiles = {CEL_FILE1.getName(), CEL_FILE2.getName()};
        final String parameterXml = getParameterXml(runName, protocolName, null, null, false, Collections.singletonMap("myFeatureSet", FEATURE_SET_DATA.getName()));
        final Map<String, String> protocolProperties = Maps.of(
                "protocolName", protocolName,
                "protocolDescription", description,
                "xmlParameters", parameterXml,
                "saveProtocol", "false");
        pipelineAnalysis.runPipelineAnalysis(importAction, targetFiles, protocolProperties);

        final String pipelineName = PIPELINE_NAME;
        final File fileRoot = TestFileUtils.getDefaultFileRoot(getProjectName());
        final Map<String, Set<String>> outputFiles = Maps.of(
                pipelineName + ".xml", Collections.emptySet(),
                protocolName + "-taskInfo.tsv", Collections.emptySet(),
                protocolName + ".log", Collections.emptySet(),
                protocolName + ".tsv", Collections.emptySet());
        PipelineAnalysisHelper.setExpectedJobCount(++expectedPipelineJobCount);
        pipelineAnalysis.verifyPipelineAnalysis(pipelineName, protocolName, runName, description, fileRoot, outputFiles);

        goToManageAssays();
        click(Locator.linkContainingText(ASSAY_NAME));
        DataRegionTable table = new DataRegionTable("Runs", this);
        table.setFilter("Name", "Equals", runName);
        Assert.assertEquals("sample-feature-set", table.getDataAsText(0, "Feature Annotation Set"));
    }
}
