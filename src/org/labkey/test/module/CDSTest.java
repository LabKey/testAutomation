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
package org.labkey.test.module;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PostgresOnlyTest;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Mar 23, 2012
 * Time: 12:47:27 PM
 */
public class CDSTest extends BaseSeleniumWebTest implements PostgresOnlyTest
{
    private static final String PROJECT_NAME = "CDSTest Project";
    private static final File STUDY_ZIP = new File(getSampledataPath(), "CDS/Dataspace.folder.zip");
    private static final String STUDIES[] = {"Demo Study", "Not Actually CHAVI 001", "NotRV144"};
    private static final String LABS[] = {"Arnold/Bellew Lab", "LabKey Lab", "Piehler/Eckels Lab"};
    private static final String GROUP_NAME = "CDSTest_AGroup";
    private static final String GROUP_NAME2 = "CDSTest_BGroup";
    private static final String GROUP_NAME3 = "CDSTest_CGroup";
    private static final String GROUP_NULL = "Group creation cancelled";
    private static final String GROUP_DESC = "Intersection of " +LABS[1]+ " and " + LABS[2];
    private static final String TOOLTIP = "Hold shift, CTRL, or CMD to select multiple";
    public final static int CDS_WAIT = 5000;

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/CDS";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public void doCleanup()
    {
        // Delete any containers and users created by the test.
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch (Exception e)
        {
        }
    }

    @Override
    public void doTestSteps()
    {
        createProject(PROJECT_NAME, "Study");
        enableModule(PROJECT_NAME, "CDS");
        importStudyFromZip(STUDY_ZIP.getPath());
        goToProjectHome();
        addWebPart("CDS Management");

        importCDSData("Antigens",          new File(getSampledataPath(), "CDS/antigens.tsv"));
        importCDSData("Assays",            new File(getSampledataPath(), "CDS/assays.tsv"));
        importCDSData("Studies",           new File(getSampledataPath(), "CDS/studies.tsv"));
        importCDSData("Labs",              new File(getSampledataPath(), "CDS/labs.tsv"));
        importCDSData("People",            new File(getSampledataPath(), "CDS/people.tsv"));
        importCDSData("Citable",           new File(getSampledataPath(), "CDS/citable.tsv"));
        importCDSData("Citations",         new File(getSampledataPath(), "CDS/citations.tsv"));
        importCDSData("AssayPublications", new File(getSampledataPath(), "CDS/assay_publications.tsv"));
        importCDSData("Vaccines",          new File(getSampledataPath(), "CDS/vaccines.tsv"));
        importCDSData("VaccineComponents", new File(getSampledataPath(), "CDS/vaccinecomponents.tsv"));

        populateFactTable();
        verifyFactTable();

        selenium.windowMaximize(); // Provides more useful screenshots on failure
        verifyCounts();
        verifyGrid();
        verifyFilters();
        verifyNounPages();
        verifyMultiNounPages();
        verifyScatterPlot();
    }

/// Test substeps

    private void importCDSData(String query, File dataFile)
    {
        clickLinkWithText(PROJECT_NAME);
        waitForTextWithRefresh("Fact Table", defaultWaitForPage*4);  //wait for study to fully load
        clickLinkWithText(query);
        ListHelper.clickImportData(this);

        setFormElement(Locator.id("tsv3"), getFileContents(dataFile), true);
        clickButton("Submit");
    }

    private void populateFactTable()
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Populate Fact Table");
        uncheckCheckbox("dataset", "HIV Test Results");
        uncheckCheckbox("dataset", "Physical Exam");
        uncheckCheckbox("dataset", "ParticipantVaccines");
        submit();

