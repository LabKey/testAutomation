/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.ms2.MS2TestBase;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.io.IOException;

public class MS2BvtTest extends MS2TestBase
{
    protected static final String TEST = "DRT1";
    protected static final String TEST2 = "DRT2";
    protected static final String VIEW2 = "proteinView";
    protected static final String VIEW3 = "proteinGroupView";
    protected static final String VIEW4 = "queryView1";
    protected static final String VIEW5 = "queryView2";
    protected static final String PEPTIDE1 = "K.GSDSLSDGPACKR.S";
    protected static final String PEPTIDE2 = "R.TIDPVIAR.K";
    protected static final String PEPTIDE3 = "K.HVSGKIIGFFY.-";
    protected static final String PEPTIDE4 = "R.ISSTKMDGIGPK.K";
    protected static final String SEARCH_TYPE = "xtandem";
    protected static final String SEARCH_NAME = "XTandem";
    protected static final String SEARCH_NAME2 = "X!Tandem";
    protected static final String SEARCH_BUTTON = "X%21Tandem";
    protected static final String SEARCH_NAME3 = "X! Tandem";
    protected static final String RAW_PEP_XML = ".pep.xml";
    protected static final String ENZYME = "trypsin";
    protected static final String MASS_SPEC = "ThermoFinnigan";
    protected static final String RUN_GROUP1_NAME1 = "Test Run Group 1";
    protected static final String RUN_GROUP1_NAME2 = "Test Run Group 1 New Name";
    protected static final String RUN_GROUP1_CONTACT = "Test Contact";
    protected static final String RUN_GROUP1_DESCRIPTION = "This is a description";
    protected static final String RUN_GROUP1_HYPOTHESIS = "I think this is happening";
    protected static final String RUN_GROUP1_COMMENTS = "Here are comments.";
    protected static final String RUN_GROUP2_NAME = "Test Run Group 2";
    protected static final String RUN_GROUP3_NAME = "Test Run Group 3";

