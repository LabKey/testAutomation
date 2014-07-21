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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.LuminexAll;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by cnathe on 4/25/14.
 *
 * This test is meant to mimic the LuminexGuideSetTest but use value-based guide sets instead of run-based guide sets.
 */
@Category({DailyA.class, LuminexAll.class, Assays.class})
public class LuminexValueBasedGuideSetTest extends LuminexGuideSetTest
{
    private final String[] UPDATED_EXPECTED_FLAGS = {"AUC, EC50-5, PCV", "", "", "", "PCV"};

    protected void runUITests()
    {
        finishAssayDesignConfigure();
        importGuideSetRun(0);
        importGuideSetRun(1);
        verifyGuideSetsNotApplied();
        createInitialGuideSets();
        importGuideSetRun(2);
        importGuideSetRun(3);
        importGuideSetRun(4);
        Map<String, Integer> guideSetIds = getGuideSetIdMap();
        verifyGuideSetsApplied(guideSetIds, GUIDE_SET_ANALYTE_NAMES, 5);
        verifyQCFlags(GUIDE_SET_ANALYTE_NAMES[0], INITIAL_EXPECTED_FLAGS);
        verifyQCReport();
        udpateGuideSets();
        verifyQCFlags(GUIDE_SET_ANALYTE_NAMES[1], UPDATED_EXPECTED_FLAGS);
        verifyGuideSetCurveFitEmpty();
    }

    private void finishAssayDesignConfigure()
    {
        addTransformScript(new File(TestFileUtils.getLabKeyRoot(), getModuleDirectory() + RTRANSFORM_SCRIPT_FILE_LABKEY), 0);
        saveAssay();
    }

    private void createInitialGuideSets()
    {
        goToLeveyJenningsGraphPage("Standard1");

        Map<String, Double> metricInputs = new TreeMap<>();

        setUpLeveyJenningsGraphParams(GUIDE_SET_ANALYTE_NAMES[0]);
        createGuideSet(true);
        metricInputs.put("EC504PLAverage", 179.78);
        metricInputs.put("EC504PLStdDev", 22.21);
        metricInputs.put("EC505PLAverage", 253.25);
        metricInputs.put("EC505PLStdDev", 16.98);
        metricInputs.put("AUCAverage", 8701.38);
        metricInputs.put("AUCStdDev", 466.81);
        metricInputs.put("MaxFIAverage", 11457.15);
        metricInputs.put("MaxFIStdDev", 549.21);
        String guideSetComment1 = "Analyte 1";
        editValueBasedGuideSet(metricInputs, guideSetComment1, true);
        applyGuideSetToRun(new String[]{"NETWORK1", "NETWORK2"}, guideSetComment1, true);

        setUpLeveyJenningsGraphParams(GUIDE_SET_ANALYTE_NAMES[1]);
        createGuideSet(true);
        metricInputs.put("EC504PLAverage", 43426.10);
        metricInputs.put("EC504PLStdDev", 794.95);
        metricInputs.put("EC505PLAverage", 40349.13);
        metricInputs.put("EC505PLStdDev", 3084.91);
        metricInputs.put("AUCAverage", 80851.83);
        metricInputs.put("AUCStdDev", 6523.08);
        metricInputs.put("MaxFIAverage", 30992.25);
        metricInputs.put("MaxFIStdDev", 2083.49);
        String guideSetComment2 = "Analyte 2";
        editValueBasedGuideSet(metricInputs, guideSetComment2, true);
        applyGuideSetToRun(new String[]{"NETWORK1", "NETWORK2"}, guideSetComment2, true);
    }

    private void verifyGuideSetCurveFitEmpty()
    {
        goToSchemaBrowser();
        selectQuery("assay.Luminex." + TEST_ASSAY_LUM, "GuideSetCurveFit");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        waitForText("No data to show");
    }

    private void udpateGuideSets()
    {
        Map<String, Double> metricInputs = new TreeMap<>();

        goToLeveyJenningsGraphPage("Standard1");

        setUpLeveyJenningsGraphParams(GUIDE_SET_ANALYTE_NAMES[0]);
        clickButtonContainingText("Edit", 0);
        metricInputs.put("EC505PLAverage", 325.0);
        metricInputs.put("EC505PLStdDev", 35.0);
        metricInputs.put("AUCAverage", 7800.0);
        metricInputs.put("MaxFIAverage", 10200.0);
        editValueBasedGuideSet(metricInputs, "Analyte 1", false);

        setUpLeveyJenningsGraphParams(GUIDE_SET_ANALYTE_NAMES[1]);
        createGuideSet(false);
        metricInputs.put("EC504PLAverage", 42158.22);
        metricInputs.put("EC504PLStdDev", 4833.76);
        metricInputs.put("EC505PLAverage", 40987.31);
        metricInputs.put("EC505PLStdDev", 4280.84);
        metricInputs.put("AUCAverage", 85268.04);
        metricInputs.put("AUCStdDev", 738.55);
        metricInputs.put("MaxFIAverage", 32507.27);
        metricInputs.put("MaxFIStdDev", 189.83);
        String guideSetComment = "New Analyte 2";
        editValueBasedGuideSet(metricInputs, guideSetComment, true);
        applyGuideSetToRun(new String[]{"NETWORK2", "NETWORK3", "NETWORK4", "NETWORK5"}, guideSetComment, true);
    }
}
