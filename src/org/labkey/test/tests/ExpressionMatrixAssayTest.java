/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PipelineAnalysisHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.RReportHelper;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class, Assays.class})
public class ExpressionMatrixAssayTest extends BaseWebDriverTest
{
    private static final String PIPELINE_NAME = "create-matrix";
    private static final String SAMPLE_SET = "ExpressionMatrix SampleSet";
    private static final String ASSAY_NAME = "Test Expression Matrix";
    private static final String FEATURE_SET_NAME = "Expression Matrix Test Feature Set";
    private static final String FEATURE_SET_VENDOR = "Vendor";
    private static final String FEATURE_SET_FILENAME = "sample-feature-set.txt";
    private static final File FEATURE_SET_DATA = TestFileUtils.getSampleData("Microarray/expressionMatrix/" + FEATURE_SET_FILENAME);
    private static final File CEL_FILE1 = TestFileUtils.getSampleData("Affymetrix/CEL_files/sample_file_1.CEL");
    private static final File CEL_FILE2 = TestFileUtils.getSampleData("Affymetrix/CEL_files/sample_file_2.CEL");
    private static final String PROTOCOL_NAME = "Expression Matrix Protocol";
    private static int featureSetId;
    private static int expectedPipelineJobCount;

    private final PipelineAnalysisHelper pipelineAnalysis = new PipelineAnalysisHelper(this);

    @BeforeClass
    public static void doSetup() throws Exception
    {
        ExpressionMatrixAssayTest initTest = (ExpressionMatrixAssayTest)getCurrentTest();

        initTest.doSetupSteps();
        expectedPipelineJobCount = 0;
    }

    private void doSetupSteps()
    {
        RReportHelper rReportHelper = new RReportHelper(this);
        rReportHelper.ensureRConfig();

        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModules(Arrays.asList("pipelinetest", "Microarray"));

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Sample Sets");
        clickButton("Import Sample Set");
        setFormElement(Locator.id("name"), SAMPLE_SET);
        setFormElement(Locator.name("data"), "KeyCol\n1");
        clickButton("Submit");

        clickProject(getProjectName());
        portalHelper.addWebPart("Feature Annotation Sets");
        featureSetId = createFeatureSet(FEATURE_SET_NAME, FEATURE_SET_VENDOR, FEATURE_SET_DATA);

        clickProject(getProjectName());
        portalHelper.addWebPart("Assay List");
        _assayHelper.createAssayWithDefaults("Expression Matrix", ASSAY_NAME);

        goToModule("FileContent");
        _fileBrowserHelper.uploadFile(CEL_FILE1);
        _fileBrowserHelper.uploadFile(CEL_FILE2);
    }

    @LogMethod
    private int createFeatureSet(@LoggedParam String name, String vendor, File featureSetData)
    {
        clickButton("Import Feature Annotation Set");
        waitForElement(Locator.name("name"));
        setFormElement(Locator.name("name"), name);
        setFormElement(Locator.name("vendor"), vendor);
        setFormElement(Locator.name("annotationFile"), featureSetData);
        clickButton("upload");
        clickAndWait(Locator.linkWithText(name));
        return Integer.parseInt(getUrlParam("rowId"));
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
        final String parameterXml = getParameterXml(runName, protocolName, null, String.valueOf(featureSetId), true, null);
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
        assertElementPresent(Locator.linkWithText(FEATURE_SET_NAME));
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
        final String parameterXml = getParameterXml(runName, protocolName, description, String.valueOf(featureSetId), false, null);
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
        assertElementPresent(Locator.linkWithText(FEATURE_SET_NAME));
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
        final String parameterXml = getParameterXml(runName, protocolName, description, FEATURE_SET_NAME, false, null);
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
        final String parameterXml = getParameterXml(runName, protocolName, null, null, false, Collections.singletonMap("myFeatureSet", FEATURE_SET_NAME));
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
        final String parameterXml = getParameterXml(runName, protocolName, null, null, false, Collections.singletonMap("myFeatureSet", FEATURE_SET_FILENAME));
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


    private String getParameterXml(String assayRunName, String protocolName, String description, String featureSet, boolean importValues, Map<String, String> otherParameters)
    {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("protocolName", protocolName);
        parameters.put("assay name", assayRunName);
        if (description != null)
            parameters.put("assay comments", description);
        if (featureSet != null)
            parameters.put("assay run property, featureSet", featureSet);
        parameters.put("assay run property, importValues", String.valueOf(importValues));

        if (otherParameters != null)
            parameters.putAll(otherParameters);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        sb.append("<bioml>\n");
        for (Map.Entry<String, String> entry : parameters.entrySet())
            sb.append(String.format("  <note label='%s' type='input'>%s</note>\n", entry.getKey(), entry.getValue()));
        sb.append("</bioml>\n");

        return sb.toString();
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + "Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("microarray");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