    protected void doCleanup() throws IOException
    {
        cleanPipe(SEARCH_TYPE);
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    protected void doTestSteps()
    {
        log("Verifying that pipeline files were cleaned up properly");
        File test2 = new File(_pipelinePath + "/bov_sample/" + SEARCH_TYPE + "/test2");
        if (test2.exists())
            fail("Pipeline files were not cleaned up; test2("+test2.toString()+") directory still exists");

        super.doTestSteps();
        
        DataRegionTable peptidesTable = new DataRegionTable("MS2Peptides", this);
        DataRegionTable proteinGroupsTable = new DataRegionTable("ProteinGroups", this);
        DataRegionTable searchRunsTable = new DataRegionTable("MS2SearchRuns", this);

        log("Upload existing MS2 data.");
        clickLinkWithText(FOLDER_NAME);
        clickNavButton("Process and Import Data");
        clickLinkWithText("bov_sample");
        clickLinkWithText(SEARCH_TYPE);
        clickLinkWithText(TEST);
        clickNavButton("Import Experiment");

        log("Going to the list of all pipeline jobs");
        clickLinkWithText("All");

        log("Verify upload started.");
        assertTextPresent(SAMPLE_BASE_NAME + ".search.xar.xml");
        int seconds = 0;
        while (countLinksWithText("COMPLETE") < 1 && seconds++ < MAX_WAIT_SECONDS)
        {
            log("Waiting upload to complete");
            if (countLinksWithText("ERROR") > 0)
            {
                fail("Job in ERROR state found in the list");
            }
            sleep(1000);
            refresh();
        }
        clickLinkWithText("MS2 Dashboard");
        assertLinkPresentContainingText("MS2 Experiment Runs");
        assertLinkPresentContainingText(SAMPLE_BASE_NAME);

        log("Verify run view.");
        clickLinkWithImage(getContextPath() + "/MS2/images/runIcon.gif");

        // Make sure we're not using a custom default view for the current user
        selectOptionByText("viewParams", "<Standard View>");
        clickNavButton("Go");

        assertTextPresent(SEARCH_NAME3);
        assertTextPresent("databases");
        //Different cases used with different search engines.
        if( !isTextPresent(ENZYME))
            assertTextPresent(ENZYME);
        assertTextPresent(MASS_SPEC);
        assertLinkPresentWithText(PEPTIDE1);

        log("Test Navigation Bar for Run");
        log("Test Show Modifications");
        pushLocation();
        beginAt(goToNavButton("Show Modifications", "MS2", "/" + PROJECT_NAME + "/" + FOLDER_NAME));
        assertTextPresent("Variable");
        assertTextPresent("E^");
        assertTextPresent("Q^");
        popLocation();

        log("Test Show Peptide Prophet Details");
        pushLocation();
        beginAt(goToNavButton("Show Peptide Prophet Details", "MS2", "/" + PROJECT_NAME + "/" + FOLDER_NAME));
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
        beginAt(goToNavButton("Show Protein Prophet Details", "MS2", "/" + PROJECT_NAME + "/" + FOLDER_NAME));
        assertTextPresent("Minimum probability");
        assertTextPresent("Error rate");
        assertTextPresent("Sensitivity");
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Sensitivity Plot"));
        popLocation();

        // Make sure we're not using a custom default view for the current user
        selectOptionByText("viewParams", "<Standard View>");
        clickNavButton("Go");

        log("Test export selected");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        peptidesTable.checkCheckbox(0);
        clickNavButton("Export Selected", 0);
        clickLinkWithText("TSV");
        assertTextPresent("K.LLASMLAK.A");
        assertTextNotPresent("R.Q^YALHVDGVGTK.A");
        assertTextPresent("\n", 2, true);
        popLocation();
        pushLocation();
        peptidesTable.checkAllOnPage();
        clickNavButton("Export Selected", 0);
        clickLinkWithText("AMT");
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
        clickNavButton("Export Selected", 0);
        click(Locator.linkWithText("AMT"));
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
        clickNavButton("Export All", 0);
        clickLinkWithText("TSV", 0);
        assertTextPresent("Scan");
        assertTextPresent("IonPercent");
        assertTextPresent("Protein");
        assertTextBefore("R.Q^YALHVDGVGTK.A", "K.LLASMLAK.A");
        assertTextBefore("R.SLADVARR.R", "-.MELFSNELLYK.T");
        assertTextPresent("gi|4689022|ribosomal_protein_");
        assertTextPresent("1373.4690");
        assertTextPresent("\n", 58, true);
        popLocation();
        clickNavButton("Export All", 0);
        clickLinkWithText("AMT", 0);
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
        clickNavButton("Save View");
        setFormElement("name", VIEW);
        clickNavButton("Save View");

        log("Continue with filters");
        setFilter("MS2Peptides", "Charge", "Equals", "2+");
        assertTextNotPresent("R.APPSTQESESPR.Q");
        assertTextPresent("R.TIDPVIAR.K");
        setFilter("MS2Peptides", "Hyper", "Is Greater Than or Equal To", "14.6");
        assertTextNotPresent("K.RLLRSMVK.F");
        assertTextPresent("R.AEIDYANK.T");
        setFilter("MS2Peptides", "Next", "Does not Equal", "9.5");
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
        pushLocation();
        beginAt(address);

        log("Verify spectrum page.");
        assertTextPresent("R.LSSMRDSR.S");
        assertTextPresent("gi|29650192|ribosomal_protein");
        assertTextPresent("56");
        assertTextPresent("0.0000");
        clickNavButton("Next >>");
        assertTextPresent("R.GGNEESTK.T");
        assertTextPresent("gi|442754|A_Chain_A,_Superoxi");

        log("Return to run.");
        popLocation();

        log("Verify still filtered.");
        assertTextPresent("(Scan > 6) AND (Scan <= 100) AND (Charge = 2) AND (Hyper >= 14.6) AND (Next <> 9.5) AND (B < 11.6) AND (Y < 11.3) AND (Expect > 1.2)");

        log("Test pick peptide columns");
        clickNavButton("Pick Peptide Columns");
        clickNavButton("Pick", 0);
        clickNavButton("Pick Columns");
        assertTextPresent("RetTime");

        log("Test export");
        pushLocation();
        clickNavButton("Export All", 0);
        clickLinkWithText("TSV", 0);
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
        clickNavButton("Export All", 0);
        clickLinkWithText("AMT", 0);
        assertTextPresent("Run");
        assertTextPresent("Peptide");
        assertTextBefore("R.LSSMRDSR.S", "R.GGNEESTK.T");
        assertTextNotPresent("K.FVKKSNDVR.L");
        assertTextPresent("\n", 5, true);
        popLocation();

        log("Make saved view for Protein Group for Comparison");
        pushLocation();
        setFilter("MS2Peptides", "DeltaMass", "Is Greater Than", "0");
        selectOptionByText("grouping", "Protein");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        setFilter("MS2Proteins", "SequenceMass", "Is Greater Than", "20000");
        log("Save view for later");
        clickNavButton("Save View");
        setFormElement("name", VIEW2);
        clickNavButton("Save View");

        log("Test using saved view");
        popLocation();
        pushLocation();
        selectOptionByText("viewParams", VIEW);
        clickNavButton("Go");

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
        selectOptionByText("grouping", "Protein");
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
        clickNavButton("Export All", 0);
        clickLinkWithText("TSV", 0);
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
        selectOptionByText("grouping", "Protein");
        checkCheckbox("expanded");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        pushLocation();
        clickNavButton("Export All", 0);
        clickLinkWithText("TSV", 0);
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
        clickNavButton("Export All", 0);
        clickLinkWithText("AMT", 0);
        assertTextPresent("Run");
        assertTextPresent("Peptide");
        assertTextBefore("K.TKDYEGMQVPVK.V", "R.RDYLHYLPKYNR.F");
        assertTextNotPresent("K.LLASMLAK.A");
        assertTextNotPresent("R.KKVAIVPEPLR.K");
        assertTextPresent("\n", 20, true);
        popLocation();

        log("Test Protein Prophet");
        pushLocation();
        selectOptionByText("grouping", "Protein Prophet");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        assertTextPresent("Group");
        assertTextPresent("Prob");
        assertTextPresent("Spectrum Ids");
        assertTextPresent("gi|4689022|ribosomal_protein_");
        assertTextPresent("14.06%");
        assertTextPresent("gi|4883902|APETALA3_homolog_R");

        log("Test Protein Prophet with filters");
        selectOptionByText("viewParams", VIEW);
        clickNavButton("Go");
        selectOptionByText("grouping", "Protein Prophet");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        assertTextNotPresent("gi|4689022|ribosomal_protein_");
        assertTextPresent("gi|16078254|similar_to_riboso");
        assertTextPresent("(Scan > 6) AND (Scan <= 100)");
        assertTextPresent("Scan DESC");
        setFilter("ProteinGroupsWithQuantitation", "GroupProbability", "Is Greater Than", "0.7");
        assertTextNotPresent("gi|30089158|low_density_lipop");

        log("Save view for later");
        clickNavButton("Save View");
        setFormElement("name", VIEW3);
        clickNavButton("Save View");

        setFilter("ProteinGroupsWithQuantitation", "PercentCoverage", "Is Not Blank");
        assertTextNotPresent("gi|13442951|MAIL");
        assertTextPresent("(GroupProbability > 0.7) AND (PercentCoverage IS NOT NULL)");

        log("Test export");
        pushLocation();
        clickNavButton("Export All", 0);
        clickLinkWithText("AMT", 0);
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
        clickNavButton("Pick Peptide Columns");
        clickNavButton("Pick", 0);
        clickNavButton("Pick Columns");
        clickNavButton("Pick Peptide Columns");
        clickNavButton("Pick", 0);
        clickNavButton("Pick Columns");
        selectOptionByText("grouping", "Protein Prophet");
        checkCheckbox("expanded");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        pushLocation();
        checkCheckbox(Locator.raw("document.forms['ProteinGroupsWithQuantitation'].elements['.select'][0]"));
        clickNavButton("Export Selected", 0);
        clickLinkWithText("TSV");
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
        clickNavButton("Export All", 0);
        clickLinkWithText("TSV", 0);
        assertTextBefore("gi|548772|RL4_HALHA_50S_RIBOS", "gi|23619029|60S_ribosomal_pro");
        assertTextPresent("MLNMAKSKMHK");
        assertTextPresent("\n", 3, true);
        popLocation();

        log("Create saved view to test query groupings");
        selectOptionByText("grouping", "None");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        selectOptionByText("views", VIEW);
        clickNavButton("Go");

        log("Test Query - Peptides Grouping");
        selectOptionByText("grouping", "Query - Peptides");
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
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        addCustomizeViewSort("Charge", "Z", "DESC");
        addCustomizeViewSort("Mass", "CalcMH+", "DESC");
        removeCustomizeViewSort("Next");
        removeCustomizeViewSort("Scan");
        addCustomizeViewFilter("DeltaMass", "dMass", "Is Less Than", "0");
        addCustomizeViewFilter("RowId", "Row Id", "Is Greater Than", "3");
        removeCustomizeViewFilter(2);
        removeCustomizeViewFilter("Hyper");
        addCustomizeViewColumn("NextAA", "Next AA");
        addCustomizeViewColumn("OrigScore", "Orig Score");
        removeCustomizeViewColumn("Expect");
        removeCustomizeViewColumn("SeqHits");
        setFormElement("ff_columnListName", VIEW4);
        clickNavButton("Save");

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
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        moveCustomizeViewSort("Z", false);
        moveCustomizeViewColumn("Peptide", false);
        clickNavButton("Save");
        assertTextBefore("K.TESGYGSESSLR.R", "K.HVSGKIIGFFY.-");
        assertTextBefore("gi|30519530|A38R_protein", "K.ISNFIANNDCRYYIDAEHQKIISDEINR.Q");
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        moveCustomizeViewSort("Z", true);
        moveCustomizeViewColumn("Peptide", true);
        clickNavButton("Save");

        log("Test Ignore View Filter");
        clickMenuButton("Views", "Views:Apply View Filter");
        assertTextPresent("K.LLASMLAK.A");
        assertTextPresent("R.GGNEESTK.T");
        assertTextPresent("Next AA");
        assertTextBefore(PEPTIDE2, PEPTIDE1);
        assertTextBefore(PEPTIDE4, PEPTIDE3);

        log("Test Apply View Filter");
        clickMenuButton("Views", "Views:Apply View Filter");
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
        clickNavButton("Export All", 0);
        clickLinkWithText("TSV", 0);
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
        clickNavButton("Export All", 0);
        clickLinkWithText("AMT", 0);
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
        clickNavButton("Export Selected", 0);
        clickLinkWithText("TSV");
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
        clickNavButton("Export Selected", 0);
        clickLinkWithText("AMT");
        assertTextPresent("Peptide");
        assertTextNotPresent("Next AA");
        assertTextBefore("K.ISNFIANNDCRYYIDAEHQKIISDEINR.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
        assertTextPresent("\n", 5, true);
        popLocation();

        log("Test default view");
        clickMenuButton("Views", "Views:default");
        waitForPageToLoad();
        assertTextPresent("K.LLASMLAK.A");
        assertTextPresent("R.GGNEESTK.T");
        assertTextNotPresent("Next AA");
        assertTextBefore(PEPTIDE2, PEPTIDE1);
        assertTextBefore(PEPTIDE4, PEPTIDE3);
        assertTextPresent("Expect");
        assertTextPresent("SeqHits");

        log("Test load saved view");
        clickMenuButton("Views", "Views:" + VIEW4);
        waitForPageToLoad();
        assertTextNotPresent("R.GGNEESTK.T");
        assertTextBefore(PEPTIDE1, PEPTIDE2);
        assertTextBefore(PEPTIDE3, PEPTIDE4);
        assertTextNotPresent("Expect");
        assertTextNotPresent("SeqHits");

        log("Test changing default view");
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        clearCustomizeViewFilters();
        clearCustomizeViewSorts();
        addCustomizeViewSort("DeltaMass", "dMass", "ASC");
        addCustomizeViewFilter("Mass", "CalcMH+", "Is Greater Than", "1000");
        addCustomizeViewColumn("Fraction");
        removeCustomizeViewColumn("Ion%");
        setFormElement("ff_columnListName", "");
        clickNavButton("Save");
        clickMenuButton("Views", "Views:default");
        assertTextNotPresent("K.LLASMLAK.A");
        assertTextPresent("Fraction");
        assertTextBefore("K.TKDYEGMQVPVK.V", "R.LGARRVSPVR.A");
        assertTextNotPresent("Ion%");

        log("Test restoring default view");
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        clickNavButton("Reset my default grid view");
        assertTextPresent("K.LLASMLAK.A");
        assertTextNotPresent("Fraction");
        assertTextBefore("R.LGARRVSPVR.A", "K.TKDYEGMQVPVK.V");
        assertTextPresent("Ion%");

        log("Test delete view");
        clickMenuButton("Views", "Views:" + VIEW4);
        waitForPageToLoad();
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        clickNavButtonContainingText("Delete my grid view");
        assertTextPresent("K.LLASMLAK.A");
        assertTextPresent("R.GGNEESTK.T");
        assertTextNotPresent("Next AA");
        assertTextBefore(PEPTIDE2, PEPTIDE1);
        assertTextBefore(PEPTIDE4, PEPTIDE3);
        assertTextPresent("Expect");
        assertTextPresent("SeqHits");

        log("Test Protein Prophet view in Query - Peptides grouping");
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        click(Locator.raw("expand_ProteinProphetData"));
        click(Locator.raw("expand_ProteinProphetData/ProteinGroupId"));
        addCustomizeViewColumn("ProteinProphetData/ProteinGroupId/Group", "Group");
        addCustomizeViewColumn("ProteinProphetData/ProteinGroupId/TotalNumberPeptides", "Peptides");
        addCustomizeViewColumn("ProteinProphetData/ProteinGroupId/GroupProbability", "Prob");
        addCustomizeViewColumn("ProteinProphetData/ProteinGroupId/BestName", "Best Name");
        removeCustomizeViewColumn("CalcMH+");
        addCustomizeViewFilter("DeltaMass", "dMass", "Is Greater Than", "0");
        addCustomizeViewFilter("ProteinProphetData/ProteinGroupId/GroupProbability", "Prob", "Is Greater Than", "0.7");
        addCustomizeViewSort("ProteinProphetData/ProteinGroupId/GroupProbability", "Prob", "ASC");
        setFormElement("ff_columnListName", VIEW4);
        clickNavButton("Save");

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
        clickNavButton("Export All", 0);
        clickLinkWithText("TSV", 0);
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
        clickNavButton("Export All", 0);
        clickLinkWithText("AMT", 0);
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
        selectOptionByText("grouping", "Query - Protein Groups");
        checkCheckbox("expanded");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        assertTextPresent("Protein");
        assertTextPresent("Protein Description");
        assertTextPresent("Group");
        assertTextPresent("APETALA3 homolog RbAP3-2 [Ranunculus bulbosus]");
        assertTextPresent("gi|4883902|APETALA3_homolog_R");

        log("Test customize view");
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        removeCustomizeViewColumn("Unique");
        selenium.click("expand_Proteins");
        selenium.click("expand_Proteins/Protein");
        addCustomizeViewColumn("Proteins/Protein/ProtSequence", "Protein Sequence");
        addCustomizeViewFilter("GroupProbability", "Prob", "Is Greater Than", "0.7");
        addCustomizeViewSort("ErrorRate", "Error", "DESC");
        setFormElement("ff_columnListName", VIEW4);
        clickNavButton("Save");

        log("Test that sorting, filtering, and columns are correct");
        assertTextNotPresent("Unique");
        assertTextPresent("Sequence");
        assertTextPresent("MSASELATSYSALILADEGIEIKSDKLLSLTKAANVDVEPIWATIFAKALEGKDLKELLLNIGSGAGAAPVAGGAGAPAAADGERPAEEKEEAKEEEESDEDMGFG");
        assertTextNotPresent("gi|30089158|low_density_lipop");
        assertTextBefore("gi|16078254|similar_to_riboso", "gi|18311790|phosphoribosylfor");

        log("Test exporting in Query - Protein View");
        pushLocation();
        log("Test exporting in TSV");
        clickNavButton("Export All", 0);
        clickLinkWithText("TSV", 0);
        assertTextNotPresent("Unique");
        assertTextPresent("Sequence");
        assertTextPresent("MSASELATSYSALILADEGIEIKSDKLLSLTKAANVDVEPIWATIFAKALEGKDLKELLLNIGSGAGAAPVAGGAGAPAAADGERPAEEKEEAKEEEESDEDMGFG");
        assertTextNotPresent("gi|30089158|low_density_lipop");
        assertTextBefore("gi|16078254|similar_to_riboso", "gi|18311790|phosphoribosylfor");
        assertTextPresent("\n", 8);
        popLocation();

        log("Test exporting selected and non-expanded view");
        uncheckCheckbox("expanded");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        pushLocation();
        proteinGroupsTable.uncheckAllOnPage();
        proteinGroupsTable.checkCheckbox(0);
        proteinGroupsTable.checkCheckbox(1);
        clickNavButton("Export Selected", 0);
        clickLinkWithText("TSV");
        assertTextBefore("0.74", "0.78");
        assertTextPresent("\n", 3);
        popLocation();

        log("Upload second MS2 Run");
        clickLinkWithText("MS2 Dashboard");
        clickNavButton("Process and Import Data");
        clickLinkWithText("bov_sample");
        clickLinkWithText(SEARCH_TYPE);
        clickLinkWithText(TEST2);
        clickNavButton("Import Experiment");

        log("Verify upload finished.");
        seconds = 0;
        clickLinkWithText("Data Pipeline");
        while (countLinksWithText("COMPLETE") < 2 && seconds++ < MAX_WAIT_SECONDS)
        {
            log("Waiting upload to complete");
            if (countLinksWithText("ERROR") > 0)
            {
                fail("Job in ERROR state found in the list");
            }
            sleep(1000);
            refresh();
        }
        clickLinkWithText(FOLDER_NAME);

        log("Test export 2 runs together");
        pushLocation();
        searchRunsTable.checkAllOnPage();
        clickNavButton("MS2 Export");
        checkCheckbox("exportFormat", "TSV", true);
        selectOptionByText("viewParams", VIEW);
        clickNavButton("Go");
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
        clickNavButton("MS2 Export");
        checkCheckbox("exportFormat", "AMT", true);
        selectOptionByText("viewParams", VIEW);
        clickNavButton("Go");
        assertTextPresent("Run");
        assertTextPresent("Peptide");
        assertTextBefore("K.QLDSIHVTILHK.E", "R.GRRNGPRPVHPTSHNR.Q");
        assertTextBefore("R.EADKVLVQMPSGK.Q", "K.E^TSSKNFDASVDVAIRLGVDPR.K");
        assertTextPresent("-.MELFSNELLYK.T");
        assertTextPresent("1386.6970");
        assertTextPresent("\n", 89, true);
        popLocation();

        log("Test Compare MS2 Runs");
        log("Test Protein Prophet Compare");

        searchRunsTable.checkAllOnPage();
        clickNavButton("Compare", 0);
        clickLinkWithText("ProteinProphet");
        selectOptionByText("viewParams", VIEW3);
        clickNavButton("Go");
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
        clickLinkWithText("MS2 Dashboard");

        searchRunsTable.checkAllOnPage();
        clickNavButton("Compare", 0);
        clickLinkWithText("ProteinProphet");
        checkCheckbox("light2HeavyRatioMean");
        uncheckCheckbox("groupProbability");
        clickNavButton("Go");
        assertTextPresent("ratiomean");
        assertTextNotPresent("GroupProbability");

        log("Test Compare Search Engine Proteins");
        clickLinkWithText("MS2 Dashboard");

        searchRunsTable.checkAllOnPage();
        clickNavButton("Compare", 0);
        clickLinkWithText("Search Engine Protein");
        selectOptionByText("viewParams", VIEW2);
        checkCheckbox("total");
        clickNavButton("Go");
        assertTextPresent("(SequenceMass > 20000)");
        assertTextPresent("(DeltaMass > 0)");
        assertTextPresent("Total");
        assertTextNotPresent("gi|32307556|ribosomal_protein");
        assertTextNotPresent("gi|136348|TRPF_YEAST_N-(5'-ph");
        assertTextPresent("gi|33241155|ref|NP_876097.1|");
        assertTextPresent("Pattern");
        setSort("MS2Compare", "Protein", SortDirection.ASC);
        assertTextBefore("gi|11499506|ref|NP_070747.1|", "gi|13507919|");

        log("Test Compare Peptides");
        clickLinkWithText("MS2 Dashboard");

        searchRunsTable.checkAllOnPage();
        clickNavButton("Compare", 0);
        clickLinkWithText("Peptide");
        selectOptionByText("viewParams", VIEW2);
        clickNavButton("Go");
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

        log("Test Compare Runs using Query");
        clickLinkWithText("MS2 Dashboard");

        log("Test creating run groups");
        clickLinkWithImage(getContextPath() + "/Experiment/images/graphIcon.gif");
        clickAndWait(Locator.id("expandCollapse-experimentRunGroup"), 0);
        clickNavButton("Create new group");
        setFormElement("name", RUN_GROUP1_NAME1);
        setFormElement("contactId", RUN_GROUP1_CONTACT);
        setFormElement("experimentDescriptionURL", RUN_GROUP1_DESCRIPTION);
        setFormElement("hypothesis", RUN_GROUP1_HYPOTHESIS);
        setFormElement("comments", RUN_GROUP1_COMMENTS);
        clickNavButton("Submit");
        clickAndWait(Locator.id("expandCollapse-experimentRunGroup"), 0);
        assertTextPresent(RUN_GROUP1_NAME1);
        assertTextPresent(RUN_GROUP1_HYPOTHESIS);
        assertTextPresent(RUN_GROUP1_COMMENTS);
        clickLinkWithText("MS2 Dashboard");
        assertTextPresent(RUN_GROUP1_NAME1);
        
        clickNavButton("Add to run group", 0);
        clickLinkWithText("Create new run group...");
        clickNavButton("Submit");
        setFormElement("name", RUN_GROUP3_NAME);
        clickNavButton("Submit");

        clickLinkWithText("Run Groups");
        clickNavButton("Create Run Group");
        setFormElement("name", RUN_GROUP2_NAME);
        clickNavButton("Submit");

        log("Test editing run group info");
        clickLinkWithText(RUN_GROUP1_NAME1);
        assertTextPresent(RUN_GROUP1_NAME1);
        assertTextPresent(RUN_GROUP1_CONTACT);
        assertTextPresent(RUN_GROUP1_DESCRIPTION);
        assertTextPresent(RUN_GROUP1_HYPOTHESIS);
        assertTextPresent(RUN_GROUP1_COMMENTS);
        clickNavButton("Edit");
        setFormElement("name", RUN_GROUP1_NAME2);
        clickNavButton("Submit");

        log("Test customizing view to include the run groups");
        clickLinkWithText("MS2 Dashboard");
        clickLinkWithText("MS2 Experiment Runs");
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        click(Locator.raw("expand_RunGroups"));
        addCustomizeViewColumn("RunGroups/" + RUN_GROUP1_NAME2, "Run Groups " + RUN_GROUP1_NAME2);
        addCustomizeViewColumn("RunGroups/" + RUN_GROUP2_NAME, "Run Groups " + RUN_GROUP2_NAME);
        addCustomizeViewColumn("RunGroups/Default Experiment", "Run Groups Default Experiment");
        clickNavButton("Save");

        assertTextPresent("Run Groups " + RUN_GROUP1_NAME2);
        assertTextPresent("Run Groups " + RUN_GROUP2_NAME);
        assertTextPresent("Run Groups Default Experiment");

        checkCheckbox("experimentMembership", 0, false);
        checkCheckbox("experimentMembership", 1, false);
        checkCheckbox("experimentMembership", 2, false);
        checkCheckbox("experimentMembership", 3, false);
        checkCheckbox("experimentMembership", 4, false);
        checkCheckbox("experimentMembership", 5, false);

        log("Test editing a run group's runs");
        clickLinkWithText("MS2 Dashboard");
        clickLinkWithText("Run Groups");
        clickLinkWithText(RUN_GROUP2_NAME);
        assertTextPresent(RUN_GROUP1_NAME2);
        assertTextPresent(RUN_GROUP2_NAME);
        assertTextPresent("Default Experiment");
        checkDataRegionCheckbox("XTandemSearchRuns", 1);
        clickNavButton("Remove");
        assert(!isTextPresent(TEST) || !isTextPresent(TEST2));
        clickLinkWithText("MS2 Dashboard");

        log("Test that the compare run groups works");
        searchRunsTable.checkAllOnPage();
        clickNavButton("Compare", 0);
        clickLinkWithText("ProteinProphet (Query)");
        clickNavButton("Go");

        clickLinkWithText("Comparison Overview", false);
        waitForText(RUN_GROUP1_NAME2, 1000);
        assertTextPresent(RUN_GROUP1_NAME2);
        assertTextPresent(RUN_GROUP2_NAME);
        assertTextPresent("Default Experiment");
        selectOptionByValue("//div[contains(text(), 'Chart:')]/../../td/select", "group1");

        log("Test Customize View");
        clickMenuButton("Views", CUSTOMIZE_VIEW_ID);
        click(Locator.raw("expand_SeqId"));
        addCustomizeViewColumn("SeqId/Mass", "Protein Mass");
        addCustomizeViewFilter("SeqId/Mass", "Protein Mass", "Is Less Than", "30000");
        setFormElement("ff_columnListName", VIEW5);
        clickNavButton("Save");

        log("Make sure the filtering and new columns worked");
        assertElementPresent(Locator.id("query:SeqId/Mass:header"));
        assertTextNotPresent("gi|34849400|gb|AAP58899.1|");

        log("Check default view works");
        clickMenuButton("Views", "Views:default");
        waitForPageToLoad();
        assertElementNotPresent(Locator.id("query:SeqId/Mass:header"));
        assertTextPresent("gi|34849400|");

        log("Check sorting");
        clickMenuButton("Views", "Views:" + VIEW5);
        waitForPageToLoad();
        setSort("query", "SeqId", SortDirection.ASC);
        assertTextBefore("gi|13470573|ref|NP_102142.1|", "gi|15828808|ref|NP_326168.1|");

        log("Test exporting Compare Runs in Query");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        clickNavButton("Export", 0);
        clickLinkContainingText("Export All to Text");
        assertTextPresent("Mass");
        assertTextNotPresent("gi|34849400|");
        assertTextBefore("gi|13470573|ref|NP_102142.1|", "gi|15828808|ref|NP_326168.1|");
        assertTextPresent("0.89");
        popLocation();

        log("Test delete run groups");
        clickLinkWithText("MS2 Dashboard");
        clickLinkWithText("Run Groups");
        checkAllOnPage("RunGroupWide");
        clickNavButton("Delete Selected");
        clickNavButton("Confirm Delete");
        assertTextNotPresent(RUN_GROUP1_NAME2);
        assertTextNotPresent(RUN_GROUP2_NAME);
        assertTextNotPresent("Default Experiment");
        clickLinkWithText("MS2 Dashboard");
        assertTextNotPresent(RUN_GROUP1_NAME2);



        // Try to put this in once GWT components are testable
//        log("Test the cross comparison feature at top of query comparison");
//        selenium.click("//div[@id='org.labkey.ms2.RunComparator']/table/tbody/tr[1]/td/table/tbody/tr[2]/td/table/tbody/tr[3]/td[5]/img");
//        selenium.click("//input[@type='checkbox'][1]");
//        clickNavButton("OK", 0);
//        assertTrue(getText(Locator.raw("//div[@id='org.labkey.ms2.RunComparator']/table/tbody/tr[1]/td/table/tbody/tr[2]/td/table/tbody/tr[3]/td[3]/div")).compareTo("10") == 0);
//        assertTrue(getText(Locator.raw("//div[contains(text(), 'Group #2')]/../td[2]")).compareTo("7") == 0);


/*      DISABLED, as we're not shipping with query-based peptides comparison for now
        log("Test Compare Runs using Query Peptides");
        clickLinkWithText("MS2 Dashboard");
        click(Locator.name(".toggle"));
        clickNavButton("Compare");
        checkCheckbox("column", "QueryPeptides", true);
        clickNavButton("Go");
        assertTextPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextPresent("R.VEKALLDNAGVR.N");
        assertTextPresent("PepProphet");

        log("Test Customize View in Query Peptides");
        clickLinkWithText("Customize View");
        selenium.click("expand_Run");
        addCustomizeViewColumn("Run/IonPercent", "Run Ion%");
        removeCustomizeViewColumn("Run Count");
        addCustomizeViewFilter("Run/IonPercent", "Run Ion%", "Is Greater Than", "0.15");
        clickNavButton("Save");

        log("Check filtering and columns were added correctly");
        assertTextPresent("Ion%");
        assertTextNotPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextPresent("33");
        assertTextPresent("34");

        log("Check Ignore/Apply View Filter");
        clickLinkWithText("Ignore View Filter");
        assertTextPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextPresent("Ion%");
        clickLinkWithText("Apply View Filter");
        assertTextNotPresent("K.EIRQRQGDDLDGLSFAELR.G");

        log("Check sorting");
        setSort("MS2Compare", "Ion%", SortDirection.ASC);
        assertTextBefore("K.KHGGPKDEER.H", "K.QGTTRYR.V");

        log("Test exporting in Query Peptides Comparision");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        clickNavButton("Export to TSV");
        assertTextPresent("Ion%");
        assertTextNotPresent("K.EIRQRQGDDLDGLSFAELR.G");
        assertTextBefore("K.KHGGPKDEER.H", "K.QGTTRYR.V");
        assertTextPresent("R.TIDPVIAR.K");
        assertTextPresent("K.YTELK.D");
        assertTextPresent("29%");
        popLocation();
*/
    }

}
