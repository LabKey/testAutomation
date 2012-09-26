/*
 * Copyright (c) 2012 LabKey Corporation
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
import org.labkey.test.BaseFlowTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;

/**
 * User: kevink
 * Date: 6/6/12
 *
 * This test is an end-to-end scenario for the Letvin lab.
 * - date-based study set up with default timepoint length of 7 days.
 * - import Flow and CBC data into different containers.
 * - copy Flow and CBC data into study with different dates but within a 7-day timespan.
 *     - verifies URLs go to correct container
 * - custom query combining CBC and Flow datasets with expression columns.
 */
public class FlowCBCTest extends BaseFlowTest
{
    public static final String STUDY_FOLDER = "KoStudy";
    public static final String CBC_FOLDER = "CBCFolder";

    public static final String ASSAY_NAME = "CBCAssay";

    public static final String PTID1 = "P4309";
    public static final String PTID2 = "P2301";

    @Override
    protected void init()
    {
        super.init();
        initializeAssayFolder();
        initializeStudyFolder();
    }

    void initializeAssayFolder()
    {
        log("** Initialize CBC Assay");
        createSubfolder(getProjectName(), getProjectName(), CBC_FOLDER, "Assay", null);

        clickLinkWithText(CBC_FOLDER);
        if (!isLinkPresentWithText("Assay List"))
            addWebPart("Assay List");
        clickButton("New Assay Design");
        checkRadioButton("providerName", "CBC");
        clickButton("Next");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", ASSAY_NAME);
        sleep(1000);

        // Remove TargetStudy field from the Batch domain
        deleteField("Batch Fields", 0);

        // Add TargetStudy to the end of the default list of Results domain
        addField("Result Fields", 25, "TargetStudy", "Target Study", ListHelper.ListColumnType.String);

        clickButton("Save", 0);
        waitForText("Save successful.", 20000);
    }

    void initializeStudyFolder()
    {
        log("** Initialize Study Folder");
        createSubfolder(getProjectName(), getProjectName(), STUDY_FOLDER, "Study", new String[] { "Study", "Letvin", "Flow" });
        clickButton("Create Study");
        // use date-based study
        click(Locator.xpath("(//input[@name='timepointType'])[1]"));
        setFormElement(Locator.xpath("//input[@name='startDate']"), "2012-03-01");
        clickButton("Create Study");

        clickLinkWithText("Manage Timepoints");
        setFormElement(Locator.xpath("//input[@name='defaultTimepointDuration']"), "7");
        clickButton("Update");
    }

    @Override
    protected void _doTestSteps() throws Exception
    {
        copyFlowResultsToStudy();

        copyCBCResultsToStudy();

        verifyQuery();
    }

    private void copyFlowResultsToStudy()
    {
        importExternalAnalysis(getContainerPath(), "/analysis.zip");
        setProtocolMetadata("Keyword SAMPLE ID", "Keyword $DATE", null, false);

        // Copy the sample wells to the STUDY_FOLDER
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");
        clickCheckbox(".toggle");
        clickButton("Copy to Study");
        selectOptionByText("targetStudy", "/" + getProjectName() + "/" + STUDY_FOLDER + " (" + STUDY_FOLDER + " Study)");
        clickButton("Next");
        assertTitleContains("Copy to " + STUDY_FOLDER + " Study: Verify Results");
        setFormElement(Locator.name("date", 0), "2012-03-17");
        setFormElement(Locator.name("participantId", 1), PTID1);
        clickButton("Copy to Study");

        assertTitleContains("Dataset: Flow");
        Assert.assertTrue("Expected go to STUDY_FOLDER container", getCurrentRelativeURL().contains("/" + STUDY_FOLDER));
        assertTextPresent(PTID1, "2012-03-17"); // PTID entered in copy verify page and DATE from FCS keywords
        assertTextPresent(PTID2, "2012-06-20"); // PTID from FCS keyword and DATE entered in copy verify page
        String href = getAttribute(Locator.linkWithText(PTID2), "href");
        Assert.assertTrue("Expected PTID link to go to STUDY_FOLDER container: " + href, href.contains("/" + STUDY_FOLDER));
        href = getAttribute(Locator.linkWithText("analysis"), "href");
        Assert.assertTrue("Expected Run link to go to flow container: " + href, href.contains("/" + getFolderName()));
        href = getAttribute(Locator.linkWithText("06-20-12 mem naive"), "href");
        Assert.assertTrue("Expected Compensation Matrix link to go to flow container: " + href, href.contains("/" + getFolderName()));

        // verify graph img is displayed (no error) and the src attribute goes to the flow container
        assertTextNotPresent("Error generating graph");
        href = getAttribute(Locator.xpath("//img[@title='(FSC-H:SSC-H)']"), "src");
        Assert.assertTrue("Expected graph img to go to flow container: " + href, href.contains("/" + getFolderName() + "/showGraph.view"));

        pushLocation();
        clickButton("View Source Assay");
        assertTitleContains("Flow Runs:");
        Assert.assertTrue("Expected source assay button to go to flow container", getCurrentRelativeURL().contains("/" + getFolderName()));
        popLocation();

        pushLocation();
        clickLinkWithText("assay");
        assertTitleContains("FCSAnalysis");
        Assert.assertTrue("Expected assay button to go to flow container", getCurrentRelativeURL().contains("/" + getFolderName()));
        popLocation();
    }

