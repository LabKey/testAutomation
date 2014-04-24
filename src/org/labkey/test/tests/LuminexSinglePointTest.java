/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
import org.labkey.test.SortDirection;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MiniTest;
import org.labkey.test.util.DataRegionTable;
import org.testng.Assert;

import java.io.File;
import java.util.Calendar;

@Category({DailyA.class, MiniTest.class, Assays.class})
public class LuminexSinglePointTest  extends LuminexTest
{
    private final String file1 = "01-11A12-IgA-Biotin.xls";
    private final String file2 = "02-14A22-IgA-Biotin.xls";
    private final String file3 = "03-31A82-IgA-Biotin.xls";
    private final String file4 = "04-17A32-IgA-Biotin.xls";

    @Override
    protected void ensureConfigured()
    {
        setUseXarImport(true);
        super.ensureConfigured();
    }

    protected void runUITests()
    {
        runSinglePointTest();
    }

    protected void runSinglePointTest()
    {
        click(Locator.name("backgroundUpload"));
        saveAssay();

        importRun(file1, 1);
        importRun(file2, 2);
        assertTextPresent(TEST_ASSAY_LUM + " Upload Jobs");
        waitForPipelineJobsToFinish(3);
        goToTestAssayHome();
        _extHelper.clickExtMenuButton(true, Locator.xpath("//a[text() = 'view qc report']"), "view single point control qc report");
        waitForText("Average Fi Bkgd");

        DataRegionTable tbl = new DataRegionTable("AnalyteSinglePointControl", this);
        tbl.setFilter("Analyte", "Equals", "ENV1 (31)");
        tbl.setSort("SinglePointControl/Run/Name", SortDirection.ASC);
        Assert.assertEquals(tbl.getDataAsText(0, "Average Fi Bkgd"), "27.0");
        Assert.assertEquals(tbl.getDataAsText(1, "Average Fi Bkgd"), "30.0");
        tbl.clearFilter("Analyte");

        clickAndWait(Locator.linkContainingText("graph"));
        assertTextNotPresent("ERROR");

        createGuideSet("ENV1 (31)", true);
        editGuideSet(new String[]{"allRunsRow_1", "allRunsRow_0"}, "Single Point Control Guide", true);

        importRun(file3, 3);
        waitForPipelineJobsToFinish(4);

        goToLeviJennings();
        waitForText("CTRL");

        importRun(file4, 4);
        waitForPipelineJobsToFinish(5);

        goToLeviJennings();
        assertTextNotPresent("04-17A32-IgA-Biotin.xls");
    }

    protected void goToLeviJennings()
    {
        goToTestAssayHome();
        _extHelper.clickExtMenuButton(true, Locator.xpath("//a[text() = 'view qc report']"), "view single point control qc report");
        waitForText("Average Fi Bkgd");
        clickAndWait(Locator.linkContainingText("graph"));
    }

    protected void importRun(String filename, int runNumber) {

        goToTestAssayHome();
        clickButton("Import Data");
        clickButton("Next");

        Calendar testDate = Calendar.getInstance();
        testDate.add(Calendar.DATE, 1);

        importLuminexRunPageTwo(filename, isotype, conjugate, "", "", "Notebook",
                "Experimental", "TECH", df.format(testDate.getTime()), new File(getLabKeyRoot(),"sampledata/Luminex/"+filename), 1);

         switch(runNumber){
             case 1 :
                 checkCheckbox(Locator.name("_singlePointControl_IH5672"));
                 break;
             case 2 :
                 assertChecked(Locator.name("_singlePointControl_IH5672"));
                 break;
             case 4 :
                 uncheckCheckbox(Locator.name("_singlePointControl_IH5672"));
                 break;
             default :
                 break;
         }

        clickButton("Save and Finish");

    }
}