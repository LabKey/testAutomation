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

import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.MS2;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.LogMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

@Category({MS2.class, DailyA.class})
public class MS2Test extends AbstractMS2ImportTest
{
    protected static final String RUN_GROUP1_NAME1 = "Test Run Group 1";
    //Issue #16260, "Exception when including run group with tricky characters in name," has been updated
    protected static final String RUN_GROUP1_NAME2 = "Test Run Group 1 New Name" + TRICKY_CHARACTERS;
    protected static final String RUN_GROUP1_CONTACT = "Test Contact";
    protected static final String RUN_GROUP1_DESCRIPTION = "This is a description";
    protected static final String RUN_GROUP1_HYPOTHESIS = "I think this is happening";
    protected static final String RUN_GROUP1_COMMENTS = "Here are comments.";
    protected static final String RUN_GROUP2_NAME = "Test Run Group 2";
    protected static final String RUN_GROUP3_NAME = "Test Run Group 3";

    @Override
    @LogMethod(category = LogMethod.MethodType.SETUP)
    protected void setupMS2()
    {
        super.setupMS2();
        importMS2Run("DRT2", 2);
    }

    @Override
    @LogMethod(category = LogMethod.MethodType.VERIFICATION)
    protected void verifyMS2()
    {
        verifyFirstRun();

        validateSecondRun();

        validateRunGroups();

        queryValidationTest();
        pepXMLtest();
    }

    private void verifyFirstRun()
    {
        log("Verify run view.");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkContainingText("DRT1"));

        // Make sure we're not using a custom default view for the current user
        selectOptionByText("viewParams", "<Standard View>");
        clickButton("Go");

        assertTextPresent(
                SEARCH_NAME3,
                "databases",
                MASS_SPEC);
        //Different cases used with different search engines.
        if( !isTextPresent(ENZYME))
            assertTextPresent(ENZYME);
        assertElementPresent(Locator.linkWithText(PEPTIDE1));

        log("Test Navigation Bar for Run");
        log("Test Show Modifications");
        click(Locator.linkWithText("Show Modifications"));
        // Wait for tooltip to show up
        waitForText("Variable", 2000);
        assertTextPresent(
                "E^",
                "Q^");