    private void copyCBCResultsToStudy()
    {
        log("** Upload CBC Data");
        clickLinkWithText(getProjectName());
        clickLinkWithText(CBC_FOLDER);
        clickLinkWithText(ASSAY_NAME);
        clickButton("Import Data");

        setFormElement("name", "run01");
        String cbcDataPath = "/server/modules/cbcassay/data/ex_20081016_131859.small.dat";
        setFormElement("TextAreaDataCollector.textArea", getFileContents(cbcDataPath));
        clickButton("Save and Finish", 8000);

        // filter to rows we'd like to copy
        clickLinkWithText("run01");
        DataRegionTable table = new DataRegionTable(ASSAY_NAME + " Data", this);
        table.setFilter("SampleId", "Equals One Of (e.g. \"a;b;c\")", "241-03A;317-03A");
        table.checkAllOnPage();

        clickButton("Copy to Study");
        selectOptionByText(Locator.name("targetStudy", 0), "/" + getProjectName() + "/" + STUDY_FOLDER + " (" + STUDY_FOLDER + " Study)");
        selectOptionByText(Locator.name("targetStudy", 1), "/" + getProjectName() + "/" + STUDY_FOLDER + " (" + STUDY_FOLDER + " Study)");
        setFormElement(Locator.name("participantId", 0), PTID1);
        setFormElement(Locator.name("participantId", 1), PTID2);
        // Note that dates are not on the same day, but within the default timespan size
        setFormElement(Locator.name("date", 0), "2012-06-19");
        setFormElement(Locator.name("date", 1), "2012-03-18");
        clickButton("Copy to Study");
    }

    private void verifyQuery()
    {
        log("** Verify mem naive query");
        beginAt("/query/" + getProjectName() + "/" + STUDY_FOLDER + "/executeQuery.view?schemaName=study&query.queryName=mem%20naive%20CBCFlow&query.sort=ParticipantId");
        DataRegionTable table = new DataRegionTable("query", this, false, true);
        assertEquals("Expected one row", 1, table.getDataRowCount());
        assertEquals(PTID1, table.getDataAsText(0, "Participant ID"));
        assertEquals("Week 16", table.getDataAsText(0, "Visit"));
        assertEquals("2880.0", table.getDataAsText(0, "Total Lymph"));
        assertEquals("78.6%", table.getDataAsText(0, "CD3+ Percent"));
        assertEquals("2263.18", table.getDataAsText(0, "CD3+ Lymph"));

        log("** Verify 8a/p11c/4/3 query");
        beginAt("/query/" + getProjectName() + "/" + STUDY_FOLDER + "/executeQuery.view?schemaName=study&query.queryName=8a%2Fp11c%2F4%2F3%20CBCFlow&query.sort=ParticipantId");
        table = new DataRegionTable("query", this, false, true);
        assertEquals("Expected one row", 1, table.getDataRowCount());
        assertEquals(PTID2, table.getDataAsText(0, "Participant ID"));
        assertEquals("Week 3", table.getDataAsText(0, "Visit"));
        assertEquals("3080.0", table.getDataAsText(0, "Total Lymph"));
        assertEquals("87.0%", table.getDataAsText(0, "CD3+ Percent"));
        assertEquals("2680.09", table.getDataAsText(0, "CD3+ Lymph"));
    }
}
