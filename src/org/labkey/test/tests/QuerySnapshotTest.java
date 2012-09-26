/*
 * Copyright (c) 2009-2012 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;

/**
 * User: klum
 * Date: Jul 31, 2009
 */
public class QuerySnapshotTest extends StudyBaseTest
{
    private final String DEMOGRAPHICS_SNAPSHOT = "Demographics Snapshot";
    private final String APX_SNAPSHOT = "APX Joined Snapshot";
    private final String CROSS_STUDY_SNAPSHOT = "Cross study query snapshot";
    protected static final String GRID_VIEW = "create_gridView";
    private static final String FOLDER_1 = "054";
    private static final String FOLDER_2 = "065";

    private String _folderName;

    private static final String CROSS_STUDY_QUERY_SQL =
            "SELECT 1 as sequenceNum, '054' as protocol, ds1.MouseId, ds1.demsex, ds1.demsexor\n" +
            "FROM Project.\"054\".study.\"DEM-1: Demographics\" ds1\n" +
            "UNION \n" +
            "SELECT 2 as sequenceNum, '065' as protocol, ds2.MouseId, ds2.demsex, ds2.demsexor\n" +
            "FROM Project.\"065\".study.\"DEM-1: Demographics\" ds2";

    @Override
    protected void doCreateSteps()
    {
        // create two study folders (054 and 065) and start importing a study in each
        setFolderName(FOLDER_1);
        importStudy();

        setFolderName(FOLDER_2);
        importStudy();

        waitForStudyLoad(FOLDER_1);
        waitForStudyLoad(FOLDER_2);
    }

    @Override
    protected void doVerifySteps()
    {
        doQuerySnapshotTest();
    }

    @Override
    public void doCleanup() throws Exception
    {
        //Check for errors before cleanup to avoid SQLException from deleting study so soon after updating it
        super.checkLeaksAndErrors();

        super.doCleanup();
    }

    @Override
    public void checkLeaksAndErrors()
    {
        //TODO: 14929: SQLException from org.labkey.study.visitmanager.VisitManager.performParticipantPurge() after dataset deletion
        //WORKAROUND: check for errors before cleanup.
    }

    private void waitForStudyLoad(String folderName)
    {
        // Navigate
        clickLinkWithText(folderName);
        clickLinkWithText("Data Pipeline");
        waitForPipelineJobsToComplete(1, "study import", false);

        // enable advanced study security
        enterPermissionsUI();
        ExtHelper.clickExtTab(this, "Study Security");
        waitAndClickButton("Study Security");
        selectOptionByValue("securityString", "BASIC_WRITE");
        waitForPageToLoad(30000);

        // shut off demographics bit to allow for insert
        clickLinkWithText(folderName);
        setDemographicsBit("DEM-1: Demographics", false);
    }

    @Override
    protected String getProjectName()
    {
        return "QuerySnapshotProject";
    }

    @Override
    protected String getFolderName()
    {
        return _folderName;
    }

    private void setFolderName(String folderName)
    {
        _folderName = folderName;
    }

    @Override
    protected String getStudyLabel()
    {
        return getFolderName();
    }

