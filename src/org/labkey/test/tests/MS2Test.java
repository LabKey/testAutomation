
/*
 * Copyright (c) 2007-2013 LabKey Corporation
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
import org.labkey.test.SortDirection;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.ms2.MS2TestBase;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MS2Test extends MS2TestBase
{

    public static final String DEFAULT_EXPERIMENT = "Default Experiment";

    protected void doTestSteps()
    {
        goTestIt("DRT1","DRT2");
        verifyGroupAudit();
    }

    //verify audit trail registers runs added to or removed from groups.
    private void verifyGroupAudit()
    {
        List<Map<String, Object>> rows = executeSelectRowCommand("auditLog", "ExperimentAuditEvent").getRows();
        Assert.assertEquals("Unexpected number of audit rows", 10, rows.size());
        int addedCount = 0;
        int removedCount = 0;
        for(Map row : rows)
        {
            if(((String)row.get("Comment")).contains("was added to the run group"))
                addedCount++;
            else if(((String)row.get("Comment")).contains("was removed from the run group"))
                removedCount++;
        }

        Assert.assertEquals(8, addedCount);
        Assert.assertEquals(1, removedCount);
        //Issue 16265: need to filter group created during export from RunGroupMap query
        //add test for this when fixed
    }

    protected static final String VIEW2 = "proteinView";
    protected static final String VIEW3 = "proteinGroupView";
    protected static final String VIEW4 = "queryView1";
    protected static final String VIEW5 = "queryView2";
    protected static final String PEPTIDE1 = "K.GSDSLSDGPACKR.S";
    protected static final String PEPTIDE2 = "R.TIDPVIAR.K";
    protected static final String PEPTIDE3 = "K.HVSGKIIGFFY.-";
    protected static final String PEPTIDE4 = "R.ISSTKMDGIGPK.K";
    protected static final String SEARCH_TYPE = "xtandem";
    protected static final String SEARCH_NAME3 = "X! Tandem";
    protected static final String ENZYME = "trypsin";
    protected static final String MASS_SPEC = "ThermoFinnigan";
    protected static final String RUN_GROUP1_NAME1 = "Test Run Group 1";
    //Issue #16260, "Exception when including run group with tricky characters in name," has been updated
    //reactivate tricky_char_names when this is fixed
    protected static final String RUN_GROUP1_NAME2 = "Test Run Group 1 New Name";// + TRICKY_CHARACTERS;
    protected static final String RUN_GROUP1_CONTACT = "Test Contact";
    protected static final String RUN_GROUP1_DESCRIPTION = "This is a description";
    protected static final String RUN_GROUP1_HYPOTHESIS = "I think this is happening";
    protected static final String RUN_GROUP1_COMMENTS = "Here are comments.";
    protected static final String RUN_GROUP2_NAME = "Test Run Group 2";
    protected static final String RUN_GROUP3_NAME = "Test Run Group 3";

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        cleanPipe(SEARCH_TYPE);
        deleteProject(getProjectName(), afterTest);
    }

    protected void goTestIt(String testFile1, String testFile2)
    {
        log("Verifying that pipeline files were cleaned up properly");
        File test2 = new File(PIPELINE_PATH + "/bov_sample/" + SEARCH_TYPE + "/test2");
        if (test2.exists())
            Assert.fail("Pipeline files were not cleaned up; test2("+test2.toString()+") directory still exists");

        super.doTestSteps();

        log("Upload existing MS2 data.");
        clickAndWait(Locator.linkWithText(FOLDER_NAME));
        clickButton("Process and Import Data");
        _extHelper.selectFileBrowserItem("bov_sample/" + SEARCH_TYPE + "/" + testFile1 + "/" + SAMPLE_BASE_NAME + ".search.xar.xml");

        selectImportDataAction("Import Experiment");

        log("Going to the list of all pipeline jobs");
        clickAndWait(Locator.linkWithText("All"));

        log("Verify upload started.");
        assertTextPresent(SAMPLE_BASE_NAME + ".search.xar.xml");
        int seconds = 0;
        while (countLinksWithText("COMPLETE") < 1 && seconds++ < MAX_WAIT_SECONDS)
        {
            log("Waiting upload to complete");
            if (countLinksWithText("ERROR") > 0)
            {
                Assert.fail("Job in ERROR state found in the list");
            }
            sleep(1000);
            refresh();
        }
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        assertElementPresent(Locator.linkWithSpan("MS2 Runs"));
        assertLinkPresentContainingText(SAMPLE_BASE_NAME);

        log("Verify run view.");
        clickLinkWithImage(getContextPath() + "/MS2/images/runIcon.gif");

        // Make sure we're not using a custom default view for the current user
        selectOptionByText("viewParams", "<Standard View>");
        clickButton("Go");

        assertTextPresent(SEARCH_NAME3);
        assertTextPresent("databases");
        //Different cases used with different search engines.
        if( !isTextPresent(ENZYME))
            assertTextPresent(ENZYME);
        assertTextPresent(MASS_SPEC);
        assertLinkPresentWithText(PEPTIDE1);

        log("Test Navigation Bar for Run");
        log("Test Show Modifications");
        click(Locator.linkWithText("Show Modifications"));
        // Wait for tooltip to show up
        sleep(2000);
        assertTextPresent("Variable");
        assertTextPresent("E^");
        assertTextPresent("Q^");

        log("Test Show Peptide Prophet Details");
        pushLocation();
        beginAt(getLinkHref("Show Peptide Prophet Details", "MS2", "/" + PROJECT_NAME + "/" + FOLDER_NAME));
        assertTextPresent("Minimum probability");
        assertTextPresent("Error rate");
        assertTextPresent("Sensitivity");
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 1+ Distribution"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Sensitivity Plot"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 1+ Cumulative Distribution"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 3+ Distribution"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 3+ Cumulative Observed vs. Model"));
        popLocation();

        log("Test Show Protein Prophet Details");
        pushLocation();
        beginAt(getLinkHref("Show Protein Prophet Details", "MS2", "/" + PROJECT_NAME + "/" + FOLDER_NAME));
        assertTextPresent("Minimum probability");
        assertTextPresent("Error rate");
        assertTextPresent("Sensitivity");
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Sensitivity Plot"));
        popLocation();

        // Make sure we're not using a custom default view for the current user
        selectOptionByText("viewParams", "<Standard View>");
        clickButton("Go");
        selectOptionByText("grouping", "Peptides (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));

        log("Test export selected");
        DataRegionTable peptidesTable = new DataRegionTable("MS2Peptides", this);
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        peptidesTable.checkCheckbox(0);
        clickMenuButton("Export Selected", "TSV");
        assertTextPresent("K.LLASMLAK.A");
        assertTextNotPresent("R.Q^YALHVDGVGTK.A");
        assertTextPresent("\n", 2, true);
        popLocation();
        pushLocation();
        peptidesTable.checkAllOnPage();
        clickMenuButton("Export Selected", "AMT");
        assertTextPresent("\n", 60, true);
        assertTextPresent("Run");
        assertTextPresent("CalcMHPlus");
        assertTextPresent("RetTime");
        assertTextPresent("Fraction");
        assertTextPresent("PepProphet");
        assertTextPresent("Peptide");
        assertTextPresent("1373.4690");
        assertTextPresent("846.5120");
        assertTextPresent("K.LLASMLAK.A");
        assertTextPresent("K.EEEESDEDMGFG.-");
        popLocation();

        log("Test export selected expects at least one selected");
        peptidesTable.uncheckAllOnPage();
        clickMenuButtonAndContinue("Export Selected", "AMT");
        assertAlert("Please select one or more peptides.");

        log("Test sort");
        pushLocation();
        setSort("MS2Peptides", "Hyper", SortDirection.DESC);
        assertTextPresent("Hyper DESC");
        assertTextBefore("R.Q^YALHVDGVGTK.A", "K.LLASMLAK.A");
        assertTextBefore("14.9", "13.0");
        setSort("MS2Peptides", "Charge", SortDirection.ASC);
        assertTextPresent("Charge ASC, Hyper DESC");
        assertTextBefore("K.KLHQK.L", "R.GGNEESTK.T");
        assertTextBefore("1272.5700", "1425.6860");
        setSort("MS2Peptides", "Charge", SortDirection.DESC);
        assertTextPresent("Charge DESC, Hyper DESC");
        setSort("MS2Peptides", "Scan", SortDirection.ASC);
        assertTextPresent("Scan ASC, Charge DESC, Hyper DESC");
        assertTextBefore("R.Q^YALHVDGVGTK.A", "K.LLASMLAK.A");
        assertTextBefore("R.SLADVARR.R", "-.MELFSNELLYK.T");

        log("Test export");
        pushLocation();
        clickMenuButton("Export All", "TSV");
        assertTextPresent("Scan");
        assertTextPresent("IonPercent");
        assertTextPresent("Protein");
        assertTextBefore("R.Q^YALHVDGVGTK.A", "K.LLASMLAK.A");
        assertTextBefore("R.SLADVARR.R", "-.MELFSNELLYK.T");
        assertTextPresent("gi|4689022|ribosomal_protein_");
        assertTextPresent("1373.4690");
        assertTextPresent("\n", 58, true);
        popLocation();
        clickMenuButton("Export All", "AMT");
        assertTextBefore("R.Q^YALHVDGVGTK.A", "K.LLASMLAK.A");
        assertTextPresent("Run");
        assertTextPresent("Peptide");
        assertTextPresent("\n", 60, true);
        popLocation();

        log("Test Scan, Z, Hyper, Next, B, Y, and Expect filters");
        pushLocation();
        setFilter("MS2Peptides", "Scan", "Is Greater Than", "6", "Is Less Than or Equal To", "100");
        assertTextNotPresent("K.FANIGDVIVASVK.Q");
        assertTextPresent("-.MELFSNELLYK.T");
        assertTextNotPresent("K.TESGYGSESSLR.R");
        assertTextPresent("R.EADKVLVQMPSGK.Q");

        log("Test sort with filters");
        setSort("MS2Peptides", "Scan", SortDirection.DESC);
        assertTextPresent("Scan DESC");

        log("Save view for later");
        clickButton("Save View");
        setFormElement("name", VIEW);
        clickButton("Save View");

        log("Continue with filters");
        setFilter("MS2Peptides", "Charge", "Equals", "2");
        assertTextNotPresent("R.APPSTQESESPR.Q");
        assertTextPresent("R.TIDPVIAR.K");
        setFilter("MS2Peptides", "Hyper", "Is Greater Than or Equal To", "14.6");
        assertTextNotPresent("K.RLLRSMVK.F");
        assertTextPresent("R.AEIDYANK.T");
        setFilter("MS2Peptides", "Next", "Does Not Equal", "9.5");
        assertTextNotPresent("R.AEIDYANK.T");
        setFilter("MS2Peptides", "B", "Is Less Than", "11.6");
        assertTextNotPresent("R.TIDPVIAR.K");
        setFilter("MS2Peptides", "Y", "Is Less Than", "11.3");
        assertTextNotPresent("R.QPNSGPYKK.Q");
        setFilter("MS2Peptides", "Expect", "Is Greater Than", "1.2");
        assertTextNotPresent("K.FVKKSNDVR.L");
        assertTextPresent("(Scan > 6) AND (Scan <= 100) AND (Charge = 2) AND (Hyper >= 14.6) AND (Next <> 9.5) AND (B < 11.6) AND (Y < 11.3) AND (Expect > 1.2)");

        log("Test spectrum page");
        assertLinkPresentWithText("R.LSSMRDSR.S");
        String address = getAttribute(Locator.linkWithText("R.LSSMRDSR.S"), "href");
        // TODO - Reenable after upgrading TeamCity Firefox installations on Linux agents
        // https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=12994
//        pushLocation();
//        beginAt(address);
//
//        log("Verify spectrum page.");
//        assertTextPresent("R.LSSMRDSR.S");
//        assertTextPresent("gi|29650192|ribosomal_protein");
//        assertTextPresent("56");
//        assertTextPresent("0.000");
//        clickAndWait(Locator.linkWithText("Next"));
//        assertTextPresent("R.GGNEESTK.T");
//        assertTextPresent("gi|442754|A_Chain_A,_Superoxi");
//
//        log("Return to run.");
//        popLocation();

        log("Verify still filtered.");
        assertTextPresent("(Scan > 6) AND (Scan <= 100) AND (Charge = 2) AND (Hyper >= 14.6) AND (Next <> 9.5) AND (B < 11.6) AND (Y < 11.3) AND (Expect > 1.2)");

        log("Test pick peptide columns");
        clickButton("Pick Peptide Columns");
        clickButton("Pick", 0);
        clickButton("Pick Columns");
        assertTextPresent("RetTime");

        log("Test export");
        pushLocation();
        clickMenuButton("Export All", "TSV");
        assertTextPresent("Scan");
        assertTextPresent("Run Description");
        assertTextPresent("Fraction Name");
        assertTextPresent("dMassPPM");
        assertTextPresent("PPErrorRate");
        assertTextPresent("SeqId");
        assertTextBefore("R.LSSMRDSR.S", "R.GGNEESTK.T");
        assertTextPresent("56");
        assertTextPresent("gi|442754|A_Chain_A,_Superoxi");
        assertTextNotPresent("K.FVKKSNDVR.L");
        assertTextPresent("\n", 3, true);
        popLocation();
        clickMenuButton("Export All", "AMT");
        assertTextPresent("Run");
        assertTextPresent("Peptide");
        assertTextBefore("R.LSSMRDSR.S", "R.GGNEESTK.T");
        assertTextNotPresent("K.FVKKSNDVR.L");
        assertTextPresent("\n", 5, true);
        popLocation();

        log("Make saved view for Protein Group for Comparison");
        pushLocation();
        setFilter("MS2Peptides", "DeltaMass", "Is Greater Than", "0");
        selectOptionByText("grouping", "Protein (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        setFilter("MS2Proteins", "SequenceMass", "Is Greater Than", "20000");
        log("Save view for later");
        clickButton("Save View");
        setFormElement("name", VIEW2);
        clickButton("Save View");

        log("Test using saved view");
        popLocation();
        pushLocation();
        selectOptionByText("viewParams", VIEW);
        clickButton("Go");

        log("Test hyper charge filters too");
        setFormElement("Charge1", "11");
        setFormElement("Charge2", "13");
        setFormElement("Charge3", "14");
        clickAndWait(Locator.id("AddChargeScoreFilterButton"));
        assertTextPresent("R.KVTTGR.A");
        assertTextNotPresent("K.KLHQK.L");
        assertTextNotPresent("K.MEVDQLK.K");
        assertTextNotPresent("-.MELFSNELLYK.T");

        log("Test Protein View and if viewParams hold");
        selectOptionByText("grouping", "Protein (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        assertTextPresent("Description");
        assertTextPresent("Coverage");
        assertTextPresent("Best Gene Name");
        assertTextPresent("(Scan > 6) AND (Scan <= 100) AND (+1:Hyper >= 11.0, +2:Hyper >= 13.0, +3:Hyper >= 14.0)");
        assertTextPresent("Scan DESC");
        assertTextNotPresent("K.LLASMLAK.A");

        log("Test filters in Protein View");
        setFilter("MS2Proteins", "SequenceMass", "Is Greater Than", "17000", "Is Less Than", "50000");
        assertTextNotPresent("gi|15925226|30S_ribosomal_pro");
        assertTextNotPresent("gi|19703691|Nicotinate_phosph");
        setFilter("MS2Proteins", "Description", "Does Not Contain", "Uncharacterized conserved protein");
        assertTextNotPresent("Uncharacterized conserved protein [Thermoplasma acidophilum]");
        assertTextPresent("(SequenceMass > 17000) AND (SequenceMass < 50000) AND (Description DOES NOT CONTAIN Uncharacterized conserved protein)");

        log("Test Single Protein View");
        assertLinkPresentContainingText("gi|13541159|30S_ribosomal_pro");
        String href = getAttribute(Locator.linkContainingText("gi|13541159|30S_ribosomal_pro"), "href");
        pushLocation();
        beginAt(href);

        log("Verify peptides.");
        assertTextPresent("gi|13541159|ref|NP_110847.1|");
        assertTextPresent("(Scan > 6) AND (Scan <= 100) AND (+1:Hyper >= 11.0, +2:Hyper >= 13.0, +3:Hyper >= 14.0)");

        log("Return to run.");
        popLocation();

        log("Test sorting in Protein View");
        setSort("MS2Proteins", "SequenceMass", SortDirection.ASC);
        assertTextPresent("SequenceMass ASC");
        assertTextBefore("gi|15668549|LSU_ribosomal_pro", "gi|14318169|AF379640_1_riboso");

        log("Test export Protein View");
        pushLocation();
        clickMenuButton("Export All", "TSV");
        assertTextPresent("Protein");
        assertTextPresent("Description");
        assertTextBefore("gi|15668549|LSU_ribosomal_pro", "gi|14318169|AF379640_1_riboso");
        assertTextPresent("gi|13541159|30S_ribosomal_pro");
        assertTextPresent("ribosomal protein S19 [Thermoplasma volcanium]");
        assertTextPresent("gi|29650192|ribosomal_protein");
        assertTextPresent("ribosomal protein S6 [Anopheles stephensi]");
        assertTextPresent("\n", 18, true);
        popLocation();

        log("Test export expanded view");
        selectOptionByText("grouping", "Protein (Legacy)");
        checkCheckbox("expanded");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        pushLocation();
        clickMenuButton("Export All", "TSV");
        assertTextPresent("Protein");
        assertTextPresent("IonPercent");
        assertTextPresent("Protein");
        assertTextBefore("gi|15668549|LSU_ribosomal_pro", "gi|14318169|AF379640_1_riboso");
        assertTextPresent("gi|13541159|30S_ribosomal_pro");
        assertTextPresent("R.KVTTGR.A");
        assertTextPresent("gi|29650192|ribosomal_protein");
        assertTextPresent("R.E^PVSPWGTPAKGYR.T");
        assertTextPresent("\n", 18, true);
        popLocation();
        clickMenuButton("Export All", "AMT");
        assertTextPresent("Run");
        assertTextPresent("Peptide");
        assertTextBefore("K.TKDYEGMQVPVK.V", "R.RDYLHYLPKYNR.F");
        assertTextNotPresent("K.LLASMLAK.A");
        assertTextNotPresent("R.KKVAIVPEPLR.K");
        assertTextPresent("\n", 20, true);
        popLocation();

        log("Test Protein Prophet");
        pushLocation();
        selectOptionByText("grouping", "ProteinProphet (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        assertTextPresent("Group");
        assertTextPresent("Prob");
        assertTextPresent("Spectrum Ids");
        assertTextPresent("gi|4689022|ribosomal_protein_");
        assertTextPresent("14.06%");
        assertTextPresent("gi|4883902|APETALA3_homolog_R");

        log("Test Protein Prophet with filters");
        selectOptionByText("viewParams", VIEW);
        clickButton("Go");
        selectOptionByText("grouping", "ProteinProphet (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        assertTextNotPresent("gi|4689022|ribosomal_protein_");
        assertTextPresent("gi|16078254|similar_to_riboso");
        assertTextPresent("(Scan > 6) AND (Scan <= 100)");
        assertTextPresent("Scan DESC");
        setFilter("ProteinGroupsWithQuantitation", "GroupProbability", "Is Greater Than", "0.7");
        assertTextNotPresent("gi|30089158|low_density_lipop");

        log("Save view for later");
        clickButton("Save View");
        setFormElement("name", VIEW3);
        clickButton("Save View");

        setFilter("ProteinGroupsWithQuantitation", "PercentCoverage", "Is Not Blank");
        assertTextNotPresent("gi|13442951|MAIL");
        assertTextPresent("(GroupProbability > 0.7) AND (PercentCoverage IS NOT NULL)");

        log("Test export");
        pushLocation();
        clickMenuButton("Export All", "AMT");
        assertTextPresent("Run");
        assertTextPresent("Peptide");
        assertTextBefore("K.MLNMAKSKMHK.M", "R.E^VNAEDLAPGEPGR.L");
        assertTextPresent("1318.6790");
        assertTextPresent("1435.6810");
        assertTextNotPresent("gi|27684893|similar_to_60S_RI");
        assertTextPresent("\n", 5, true);

        log("Test export selected in expanded view with different protein and peptide columns and sorting");
        popLocation();
        log("Test sorting in Protein Prophet");
        setSort("ProteinGroupsWithQuantitation", "GroupProbability", SortDirection.ASC);
        assertTextPresent("GroupProbability ASC");
        assertTextBefore("gi|548772|RL4_HALHA_50S_RIBOS", "gi|23619029|60S_ribosomal_pro");
        clickButton("Pick Peptide Columns");
        clickButton("Pick", 0);
        clickButton("Pick Columns");
        clickButton("Pick Peptide Columns");
        clickButton("Pick", 0);
        clickButton("Pick Columns");
        selectOptionByText("grouping", "ProteinProphet (Legacy)");
        checkCheckbox("expanded");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        pushLocation();
        checkCheckbox(Locator.raw("document.forms['ProteinGroupsWithQuantitation'].elements['.select'][0]"));
        clickMenuButton("Export Selected", "TSV");
        assertTextPresent("Group");
        assertTextPresent("PP Unique");
        assertTextPresent("Run Description");
        assertTextPresent("IonPercent");
        assertTextPresent("ObsMHPlus");
        assertTextPresent("Peptide");
        assertTextPresent("SeqId");
        assertTextPresent("gi|548772|RL4_HALHA_50S_RIBOS");
        assertTextPresent("EVNAEDLAPGEPGR");
        assertTextNotPresent("gi|23619029|60S_ribosomal_pro");
        assertTextPresent("\n", 2, true);
        popLocation();

        log("Make sure sort is exported correctly too");
        clickMenuButton("Export All", "TSV");
        assertTextBefore("gi|548772|RL4_HALHA_50S_RIBOS", "gi|23619029|60S_ribosomal_pro");
        assertTextPresent("MLNMAKSKMHK");
        assertTextPresent("\n", 3, true);
        popLocation();

        log("Create saved view to test query groupings");
        selectOptionByText("grouping", "Peptides (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        selectOptionByText("views", VIEW);
        clickButton("Go");

        log("Test Query - Peptides Grouping");
        selectOptionByText("grouping", "Standard");
        checkCheckbox("expanded");
        clickAndWait(Locator.id("viewTypeSubmitButton"));

        log("Check that saved view is working");
        assertTextNotPresent("K.KTEENYTLVFIVDVK.A");
        assertTextBefore("R.EADKVLVQMPSGK.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");

        log("Test adding a sort and a filter");
        setFilter("MS2Peptides", "Hyper", "Is Greater Than", "10.6");
        assertTextNotPresent("K.RFSGTVKLK.Y");
        setSort("MS2Peptides", "Next", SortDirection.ASC);
        assertTextBefore("K.ERQPPPR.L", "K.KLHQK.L");

        log("Test customize view");
        clickButton("Clear All");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewSort("Charge", "Z", "Descending");
        _customizeViewsHelper.addCustomizeViewSort("Mass", "CalcMH+", "Descending");
        _customizeViewsHelper.addCustomizeViewFilter("DeltaMass", "dMass", "Is Less Than", "0");
        _customizeViewsHelper.addCustomizeViewFilter("RowId", "Row Id", "Is Greater Than", "3");
        _customizeViewsHelper.addCustomizeViewColumn("NextAA", "Next AA");
        _customizeViewsHelper.removeCustomizeViewColumn("Expect");
        _customizeViewsHelper.removeCustomizeViewColumn("ProteinHits");
        _customizeViewsHelper.saveCustomView(VIEW4);

        log("Test that the sorting and filtering worked and that the columns were changed");
        assertTextNotPresent("K.LLASMLAK.A");
        assertTextNotPresent("R.GGNEESTK.T");
        assertTextPresent("Next AA");
        assertTextNotPresent("Orig Score");
        assertTextPresent("K.TESGYGSESSLR.R");
        assertTextBefore(PEPTIDE1, PEPTIDE2);
        assertTextBefore(PEPTIDE3, PEPTIDE4);
        assertTextPresent("Protein");
        assertTextNotPresent("Expect");
        assertTextNotPresent("SeqHits");
        assertTextPresent("gi|27805893|guanine_nucleotid");

        log("Test changing order of sorts and columns");
        _customizeViewsHelper.openCustomizeViewPanel();
        selenium.windowMaximize();
        sleep(500);
        _customizeViewsHelper.moveCustomizeViewSort("Charge", false);
        // XXX: selenium test can't move columns that require scrolling the column list
        //_customizeViewsHelper.moveCustomizeViewColumn(this, "Peptide", false);
        _customizeViewsHelper.applyCustomView();

        assertTextBefore("K.TESGYGSESSLR.R", "K.HVSGKIIGFFY.-");
        //assertTextBefore("gi|30519530|A38R_protein", "K.ISNFIANNDCRYYIDAEHQKIISDEINR.Q");

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.moveCustomizeViewSort("Charge", true);
        // XXX: selenium test can't move columns that require scrolling the column list
        //_customizeViewsHelper.moveCustomizeViewColumn(this, "Peptide", true);
        _customizeViewsHelper.applyCustomView();

        log("Test Ignore View Filter");
        clickMenuButton("Views", "Apply View Filter");
        assertTextPresent("K.LLASMLAK.A");
        assertTextPresent("R.GGNEESTK.T");
        assertTextPresent("Next AA");
        assertTextBefore(PEPTIDE2, PEPTIDE1);
        assertTextBefore(PEPTIDE4, PEPTIDE3);

        log("Test Apply View Filter");
        clickMenuButton("Views", "Apply View Filter");
        assertTextNotPresent("K.LLASMLAK.A");
        assertTextNotPresent("R.GGNEESTK.T");
        assertTextPresent("Next AA");
        assertTextBefore(PEPTIDE1, PEPTIDE2);
        assertTextBefore(PEPTIDE3, PEPTIDE4);
        assertTextNotPresent("Expect");
        assertTextNotPresent("SeqHits");

        log("Test exporting Query - Peptides grouping");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        log("Test exporting in TSV");
        clickMenuButton("Export All", "TSV");
        assertTextPresent("Scan");
        assertTextPresent("dMass");
        assertTextNotPresent("K.LLASMLAK.A");
        assertTextNotPresent("R.GGNEESTK.T");
        assertTextPresent("Next AA");
        assertTextBefore(PEPTIDE1, PEPTIDE2);
        assertTextBefore(PEPTIDE3, PEPTIDE4);
        assertTextNotPresent("Expect");
        assertTextNotPresent("SeqHits");
        assertTextPresent("Protein");
        assertTextPresent("gi|27805893|guanine_nucleotid");
        assertTextPresent("\n", 24, true);
        popLocation();
        pushLocation();
        log("Test exporting in AMT");
        clickMenuButton("Export All", "AMT");
        assertTextPresent("Run");
        assertTextPresent("Peptide");
        assertTextNotPresent("K.LLASMLAK.A");
        assertTextNotPresent("R.GGNEESTK.T");
        assertTextBefore(PEPTIDE1, PEPTIDE2);
        assertTextBefore(PEPTIDE3, PEPTIDE4);
        assertTextNotPresent("Protein");
        assertTextPresent("RetTime");
        assertTextPresent("\n", 26, true);
        popLocation();

        log("Test exporting selected in TSV");
        pushLocation();
        peptidesTable.uncheckAllOnPage();
        peptidesTable.checkCheckbox(0);
        peptidesTable.checkCheckbox(1);
        clickMenuButton("Export Selected", "TSV");
        assertTextNotPresent("Expect");
        assertTextNotPresent("SeqHits");
        assertTextPresent("Next AA");
        assertTextBefore("K.ISNFIANNDCRYYIDAEHQKIISDEINR.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
        assertTextPresent("gi|25027045|putative_50S_ribo");
        assertTextPresent("\n", 3, true);
        popLocation();

        log("Test exporting selected in AMT");
        pushLocation();
        peptidesTable.uncheckAllOnPage();
        peptidesTable.checkCheckbox(0);
        peptidesTable.checkCheckbox(1);
        clickMenuButton("Export Selected", "AMT");
        assertTextPresent("Peptide");
        assertTextNotPresent("Next AA");
        assertTextBefore("K.ISNFIANNDCRYYIDAEHQKIISDEINR.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
        assertTextPresent("\n", 5, true);
        popLocation();

        log("Test default view");
        clickMenuButton("Views", "default");
        assertTextPresent("K.LLASMLAK.A");
        assertTextPresent("R.GGNEESTK.T");
        assertTextNotPresent("Next AA");
        assertTextBefore(PEPTIDE2, PEPTIDE1);
        assertTextBefore(PEPTIDE4, PEPTIDE3);
        assertTextPresent("Expect");
        assertTextPresent("SeqHits");

        log("Test load saved view");
        clickMenuButton("Views", VIEW4);
        assertTextNotPresent("R.GGNEESTK.T");
        assertTextBefore(PEPTIDE1, PEPTIDE2);
        assertTextBefore(PEPTIDE3, PEPTIDE4);
        assertTextNotPresent("Expect");
        assertTextNotPresent("SeqHits");

        log("Test changing default view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.clearCustomizeViewFilters();
        _customizeViewsHelper.clearCustomizeViewSorts();
        _customizeViewsHelper.addCustomizeViewSort("DeltaMass", "dMass", "Ascending");
        _customizeViewsHelper.addCustomizeViewFilter("Mass", "CalcMH+", "Is Greater Than", "1000");
        _customizeViewsHelper.addCustomizeViewColumn("Fraction");
        _customizeViewsHelper.removeCustomizeViewColumn("IonPercent");
        _customizeViewsHelper.saveCustomView("");
        clickMenuButton("Views", "default");
        assertTextNotPresent("K.LLASMLAK.A");
        assertTextPresent("Fraction");
        assertTextBefore("K.TKDYEGMQVPVK.V", "R.LGARRVSPVR.A");
        assertTextNotPresent("Ion%");

        log("Test restoring default view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.revertUnsavedView();
        assertTextPresent("K.LLASMLAK.A");
        assertTextNotPresent("Fraction");
        assertTextBefore("R.LGARRVSPVR.A", "K.TKDYEGMQVPVK.V");
        assertTextPresent("Ion%");

        log("Test delete view");
        clickMenuButton("Views", VIEW4);
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.deleteView();
        assertTextPresent("K.LLASMLAK.A");
        assertTextPresent("R.GGNEESTK.T");
        assertTextNotPresent("Next AA");
        assertTextBefore(PEPTIDE2, PEPTIDE1);
        assertTextBefore(PEPTIDE4, PEPTIDE3);
        assertTextPresent("Expect");
        assertTextPresent("SeqHits");

        log("Test Protein Prophet view in Query - Peptides grouping");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("ProteinProphetData/ProteinGroupId/Group", "Group");
        _customizeViewsHelper.addCustomizeViewColumn("ProteinProphetData/ProteinGroupId/TotalNumberPeptides", "Peptides");
        _customizeViewsHelper.addCustomizeViewColumn("ProteinProphetData/ProteinGroupId/GroupProbability", "Prob");
        _customizeViewsHelper.addCustomizeViewColumn("ProteinProphetData/ProteinGroupId/BestName", "Best Name");
        _customizeViewsHelper.removeCustomizeViewColumn("Mass");
        _customizeViewsHelper.addCustomizeViewFilter("DeltaMass", "dMass", "Is Greater Than", "0");
        _customizeViewsHelper.addCustomizeViewFilter("ProteinProphetData/ProteinGroupId/GroupProbability", "Prob", "Is Greater Than", "0.7");
        _customizeViewsHelper.addCustomizeViewSort("ProteinProphetData/ProteinGroupId/GroupProbability", "Prob", "Ascending");
        _customizeViewsHelper.saveCustomView(VIEW4);

        log("Test that Protein Prophet view is displayed and that it sorts and filters correctly");
        assertTextPresent("Group");
        assertTextPresent("Peptides");
        assertTextPresent("Prob");
        assertTextPresent("gi|4689022|");
        assertTextPresent("gi|23619029|ref|NP_704991.1|");
        assertTextPresent("PepProphet");
        assertTextPresent("Scan");
        assertTextPresent("K.MLNMAKSKMHK.M");
        assertTextNotPresent("CalcMH+");
        assertTextNotPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextNotPresent("K.GSDSLSDGPACKR.S");
        assertTextBefore("gi|16078254|ref|NP_389071.1|", "gi|18311790|ref|NP_558457.1|");

        log("Test exporting from Protein Prophet view");
        pushLocation();
        log("Test exporting in TSV");
        clickMenuButton("Export All", "TSV");
        assertTextPresent("Group");
        assertTextPresent("Peptides");
        assertTextPresent("Prob");
        assertTextPresent("gi|4689022|");
        assertTextPresent("gi|23619029|ref|NP_704991.1|");
        assertTextPresent("PepProphet");
        assertTextPresent("Scan");
        assertTextPresent("K.MLNMAKSKMHK.M");
        assertTextNotPresent("CalcMH+");
        assertTextNotPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextNotPresent("K.GSDSLSDGPACKR.S");
        assertTextBefore("gi|16078254|ref|NP_389071.1|", "gi|18311790|ref|NP_558457.1|");
        assertTextPresent("\n", 6, true);
        popLocation();
        pushLocation();
        log("Test exporting in AMT");
        clickMenuButton("Export All", "AMT");
        assertTextPresent("Run");
        assertTextPresent("Peptide");
        assertTextNotPresent("Best Name");
        assertTextPresent("RetTime");
        assertTextNotPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextNotPresent("K.GSDSLSDGPACKR.S");
        assertTextBefore("R.KKVAIVPEPLR.K", "R.Q^YALHVDGVGTK.A");
        assertTextPresent("\n", 8, true);
        popLocation();

        log("Test Query - Proteins Grouping");
        selectOptionByText("grouping", "Protein Groups");
        checkCheckbox("expanded");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        assertTextPresent("Protein");
        assertTextPresent("Description");
        assertTextPresent("Group");
        assertTextPresent("APETALA3 homolog RbAP3-2 [Ranunculus bulbosus]");
        assertTextPresent("gi|4883902|APETALA3_homolog_R");

        log("Test customize view");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeCustomizeViewColumn("UniquePeptidesCount");
        _customizeViewsHelper.addCustomizeViewColumn("Proteins/Protein/ProtSequence", "Protein Sequence");
        _customizeViewsHelper.addCustomizeViewFilter("GroupProbability", "Prob", "Is Greater Than", "0.7");
        _customizeViewsHelper.addCustomizeViewSort("ErrorRate", "Error", "Descending");
        _customizeViewsHelper.saveCustomView(VIEW4);

        log("Test that sorting, filtering, and columns are correct");
        assertTextNotPresent("Unique");
        assertTextPresent("Sequence");
        assertTextPresent("MSASELATSYSALILADEGIEIKSDKLLSLTKAANVDVEPIWATIFAKALEGKDLKELLLNIGSGAGAAPVAGGAGAPAAADGERPAEEKEEAKEEEESDEDMGFG");
        assertTextNotPresent("gi|30089158|low_density_lipop");
        assertTextBefore("gi|16078254|similar_to_riboso", "gi|18311790|phosphoribosylfor");

        log("Test exporting in Query - Protein View");
        pushLocation();
        log("Test exporting in TSV");
        clickMenuButton("Export All", "TSV");
        assertTextNotPresent("Unique");
        assertTextPresent("Sequence");
        assertTextPresent("MSASELATSYSALILADEGIEIKSDKLLSLTKAANVDVEPIWATIFAKALEGKDLKELLLNIGSGAGAAPVAGGAGAPAAADGERPAEEKEEAKEEEESDEDMGFG");
        assertTextNotPresent("gi|30089158|low_density_lipop");
        assertTextBefore("gi|16078254|similar_to_riboso", "gi|18311790|phosphoribosylfor");
        assertTextPresent("\n", 8);
        popLocation();

        log("Test exporting selected and non-expanded view");
        DataRegionTable proteinGroupsTable = new DataRegionTable("ProteinGroups", this);
        uncheckCheckbox("expanded");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        pushLocation();
        proteinGroupsTable.uncheckAllOnPage();
        proteinGroupsTable.checkCheckbox(0);
        proteinGroupsTable.checkCheckbox(1);
        clickMenuButton("Export Selected", "TSV");
        assertTextBefore("0.74", "0.78");
        assertTextPresent("\n", 3);
        popLocation();
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

        log("Upload second MS2 Run");
        clickButton("Process and Import Data");
        _extHelper.selectFileBrowserItem("bov_sample/" + SEARCH_TYPE + "/" + testFile2 + "/" + SAMPLE_BASE_NAME + ".search.xar.xml");

        selectImportDataAction("Import Experiment");

        log("Verify upload finished.");
        seconds = 0;
        clickAndWait(Locator.linkWithText("Data Pipeline"));
        while (countLinksWithText("COMPLETE") < 2 && seconds++ < MAX_WAIT_SECONDS)
        {
            log("Waiting upload to complete");
            if (countLinksWithText("ERROR") > 0)
            {
                Assert.fail("Job in ERROR state found in the list");
            }
            sleep(1000);
            refresh();
        }
        clickAndWait(Locator.linkWithText(FOLDER_NAME));

        log("Test export 2 runs together");
        pushLocation();
        DataRegionTable searchRunsTable = new DataRegionTable("MS2SearchRuns", this);
        searchRunsTable.checkAllOnPage();
        clickButton("MS2 Export");
        checkRadioButton("exportFormat", "TSV");
        selectOptionByText("viewParams", VIEW);
        clickButton("Export");
        assertTextPresent("Scan");
        assertTextPresent("Protein");
        assertTextBefore("K.QLDSIHVTILHK.E", "R.GRRNGPRPVHPTSHNR.Q");
        assertTextBefore("R.EADKVLVQMPSGK.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
        assertTextPresent("gi|5002198|AF143203_1_interle");
        assertTextPresent("1386.6970");
        assertTextPresent("gi|6049221|AF144467_1_nonstru");
        assertTextPresent("\n", 86, true);
        popLocation();
        pushLocation();

        searchRunsTable.checkAllOnPage();
        clickButton("MS2 Export");
        checkRadioButton("exportFormat", "AMT");
        selectOptionByText("viewParams", VIEW);
        clickButton("Export");
        assertTextPresent("Run");
        assertTextPresent("Peptide");
        assertTextBefore("K.QLDSIHVTILHK.E", "R.GRRNGPRPVHPTSHNR.Q");
        assertTextBefore("R.EADKVLVQMPSGK.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
        assertTextPresent("-.MELFSNELLYK.T");
        assertTextPresent("1386.6970");
        assertTextPresent("\n", 89, true);
        popLocation();

        if ("DRT2".equals(testFile1) || "DRT2".equals(testFile2))
        {
            clickAndWait(Locator.linkWithText("drt/CAexample_mini (DRT2)"));

            selectOptionByText("viewParams", "<Standard View>");
            clickButton("Go");

            log("Test peptide filtering on protein page");
            assertLinkPresentWithText("gi|15645924|ribosomal_protein");
            address = getAttribute(Locator.linkWithText("gi|15645924|ribosomal_protein"), "href");
            pushLocation();
            beginAt(address);

            log("Verify protein page.");
            assertTextPresent("gi|15645924|ribosomal_protein");
            assertTextPresent("7,683");
            String selectedValue = getSelectedOptionValue(Locator.name("allPeps"));
            boolean userPref = selectedValue == null || "".equals(selectedValue) || "false".equals(selectedValue);
            if (!userPref)
            {
                // User last viewed all peptides, regardless of search engine assignment, so flip to the other option
                // before checking that the values match our expectations
                selectOptionByValue(Locator.name("allPeps"), "false");
                waitForPageToLoad();
            }
            assertTextPresent("27% (18 / 66)");
            assertTextPresent("27% (2,050 / 7,683)");
            assertTextPresent("1 total, 1 distinct");
            assertTextPresent("R.VKLKAMQLSNPNEIKKAR.N");
            assertTextNotPresent("K.YTELK.D");

            selectOptionByValue(Locator.name("allPeps"), "true");
            waitForPageToLoad();

            assertTextPresent("35% (23 / 66)");
            assertTextPresent("35% (2,685 / 7,683)");
            assertTextPresent("Matches sequence of");
            assertTextPresent("2 total, 2 distinct");
            assertTextPresent("R.VKLKAMQLSNPNEIKKAR.N");
            assertTextPresent("K.YTELK.D");

            log("Return to run and set a filter");
            popLocation();
            setFilter("MS2Peptides", "Scan", "Is Less Than", "25");
            address = getAttribute(Locator.linkWithText("gi|15645924|ribosomal_protein"), "href");
            pushLocation();
            beginAt(address);

            // Be sure that our selection is sticky
            assertTextPresent("Matches sequence of");
            // Be sure that our scan filter was propagated to the protein page
            assertTextPresent("1 total, 1 distinct");
            assertTextPresent("27% (18 / 66)");
            assertTextPresent("27% (2,050 / 7,683)");
            assertTextPresent("R.VKLKAMQLSNPNEIKKAR.N");
            assertTextNotPresent("K.YTELK.D");

            if (userPref)
            {
                // User last only peptides assigned by the search engine, so flip back to restore their preference
                selectOptionByValue(Locator.name("allPeps"), "false");
                waitForPageToLoad();
            }

            popLocation();
            clickAndWait(Locator.linkWithText("MS2 Dashboard"));

            log("Test Compare MS2 Runs");

            log("Test Compare Peptides using Query");
            searchRunsTable.checkAllOnPage();
            waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
            clickButton("Compare", 0);
            clickAndWait(Locator.linkWithText("Peptide"));
            click(Locator.radioButtonByNameAndValue("peptideFilterType", "none"));
            setFormElement(Locator.input("targetProtein"), "");
            clickButton("Compare");
            assertTextPresent("K.EEEESDEDMGFG.-");
            assertTextPresent("R.Q^YALHVDGVGTK.A");
            assertTextPresent("K.GSDSLSDGPACKR.S");
            assertTextPresent("K.EYYLLHKPPKTISSTK.D");

            // verify the bulk protein coverage map export
            pushLocation();
            addUrlParameter("exportAsWebPage=true");
            clickButton("Export Protein Coverage");
            assertTextPresentInThisOrder("22001886", "Q963B6");
            assertTextPresentInThisOrder("29827410", "NP_822044.1");
            assertTextPresentInThisOrder("17508693", "NP_492384.1");
            assertTextPresentInThisOrder("27716987", "XP_233992.1");
            assertTextPresent("(search engine matches)");
            assertTextNotPresent("(all matching peptides)");
            assertTextPresent("57 Total qualifying peptides in run", 56); // two peptides have the same search engine protein
            assertTextPresent("57 Distinct qualifying peptides in run", 56); // two peptides have the same search engine protein
            assertTextPresent("59 Total qualifying peptides in run", 59);
            assertTextPresent("59 Distinct qualifying peptides in run", 59);
            assertTextPresent("peptide-marker", 117);
            popLocation();

            clickAndWait(Locator.linkWithText("Setup Compare Peptides"));
            click(Locator.radioButtonByNameAndValue("peptideFilterType", "probability"));
            setFormElement(Locator.input("peptideProphetProbability"), "0.9");
            clickButton("Compare");
            assertTextPresent("K.EEEESDEDMGFG.-");
            assertTextPresent("R.Q^YALHVDGVGTK.A");
            assertTextNotPresent("K.GSDSLSDGPACKR.S");
            assertTextPresent("K.EYYLLHKPPKTISSTK.D");

            // verify the bulk protein coverage map export for the peptideProphet probability filter
            pushLocation();
            addUrlParameter("exportAsWebPage=true");
            clickButton("Export Protein Coverage");
            assertTextPresentInThisOrder("4689022", "CAA80880.2");
            assertTextPresentInThisOrder("18311790", "NP_558457.1");
            assertTextPresentInThisOrder("15828808", "NP_326168.1");
            assertTextPresentInThisOrder("34849400", "AAP58899.1");
            assertTextNotPresent("BAB39767.1"); // for peptide K.GSDSLSDGPACKR.S
            assertTextPresent("(search engine matches)");
            assertTextNotPresent("(all matching peptides)");
            assertTextPresent("2 Total qualifying peptides in run", 4);
            assertTextPresent("2 Distinct qualifying peptides in run", 4);
            assertTextPresent("peptide-marker", 4);
            assertTextPresent(" 1  / 1(Q^) ", 1); // TODO: how do we verify the location of the match in the coverage map table?
            popLocation();

            clickAndWait(Locator.linkWithText("Setup Compare Peptides"));
            setFormElement(Locator.input("targetProtein"), "gi|18311790|phosphoribosylfor");
            clickButton("Compare");
            assertTextPresent("R.Q^YALHVDGVGTK.A");
            assertTextNotPresent("K.EEEESDEDMGFG.-");
            assertTextNotPresent("K.GSDSLSDGPACKR.S");
            assertTextNotPresent("K.EYYLLHKPPKTISSTK.D");

            // verify the bulk protein coverage map export for peptideProphet filter with target protein
            pushLocation();
            addUrlParameter("exportAsWebPage=true");
            clickButton("Export Protein Coverage");
            assertTextPresentInThisOrder("18311790", "NP_558457.1");
            assertTextNotPresent("CAA80880.2"); // for peptide K.EEEESDEDMGFG.-
            assertTextPresent("(all matching peptides)");
            assertTextNotPresent("(search engine matches)");
            assertTextPresent("(PeptideProphet &gt;= 0.9) AND (Matches sequence of ", 2);
            assertTextPresent("Peptide Counts:", 2);
            assertTextPresent("1 Total peptide matching sequence", 1);
            assertTextPresent("1 Distinct peptide matching sequence", 1);
            assertTextPresent("0 Total peptides matching sequence", 1);
            assertTextPresent("0 Distinct peptides matching sequence", 1);
            assertTextPresent("2 Total qualifying peptides in run", 2);
            assertTextPresent("2 Distinct qualifying peptides in run", 2);
            assertTextPresent("peptide-marker", 1);
            assertTextPresent(" 1  / 1(Q^) ", 1); // TODO: how do we verify the location of the match in the coverage map table?
            popLocation();            

            clickAndWait(Locator.linkWithText("Setup Compare Peptides"));
            setFormElement(Locator.input("targetProtein"), "gi|15645924|ribosomal_protein");
            click(Locator.radioButtonByNameAndValue("peptideFilterType", "none"));
            clickButton("Compare");
            assertTextPresent("K.YTELK.D");
            assertTextPresent("R.VKLKAMQLSNPNEIKKAR.N");
            assertTextNotPresent("R.Q^YALHVDGVGTK.A");
            assertTextNotPresent("K.EEEESDEDMGFG.-");
            assertTextNotPresent("K.GSDSLSDGPACKR.S");
            assertTextNotPresent("K.EYYLLHKPPKTISSTK.D");

            // verify the bulk protein coverage map export for target protein
            pushLocation();
            addUrlParameter("exportAsWebPage=true");
            clickButton("Export Protein Coverage");
            assertTextPresentInThisOrder("15645924", "NP_208103.1");
            assertTextPresent("NP_208103.1", 4);
            assertTextNotPresent("15612296", "NP_223949.1");
            assertTextPresent("(all matching peptides)");
            assertTextNotPresent("(search engine matches)");
            assertTextPresent("Peptide Counts:", 2);
            assertTextPresent("0 Total peptides matching sequence", 1);
            assertTextPresent("0 Distinct peptides matching sequence", 1);
            assertTextPresent("57 Total qualifying peptides in run", 1);
            assertTextPresent("57 Distinct qualifying peptides in run", 1);
            assertTextPresent("2 Total peptides matching sequence", 1);
            assertTextPresent("2 Distinct peptides matching sequence", 1);
            assertTextPresent("59 Total qualifying peptides in run", 1);
            assertTextPresent("59 Distinct qualifying peptides in run", 1);
            assertTextPresent("peptide-marker", 2);
            popLocation();

            clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        }

        log("Test Protein Prophet Compare");
        searchRunsTable.checkAllOnPage();
        waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
        clickButton("Compare", 0);
        clickAndWait(Locator.linkWithText("ProteinProphet (Legacy)"));
        selectOptionByText("viewParams", VIEW3);
        clickButton("Compare");
        assertTextPresent("(GroupProbability > 0.7)");
        assertTextNotPresent("gi|30089158|emb|CAD89505.1|");
        assertTextPresent("GroupNumber");
        assertTextPresent("0.78");
        setSort("MS2Compare", "Protein", SortDirection.ASC);
        assertTextBefore("gi|13442951|dbj|BAB39767.1|", "gi|13470573|ref|NP_102142.1|");
        setSort("MS2Compare", "Run0GroupProbability", SortDirection.DESC);
        if (!isTextBefore("gi|13470573|ref|NP_102142.1|", "gi|13442951|dbj|BAB39767.1|"))
            setSort("MS2Compare", "Run0GroupProbability", SortDirection.ASC);
        assertTextBefore("gi|13470573|ref|NP_102142.1|", "gi|13442951|dbj|BAB39767.1|");

        log("Test adding columns");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

        searchRunsTable.checkAllOnPage();
        waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
        clickButton("Compare", 0);
        clickAndWait(Locator.linkWithText("ProteinProphet (Legacy)"));
        checkCheckbox("light2HeavyRatioMean");
        uncheckCheckbox("groupProbability");
        clickButton("Compare");
        assertTextPresent("ratiomean");
        assertTextNotPresent("GroupProbability");

        log("Test Compare Search Engine Proteins");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

        searchRunsTable.checkAllOnPage();
        waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
        clickButton("Compare", 0);
        clickAndWait(Locator.linkWithText("Search Engine Protein"));
        selectOptionByText("viewParams", VIEW2);
        checkCheckbox("total");
        clickButton("Compare");
        assertTextPresent("(SequenceMass > 20000)");
        assertTextPresent("(DeltaMass > 0)");
        assertTextPresent("Total");
        assertTextNotPresent("gi|32307556|ribosomal_protein");
        assertTextNotPresent("gi|136348|TRPF_YEAST_N-(5'-ph");
        assertTextPresent("gi|33241155|ref|NP_876097.1|");
        assertTextPresent("Pattern");
        setSort("MS2Compare", "Protein", SortDirection.ASC);
        assertTextBefore("gi|11499506|ref|NP_070747.1|", "gi|13507919|");

        log("Test Compare Peptides (Legacy)");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

        searchRunsTable.checkAllOnPage();
        waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
        clickButton("Compare", 0);
        clickAndWait(Locator.linkWithText("Peptide (Legacy)"));
        selectOptionByText("viewParams", VIEW2);
        clickButton("Compare");
        assertTextPresent("(DeltaMass > 0)");
        assertTextNotPresent("R.TIDPVIAR.K");
        assertTextNotPresent("K.KLYNEELK.A");
        assertTextPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextPresent("R.SVAHITK.L");
        assertTextPresent("Pattern");
        setSort("MS2Compare", "Peptide", SortDirection.DESC);
        if (!isTextBefore("-.MELFSNELLYK.T", "K.EIRQRQGDDLDGLSFAELR.G"))
            setSort("MS2Compare", "Peptide", SortDirection.ASC);
        assertTextBefore("-.MELFSNELLYK.T", "K.EIRQRQGDDLDGLSFAELR.G");

        log("Test creating run groups");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickLinkWithImage(getContextPath() + "/Experiment/images/graphIcon.gif");
        clickAndWait(Locator.id("expandCollapse-experimentRunGroup"), 0);
        clickButton("Create new group");
        setFormElement("name", RUN_GROUP1_NAME1);
        setFormElement("contactId", RUN_GROUP1_CONTACT);
        setFormElement("experimentDescriptionURL", RUN_GROUP1_DESCRIPTION);
        setFormElement("hypothesis", RUN_GROUP1_HYPOTHESIS);
        setFormElement("comments", RUN_GROUP1_COMMENTS);
        clickButton("Submit");
        clickAndWait(Locator.id("expandCollapse-experimentRunGroup"), 0);
        assertTextPresent(RUN_GROUP1_NAME1);
        assertTextPresent(RUN_GROUP1_HYPOTHESIS);
        assertTextPresent(RUN_GROUP1_COMMENTS);
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        assertTextPresent(RUN_GROUP1_NAME1);

        clickAndWait(Locator.linkWithText("Run Groups"));
        clickButton("Create Run Group");
        clickButton("Submit");
        setFormElement("name", RUN_GROUP3_NAME);
        clickButton("Submit");

        clickButton("Create Run Group");
        setFormElement("name", RUN_GROUP2_NAME);
        clickButton("Submit");

        log("Test editing run group info");
        clickAndWait(Locator.linkWithText(RUN_GROUP1_NAME1));
        assertTextPresent(RUN_GROUP1_NAME1);
        assertTextPresent(RUN_GROUP1_CONTACT);
        assertTextPresent(RUN_GROUP1_DESCRIPTION);
        assertTextPresent(RUN_GROUP1_HYPOTHESIS);
        assertTextPresent(RUN_GROUP1_COMMENTS);
        clickButton("Edit");
        setFormElement("name", RUN_GROUP1_NAME2);
        clickButton("Submit");

        log("Test customizing view to include the run groups");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkWithText("MS2 Runs"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("RunGroupToggle/" + RUN_GROUP1_NAME2, "Run Groups " + RUN_GROUP1_NAME2);
        _customizeViewsHelper.addCustomizeViewColumn("RunGroupToggle/" + RUN_GROUP2_NAME, "Run Groups " + RUN_GROUP2_NAME);
        _customizeViewsHelper.addCustomizeViewColumn("RunGroupToggle/Default Experiment", "Run Groups Default Experiment");
        _customizeViewsHelper.applyCustomView();

        assertTextPresent(RUN_GROUP1_NAME2);
        assertTextPresent(RUN_GROUP2_NAME);
        assertTextPresent(DEFAULT_EXPERIMENT);

        checkCheckbox("experimentMembership", 0);
        checkCheckbox("experimentMembership", 1);
        checkCheckbox("experimentMembership", 2);
        checkCheckbox("experimentMembership", 3);
        checkCheckbox("experimentMembership", 4);
        checkCheckbox("experimentMembership", 5);

        log("Test editing a run group's runs");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkWithText("Run Groups"));
        clickAndWait(Locator.linkWithText(RUN_GROUP2_NAME));
        assertTextPresent(RUN_GROUP1_NAME2);
        assertTextPresent(RUN_GROUP2_NAME);
        assertTextPresent(DEFAULT_EXPERIMENT);
        checkDataRegionCheckbox("XTandemSearchRuns", 1);
        clickButton("Remove");
        assert(!isTextPresent(testFile1) || !isTextPresent(testFile2));

        verifyRunGroupMap(testFile1, testFile2);
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

        log("Test that the compare run groups works");
        searchRunsTable.checkAllOnPage();
        waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
        clickButton("Compare", 0);
        clickAndWait(Locator.linkWithText("ProteinProphet"));
        clickButton("Compare");

        click(Locator.linkWithText("Comparison Overview"));
        waitForText(RUN_GROUP1_NAME2, 1000);
        assertTextPresent(RUN_GROUP1_NAME2);
        assertTextPresent(RUN_GROUP2_NAME);
        assertTextPresent(DEFAULT_EXPERIMENT);
        selectOptionByValue("//div[text() = 'A']/../../../td/select", "group1");

        log("Test Customize View");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("SeqId/Mass", "Protein Mass");
        _customizeViewsHelper.addCustomizeViewFilter("SeqId/Mass", "Protein Mass", "Is Less Than", "30000");
        _customizeViewsHelper.saveCustomView(VIEW5);

        log("Make sure the filtering and new columns worked");
        assertElementPresent(Locator.id("query:SeqId/Mass:header"));
        assertTextNotPresent("gi|34849400|gb|AAP58899.1|");

        log("Check default view works");
        clickMenuButton("Views", "default");
        assertElementNotPresent(Locator.id("query:SeqId/Mass:header"));
        assertTextPresent("gi|34849400|");

        log("Check sorting");
        clickMenuButton("Views", VIEW5);
        setSort("query", "SeqId", SortDirection.ASC);
        assertTextBefore("gi|13470573|ref|NP_102142.1|", "gi|15828808|ref|NP_326168.1|");

        log("Test exporting Compare Runs in Query");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        clickExportToText();
        assertTextPresent("Mass");
        assertTextNotPresent("gi|34849400|");
        assertTextBefore("gi|13470573|ref|NP_102142.1|", "gi|15828808|ref|NP_326168.1|");
        assertTextPresent("0.89");
        popLocation();

        log("Test delete run groups");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkWithText("Run Groups"));
        checkAllOnPage("RunGroupWide");
        clickButton("Delete");
        clickButton("Confirm Delete");
        assertTextNotPresent(RUN_GROUP1_NAME2);
        assertTextNotPresent(RUN_GROUP2_NAME);
        assertTextNotPresent(DEFAULT_EXPERIMENT);
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        assertTextNotPresent(RUN_GROUP1_NAME2);



        // Try to put this in once GWT components are testable
//        log("Test the cross comparison feature at top of query comparison");
//        selenium.click("//div[@id='org.labkey.ms2.RunComparator']/table/tbody/tr[1]/td/table/tbody/tr[2]/td/table/tbody/tr[3]/td[5]/img");
//        selenium.click("//input[@type='checkbox'][1]");
//        clickButton("OK", 0);
//        Assert.assertTrue(getText(Locator.raw("//div[@id='org.labkey.ms2.RunComparator']/table/tbody/tr[1]/td/table/tbody/tr[2]/td/table/tbody/tr[3]/td[3]/div")).compareTo("10") == 0);
//        Assert.assertTrue(getText(Locator.raw("//div[contains(text(), 'Group #2')]/../td[2]")).compareTo("7") == 0);


/*      DISABLED, as we're not shipping with query-based peptides comparison for now
        log("Test Compare Runs using Query Peptides");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        click(Locator.name(".toggle"));
        waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
        clickButton("Compare");
        checkCheckbox("column", "QueryPeptides", true);
        clickButton("Compare");
        assertTextPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextPresent("R.VEKALLDNAGVR.N");
        assertTextPresent("PepProphet");

        log("Test Customize View in Query Peptides");
        clickAndWait(Locator.linkWithText("Customize View"));
        selenium.click("expand_Run");
        addCustomizeViewColumn("Run/IonPercent", "Run Ion%");
        removeCustomizeViewColumn("Run Count");
        addCustomizeViewFilter("Run/IonPercent", "Run Ion%", "Is Greater Than", "0.15");
        clickButton("Save");

        log("Check filtering and columns were added correctly");
        assertTextPresent("Ion%");
        assertTextNotPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextPresent("33");
        assertTextPresent("34");

        log("Check Ignore/Apply View Filter");
        clickAndWait(Locator.linkWithText("Ignore View Filter"));
        assertTextPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextPresent("Ion%");
        clickAndWait(Locator.linkWithText("Apply View Filter"));
        assertTextNotPresent("K.EIRQRQGDDLDGLSFAELR.G");

        log("Check sorting");
        setSort("MS2Compare", "Ion%", SortDirection.ASC);
        assertTextBefore("K.KHGGPKDEER.H", "K.QGTTRYR.V");

        log("Test exporting in Query Peptides Comparision");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        clickButton("Export to TSV");
        assertTextPresent("Ion%");
        assertTextNotPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextBefore("K.KHGGPKDEER.H", "K.QGTTRYR.V");
        assertTextPresent("R.TIDPVIAR.K");
        assertTextPresent("K.YTELK.D");
        assertTextPresent("29%");
        popLocation();
*/

        pepXMLtest();
        queryValidationTest();

    }

    private void verifyRunGroupMap(String protocol1Name, String protocol2Name)
    {

        //have to go to the actual page to test lookup
        beginAt("/query/MS2VerifyProject/ms2folder/executeQuery.view?schemaName=exp&query.queryName=RunGroupMap");

        List<Map<String, Object>> rows = executeSelectRowCommand("exp", "RunGroupMap").getRows();
        Assert.assertEquals("Unexpected number of rows in RunGroupMap", 5, rows.size());

        Set<String> keys = rows.get(0).keySet();
        for(String header : new String[] {"RunGroup", "Created", "CreatedBy", "Run"})
        {
            Assert.assertTrue("Run Group Map missing column: " + header, keys.contains(header));
        }
        Map<String, Integer> textAndCount = new HashMap<String, Integer>();
        textAndCount.put(DEFAULT_EXPERIMENT, new Integer(2));
        textAndCount.put(RUN_GROUP1_NAME2, new Integer(2));
        textAndCount.put("Test Run Group 2", new Integer(1));
        textAndCount.put(protocol2Name, new Integer(3));
        textAndCount.put(protocol1Name, new Integer(2));

        for(String key : textAndCount.keySet())
        {
            assertTextPresent(key, textAndCount.get(key).intValue());
        }
    }

    private void pepXMLtest()
    {
        clickButton("Process and Import Data");
//        sleep(2000);
        _extHelper.selectFileBrowserItem("pepXML/truncated.pep.xml");
        selectImportDataAction("Import Search Results");
        String ms2Run = "ms2pipe/truncated (pepXML)";
        waitForTextWithRefresh(ms2Run, defaultWaitForPage);
        clickAndWait(Locator.linkWithText(ms2Run));
        String windowName = "peptideProphetSummary";
        selenium.openWindow("", windowName);
        click(Locator.linkWithText("Show Peptide Prophet Details"));
        selenium.waitForPopUp(windowName, "10000");
        selenium.selectWindow(windowName);
        assertElementPresent(Locator.imageWithAltText("Charge 3+ Cumulative Observed vs. Model", false));
        Assert.assertEquals("Incorrect number of graphs", 13, getXpathCount(Locator.imageWithSrc("labkey/ms2/MS2VerifyProject/ms2folder", true)));
        assertTextPresent("PeptideProphet Details: ms2pipe/truncated (pepXML)");
        selenium.close();
        selenium.selectWindow(null);

    }

    //issue 12342
    private void queryValidationTest()
    {
        log("Validate previously failing queiries");


        String  sqlGroupNumberDisplay =    "SELECT ProteinGroups.\"Group\", \n" +
                "ProteinGroups.GroupProbability, \n" +
                "ProteinGroups.ErrorRate, \n" +
                "ProteinGroups.UniquePeptidesCount, \n" +
                "ProteinGroups.TotalNumberPeptides \n" +
                "FROM ProteinGroups ";

        String expectedError = "Could not resolve IndistinguishableCollectionId column";

        createQuery(getProjectName() + "/ms2folder", "GroupNumberTest", "ms2", sqlGroupNumberDisplay, "", false);
        _extHelper.clickExtTab("Source");
        clickButtonContainingText("Execute Query", 0);
        waitForText(expectedError);
        assertTextPresent(expectedError, 13);

        //add correct text
        String  sqlGroupNumberDisplay2 =    "SELECT ProteinGroups.\"Group\", \n" +
                "ProteinGroups.GroupProbability, \n" +
                "ProteinGroups.ErrorRate, \n" +
                "ProteinGroups.UniquePeptidesCount, \n" +
                "ProteinGroups.TotalNumberPeptides, \n" +
                "ProteinGroups.IndistinguishableCollectionId \n" +
                "FROM ProteinGroups ";

        createQuery(getProjectName() + "/ms2folder", "GroupNumberTestCorrect", "ms2", sqlGroupNumberDisplay2 + "\n", "", false);
        _extHelper.clickExtTab("Source");
        clickButtonContainingText("Execute Query", 0);
        assertTextNotPresent(expectedError);
        goToHome();

    }
}
