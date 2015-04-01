/*
 * Copyright (c) 2014 LabKey Corporation
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Reports;
import org.labkey.test.pages.DomainEditor;
import org.labkey.test.util.BoxPlotReportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DatasetDomainEditor;
import org.labkey.test.util.GenericChartHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyA.class, Reports.class, Charting.class})
public class GenericMeasurePickerTest extends BaseWebDriverTest
{
    private static final File STUDY_ZIP = TestFileUtils.getSampleData("studies/LabkeyDemoStudy.zip");
    private static final String DATASET = "HIV Test Results";

    private static final String TAGGED_MEASURE = "Viral Load Quantified (copies/ml)";
    private static final String UNTAGGED_MEASURE = "Day";
    private static final String TAGGED_DIMENSION = "HIV Rapid Blood Test";
    private static final String UNTAGGED_DIMENSION = "HIV Western Blot";

    private static final String DEFAULT_Y_DIMENSION = "Cohort";


    @Nullable
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + "Project";
    }

    @BeforeClass
    public static void beforeTestClass()
    {
        GenericMeasurePickerTest init = (GenericMeasurePickerTest)getCurrentTest();

        init.initProject();
    }

    private void initProject()
    {
        _containerHelper.createProject(getProjectName(), "Study");
        importStudyFromZip(STUDY_ZIP);

        goToManageStudy();
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText(DATASET));
        clickButton("Edit Definition");

        DomainEditor domainEditor = new DatasetDomainEditor(this);
        domainEditor.selectField(5); // HIVRapidTest
        domainEditor.clickTab("Reporting");
        checkCheckbox(Locator.checkboxByName("dimension"));
        clickButton("Save");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testRestrictedMeasures()
    {
        enableColumnRestricting();

        goToProjectHome();
        clickAndWait(Locator.linkWithText("12 datasets"));
        clickAndWait(Locator.linkWithText(DATASET));

        DataRegionTable datasetTable = new DataRegionTable("Dataset", this);
        datasetTable.clickHeaderButton("Charts", "Create Box Plot");

        GenericChartHelper chartHelper = new BoxPlotReportHelper(this, DATASET);

        chartHelper.selectMeasure(TAGGED_MEASURE);
        assertElementNotPresent(GenericChartHelper.Locators.pickerRow.withText(UNTAGGED_MEASURE));
        assertElementNotPresent(GenericChartHelper.Locators.pickerRow.withText(TAGGED_DIMENSION));
        assertElementNotPresent(GenericChartHelper.Locators.pickerRow.withText(UNTAGGED_DIMENSION));
        chartHelper.confirmSelection();

        assertTrue(chartHelper.getCurrentSvg().getText().contains(TAGGED_MEASURE));

        chartHelper.openXAxisWindow();
        chartHelper.selectMeasure(TAGGED_DIMENSION);
        assertElementNotPresent(GenericChartHelper.Locators.pickerRow.withText(UNTAGGED_DIMENSION));
        assertElementPresent(GenericChartHelper.Locators.pickerRow.withText(TAGGED_MEASURE));
        assertElementNotPresent(GenericChartHelper.Locators.pickerRow.withText(UNTAGGED_MEASURE));
        chartHelper.confirmSelection();

        assertTrue(chartHelper.getCurrentSvg().getText().contains(TAGGED_DIMENSION));

        chartHelper.saveChart("restricted chart");
    }

    @Test
    public void testUnrestrictedMeasures()
    {
        disableColumnRestricting();

        goToProjectHome();
        clickAndWait(Locator.linkWithText("12 datasets"));
        clickAndWait(Locator.linkWithText(DATASET));

        DataRegionTable datasetTable = new DataRegionTable("Dataset", this);
        datasetTable.clickHeaderButton("Charts", "Create Box Plot");

        GenericChartHelper chartHelper = new BoxPlotReportHelper(this, DATASET);

        chartHelper.selectMeasure(TAGGED_MEASURE);
        assertElementPresent(GenericChartHelper.Locators.pickerRow.withText(UNTAGGED_MEASURE));
        assertElementNotPresent(GenericChartHelper.Locators.pickerRow.withText(TAGGED_DIMENSION));
        assertElementNotPresent(GenericChartHelper.Locators.pickerRow.withText(UNTAGGED_DIMENSION));
        chartHelper.confirmSelection();

        assertTrue(chartHelper.getCurrentSvg().getText().contains(TAGGED_MEASURE));

        chartHelper.openXAxisWindow();
        chartHelper.selectMeasure(TAGGED_DIMENSION);
        assertElementPresent(GenericChartHelper.Locators.pickerRow.withText(UNTAGGED_DIMENSION));
        assertElementPresent(GenericChartHelper.Locators.pickerRow.withText(TAGGED_MEASURE));
        assertElementPresent(GenericChartHelper.Locators.pickerRow.withText(UNTAGGED_MEASURE));
        chartHelper.confirmSelection();

        assertTrue(chartHelper.getCurrentSvg().getText().contains(TAGGED_DIMENSION));

        chartHelper.saveChart("unrestricted chart");
    }

    private void enableColumnRestricting()
    {
        goToProjectSettings();
        checkCheckbox(Locator.name("restrictedColumnsEnabled"));
        clickButton("Save");
    }

    private void disableColumnRestricting()
    {
        goToProjectSettings();
        uncheckCheckbox(Locator.name("restrictedColumnsEnabled"));
        clickButton("Save");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("visualization");
    }
}
