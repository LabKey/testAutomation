/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.tests.assay;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.assay.AssayConstants;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.labkey.test.pages.admin.UsageStatisticsPage;
import org.labkey.test.pages.assay.AssayImportPage;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.assay.GeneralAssayDesign;
import org.labkey.test.util.core.admin.ServerUsageUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static org.labkey.test.pages.ReactAssayDesignerPage.ScriptFileEvent.Edit;
import static org.labkey.test.pages.ReactAssayDesignerPage.ScriptFileEvent.Import;


@Category({Assays.class, Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class AssayTransformImportUpdateTest extends AbstractAssayTransformTest
{
    // GH Issue 1130: feature areas that ScriptPackageUsageTracker records package usage under, one per language
    private static final String R_PACKAGE_USAGE_AREA = "rPackageUsage";
    private static final String PYTHON_PACKAGE_USAGE_AREA = "pythonPackageUsage";

    @Test
    public void testEnableTransformForUpdate() throws Exception
    {
        String insertOrUpdateTransform = "insertOrUpdateTransform.R";
        String insertOrUpdateTransformAssay = "insertOrUpdateTransformAssay";
        String transformContent = """
                library(Rlabkey);
                
                run.props = labkey.transform.readRunPropertiesFile("${runInfo}");
                
                run.data.file = labkey.transform.getRunPropertyValue(run.props, "runDataFile");
                run.output.file = run.props$val3[run.props$name == "runDataFile"];
                error.file = labkey.transform.getRunPropertyValue(run.props, "errorsFile");
                
                if (file.exists(run.data.file)) {
                    run.data = read.delim(run.data.file, header=TRUE, sep="\\t", check.names = FALSE);
                    run.data$M2 = 111;
                    run.data$TransformType = "${transformOperation} testing";
                    write.table(run.data, file=run.output.file, sep="\\t", na="", row.names=FALSE, quote=FALSE);
                }
                """;
        File transformFile = TestFileUtils.writeTempFile(insertOrUpdateTransform, transformContent);
        var protocolResponse = new GeneralAssayDesign(insertOrUpdateTransformAssay)
                .setDataFields(List.of(new FieldDefinition("M2", FieldDefinition.ColumnType.Decimal),
                        new FieldDefinition("TransformType", FieldDefinition.ColumnType.String),
                        new FieldDefinition("Comment", FieldDefinition.ColumnType.String)), true)
                .createAssay(getProjectName(), createDefaultConnection());
        goToProjectHome();

        var assayDesignerPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocolResponse.getProtocolId(),
                "general", getURL().toString());
        assayDesignerPage.addTransformScript(transformFile, true);
        assayDesignerPage.goToBatchFields().removeAllFields(true);

        checker().verifyTrue("expect run on import to be enabled by default",
                assayDesignerPage.isScriptActionCheckboxEnabled(insertOrUpdateTransform, Import));
        checker().verifyFalse("expect run on edit not to be enabled by default",
                assayDesignerPage.isScriptActionCheckboxEnabled(insertOrUpdateTransform, Edit));
        checker().verifyTrue("expect run on import to be checked by default",
                assayDesignerPage.getScriptActionCheckbox(insertOrUpdateTransform, Import));
        checker().verifyFalse("expect run on edit not to be checked by default",
                assayDesignerPage.getScriptActionCheckbox(insertOrUpdateTransform, Edit));

        // now enable editable results
        assayDesignerPage.setEditableResults(true);
        checker().verifyTrue("expect run on edit to be enabled when editable results are enabled",
                assayDesignerPage.isScriptActionCheckboxEnabled(insertOrUpdateTransform, Edit));

        // check the run on edit box
        assayDesignerPage.setScriptActionCheckbox(insertOrUpdateTransform, Edit, true);
        checker().verifyTrue("expect run on edit to be successfully enabled and checked",
                assayDesignerPage.getScriptActionCheckbox(insertOrUpdateTransform, Edit));

        // disable editable results and verify that deselects the run on edit box
        assayDesignerPage.setEditableResults(false);
        checker().verifyFalse("expect run on edit to be disabled and deselected when editable results are disabled",
                assayDesignerPage.getScriptActionCheckbox(insertOrUpdateTransform, Edit));
        checker().verifyFalse("expect run on edit to be disabled and deselected when editable results are disabled",
                assayDesignerPage.isScriptActionCheckboxEnabled(insertOrUpdateTransform, Edit));

        // now re-enable editable results and check run on edit
        assayDesignerPage.setEditableResults(true);
        assayDesignerPage.setScriptActionCheckbox(insertOrUpdateTransform, Edit, true);
        assayDesignerPage.clickSave();

        // GH Issue 1130: baseline the tracked usage count before any transform script has run in this test
        long initialRlabkeyUsage = getRPackageUsageCount("Rlabkey");
        long initialDependentUsage = getRPackageUsageCount("httr");

        // now import data and ensure the expected transform operation occurred
        String importData = """
                VisitID	ParticipantID	Comment
                1	1	this is the import
                1	2	also import
                """;

        clickAndWait(Locator.linkWithText(insertOrUpdateTransformAssay));
        new AssayRunsPage(getDriver()).getTable().clickHeaderButtonAndWait("Import Data");
        var importPage = new AssayImportPage(getDriver());
        importPage.setNamedInputText("Name", "transformTestImport");
        importPage.setNamedTextAreaValue(AssayConstants.TEXT_AREA_DATA_COLLECTOR_TEXT_AREA_NAME, importData);
        importPage.clickSaveAndFinish();

        var assayDataPage = new AssayRunsPage(getDriver()).clickAssayIdLink("transformTestImport");
        var m2Data = assayDataPage.getDataTable().getColumnDataAsText("M2");
        var transformTypeData = assayDataPage.getDataTable().getColumnDataAsText("Transform Type");
        checker().wrapAssertion(()-> Assertions.assertThat(m2Data)
                .as("expect m2Data to contain only hard-coded transform value")
                .containsOnly("111.0"));
        checker().wrapAssertion(()-> Assertions.assertThat(transformTypeData)
                .as("expect transformTypeData to contain only insert")
                .containsOnly("INSERT testing"));
        checker().screenShotIfNewError("unexpected import transform data");

        assayDataPage.getDataTable().clickEditRow(0)
                .setField("Comment", "this is the update")
                .submit();

        var dataMap = assayDataPage.getDataTable().getRowDataAsMap(0);
        checker().verifyEquals("expect dataMap transform to show update",
                "UPDATE testing", dataMap.get("TransformType"));
        checker().screenShotIfNewError("unexpected update transform data");

        // now let's disable both import and edit
        assayDesignerPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocolResponse.getProtocolId(),
                "general", getURL().toString());
        assayDesignerPage.setScriptActionCheckbox(transformFile.getName(), Edit, false);
        assayDesignerPage.setScriptActionCheckbox(transformFile.getName(), Import, false);
        assayDesignerPage.clickSave();

        // now import some data to a new run called non_transform_import
        goToProjectHome();
        clickAndWait(Locator.linkWithText(insertOrUpdateTransformAssay));
        new AssayRunsPage(getDriver()).getTable().clickHeaderButtonAndWait("Import Data");
        importPage = new AssayImportPage(getDriver());
        importPage.setNamedInputText("Name", "non_transform_import");
        importPage.setNamedTextAreaValue(AssayConstants.TEXT_AREA_DATA_COLLECTOR_TEXT_AREA_NAME, importData);
        importPage.clickSaveAndFinish();

        var assayDataPage2 = new AssayRunsPage(getDriver()).clickAssayIdLink("non_transform_import");
        var m2Data2 = assayDataPage2.getDataTable().getColumnDataAsText("M2");
        var transformTypeData2 = assayDataPage2.getDataTable().getColumnDataAsText("Transform Type");
        checker().wrapAssertion(()-> Assertions.assertThat(m2Data2)
                .as("expect m2Data not to contain any transform values")
                .containsOnly(" "));
        checker().wrapAssertion(()-> Assertions.assertThat(transformTypeData2)
                .as("expect transformTypeData to contain no transform values")
                .containsOnly(" "));
        checker().screenShotIfNewError("unexpected import transform data");

        // now update the data we just imported
        assayDataPage.getDataTable().clickEditRow(0)
                .setField("Comment", "this is an update but no transform should happen")
                .submit();
        var dataMap2 = assayDataPage.getDataTable().getRowDataAsMap(0);
        checker().verifyEquals("expect dataMap transform data not to appear in transformType field",
                " ", dataMap2.get("TransformType"));
        checker().verifyEquals("expect M2 transform data not to appear in M2 field",
                " ", dataMap2.get("M2"));
        checker().screenShotIfNewError("unexpected update transform data");

        // re-enable protocol run-on-import and edit so we can measure their metrics
        assayDesignerPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocolResponse.getProtocolId(),
                "general", getURL().toString());
        assayDesignerPage.setScriptActionCheckbox(transformFile.getName(), Edit, true);
        assayDesignerPage.setScriptActionCheckbox(transformFile.getName(), Import, true);
        assayDesignerPage.clickSave();

        // validate some metrics for this feature
        var metricsPage = UsageStatisticsPage.beginAt(this);
        metricsPage.setJsonPathInput("modules.Experiment.assay");
        checker().verifyTrue("expect protocolsWithTransformScriptCount to be present",
                metricsPage.isValidKeyPresent("protocolsWithTransformScriptCount"));
        checker().verifyTrue("expect protocolsWithTransformScriptRunOnEditCount to be present",
                metricsPage.isValidKeyPresent("protocolsWithTransformScriptRunOnEditCount"));
        checker().verifyTrue("expect protocolsWithTransformScriptRunOnImportCount to be present",
                metricsPage.isValidKeyPresent("protocolsWithTransformScriptRunOnImportCount"));
        checker().screenShotIfNewError("missing metrics");

        metricsPage.clickValidKey("protocolsWithTransformScriptRunOnImportCount");
        int assaysWithTransformOnImport = Integer.parseInt(metricsPage.getValue());
        checker().verifyTrue("expect protocolsWithTransformScriptCount to have value >0",
                assaysWithTransformOnImport > 0);
        checker().screenShotIfNewError("missing assayWithTranformOnImport");

        metricsPage.clickClearButton();
        metricsPage.setJsonPathInput("modules.Experiment.assay.protocolsWithTransformScriptCount");
        int assaysWithTransformScripts = Integer.parseInt(metricsPage.getValue());
        checker().verifyTrue("expect protocolsWithTransformScriptCount to have value >0",
                assaysWithTransformScripts > 0);

        // GH Issue 1130: check the metric package tracking for R packages on the successful executions (2 in this case)
        metricsPage.clickClearButton();
        metricsPage.setJsonPathInput("modules.API.simpleMetricCounts." + R_PACKAGE_USAGE_AREA);
        checker().verifyTrue("expect Rlabkey to be present in the R package usage metrics",
                metricsPage.isValidKeyPresent("Rlabkey"));
        checker().verifyTrue("expect httr to be present in the R package usage metrics",
                metricsPage.isValidKeyPresent("httr"));
        checker().screenShotIfNewError("missing rPackageUsage metrics");

        long finalRlabkeyUsage = getRPackageUsageCount("Rlabkey");
        long finalDependentUsage = getRPackageUsageCount("httr");
        checker().wrapAssertion(() -> Assertions.assertThat(finalRlabkeyUsage)
                .as("expect one Rlabkey usage to be recorded for each of the two transform script runs")
                .isEqualTo(initialRlabkeyUsage + 2));
        checker().wrapAssertion(() -> Assertions.assertThat(finalDependentUsage)
                .as("expect one Rlabkey usage to be recorded for each of the two transform script runs")
                .isEqualTo(initialDependentUsage + 2));
    }

    // GH Issue 1130
    @Test
    public void testPythonTransformPackageUsage() throws Exception
    {
        String pythonTransform = "packageUsageTransform.py";
        String pythonTransformAssay = "pythonTransformAssay";
        String transformContent = """
                from labkey.utils import transform_helper
                filepath = '${runInfo}'
                def transform(grid):
                    isHeaderChecked = False
                    for row in grid:
                        if isHeaderChecked == False:
                            row.append('testing')
                            isHeaderChecked = True
                        else:
                            row.append(123)
                    return grid
                transform_helper(transform, filepath)
                """;
        File transformFile = TestFileUtils.writeTempFile(pythonTransform, transformContent);
        var protocolResponse = new GeneralAssayDesign(pythonTransformAssay)
                .setDataFields(List.of(new FieldDefinition("Comment", FieldDefinition.ColumnType.String)), true)
                .createAssay(getProjectName(), createDefaultConnection());
        goToProjectHome();

        var assayDesignerPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocolResponse.getProtocolId(),
                "general", getURL().toString());
        assayDesignerPage.addTransformScript(transformFile, true);
        assayDesignerPage.goToBatchFields().removeAllFields(true);
        assayDesignerPage.clickSave();

        long initialUsage = getPythonPackageUsageCount("labkey");

        String importData = """
                VisitID	ParticipantID	Comment
                1	1	this is the python transform import
                """;

        clickAndWait(Locator.linkWithText(pythonTransformAssay));
        new AssayRunsPage(getDriver()).getTable().clickHeaderButtonAndWait("Import Data");
        var importPage = new AssayImportPage(getDriver());
        importPage.setNamedInputText("Name", "pythonTransformImport");
        importPage.setNamedTextAreaValue(AssayConstants.TEXT_AREA_DATA_COLLECTOR_TEXT_AREA_NAME, importData);
        importPage.clickSaveAndFinish();

        var assayDataPage = new AssayRunsPage(getDriver()).clickAssayIdLink("pythonTransformImport");
        var commentData = assayDataPage.getDataTable().getColumnDataAsText("Comment");
        checker().wrapAssertion(() -> Assertions.assertThat(commentData)
                .as("expect the no-op python transform script to leave the imported data unchanged")
                .containsOnly("this is the python transform import"));
        checker().screenShotIfNewError("unexpected python transform data");

        // GH Issue 1130: check the metric package tracking for python packages on the single successful execution
        var metricsPage = UsageStatisticsPage.beginAt(this);
        metricsPage.setJsonPathInput("modules.API.simpleMetricCounts." + PYTHON_PACKAGE_USAGE_AREA);
        checker().verifyTrue("expect labkey to be present in the python package usage metrics",
                metricsPage.isValidKeyPresent("labkey"));
        checker().screenShotIfNewError("missing pythonPackageUsage metrics");

        long finalUsage = getPythonPackageUsageCount("labkey");
        checker().wrapAssertion(() -> Assertions.assertThat(finalUsage)
                .as("expect one labkey usage to be recorded for the single python transform script run")
                .isEqualTo(initialUsage + 1));
    }

    private long getRPackageUsageCount(String packageName) throws IOException, CommandException
    {
        return getPackageUsageCount(R_PACKAGE_USAGE_AREA, packageName);
    }

    private long getPythonPackageUsageCount(String packageName) throws IOException, CommandException
    {
        return getPackageUsageCount(PYTHON_PACKAGE_USAGE_AREA, packageName);
    }

    /**
     * GH Issue 1130: script package usage is recorded via SimpleMetricsService, which reports a cumulative per-package
     * count under the API module's "simpleMetricCounts" node. Returns 0 if this server hasn't recorded the package
     * yet, which is the case until the first successful script run for that language.
     */
    private long getPackageUsageCount(String featureArea, String packageName) throws IOException, CommandException
    {
        try
        {
            String metricPath = "simpleMetricCounts." + featureArea + "." + packageName;
            return ServerUsageUtils.getModuleMetricValue(createDefaultConnection(), "API", metricPath);
        }
        catch (NoSuchElementException e)
        {
            return 0;
        }
    }

    // Issue 50774
    @Test
    public void testCancelAsyncAssayTransformJob() throws Exception
    {
        String transformCancelFile = "importCancelTransform.R";
        String importCancelTransformAssay = "importCancelTransformAssay";
        String transformContent = """
                library(Rlabkey);
                
                run.props = labkey.transform.readRunPropertiesFile("${runInfo}");
                
                run.data.file = labkey.transform.getRunPropertyValue(run.props, "runDataFile");
                run.output.file = run.props$val3[run.props$name == "runDataFile"];
                error.file = labkey.transform.getRunPropertyValue(run.props, "errorsFile");
                
                # sleep a bit before writing the table, give the test time to cancel the job before it is complete
                labkey.setDebugMode(TRUE);
                print("before");
                Sys.sleep(30);
                print("after");
                labkey.setDebugMode(FALSE);

                if (file.exists(run.data.file)) {
                    run.data = read.delim(run.data.file, header=TRUE, sep="\\t", check.names = FALSE);
                    run.data$M2 = 111;
                    run.data$TransformType = "${transformOperation} testing";
                    write.table(run.data, file=run.output.file, sep="\\t", na="", row.names=FALSE, quote=FALSE);
                }
                
                """;
        File transformFile = TestFileUtils.writeTempFile(transformCancelFile, transformContent);
        var protocolResponse = new GeneralAssayDesign(importCancelTransformAssay)
                .setDataFields(List.of(new FieldDefinition("M2", FieldDefinition.ColumnType.Decimal),
                        new FieldDefinition("TransformType", FieldDefinition.ColumnType.String),
                        new FieldDefinition("Comment", FieldDefinition.ColumnType.String)), true)
                .createAssay(getProjectName(), createDefaultConnection());
        goToProjectHome();

        var assayDesignerPage = ReactAssayDesignerPage.beginAt(this, getProjectName(), protocolResponse.getProtocolId(),
                "general", getURL().toString());
        assayDesignerPage.addTransformScript(transformFile, true);
        assayDesignerPage.goToBatchFields().removeAllFields(true);
        assayDesignerPage.setBackgroundImport(true);
        assayDesignerPage.clickSave();

        StringBuilder importDataBuilder = new StringBuilder("VisitID\tParticipantID\tComment\n");
        for (int i=1; i<=10; i++)
            importDataBuilder.append(String.format("%d\t%d\tComment-%d\n", i, i, i));

        clickAndWait(Locator.linkWithText(importCancelTransformAssay));
        new AssayRunsPage(getDriver()).getTable().clickHeaderButtonAndWait("Import Data");
        var importPage = new AssayImportPage(getDriver());
        importPage.setNamedInputText("Name", "cancelTransformTestImport");
        importPage.setNamedTextAreaValue(AssayConstants.TEXT_AREA_DATA_COLLECTOR_TEXT_AREA_NAME, importDataBuilder.toString());
        Instant before = Instant.now();
        importPage.clickSaveAndFinish();

        waitAndClick(WAIT_FOR_JAVASCRIPT, Locator.linkWithText("Assay upload RUNNING"), WAIT_FOR_JAVASCRIPT);
        PipelineStatusDetailsPage pipelineStatusDetailsPage = new PipelineStatusDetailsPage(getDriver());
        pipelineStatusDetailsPage.clickCancel();

        pipelineStatusDetailsPage.showLogDetails();
        pipelineStatusDetailsPage.assertLogTextContains("INFO : Attempting to cancel as requested",
                "INFO : Interrupting job by sending interrupt request.",
                "ERROR: The following error was generated by the assay upload",
                "INFO : Failed to complete task 'org.labkey.api.assay.pipeline.AssayUploadPipelineTask'");
        Instant after = Instant.now();
        Duration duration = Duration.between(before, after);
        log(String.format("duration of pipeline run from start to cancel is %d seconds", duration.getSeconds()));
        checker().withScreenshot("unexpected duration")
                    .wrapAssertion(()-> Assertions.assertThat(duration.getSeconds())
                            .as("expect cancel to interrupt the sleep in the transform script")
                            .isLessThan(20));

        // verify in the server log the process termination logging
        ShowAdminPage adminPage = goToAdminConsole();
        adminPage.clickViewPrimarySiteLogFile();
        assertTextPresent("Attempting to kill forked process gracefully",
                "Finished dealing with forked process");

        resetErrors();
    }
}
