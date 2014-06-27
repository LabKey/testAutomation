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
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Reports;

@Category({DailyB.class, Reports.class})
public class ReportAndDatasetNotificationTest extends StudyBaseTest
{
    @Override
    protected String getProjectName()
    {
        return "ReportDatasetNotifyVerifyProject";
    }

    @Override
    protected void doCreateSteps()
    {
        enableEmailRecorder();

        initializeFolder();
        initializePipeline(null);

        clickFolder(getFolderName());

        log("Import study with reports and datasets");
        importFolderFromPipeline("ReportDatasetNotifyTest.folder.zip");
    }

    @Override
    protected void doVerifySteps()
    {
        log("Subscribe to some categories");
        clickFolder(getFolderName());
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Views"));
        clickAndWait(Locator.linkContainingText("Manage Notifications"));
        _ext4Helper.checkGridRowCheckboxAlt("Uncategorized", 0, false);
        _ext4Helper.checkGridRowCheckboxAlt("Cons", 0, false);
        _ext4Helper.checkGridRowCheckboxAlt("Reports", 0, false);
        _ext4Helper.checkGridRowCheckboxAlt("Exams", 0, true);
        clickButton("Save");

        log("Send notification and check email in dumbster");
        beginAt("/reports/" + getProjectName() + "/sendDailyDigest.view");
        clickFolder(getFolderName());
        goToModule("Dumbster");
        click(Locator.linkContainingText("Report/Dataset Change Notification"));
        assertTextPresent("Participation and Genetic Consent",
                "Box Plot - Antigen Spot Counts",
                "Renal Study",
                "Participant Weight",
                "Physical Exam",
                "R Regression: Blood Pressure: All");
        assertTextNotPresent("HIV Test Results",
                "Participant Views: CD4, Virus vs. T",
                "Luminex Assay 100",
                "Status Assessment");
        clickFolder(getFolderName());
    }

}
