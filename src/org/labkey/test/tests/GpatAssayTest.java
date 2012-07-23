/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;

/**
 * Created by IntelliJ IDEA.
 * User: Trey Chadick
 * Date: Apr 6, 2011
 * Time: 2:19:10 PM
 */
public class GpatAssayTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "GpatAssayTest Project";

    private static final String GPAT_ASSAY_XLS = "trial01.xls";
    private static final String GPAT_ASSAY_XLSX = "trial01a.xlsx";
    private static final String GPAT_ASSAY_TSV = "trial02.tsv";
    private static final String ALIASED_ASSAY_1 = "trial01columns1.tsv";
    private static final String ALIASED_ASSAY_2 = "trial01columns2.tsv";
    private static final String ALIASED_ASSAY_3 = "trial01columns3.tsv";
    private static final String ALIASED_ASSAY_4 = "trial01columns4.tsv";
    private static final String GPAT_ASSAY_FNA = "trial03.fna";
    private static final String ASSAY_NAME_XLS = "XLS Assay";
    private static final String ASSAY_NAME_XLSX = "XLSX Assay";
    private static final String ASSAY_NAME_TSV = "TSV Assay";
    private static final String ASSAY_NAME_FNA = "FASTA Assay";


    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/assay";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public void doCleanup()
    {
        try { deleteProject(PROJECT_NAME); } catch (Throwable e) { }
    }

    @Override
    public void doTestSteps()
    {
        _containerHelper.createProject(PROJECT_NAME, "Assay");
        addWebPart("Pipeline Files");
        setPipelineRoot(getLabKeyRoot() + "/sampledata/GPAT");
        clickLinkWithText(PROJECT_NAME);

        log("Import XLS GPAT assay");
        sleep(2000);
        ExtHelper.clickFileBrowserFileCheckbox(this, GPAT_ASSAY_XLS);
        selectImportDataAction("Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        setFormElement("AssayDesignerName", ASSAY_NAME_XLS);
        uncheckCheckbox(Locator.xpath("//span[@id='id_import_Role']/input"));
        click(Locator.xpath("//tr[./td/span[@id='id_import_Score']]//div[contains(@class, 'x-tbar-page-next')]"));
        ExtHelper.waitForExtDialog(this, "Score Column Properties");
        ExtHelper.clickExtTab(this, "Validators");
        checkCheckbox("required");
        click(Locator.xpath("//tr[./td/span[@id='id_import_Primary']]//div[contains(@class, 'x-tbar-page-next')]"));
        ExtHelper.waitForExtDialog(this, "Primary Column Properties");
        ExtHelper.clickExtTab(this, "Advanced");
        checkCheckbox("mvEnabled");
        clickNavButton("OK", 0);
        assertFormElementEquals("SpecimenID", "SpecimenID");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "VisitID");
        assertFormElementEquals("Date", "DrawDt");
        // Unable to check fail state: Selenium can't handle GWT alert.
        // clickNavButton("Begin import", 0);
        // assertAlert("Could not convert the value 'text' from line #202 in column #6 (Primary) to Integer");
        ListHelper.setColumnType(this, 5, ListHelper.ListColumnType.String); // Row 201 is a string
        clickNavButton("Begin import");
        clickNavButton("Next");
        clickNavButton("Save and Finish");
        clickLinkWithText(GPAT_ASSAY_XLS);
        assertTextNotPresent("Role"); // excluded column
        assertTextPresent("1 - 100 of 201");

        log("Import XLSX GPAT assay");
        clickLinkWithText(PROJECT_NAME);
        ExtHelper.waitForFileGridReady(this);
        ExtHelper.clickFileBrowserFileCheckbox(this, GPAT_ASSAY_XLSX);
        selectImportDataAction("Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        setFormElement("AssayDesignerName", ASSAY_NAME_XLSX);
        uncheckCheckbox(Locator.xpath("//span[@id='id_import_Role']/input"));
        click(Locator.xpath("//tr[./td/span[@id='id_import_Score']]//div[contains(@class, 'x-tbar-page-next')]"));
        ExtHelper.waitForExtDialog(this, "Score Column Properties");
        ExtHelper.clickExtTab(this, "Validators");
        checkCheckbox("required");
        click(Locator.xpath("//tr[./td/span[@id='id_import_Primary']]//div[contains(@class, 'x-tbar-page-next')]"));
        ExtHelper.waitForExtDialog(this, "Primary Column Properties");
        ExtHelper.clickExtTab(this, "Advanced");
        checkCheckbox("mvEnabled");
        clickNavButton("OK", 0);
        assertFormElementEquals("SpecimenID", "SpecimenID");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "VisitID");
        assertFormElementEquals("Date", "DrawDt");
        ListHelper.setColumnType(this, 5, ListHelper.ListColumnType.String); // Row 201 is a string
        clickNavButton("Begin import");
        clickNavButton("Next");
        clickNavButton("Save and Finish");
        clickLinkWithText(GPAT_ASSAY_XLSX);
        assertTextNotPresent("Role"); // excluded column
        assertTextPresent("1 - 100 of 201");

        log("Import TSV GPAT assay");
        clickLinkWithText(PROJECT_NAME);
        ExtHelper.waitForFileGridReady(this);
        ExtHelper.clickFileBrowserFileCheckbox(this, GPAT_ASSAY_TSV);
        selectImportDataAction("Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        setFormElement("AssayDesignerName", ASSAY_NAME_TSV);
        uncheckCheckbox(Locator.xpath("//span[@id='id_import_Role']/input"));
        click(Locator.xpath("//tr[./td/span[@id='id_import_Score']]//div[contains(@class, 'x-tbar-page-next')]"));
        ExtHelper.waitForExtDialog(this, "Score Column Properties");
        ExtHelper.clickExtTab(this, "Validators");
        checkCheckbox("required");
        click(Locator.xpath("//tr[./td/span[@id='id_import_Primary']]//div[contains(@class, 'x-tbar-page-next')]"));
        ExtHelper.waitForExtDialog(this, "Primary Column Properties");
        ExtHelper.clickExtTab(this, "Advanced");
        checkCheckbox("mvEnabled");
        clickNavButton("OK", 0);
        assertFormElementEquals("SpecimenID", "SpecimenID");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "VisitID");
        assertFormElementEquals("Date", "DrawDt");
        ListHelper.setColumnType(this, 5, ListHelper.ListColumnType.String);
        clickNavButton("Show Assay Designer");

        waitForElement(Locator.xpath( getPropertyXPath(ASSAY_NAME_TSV + " Data Fields")), WAIT_FOR_JAVASCRIPT);
        ListHelper.setColumnLabel(this, getPropertyXPath(ASSAY_NAME_TSV + " Data Fields"), 4, "Blank");
        ListHelper.setColumnLabel(this, 7, "Result");
        ListHelper.setColumnName(this, 7, "Result");
        click(Locator.xpath(getPropertyXPath(ASSAY_NAME_TSV + " Data Fields") + "//span[contains(@class,'x-tab-strip-text') and text()='" + "Advanced" + "']"));
        setFormElement(Locator.xpath(getPropertyXPath(ASSAY_NAME_TSV + " Data Fields") + "//td/input[@id='importAliases']") , "Score");                   
        
        clickNavButton("Save & Close");
        clickNavButton("Next");        
        clickNavButton("Save and Finish");
        clickLinkWithText(GPAT_ASSAY_TSV);
        assertTextNotPresent("Role"); // excluded column
        assertTextPresent("1 - 100 of 201");

        log("Verify standard column aliases");
        clickLinkWithText(PROJECT_NAME);
        ExtHelper.selectFileBrowserItem(this, ALIASED_ASSAY_1);
        selectImportDataAction("Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        assertFormElementEquals("SpecimenID", "specId");
        assertFormElementEquals("ParticipantID", "ParticipantID");
        assertFormElementEquals("VisitID", "visitNo");
        assertFormElementEquals("Date", "draw_date");
        clickNavButton("Cancel");
        refresh(); // avoid file selection timeout
        ExtHelper.selectFileBrowserItem(this, ALIASED_ASSAY_2);
        selectImportDataAction("Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        assertFormElementEquals("SpecimenID", "vialId1");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "visit_no");
        assertFormElementEquals("Date", "drawDate");
        clickNavButton("Cancel");
        refresh(); // avoid file selection timeout
        ExtHelper.selectFileBrowserItem(this, ALIASED_ASSAY_3);
        selectImportDataAction("Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        assertFormElementEquals("SpecimenID", "vialId");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "visitId");
        assertFormElementEquals("Date", "date");
        clickNavButton("Cancel");
        refresh(); // avoid file selection timeout
        ExtHelper.selectFileBrowserItem(this, ALIASED_ASSAY_4);
        selectImportDataAction("Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        assertFormElementEquals("SpecimenID", "guspec");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "visitId");
        assertFormElementEquals("Date", "date");
        clickNavButton("Cancel");

        log("Import FASTA GPAT assay");
        clickLinkWithText(PROJECT_NAME);
        ExtHelper.waitForFileGridReady(this);
        ExtHelper.clickFileBrowserFileCheckbox(this, GPAT_ASSAY_FNA);
        selectImportDataAction("Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        setFormElement("AssayDesignerName", ASSAY_NAME_FNA);

        clickNavButton("Begin import");
        clickNavButton("Next");
        clickNavButton("Save and Finish");
        clickLinkWithText(GPAT_ASSAY_FNA);

        assertTextPresent("Header");
        assertTextPresent("Sequence");

        assertTextPresent("HCJDRSZ07IVO6P", "HCJDRSZ07IL1GX", "HCJDRSZ07H5SPZ");
        assertTextPresent("CACCAGACAGGTGTTATGGTGTGTGCCTGTAATCCCAGCTACTTGGGAGGGAGCTCAGGT");
    }
}