    protected void doQuerySnapshotTest()
    {
        // create a snapshot from a dataset
        log("create a snapshot from a dataset");
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("DEM-1: Demographics");
        createQuerySnapshot(DEMOGRAPHICS_SNAPSHOT, true, false);

        assertTextPresent("Dataset: " + DEMOGRAPHICS_SNAPSHOT);

        // test automatic updates by altering the source dataset
        log("test automatic updates by altering the source dataset");
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("DEM-1: Demographics");
        clickButton("Insert New");
        setFormElement("quf_MouseId", "999121212");
        setFormElement("quf_SequenceNum", "101");
        setFormElement("quf_DEMraco", "Armenian");

        clickButton("Submit");

        clickLinkWithText(getStudyLabel());
        clickLinkWithText(DEMOGRAPHICS_SNAPSHOT);
        clickMenuButton("QC State", "All data");
        waitForSnapshotUpdate("Armenian");

        log("delete the snapshot");
        clickMenuButton("Views", "Edit Snapshot");
        clickButton("Delete Snapshot", 0);
        getConfirmationAndWait();

        // snapshot over a custom view
        // test automatic updates by altering the source dataset
        log("create a snapshot over a custom view");
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("APX-1: Abbreviated Physical Exam");
        CustomizeViewsHelper.openCustomizeViewPanel(this);

        CustomizeViewsHelper.addCustomizeViewColumn(this, "MouseId/DataSet/DEM-1: Demographics/seq101/DEMraco", "DEM-1: Demographics Screening 4f.Other specify");
        CustomizeViewsHelper.saveCustomView(this, "APX Joined View");

        createQuerySnapshot(APX_SNAPSHOT, true, false);
        assertTextNotPresent("Slovakian");

        log("test automatic updates for a joined snapshot view");
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("DEM-1: Demographics");
        clickLink(Locator.xpath("//a[.='999320016']/../..//td/a[.='edit']"));
        setFormElement("quf_DEMraco", "Slovakian");
        clickButton("Submit");

        clickLinkWithText(getStudyLabel());
        clickLinkWithText(APX_SNAPSHOT);
        clickMenuButton("QC State", "All data");

        waitForSnapshotUpdate("Slovakian");

        log("delete the snapshot");
        clickMenuButton("Views", "Edit Snapshot");
        clickButton("Delete Snapshot", 0);
        getConfirmationAndWait();

        // snapshot over a custom query
        log("create a snapshot over a custom query");
        clickLinkWithText(getStudyLabel());
        clickTab("Manage");
        clickLinkWithText("Manage Views");
        createReport(GRID_VIEW);

        clickLinkWithText("Modify Dataset List (Advanced)");
        createNewQuery("study");

        setFormElement("ff_newQueryName", "APX: Custom Query");
        selectOptionByText("ff_baseTableName", "APX-1: Abbreviated Physical Exam");
        clickButton("Create and Edit Source");
        clickButton("Save", 0);
        waitForText("Saved", WAIT_FOR_JAVASCRIPT);
        clickButton("Edit Properties");
        setFormElement("rename", "APX: Custom Query Advanced");        
        clickButton("Save");
        waitForText("study.APX: Custom Query Advanced", WAIT_FOR_PAGE);
        waitForElement(Locator.linkWithText("view data"), WAIT_FOR_JAVASCRIPT);
        clickLinkWithText("view data");
        assertTextPresent("Custom Query Advanced");

        createQuerySnapshot("Custom Query Snapshot", true, true);
        assertTextPresent("Dataset: Custom Query Snapshot");

        // edit snapshot then delete
        log("edit the snapshot");
        clickMenuButton("Views", "Edit Snapshot");
        checkCheckbox(Locator.xpath("//input[@type='radio' and @name='updateType' and not (@id)]"));
        clickButton("Save");
        Assert.assertTrue(isChecked(Locator.xpath("//input[@type='radio' and @name='updateType' and not (@id)]")));
        clickButton("Update Snapshot", 0);
        getConfirmationAndWait();
        waitForText("Dataset: Custom Query Snapshot", 10000);

        log("delete the snapshot");
        clickMenuButton("Views", "Edit Snapshot");
        clickButton("Delete Snapshot", 0);
        getConfirmationAndWait();

        clickTab("Manage");
        waitForText("Manage Datasets", 10000);
        assertLinkNotPresentWithText("Custom Query Snapshot");

        // create a custom query for a cross study scenario
        goToModule("Query");
        createNewQuery("study");

        setFormElement("ff_newQueryName", "cross study query");
        clickButton("Create and Edit Source");
        setQueryEditorValue("queryText", CROSS_STUDY_QUERY_SQL);
        clickButton("Save & Finish");
        
        createQuerySnapshot(CROSS_STUDY_SNAPSHOT, true, false, "keyField", 3);

        // verify refresh from both datasets
        clickLinkWithText(FOLDER_1);
        clickLinkWithText("DEM-1: Demographics");
        clickButton("Insert New");
        setFormElement("quf_MouseId", "999121212");
        setFormElement("quf_SequenceNum", "101");
        setFormElement("quf_DEMsex", "Unknown");

        clickButton("Submit");

        clickLinkWithText(FOLDER_2);
        clickLinkWithText(CROSS_STUDY_SNAPSHOT);
        waitForSnapshotUpdate("Unknown");
        
        clickLinkWithText(FOLDER_2);
        clickLinkWithText("DEM-1: Demographics");
        clickButton("Insert New");
        setFormElement("quf_MouseId", "999151515");
        setFormElement("quf_SequenceNum", "104");
        setFormElement("quf_DEMsexor", "Undecided");

        clickButton("Submit");

        clickLinkWithText(FOLDER_2);
        clickLinkWithText(CROSS_STUDY_SNAPSHOT);
        waitForSnapshotUpdate("Undecided");

        clickMenuButton("Views", "Edit Snapshot");
        clickButton("Delete Snapshot", 0);
        getConfirmationAndWait();
    }

    private void createQuerySnapshot(String snapshotName, boolean autoUpdate, boolean isDemographic)
    {
        createQuerySnapshot(snapshotName, autoUpdate, isDemographic, null, 0);
    }

    private void createQuerySnapshot(String snapshotName, boolean autoUpdate, boolean isDemographic, String keyField, int index)
    {
        clickMenuButton("Views", "Create", "Query Snapshot");

        setFormElement("snapshotName", snapshotName);
        if (autoUpdate)
            checkCheckbox(Locator.xpath("//input[@type='radio' and @name='updateType' and not (@id)]"));

        if (keyField != null)
        {
            clickButton("Edit Dataset Definition");
            waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_JAVASCRIPT);

            String xpath = getPropertyXPath("Dataset Fields") + "//span" + Locator.navButton("Add Field").getPath();
            selenium.click(xpath);
            ListHelper.setColumnName(this, index, keyField);
            ListHelper.setColumnType(this, index, ListHelper.ListColumnType.Integer);

            click(Locator.name("ff_name0"));
            clickRadioButtonById("button_managedField");
            selectOptionByText("list_managedField", keyField);
            clickButton("Save", WAIT_FOR_JAVASCRIPT);
        }
        clickButton("Create Snapshot");
    }

    private void waitForSnapshotUpdate(String text)
    {
        int time = 0;
        while (!isTextPresent(text) && time < defaultWaitForPage)
        {
            sleep(3000);
            time += 3000;
            refresh();
        }
        assertTextPresent(text);
    }
}