        log("Test Show Peptide Prophet Details");
        pushLocation();
        beginAt(getLinkHref("Show Peptide Prophet Details", "MS2", "/" + PROJECT_NAME + "/" + FOLDER_NAME));
        assertTextPresent(
                "Minimum probability",
                "Error rate",
                "Sensitivity");
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 1+ Distribution"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Sensitivity Plot"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 1+ Cumulative Distribution"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 3+ Distribution"));
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Charge 3+ Cumulative Observed vs. Model"));
        popLocation();

        log("Test Show Protein Prophet Details");
        pushLocation();
        beginAt(getLinkHref("Show Protein Prophet Details", "MS2", "/" + PROJECT_NAME + "/" + FOLDER_NAME));
        assertTextPresent(
                "Minimum probability",
                "Error rate",
                "Sensitivity");
        assertElementPresent(Locator.tagWithAttribute("img", "alt", "Sensitivity Plot"));
        popLocation();

        // Make sure we're not using a custom default view for the current user
        selectOptionByText(Locator.name("viewParams"), "<Standard View>");
        clickButton("Go");
        selectOptionByText(Locator.name("grouping"), "Peptides (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));

        log("Test export selected");
        DataRegionTable peptidesTable = new DataRegionTable("MS2Peptides", this);
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        peptidesTable.checkCheckbox(0);
        clickMenuButton("Export Selected", "TSV");
        assertTextPresent("K.LLASMLAK.A");
        assertTextNotPresent("R.Q^YALHVDGVGTK.A");
        assertTextPresent("\n", 2);
        popLocation();
        pushLocation();
        peptidesTable.checkAllOnPage();
        clickMenuButton("Export Selected", "AMT");
        assertTextPresent("\n", 60, true);
        assertTextPresent(
                "Run",
                "CalcMHPlus",
                "RetTime",
                "Fraction",
                "PepProphet",
                "Peptide",
                "1373.4690",
                "846.5120",
                "K.LLASMLAK.A",
                "K.EEEESDEDMGFG.-");
        popLocation();

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
        selectOptionByText(Locator.name("viewParams"), LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME);
        clickButton("Go");
        assertTextNotPresent("K.FANIGDVIVASVK.Q");
        assertTextPresent("-.MELFSNELLYK.T");
        assertTextNotPresent("K.TESGYGSESSLR.R");
        assertTextPresent("R.EADKVLVQMPSGK.Q");

        log("Test filter was remembered");
        assertTextPresent("Scan DESC");

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
        assertElementPresent(Locator.linkWithText("R.LSSMRDSR.S"));
        String address = getAttribute(Locator.linkWithText("R.LSSMRDSR.S"), "href");
        beginAt(address);

        log("Verify spectrum page.");
        assertTextPresent("R.LSSMRDSR.S");
        assertTextPresent("gi|29650192|ribosomal_protein");
        assertTextPresent("56");
        assertTextPresent("0.000");
        clickAndWait(Locator.linkWithText("Next"));
        assertTextPresent("R.GGNEESTK.T");
        assertTextPresent("gi|442754|A_Chain_A,_Superoxi");

        log("Return to run.");
        goBack();
        goBack();

        log("Verify still filtered.");
        assertTextPresent("(Scan > 6) AND (Scan <= 100) AND (Charge = 2) AND (Hyper >= 14.6) AND (Next <> 9.5) AND (B < 11.6) AND (Y < 11.3) AND (Expect > 1.2)");

        log("Test pick peptide columns");
        clickButton("Pick Peptide Columns");
        clickButton("Pick", 0);
        clickButton("Pick Columns");
        assertTextPresent("RetTime");

        log("Test export");
        addUrlParameter("exportAsWebPage=true");
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
        goBack();
        clickMenuButton("Export All", "AMT");
        assertTextPresent("Run");
        assertTextPresent("Peptide");
        assertTextBefore("R.LSSMRDSR.S", "R.GGNEESTK.T");
        assertTextNotPresent("K.FVKKSNDVR.L");
        assertTextPresent("\n", 5, true);
        popLocation();

        log("Test using saved view");
        pushLocation();
        selectOptionByText(Locator.name("viewParams"), LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME);
        clickButton("Go");

        log("Test hyper charge filters too");
        setFormElement(Locator.id("Charge1"), "11");
        setFormElement(Locator.id("Charge2"), "13");
        setFormElement(Locator.id("Charge3"), "14");
        clickAndWait(Locator.id("AddChargeScoreFilterButton"));
        assertTextPresent("R.KVTTGR.A");
        assertTextNotPresent("K.KLHQK.L");
        assertTextNotPresent("K.MEVDQLK.K");
        assertTextNotPresent("-.MELFSNELLYK.T");

        log("Test Protein View and if viewParams hold");
        selectOptionByText(Locator.name("grouping"), "Protein (Legacy)");
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
        assertElementPresent(Locator.linkContainingText("gi|13541159|30S_ribosomal_pro"));
        String href = getAttribute(Locator.linkContainingText("gi|13541159|30S_ribosomal_pro"), "href");
        pushLocation();
        beginAt(href);

        log("Verify peptides.");
        assertTextPresent("gi|13541159|ref|NP_110847.1|");
        assertTextPresent("(Scan > 6) AND (Scan <= 100) AND (+1:Hyper >= 11.0, +2:Hyper >= 13.0, +3:Hyper >= 14.0)");

        log("Return to run.");
        popLocation();

        log("Test sorting in Protein View");
        addUrlParameter("exportAsWebPage=true");
        setSort("MS2Proteins", "SequenceMass", SortDirection.ASC);
        assertTextPresent("SequenceMass ASC");
        assertTextBefore("gi|15668549|LSU_ribosomal_pro", "gi|14318169|AF379640_1_riboso");

        log("Test export Protein View");
        clickMenuButton("Export All", "TSV");
        assertTextPresent("Protein");
        assertTextPresent("Description");
        assertTextBefore("gi|15668549|LSU_ribosomal_pro", "gi|14318169|AF379640_1_riboso");
        assertTextPresent("gi|13541159|30S_ribosomal_pro");
        assertTextPresent("ribosomal protein S19 [Thermoplasma volcanium]");
        assertTextPresent("gi|29650192|ribosomal_protein");
        assertTextPresent("ribosomal protein S6 [Anopheles stephensi]");
        assertTextPresent("\n", 18, true);
        goBack();

        log("Test export expanded view");
        selectOptionByText(Locator.name("grouping"), "Protein (Legacy)");
        checkCheckbox("expanded");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
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
        goBack();
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
        selectOptionByText(Locator.name("grouping"), "ProteinProphet (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        assertTextPresent("Group");
        assertTextPresent("Prob");
        assertTextPresent("Spectrum Ids");
        assertTextPresent("gi|4689022|ribosomal_protein_");
        assertTextPresent("14.06%");
        assertTextPresent("gi|4883902|APETALA3_homolog_R");

        log("Test Protein Prophet with filters");
        selectOptionByText(Locator.name("viewParams"), LEGACY_PROTEIN_PROPHET_VIEW_NAME);
        clickButton("Go");
        assertTextPresent("(Scan > 6) AND (Scan <= 100)");
        assertTextPresent("Scan DESC");
        assertTextNotPresent("gi|30089158|low_density_lipop");

        setFilter("ProteinGroupsWithQuantitation", "PercentCoverage", "Is Not Blank");
        assertTextNotPresent("gi|13442951|MAIL");
        assertTextPresent("(GroupProbability > 0.7) AND (PercentCoverage IS NOT NULL)");

        validateLegacySingleRunExport();

        log("Create saved view to test query groupings");
        selectOptionByText(Locator.name("grouping"), "Peptides (Legacy)");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        selectOptionByText(Locator.id("views"), LEGACY_PEPTIDES_SCAN_6_100_VIEW_NAME);
        clickButton("Go");

        log("Test Query - Peptides Grouping");
        selectOptionByText(Locator.name("grouping"), "Standard");
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
        // Explicitly clear out the sorts, since we want to be just dealing with the ones set in Customize View
        clearSort("MS2Peptides", "Next");
        clearSort("MS2Peptides", "Scan");

        log("Test customize view");
        fireEvent(Locator.navButton("Clear All"), SeleniumEvent.click);
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
        windowMaximize();
        sleep(500);
        _customizeViewsHelper.moveCustomizeViewSort("Charge", false);
        // XXX: selenium test can't move columns that require scrolling the column list
        //_customizeViewsHelper.moveCustomizeViewColumn(this, "Peptide", false);
        _customizeViewsHelper.applyCustomView();

        assertTextBefore("K.TESGYGSESSLR.R", "K.HVSGKIIGFFY.-");
        //assertTextBefore("gi|30519530|A38R_protein", "K.ISNFIANNDCRYYIDAEHQKIISDEINR.Q");

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.moveCustomizeViewSort("Mass", false);
        // XXX: selenium test can't move columns that require scrolling the column list
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
        selectOptionByText(Locator.name("grouping"), "Protein Groups");
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
    }

    private void validateLegacySingleRunExport()
    {
        log("Test export");
        addUrlParameter("exportAsWebPage=true");
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
        selectOptionByText(Locator.name("grouping"), "ProteinProphet (Legacy)");
        checkCheckbox("expanded");
        clickAndWait(Locator.id("viewTypeSubmitButton"));
        pushLocation();
        checkDataRegionCheckbox("ProteinGroupsWithQuantitation", 0);
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
    }

    protected void validateSecondRun()
    {
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkWithText("drt/CAexample_mini (DRT2)"));

        selectOptionByText("viewParams", "<Standard View>");
        clickButton("Go");

        log("Test peptide filtering on protein page");
        assertElementPresent(Locator.linkWithText("gi|15645924|ribosomal_protein"));
        String address = getAttribute(Locator.linkWithText("gi|15645924|ribosomal_protein"), "href");
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
            prepForPageLoad();
            selectOptionByValue(Locator.name("allPeps"), "false");
            newWaitForPageToLoad();
        }
        assertTextPresent("27% (18 / 66)");
        assertTextPresent("27% (2,050 / 7,683)");
        assertTextPresent("1 total, 1 distinct");
        assertTextPresent("R.VKLKAMQLSNPNEIKKAR.N");
        assertTextNotPresent("K.YTELK.D");

        prepForPageLoad();
        selectOptionByValue(Locator.name("allPeps"), "true");
        newWaitForPageToLoad();

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
            prepForPageLoad();
            selectOptionByValue(Locator.name("allPeps"), "false");
            newWaitForPageToLoad();
        }

        popLocation();
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

        validateCompare();
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
    }

    private void validateRunGroups()
    {
        log("Test creating run groups");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkWithImage(getContextPath() + "/Experiment/images/graphIcon.gif"));
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
        assertTextPresent(
                RUN_GROUP1_NAME1,
                RUN_GROUP1_CONTACT,
                RUN_GROUP1_DESCRIPTION,
                RUN_GROUP1_HYPOTHESIS,
                RUN_GROUP1_COMMENTS);
        clickButton("Edit");
        setFormElement("name", RUN_GROUP1_NAME2);
        clickButton("Submit");

        log("Test customizing view to include the run groups");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkWithText("MS2 Runs"));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn(new String[] { "RunGroupToggle", EscapeUtil.fieldKeyEncodePart(RUN_GROUP1_NAME2) }, RUN_GROUP1_NAME2);
        _customizeViewsHelper.addCustomizeViewColumn(new String[] { "RunGroupToggle", RUN_GROUP2_NAME } , "Run Groups " + RUN_GROUP2_NAME);
        _customizeViewsHelper.addCustomizeViewColumn(new String[] { "RunGroupToggle", "Default Experiment" }, "Run Groups Default Experiment");
        _customizeViewsHelper.applyCustomView();

        assertTextPresent(
                RUN_GROUP1_NAME2,
                RUN_GROUP2_NAME,
                DEFAULT_EXPERIMENT);

        for (int i = 0; i <= 5; i++)
        {
            checkCheckbox(Locator.checkboxByName("experimentMembership").index(i));
        }

        log("Test editing a run group's runs");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        clickAndWait(Locator.linkWithText("Run Groups"));
        clickAndWait(Locator.linkWithText(RUN_GROUP2_NAME));
        assertTextPresent(RUN_GROUP1_NAME2);
        assertTextPresent(RUN_GROUP2_NAME);
        assertTextPresent(DEFAULT_EXPERIMENT);
        checkDataRegionCheckbox("XTandemSearchRuns", 1);
        clickButton("Remove");

        assertTextPresent("DRT2");
        assertTextNotPresent("DRT1");

        verifyRunGroupMapQuery();
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));

        log("Test that the compare run groups works");
        DataRegionTable searchRunsTable = new DataRegionTable("MS2SearchRuns", this);
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
        selectOptionByValue(Locator.xpath("//div[text() = 'A']/../../../td/select"), "group1");

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

        verifyGroupAudit();
    }

        //verify audit trail registers runs added to or removed from groups.
    private void verifyGroupAudit()
    {
        List<Map<String, Object>> rows = executeSelectRowCommand("auditLog", "ExperimentAuditEvent").getRows();
        assertEquals("Unexpected number of audit rows", 9, rows.size());
        int addedCount = 0;
        int removedCount = 0;
        for(Map row : rows)
        {
            if(((String)row.get("Comment")).contains("was added to the run group"))
                addedCount++;
            else if(((String)row.get("Comment")).contains("was removed from the run group"))
                removedCount++;
        }

        assertEquals(8, addedCount);
        assertEquals(1, removedCount);

        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
    }

    private void verifyRunGroupMapQuery()
    {
        goToSchemaBrowser();

        selectQuery("exp", "RunGroupMap");
        waitAndClickAndWait(Locator.linkWithText("view data"));

        List<Map<String, Object>> rows = executeSelectRowCommand("exp", "RunGroupMap").getRows();
        assertEquals("Unexpected number of rows in RunGroupMap", 5, rows.size());

        Set<String> keys = rows.get(0).keySet();
        for(String header : new String[] {"RunGroup", "Created", "CreatedBy", "Run"})
        {
            assertTrue("Run Group Map missing column: " + header, keys.contains(header));
        }
        Map<String, Integer> textAndCount = new HashMap<>();
        textAndCount.put(DEFAULT_EXPERIMENT, 2);
        textAndCount.put("Test Run Group 1 New Name", 2); // Intentionally don't include the special characters because it's hard to match up the HTML encoding exactly
        textAndCount.put(RUN_GROUP2_NAME, 1);
        textAndCount.put("DRT2", 3);
        textAndCount.put("DRT1", 2);

        for(String key : textAndCount.keySet())
        {
            assertTextPresent(key, textAndCount.get(key).intValue());
        }
    }


    private void validateCompare()
    {
        log("Test Compare MS2 Runs");

        log("Test Compare Peptides using Query");
        DataRegionTable searchRunsTable = new DataRegionTable("MS2SearchRuns", this);
        searchRunsTable.checkAllOnPage();
        waitForElement(Locator.navButton("Compare"), WAIT_FOR_JAVASCRIPT);
        clickButton("Compare", 0);
        clickAndWait(Locator.linkWithText("Peptide"));
        click(Locator.radioButtonByNameAndValue("peptideFilterType", "none"));
        setFormElement(Locator.input("targetProtein"), "");
        clickButton("Compare");
        assertTextPresent(
                "K.EEEESDEDMGFG.-",
                "R.Q^YALHVDGVGTK.A",
                "K.GSDSLSDGPACKR.S",
                "K.EYYLLHKPPKTISSTK.D");

        // verify the bulk protein coverage map export
        pushLocation();
        addUrlParameter("exportAsWebPage=true");
        clickButton("Export Protein Coverage");
        assertTextPresent(
                "22001886|sp|Q963B6",
                "29827410|ref|NP_822044.1",
                "17508693|ref|NP_492384.1",
                "27716987|ref|XP_233992.1",
                "(search engine matches)");
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
        assertTextPresent(
                "K.EEEESDEDMGFG.-",
                "R.Q^YALHVDGVGTK.A",
                "K.EYYLLHKPPKTISSTK.D");
        assertTextNotPresent("K.GSDSLSDGPACKR.S");

        // verify the bulk protein coverage map export for the peptideProphet probability filter
        pushLocation();
        addUrlParameter("exportAsWebPage=true");
        clickButton("Export Protein Coverage");
        assertTextPresent(
                "4689022|emb|CAA80880.2",
                "18311790|ref|NP_558457.1",
                "15828808|ref|NP_326168.1",
                "34849400|gb|AAP58899.1",
                "(search engine matches)");
        assertTextNotPresent(
                "BAB39767.1", // for peptide K.GSDSLSDGPACKR.S
                "(all matching peptides)");
        assertTextPresent("2 Total qualifying peptides in run", 4);
        assertTextPresent("2 Distinct qualifying peptides in run", 4);
        assertTextPresent("peptide-marker", 4);
        assertTextPresent(" 1  / 1(Q^) ", 1); // TODO: how do we verify the location of the match in the coverage map table?
        popLocation();

        clickAndWait(Locator.linkWithText("Setup Compare Peptides"));
        setFormElement(Locator.input("targetProtein"), "gi|18311790|phosphoribosylfor");
        clickButton("Compare");
        assertTextPresent("R.Q^YALHVDGVGTK.A");
        assertTextNotPresent(
                "K.EEEESDEDMGFG.-",
                "K.GSDSLSDGPACKR.S",
                "K.EYYLLHKPPKTISSTK.D");

        // verify the bulk protein coverage map export for peptideProphet filter with target protein
        pushLocation();
        addUrlParameter("exportAsWebPage=true");
        clickButton("Export Protein Coverage");
        assertTextPresent(
                "18311790|ref|NP_558457.1",
                "(all matching peptides)");
        assertTextNotPresent(
                "CAA80880.2", // for peptide K.EEEESDEDMGFG.-
                "(search engine matches)");
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
        assertTextPresent(
                "K.YTELK.D",
                "R.VKLKAMQLSNPNEIKKAR.N");
        assertTextNotPresent(
                "R.Q^YALHVDGVGTK.A",
                "K.EEEESDEDMGFG.-",
                "K.GSDSLSDGPACKR.S",
                "K.EYYLLHKPPKTISSTK.D");

        // verify the bulk protein coverage map export for target protein
        pushLocation();
        addUrlParameter("exportAsWebPage=true");
        clickButton("Export Protein Coverage");
        assertTextPresent(
                "15645924|ref|NP_208103.1",
                "(all matching peptides)");
        assertTextNotPresent(
                "15612296",
                "NP_223949.1",
                "(search engine matches)");
        assertTextPresent("NP_208103.1", 4);
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

        log("Test Compare Runs using Query Peptides");
        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
        click(Locator.name(".toggle"));
        clickMenuButton("Compare", "Peptide");
        checkRadioButton(Locator.radioButtonByNameAndValue("peptideFilterType", "none"));
        setFormElement(Locator.name("targetProtein"), "");
        clickButton("Compare");
        assertTextPresent(
                "K.EIRQRQGDDLDGLSFAELR.G",
                "R.TQMPAASICVNYK.G",
                "Avg PepProphet");

        log("Test Customize View in Query Peptides");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("CTAGG_AVG_XCorr");
        _customizeViewsHelper.removeCustomizeViewColumn("InstanceCount");
        _customizeViewsHelper.addCustomizeViewFilter("CTAGG_AVG_XCorr", "Avg XCorr", "Is Greater Than", "10");
        _customizeViewsHelper.saveCustomView();

        log("Check filtering and columns were added correctly");
        assertTextPresent(
                "Avg XCorr",
                "K.EIRQRQGDDLDGLSFAELR.G",
                "11.200",
                "13.800");
        assertTextNotPresent(
                "R.TQMPAASICVNYK.G");

        log("Check Ignore/Apply View Filter");
        clickMenuButton("Views", "Apply View Filter");
        assertTextPresent(
                "K.EIRQRQGDDLDGLSFAELR.G",
                "R.TQMPAASICVNYK.G",
                "Avg XCorr");

        clickMenuButton("Views", "Apply View Filter");
        assertTextPresent(
                "Avg XCorr",
                "K.EIRQRQGDDLDGLSFAELR.G");
        assertTextNotPresent(
                "R.TQMPAASICVNYK.G");

        log("Test exporting in Query Peptides Comparision");
        addUrlParameter("exportAsWebPage=true");
        clickExportToText();
        assertTextPresent(
                "AVG_XCorr",
                "K.EIRQRQGDDLDGLSFAELR.G",
                "11.200",
                "13.800");
        assertTextNotPresent(
                "R.TQMPAASICVNYK.G");
        goBack();
    }

    private void pepXMLtest()
    {
        clickButton("Process and Import Data");
        _fileBrowserHelper.importFile("pepXML/truncated.pep.xml", "Import Search Results");
        String ms2Run = "ms2pipe/truncated (pepXML)";
        waitForTextWithRefresh(ms2Run, defaultWaitForPage);
        clickAndWait(Locator.linkWithText(ms2Run));

        click(Locator.linkWithText("Show Peptide Prophet Details"));
        Object[] windows = getDriver().getWindowHandles().toArray();
        getDriver().switchTo().window((String) windows[1]);
        assertElementPresent(Locator.imageWithAltText("Charge 3+ Cumulative Observed vs. Model", false));
        assertEquals("Incorrect number of graphs", 13, getXpathCount(Locator.imageWithSrc("labkey/ms2/MS2VerifyProject/ms2folder", true)));
        assertTextPresent("PeptideProphet Details: ms2pipe/truncated (pepXML)");
        getDriver().close();
        getDriver().switchTo().window((String) windows[0]);
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

        clickAndWait(Locator.linkWithText("MS2 Dashboard"));
    }
}