        assertLinkPresentWithText("NAb");
        assertLinkPresentWithText("Luminex");
        assertLinkPresentWithText("Lab Results");
        assertLinkPresentWithText("MRNA");
        assertLinkPresentWithText("ADCC");
    }

    private void verifyFactTable()
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Verify");
        waitForText("No data to show.", CDS_WAIT);
    }

    private void verifyCounts()
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Application");

        assertLinkNotPresentWithText("Home");
        assertLinkNotPresentWithText("Admin");

        assertAllParticipantsPortalPage();

        // 14902
        click("Studies");
        assertFilterStatusPanel(STUDIES[0], "Demo Study", 6, 1, 3, 2, 20, 12);

        // Verify multi-select tooltip -- this only shows the first time
        assertTextPresent(TOOLTIP);

        // 14992
        goToAppHome();
        selectCDSGroup("Active filters", true, "Current Active Filters");
        selectCDSGroup("All participants", false);
        click("Studies");
        assertFilterStatusPanel(STUDIES[0], STUDIES[0], 6, 1, 3, 2, 20, 12);

        clickButton("use as filter", 0);
        waitForTextToDisappear(STUDIES[1], CDS_WAIT);
        assertFilterStatusCounts(6, 1, 3, 2, 20);
        goToAppHome();

        // Verify multi-select tooltip has dissappeared
        assertTextNotPresent(TOOLTIP);

        waitForText("Current Active Filters", CDS_WAIT);
        selectCDSGroup("All participants", false);
        waitForText(STUDIES[1], CDS_WAIT);
        selectCDSGroup("Active filters", true, "Current Active Filters");
        assertTextPresent("This is the set of filters");
        click("Studies");
        assertFilterStatusPanel(STUDIES[0], STUDIES[0], 6, 1, 3, 2, 20, 12);
        clickButton("clear filters", 0);
        waitForText(STUDIES[2], CDS_WAIT);
        goToAppHome();
        waitForText(STUDIES[1], CDS_WAIT);
        // end 14902

        click("Studies");
        assertFilterStatusPanel(STUDIES[1], "Not Actually ...", 12, 1, 3, 2, 8, 12);
        assertTextNotPresent(TOOLTIP);
        assertFilterStatusPanel(STUDIES[2], STUDIES[2], 11, 1, 3, 2, 3, 12);
        goToAppHome();
        click("Assay Antigens");
        sortBy("Tier", "1A");
        toggleExplorerBar("3");
        assertFilterStatusPanel("H061.14", "H061.14", 12, 1, 1, 1, 8, 12);
        toggleExplorerBar("1A");
        assertFilterStatusPanel("SF162.LS", "SF162.LS", 6, 1, 1, 1, 20, 12);
        toggleExplorerBar("1B");
        assertFilterStatusPanel("ZM109F.PB4", "ZM109F.PB4", 6, 1, 1, 1, 20, 6);
        goToAppHome();
        click("Assays");
        assertFilterStatusPanel("Lab Results", "Lab Results", 23, 3, 5, 0, 0, 29);
        assertFilterStatusPanel("ADCC-Ferrari", "ADCC-Ferrari", 12, 1, 3, 1, 4, 29);
        assertFilterStatusPanel("Luminex-Sample-LabKey", "Luminex-Sampl...", 6, 1, 3, 1, 1, 29);
        assertFilterStatusPanel("NAb-Sample-LabKey", "NAb-Sample-La...", 29, 3, 5, 2, 26, 29);
        assertFilterStatusPanel("mRNA assay", "mRNA assay", 5, 1, 3, 1, 0, 0);
        goToAppHome();
        click("Labs");
        assertFilterStatusPanel(LABS[0], "Arnold/Bellew...", 6, 1, 1, 2, 1, 23);
        assertFilterStatusPanel(LABS[1], "LabKey Lab", 23, 3, 2, 3, 26, 23);
        assertFilterStatusPanel(LABS[2], "Piehler/Eckel...", 18, 2, 2, 2, 7, 23);
        goToAppHome();
        click("Participants");
        pickCDSSort("Country");
        assertFilterStatusPanel("South Africa", "South Africa", 5, 1, 1, 1, 3, 18);
        assertFilterStatusPanel("USA", "USA", 19, 3, 4, 3, 31, 19);
        assertFilterStatusPanel("Thailand", "Thailand", 5, 1, 3, 1, 3, 18);
        goToAppHome();
    }

    private void verifyFilters()
    {
        log("Verify multi-select");

        // 14910
        click("Assay Antigens");
        sortBy("Tier", "1A");
        shiftSelectBars("MW965.26", "ZM197M.PB7");
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(), 'DJ263.8')]"), WAIT_FOR_JAVASCRIPT);
        assertElementPresent(Locator.xpath("//div[@class='filtermember']"), 6);
        assertFilterStatusCounts(6, 1, 1, 1, 20);
        clickButton("clear selection", 0);
        goToAppHome();
        // end 14910

        click("Labs");
        selectBars(LABS[0], LABS[1]);
        assertFilterStatusCounts(6, 1, 0, 2, 0);
        selectBars(LABS[0], LABS[2]);
        assertFilterStatusCounts(0, 0, 0, 0, 0);
        selectBars(LABS[1], LABS[2]);
        assertFilterStatusCounts(12, 1, 0, 2, 0);
        clickButton("use as filter", 0);
        sleep(750);// wait for animation
        clickButton("save group", 0);
        waitForText("Selection and Active Filters");
        waitForText("Selection and Active Filters (12)");
        waitForText("Only Active Filters (12)");
        setFormElement("groupname", GROUP_NAME);
        setFormElement("groupdescription", GROUP_DESC);
        clickButton("Save", 0);
        waitForTextToDisappear(LABS[0]);
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(), '"+ GROUP_NAME +"')]"), WAIT_FOR_JAVASCRIPT);
        assertFilterStatusCounts(12,1,0,2,0);
        clickButton("clear filters", 0);
        waitForText(LABS[0]);
        assertFilterStatusCounts(29,3,5,3,31);

        //TODO: Shouldn't be able to create unfiltered group
//        clickButton("save group", 0);
//        waitForText("Selection and Active Filters (29)");
//        assertTextPresent("Only Active Filters (29)");
//        setFormElement("groupname", "Unfiltered" + GROUP_NAME);
//        clickButton("Save", 0);

        goToAppHome();
        selectCDSGroup(GROUP_NAME, true);
        assertTextPresent(GROUP_DESC);

        waitForText("No Matching Assays Found.", WAIT_FOR_JAVASCRIPT);
        assertCDSPortalRow("Studies", STUDIES[1], "1 total");
        assertCDSPortalRow("Assay Antigens", "1 clades, 1 tiers, 1 sources (Unknown)", "0 total");
        assertCDSPortalRow("Assays", "No Matching Assays Found.", "0 total");
        assertCDSPortalRow("Labs", "LabKey Lab, Piehler/Eckels Lab", "2 total labs");
        assertCDSPortalRow("Participants", "4 races, 1 locations", "12 total participants");

        click("Labs");
        assertFilterStatusCounts(12,1,0,2,0);

        goToAppHome();
        selectCDSGroup("All participants", false);
        assertAllParticipantsPortalPage();

        log("Verify operator filtering");
        click("Studies");
        selectBars(STUDIES[0], STUDIES[1]);
        assertFilterStatusCounts(0, 0, 0, 0, 0);  // and
        assertElementPresent(Locator.xpath("//div[@class='showopselect' and text()='AND']"));
        mouseOver(Locator.xpath("//div[@class='showopselect' and text()='AND']"));
        assertFormElementEquals(Locator.xpath("//div[@class='opselect']//select"), "INTERSECT");
        setFormElement(Locator.xpath("//div[@class='opselect']//select"), "UNION");
        assertFilterStatusCounts(18, 2, 4, 3, 28); // or
        clickButton("use as filter", 0);
        waitForTextToDisappear(STUDIES[2]);
        assertFilterStatusCounts(18, 2, 4, 3, 28); // or
        assertElementPresent(Locator.xpath("//div[@class='showopselect' and text()='OR']"));
        assertFormElementEquals(Locator.xpath("//div[@class='opselect']//select"), "UNION");
        setFormElement(Locator.xpath("//div[@class='opselect']//select"), "INTERSECT");
        assertFilterStatusCounts(0, 0, 0, 0, 0);  // and
        assertTextNotPresent(STUDIES[1]);
        goToAppHome();
        waitForText("No Matching Studies Found.", CDS_WAIT);
        click("Labs");
        waitForText(STUDIES[0], CDS_WAIT);
        assertElementPresent(Locator.xpath("//div[@class='showopselect' and text()='AND']"));
        assertFilterStatusCounts(0, 0, 0, 0, 0);  // and
        clickButton("clear filters", 0);
        waitForText(LABS[2]);
        assertFilterStatusCounts(29, 3, 5, 3, 31);
        assertTextPresent("All participants");
        goToAppHome();

        log("Verify selection messaging");
        click("Assays");
        selectBars("ADCC-Ferrari", "Luminex-Sample-LabKey");
        assertFilterStatusCounts(0, 0, 0, 0, 0);
        pickCDSDimension("Studies");
        waitForText("Your selection of \"Assay\" was removed.", CDS_WAIT);
        assertFilterStatusCounts(29, 3, 5, 3, 31);
        click(Locator.xpath("//a[@id='usefiltermsg']"));
        waitForTextToDisappear(STUDIES[0], CDS_WAIT);
        assertFilterStatusCounts(0, 0, 0, 0, 0);
        clickButton("clear filters", 0);
        waitForText(STUDIES[2], CDS_WAIT);
        selectBars(STUDIES[0]);
        pickCDSDimension("Assays");
        waitForText("Your selection of \"Study\" was removed.", CDS_WAIT);
        click(Locator.xpath("//a[@id='ignorefiltermsg']"));
        waitForTextToDisappear("Your selection of \"Study\" was removed.", CDS_WAIT);
        assertFilterStatusCounts(29, 3, 5, 3, 31);
        goToAppHome();

        //test more group saving
        selectCDSGroup(GROUP_NAME, true);
        click("Participants");
        pickCDSSort("Gender");
        selectBars("f");

        clickButton("save group", 0);
        waitForText("Selection and Active Filters");
        waitForText("Selection and Active Filters (8)");
        assertTextPresent("Only Active Filters (12)");
        click(Locator.css(".withSelectionRadio input"));
        setFormElement("groupname", GROUP_NULL);
        clickButton("Cancel", 0);
        waitForTextToDisappear("Selection and Active Filters (8)");

        selectBars("f");
        clickButton("save group", 0);
        waitForText("Selection and Active Filters");
        waitForText("Selection and Active Filters (8)");
        assertTextPresent("Only Active Filters (12)");
        click(Locator.css(".filterOnlyRadio input"));
        setFormElement("groupname", GROUP_NAME2);
        clickButton("Save", 0);
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(), '"+ GROUP_NAME2 +"')]"), WAIT_FOR_JAVASCRIPT);

        selectBars("f");
        clickButton("save group", 0);
        waitForText("Selection and Active Filters");
        waitForText("Selection and Active Filters (8)");
        assertTextPresent("Only Active Filters (12)");
        click(Locator.css(".withSelectionRadio input"));
        setFormElement("groupname", GROUP_NAME3);
        clickButton("Save", 0);
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(), '"+ GROUP_NAME3 +"')]"), WAIT_FOR_JAVASCRIPT);

        // saved filter without including current selection (should be the same as initial group)
        goToAppHome();
        selectCDSGroup(GROUP_NAME2, true);
        assertTextNotPresent(GROUP_DESC);

        waitForText("12 total participants", WAIT_FOR_JAVASCRIPT);
        assertCDSPortalRow("Studies", STUDIES[1], "1 total");
        assertCDSPortalRow("Assay Antigens", "1 clades, 1 tiers, 1 sources (Unknown)", "0 total");
        assertCDSPortalRow("Assays", "No Matching Assays Found.", "0 total");
        assertCDSPortalRow("Labs", "LabKey Lab, Piehler/Eckels Lab", "2 total labs");
        assertCDSPortalRow("Participants", "4 races, 1 locations", "12 total participants");

        click("Labs");
        assertFilterStatusCounts(12,1,0,2,0);

        // saved filter including current selection (Gender: f)
        goToAppHome();
        selectCDSGroup(GROUP_NAME3, true);
        assertTextNotPresent(GROUP_DESC);
        assertTextPresent("Gender:");

        waitForText("8 total participants", WAIT_FOR_JAVASCRIPT);
        assertCDSPortalRow("Studies", STUDIES[1], "1 total");
        assertCDSPortalRow("Assay Antigens", "1 clades, 1 tiers, 1 sources (Unknown)", "0 total");
        assertCDSPortalRow("Assays", "No Matching Assays Found.", "0 total");
        assertCDSPortalRow("Labs", "LabKey Lab, Piehler/Eckels Lab", "2 total labs");
        assertCDSPortalRow("Participants", "4 races, 1 locations", "8 total participants");

        click("Labs");
        assertFilterStatusCounts(8,1,0,2,0);

        // Group creation cancelled
        goToAppHome();
        assertTextNotPresent(GROUP_NULL);
    }

    private void verifyGrid()
    {
        log("Verify Grid");
        goToAppHome();
        selectCDSGroup("All participants", false);
        click("Studies");

        click(Locator.tagContainingText("span", "View raw data"));
        addGridColumn("NAb", "Point IC50", true, true);
        addGridColumn("NAb", "Study Name", false, true);

        waitForGridCount(668);
        assertElementPresent(Locator.tagWithText("span", "Point IC50"));
        assertElementPresent(Locator.tagWithText("span", "Study Name"));
        click(Locator.tagWithText("span", "Explore Categories"));
        waitForText("Demo Study");
        selectBars("Demo Study");
        clickButton("use as filter", 0);

        waitForTextToDisappear("Not Actually CHAVI 001", CDS_WAIT);

        //Check to see if grid is properly filtering based on explorer filter
        click(Locator.tagWithText("span", "View raw data"));
        waitForGridCount(437);
        click(Locator.tagWithText("span", "Explore Categories"));
        clickButton("clear filters", 0);
        waitForElement(Locator.tagWithText("span", "NotRV144"));
        click(Locator.tagContainingText("span", "View raw data"));
        waitForGridCount(668);

        addGridColumn("Demographics", "Gender", true, true);
        addGridColumn("Demographics", "Ethnicity", false, true);

        waitForElement(Locator.tagWithText("span", "Gender"));
        waitForElement(Locator.tagWithText("span", "Ethnicity"));

        log("Remove a column");
        addGridColumn("NAb", "Point IC50", false, true);
        waitForTextToDisappear("Point IC50");
        //But other column from same table is still there
        waitForElement(Locator.tagContainingText("span", "Study Name"));

        setRawDataFilter("Ethnicity", "White");
        waitForGridCount(246);

        log("Change column set and ensure still filtered");
        addGridColumn("NAb", "Point IC50", false, true);
        waitForElement(Locator.tagWithText("span", "Point IC50"));
        waitForGridCount(246);

        openFilterPanel("Study Name");
        waitForElement(Locator.tagWithText("div", "PI1"));
        ExtHelper.clickX4GridPanelCheckbox(this, 3, "lookupcols", true);
        clickButton("OK", 0);

        log("Filter on a looked-up column");
        waitForElement(Locator.tagWithText("span", "PI1"));
        waitForElement(Locator.tagWithText("div", "Igra M"));
        setRawDataFilter("PI1", "Igra");
        waitForGridCount(152);

        log("Ensure filtering goes away when column does");
        openFilterPanel("Study Name");
        waitForElement(Locator.tagWithText("div", "PI1"));
        ExtHelper.clickX4GridPanelCheckbox(this, 2, "lookupcols", false);
        clickButton("OK", 0);
        waitForTextToDisappear("PI1");
        waitForGridCount(246);

        setRawDataFilter("Point IC50", "gt", "60");
        waitForGridCount(2);
        openFilterPanel("Ethnicity");
        clickButton("Clear Filters", 0);
        waitForGridCount(5);

        openFilterPanel("Point IC50");
        clickButton("Clear Filters", 0);
        waitForGridCount(668);

        log("Verify citation sources");
        clickButton("Sources", 0);
        waitForText("References", CDS_WAIT);
        assertTextPresent(
            "Demo study final NAb data",
            "NAb Data From Igra Lab",
            "Data extracted from LabKey lab site on Atlas",
            "Piehler B, Eckels J"
        );
        clickAt(Locator.xpath("//a[@class='cite-ref']"), "1,1");
        waitForText("Todd CA, Greene KM", CDS_WAIT);
        clickButton("Close", 0);
        waitForGridCount(668);

        log("Verify multiple citation sources");
        addGridColumn("Physical Exam", "Weight Kg", false, true);
        waitForElement(Locator.tagWithText("span", "Weight Kg"));
        waitForGridCount(700);

        clickButton("Sources", 0);
        waitForText("Demo study physical exam", CDS_WAIT);
        clickAt(Locator.xpath("//a[@class='cite-src']"), "1,1");
        waitForText("Pulled from Atlas", CDS_WAIT);
        assertTextPresent("Demo study data delivered");
        clickButton("Close", 0);
        waitForGridCount(700);

        addGridColumn("Physical Exam", "Weight Kg", false, true); // removes column
        waitForGridCount(668);

        // 15267
        addGridColumn("Physical Exam", "Source", true, true);
        addGridColumn("NAb", "Source", false, true);
        waitForText("Demo study physical exam", CDS_WAIT);
        waitForText("Demo study final NAb data", CDS_WAIT);

        goToAppHome();

    }

    private void addGridColumn(String source, String measure, boolean keepOpen, boolean keepSelection)
    {
        assertTextPresent("Data Grid"); // make sure we are looking at grid

        // allow for already open measures
        if (!isTextPresent("Add Measures"))
        {
            clickButton("Choose Columns", 0);
            waitForElement(Locator.css("div.sourcepanel"));
        }

        ExtHelper.pickMeasure(this, source, measure, keepSelection);

        if (!keepOpen)
        {
            clickButton("select", 0);
        }
    }

    private void setRawDataFilter(String colName, String value)
    {
        setRawDataFilter(colName, null, value);
    }

    private void setRawDataFilter(String colName, String filter, String value)
    {
        openFilterPanel(colName);
        if (null != filter)
            Ext4Helper.selectComboBoxItem(this, "Value", filter);

        waitForElement(Locator.id("value_1"));
        setFormElement(Locator.css("#value_1 input"), value);
        clickButton("OK", 0);
    }

    private void openFilterPanel (String colHeader)
    {
        List<Ext4CmpRef> headers = Ext4Helper.componentQuery(this, "#raw-data-view grid gridcolumn", Ext4CmpRef.class);
        for (Ext4CmpRef ref : headers)
        {
            String colNameStr = ref.eval("this.text");
            if (null != colNameStr && colNameStr.contains(colHeader))
            {
                String triggerid = ref.eval("this.triggerEl.id");
                mouseOver(Locator.id(triggerid));
                click(Locator.id(triggerid));
                waitFor(new Ext4Helper.Ext4SelectorChecker(this, "rawdatafilterwin"), "No filter win", WAIT_FOR_JAVASCRIPT);
            }
        }
    }
    private void waitForGridCount(int count)
    {
        String displayText;
        if (count == 0)
            displayText = "No data to display";
        else if (count < 100)
            displayText = "Displaying 1 - " + count + " of " + count;
        else
            displayText = "Displaying 1 - 100 of " + count;

        waitForElement(Locator.tagContainingText("div", displayText));
    }

    private void verifyNounPages()
    {
        selectCDSGroup("All participants", false);

        // placeholder pages
        click("Assay Antigens");
        sortBy("Tier", "1A");
        toggleExplorerBar("1A");
        assertNounInfoPage("MW965.26", Arrays.asList("Clade", "Tier", "MW965.26", "U08455"));
        assertNounInfoPage("SF162.LS", Arrays.asList("Clade", "Tier", "SF162.LS", "EU123924"));
        toggleExplorerBar("1B");
        assertNounInfoPage("ZM109F.PB4", Arrays.asList("Zambia", "Tier", "AY424138"));

        click("Studies");
        assertNounInfoPage("Demo Study", Arrays.asList("Igra M", "Fitzsimmons K", "Trial", "LabKey"));
        assertNounInfoPage("Not Actually CHAVI 001", Arrays.asList("Bellew M", "Arnold N", "Observational", "CHAVI"));
        assertNounInfoPage("NotRV144", Arrays.asList("Piehler B", "Lum K", "Trial", "USMHRP"));

        click("Labs");
        assertNounInfoPage("Arnold/Bellew Lab", Arrays.asList("Description", "PI", "Nick Arnold"));
        assertNounInfoPage("LabKey Lab", Arrays.asList("Description", "PI", "Mark Igra"));
        assertNounInfoPage("Piehler/Eckels Lab", Arrays.asList("Description", "PI", "Britt Piehler"));

        click("Assays");

        // check placeholders
        assertAssayInfoPage("Lab Results", "default.png", "default.png", "", "", "", "", "");
        assertAssayInfoPage("ADCC-Ferrari", "team_Mark_Igra.jpg", "team_Alan_Vezina.jpg",
                "Mark Igra\n" +
                        "marki@labkey.com\n" +
                        "Partner",
                "Alan Vezina\n" +
                        "alanv@labkey.com\n" +
                        "Developer",
                "Methodology: ICS\n" +
                        "Target Area: Adaptive: humoral and B-cell",
                "This is an ADCC assay.",
                "Immune escape from HIV-specific antibody-dependent cellular cytotoxicity (ADCC) pressure.");
        assertAssayInfoPage("Luminex-Sample-LabKey", "team_Nick_Arnold.jpg", "team_Nick_Arnold.jpg",
                "Nick Arnold\n" +
                        "nicka@labkey.com\n" +
                        "Developer",
                "Nick Arnold\n" +
                        "nicka@labkey.com\n" +
                        "Developer",
                "Methodology: Luminex\n" +
                        "Target Area: Adaptive: humoral and B-cell",
                "We measured something using a Luminex assay",
                "Inhibition of HIV-1 replication in human lymphoid tissues ex vivo by measles virus.");
        assertAssayInfoPage("mRNA assay", "team_Mark_Igra.jpg", "team_Nick_Arnold.jpg",
                "Mark Igra\n" +
                        "marki@labkey.com\n" +
                        "Partner",
                "Nick Arnold\n" +
                        "nicka@labkey.com\n" +
                        "Developer",
                "Methodology: ICS\n" +
                        "Target Area: Innate",
                "This one tested gene expression.",
                "Development of an in vitro mRNA degradation assay utilizing extracts from HIV-1- and SIV-infected cells.");
        assertAssayInfoPage("NAb-Sample-LabKey", "team_Karl_Lum.jpg", "team_Kristin_Fitzsimmons.jpg",
                "Karl Lum\n" +
                        "klum@labkey.com\n" +
                        "Developer",
                "Kristin Fitzsimmons\n" +
                        "kristinf@labkey.com\n" +
                        "ScrumMaster",
                "Methodology: NAb\n" +
                        "Target Area: Adaptive: humoral and B-cell",
                "This tested antibodies.",
                "Vaccinology: precisely tuned antibodies nab HIV.");
    }

    private void verifyMultiNounPages()
    {
        click("Assays");
        assertMultiAssayInfoPage();
        assertMultiStudyInfoPage();
        assertMultiAntigenInfoPage();
    }

    private static final String CD4_LYMPH = "Created with Rapha\u00ebl 2.1.0CD4050100150200250300350400450Lymphocytes200400600800100012001400160018002000";
    private static final String HEMO_CD4 = "Created with Rapha\u00ebl 2.1.0Hemoglobin05101520CD450100150200250300350400450";
    private static final String HEMO_CD4_UNFILTERED = "Created with Rapha\u00ebl 2.1.0Hemoglobin05101520CD41002003004005006007008009001000110012001300";
    private void verifyScatterPlot()
    {
        selectCDSGroup(GROUP_NAME, true);
        click("Studies");

        click(Locator.tagWithText("span", "Plot Data"));
        ExtHelper.pickMeasure(this, "XaxisPicker", "Lab Results", "CD4");
        clickButton("Plot", 0);
        ExtHelper.pickMeasure(this, "YaxisPicker", "Lab Results", "Lymphocytes");
        clickButton("Plot", 0);
        waitForText(CD4_LYMPH); // svg to text

        click(Locator.xpath("(//div[contains(@class, 'x4-btn-dropdown-small')])[2]")); // Choose variables button
        ExtHelper.pickMeasure(this, "YaxisPicker", "Lab Results", "CD4");
        Locator.xpath("(//div[contains(@class, 'curselhdr')])[1]");
        ExtHelper.pickMeasure(this, "XaxisPicker", "Lab Results", "Hemoglobin");
        clickButton("Plot", 0);
        waitForText(HEMO_CD4); // svg to text

        //TODO: Test cancel button [BLOCKED] 15095: Measure picker cancel button doesn't reset selections

        click(Locator.tagWithText("span", "Explore Categories"));
        waitForTextToDisappear(HEMO_CD4);

        click(Locator.tagWithText("span", "Plot Data"));
        waitForText(HEMO_CD4);

        clickButton("clear filters", 0);
        waitForTextToDisappear(HEMO_CD4);
        waitForText(HEMO_CD4_UNFILTERED);
    }

/// CDS App helpers

    private void pickCDSSort(String sortBy)
    {
        click(Locator.css(".sortDropdown"));
        waitAndClick(Locator.xpath("//span[text()='"+sortBy+"']"));
    }

    private void pickCDSDimension(String dimension)
    {
        click(Locator.xpath("//div[contains(@class, 'dropdown')]"));
        waitAndClick(Locator.xpath("//span[text()='" + dimension + "']"));
    }

    private void selectBars(String... bars)
    {
        String subselect = bars[0];
        if (subselect.length() > 10)
            subselect = subselect.substring(0, 9);
        sleep(1000);
        waitAndClick(Locator.xpath("//span[@class='barlabel' and text() = '" + bars[0] + "']"));
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(),'" + subselect + "')]"));
        if(bars.length > 1)
        {
            selenium.controlKeyDown();
            for(int i = 1; i < bars.length; i++)
            {
                click(Locator.xpath("//span[@class='barlabel' and text() = '"+bars[i]+"']"));
                subselect = bars[i];
                if (subselect.length() > 10)
                    subselect = subselect.substring(0, 9);
                waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(),'" + subselect + "')]"));
            }
            selenium.controlKeyUp();
        }
    }

    private void shiftSelectBars(String... bars)
    {
        String subselect = bars[0];
        if (subselect.length() > 10)
            subselect = subselect.substring(0, 9);
        sleep(1000);
        waitAndClick(Locator.xpath("//span[@class='barlabel' and text() = '" + bars[0] + "']"));
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(),'" + subselect + "')]"));
        if(bars.length > 1)
        {
            selenium.shiftKeyDown();
            for(int i = 1; i < bars.length; i++)
            {
                click(Locator.xpath("//span[@class='barlabel' and text() = '"+bars[i]+"']"));
                subselect = bars[i];
                if (subselect.length() > 10)
                    subselect = subselect.substring(0, 9);
                waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(),'" + subselect + "')]"));
            }
            selenium.shiftKeyUp();
        }
    }

    private void selectCDSGroup(String group, boolean titleShown)
    {
        selectCDSGroup(group, titleShown, null);
    }

    private void selectCDSGroup(String group, boolean titleShown, String title)
    {
        waitAndClick(Locator.xpath("//span[text()='"+group+"']"));
        if(titleShown)
            waitForElement(Locator.css(".title:contains('" + ((title != null) ? title : group) + "')"));
        else
            waitForElementToDisappear(Locator.xpath("//div[@class='title' and "+Locator.NOT_HIDDEN+"]"), WAIT_FOR_JAVASCRIPT);
    }

    private void goToAppHome()
    {
        clickAt(Locator.xpath("//div[contains(@class, 'connectorheader')]//div[contains(@class, 'logo')]"), "1,1");
        waitForElement(Locator.xpath("//div[contains(@class, 'connectorheader')]//div[contains(@class, 'logo')]/h2/br"), WAIT_FOR_JAVASCRIPT);
    }

    private void click(String by)
    {
        clickAt(Locator.xpath("//span[@class = 'label' and text() = ' "+by+"']"), "1,1");
        waitForText("Showing number of: Participants", CDS_WAIT);
    }

    private void sortBy(String sort, String waitValue)
    {
        click(Locator.xpath("//div[contains(@class, 'sortDropdown')]//span"));
        click(Locator.xpath("//span[contains(@class, 'x4-menu-item-text') and text()='" + sort + "']"));
        waitForText(waitValue, CDS_WAIT);
    }

    private void viewInfo(String barLabel)
    {
        mouseOver(Locator.xpath("//span[@class='barlabel' and text() = '" + barLabel + "']/.."));
        mouseOver(Locator.xpath("//span[@class='barlabel' and text() = '" + barLabel + "']/..//button"));
        click(Locator.xpath("//span[@class='barlabel' and text() = '"+barLabel+"']/..//button"));
        waitForElement(Locator.button("Close"));
        waitForElement(Locator.css(".savetitle"), WAIT_FOR_JAVASCRIPT);
        waitForText(barLabel);
        waitForElement(Locator.xpath("//div[contains(@class, 'savetitle') and text() = '" + barLabel +"']"));
    }

    private void viewMulti(String btnLabel, String infoLabel, String titleCss)
    {
        Locator btnLocator = Locator.xpath("//div[contains(@class, 'status-row')]/span[contains(text(), '" + btnLabel + "')]");
        waitForElement(btnLocator);
        mouseOver(btnLocator);
        click(btnLocator);
        waitForElement(Locator.button("Close"));
        if(!isElementPresent(Locator.css(titleCss)))
        {
            refresh();
            waitForElement(Locator.css(titleCss), WAIT_FOR_JAVASCRIPT);
        }
        waitForText(infoLabel);
        waitForElement(Locator.xpath("//div[contains(@class, 'savetitle') and text() = '" + infoLabel +"']"));
    }

    private void closeInfoPage()
    {
        clickButton("Close", 0);
        waitForElementToDisappear(Locator.button("Close"), WAIT_FOR_JAVASCRIPT);
    }

/// CDS App asserts

    private void assertAllParticipantsPortalPage()
    {
        assertCDSPortalRow("Studies", STUDIES[0] + ", " + STUDIES[1] + ", " + STUDIES[2], "3 total");
        assertCDSPortalRow("Assay Antigens", "5 clades, 5 tiers, 5 sources (Unknown, ccPBMC, Lung, Plasma, ucPBMC)", "31 total");
        assertCDSPortalRow("Assays", "Lab Results, ADCC-Ferrari, Luminex-Sample-LabKey, NAb-Sample-LabKey, mRNA assay", "5 total");
        assertCDSPortalRow("Labs", "Arnold/Bellew Lab, LabKey Lab, Piehler/Eckels Lab", "3 total labs");
        assertCDSPortalRow("Participants", "6 races, 3 locations", "29 total participants");
    }

    private void assertCDSPortalRow(String by, String expectedDetail, String expectedTotal)
    {
        waitForText(" " + by, 120000);
        assertTrue("'by "+by+"' search option is not present", isElementPresent(Locator.xpath("//div[starts-with(@id, 'summarydataview')]/div["+
                "./div[contains(@class, 'bycolumn')]/span[@class = 'label' and text() = ' "+by+"']]")));
        String actualDetail = getText(Locator.xpath("//div[starts-with(@id, 'summarydataview')]/div["+
                "./div[contains(@class, 'bycolumn')]/span[@class = 'label' and text() = ' "+by+"']]"+
                "/div[contains(@class, 'detailcolumn')]"));
        assertEquals("Wrong details for search by "+by+".", expectedDetail, actualDetail);
        String actualTotal = getText(Locator.xpath("//div[starts-with(@id, 'summarydataview')]/div["+
                "./div[contains(@class, 'bycolumn')]/span[@class = 'label' and text() = ' "+by+"']]"+
                "/div[contains(@class, 'totalcolumn')]"));
        assertEquals("Wrong total for search by " + by + ".", expectedTotal, actualTotal);
    }

    // Sequential calls to this should have different participant counts.
    private void assertFilterStatusPanel(String barLabel, String filteredLabel, int participantCount, int studyCount, int assayCount, int contributorCount, int antigenCount, int maxCount)
    {
        selectBars(barLabel);
        assertFilterStatusCounts(participantCount, studyCount, assayCount, contributorCount, antigenCount);
        waitForElement(Locator.xpath("//div[@class='filtermember' and contains(text(), '" + filteredLabel + "')]"), WAIT_FOR_JAVASCRIPT);
    }

    private void assertFilterStatusCounts(int participantCount, int studyCount, int assayCount, int contributorCount, int antigenCount)
    {
        waitForElement(Locator.xpath("//div[@class='highlight-value' and text()='" + participantCount + "']"), WAIT_FOR_JAVASCRIPT);
        waitForText(studyCount+(studyCount!=1?" Studies":" Study"));
        waitForText(assayCount+(assayCount!=1?" Assays":" Assay"));
        waitForText(contributorCount!=1?" Contributors":" Contributor");
        waitForText(antigenCount + (antigenCount != 1 ? " Antigens" : " Antigen"));
    }

    // Assumes you are on find-by-assay page, returns there when done
    private void assertAssayInfoPage(String assay, String contributorImg, String pocImg, String leadContributor, String pointOfContact, String details, String assayAbstract, String relatedPubs)
    {
        viewInfo(assay);
        if(contributorImg.equals(pocImg))
        {
            Locator.XPathLocator imgLoc = Locator.xpath("//img[@src='/labkey/cds/images/pictures/"+pocImg+"']");
            waitForElement(imgLoc);
            assertElementPresent(imgLoc, 2);
        }
        else
        {
            Locator.XPathLocator imgLead = Locator.xpath("//img[@src='/labkey/cds/images/pictures/"+pocImg+"']");
            Locator.XPathLocator imgContact= Locator.xpath("//img[@src='/labkey/cds/images/pictures/"+contributorImg+"']");

            waitForElement(imgLead);
            waitForElement(imgContact);

            assertElementPresent(imgLead, 1);
            assertElementPresent(imgContact, 1);
        }
        assertEquals("Incorrect Lead Contributor", leadContributor.replace("\n", ""), getText(Locator.css(".assayInfoLeadContributor")).replace("\n", ""));
        assertEquals("Incorrect Assay Point of Contact", pointOfContact.replace("\n", ""), getText(Locator.css(".assayInfoPointOfContact")).replace("\n", ""));
        assertEquals("Incorrect Assay Details", details.replace("\n", ""), getText(Locator.css(".assayInfoDetails")).replace("\n", ""));
        //assertEquals("Incorrect Description", ("Description" + pointOfContact).replace("\n", ""), getText(Locator.css(".assayInfoDescription")).replace("\n", ""));
        assertEquals("Incorrect Assay Abstract", assayAbstract.replace("\n", ""), getText(Locator.css(".assayInfoAbstract")).replace("\n", ""));
        assertEquals("Incorrect Related Publications", relatedPubs.replace("\n", ""), getText(Locator.css(".assayInfoRelatedPublications")).replace("\n", ""));
        closeInfoPage();
    }

    private void assertNounInfoPage(String noun, List<String> textToCheck)
    {
        viewInfo(noun);

        // just do simple checks for the placeholder noun pages for now, layout will change so there is no use
        // investing too much automation right now.
        for (String text : textToCheck)
        {
            waitForText(text);
            assertTextPresent(text);
        }
        closeInfoPage();
    }

    private void assertMultiAssayInfoPage()
    {
        viewMulti("Assays", "5 Assays", ".assaytitle");

        List<String> assays = Arrays.asList("ADCC-Ferrari", "Lab Results", "Luminex-Sample-LabKey", "mRNA assay", "NAb-Sample-LabKey");
        for (String assay : assays)
        {
            waitForText(assay);
            assertTextPresent(assay);
        }

        // people
        assertPeopleTip("lead-contributor", "Mark Igra", "team_Mark_Igra.jpg", "Partner");
        assertPeopleTip("contact-person", "Nick Arnold", "team_Nick_Arnold.jpg", "Developer");
        assertPeopleTip("contact-person", "Alan Vezina", "team_Alan_Vezina.jpg", "Developer");

        closeInfoPage();
    }

    private void assertMultiStudyInfoPage()
    {
        viewMulti("Studies", "3 Studies", ".studytitle");

        // just do simple checks for the placeholder noun pages for now, layout will change so there is no use
        // investing too much automation right now.
        List<String> labels = Arrays.asList("Demo Study", "Not Actually CHAVI 001", "NotRV144",
                "Igra M", "Fitzsimmons K", "Piehler B",
                "LabKey", "CHAVI", "USMHRP");
        for (String label : labels)
        {
            waitForText(label);
            assertTextPresent(label);
        }

        closeInfoPage();
    }

    private void assertMultiAntigenInfoPage()
    {
        viewMulti("Antigens", "31 Antigens", ".antigentitle");

        // just do simple checks for the placeholder noun pages for now, layout will change so there is no use
        // investing too much automation right now.
        List<String> labels = Arrays.asList("96ZM651.02", "CAP210.2.00.E8", "BaL.01",
                "Zambia", "S. Africa", "USA",
                "AF286224", "DQ435683", "AF063223");
        for (String label : labels)
        {
            waitForText(label);
            assertTextPresent(label);
        }

        closeInfoPage();
    }

    private void assertPeopleTip(String cls, String name, String img, String description)
    {
        Locator btnLocator = Locator.xpath("//a[contains(@class, '" + cls + "') and contains(text(), '" + name + "')]");
        waitForElement(btnLocator);
        mouseOver(btnLocator);

        Locator.XPathLocator imgLoc = Locator.xpath("//img[@src='/labkey/cds/images/pictures/" + img + "']");
        waitForElement(imgLoc);
        assertElementPresent(imgLoc);
        mouseOut(btnLocator);
        sleep(500);
    }

    private void toggleExplorerBar(String largeBarText)
    {
        click(Locator.xpath("//div[@class='bar large']//span[contains(@class, 'barlabel') and text()='" + largeBarText + "']//..//..//div[contains(@class, 'saecollapse')]"));
        sleep(350);
    }
}
