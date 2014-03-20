/*
 * Copyright (c) 2011-2013 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.ListHelper;

/**
 * User: Trey Chadick
 * Date: Apr 6, 2011
 * Time: 2:19:10 PM
 */
@Category({DailyB.class})
public class GpatAssayTest extends BaseWebDriverTest
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
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public void doTestSteps()
    {
        _containerHelper.createProject(PROJECT_NAME, "Assay");
        addWebPart("Pipeline Files");
        setPipelineRoot(getLabKeyRoot() + "/sampledata/GPAT");
        clickProject(PROJECT_NAME);

        log("Import XLS GPAT assay");
        _fileBrowserHelper.importFile(GPAT_ASSAY_XLS, "Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_XLS);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);
        uncheckCheckbox(Locator.gwtCheckBoxOnImportGridByColLabel("Role"));
        click(Locator.gwtNextButtonOnImportGridByColLabel("Score"));
        _extHelper.waitForExtDialog("Score Column Properties");
        _extHelper.clickExtTab("Validators");
        checkCheckbox("required");
        clickButton("OK", 0);
        click(Locator.gwtNextButtonOnImportGridByColLabel("Primary"));
        _extHelper.waitForExtDialog("Primary Column Properties");
        _extHelper.clickExtTab("Advanced");
        checkCheckbox("mvEnabled");
        clickButton("OK", 0);
        assertFormElementEquals("SpecimenID", "SpecimenID");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "VisitID");
        assertFormElementEquals("Date", "DrawDt");
        // Unable to check fail state: Selenium can't handle GWT alert.
        // clickButton("Begin import", 0);
        // assertAlert("Could not convert the value 'text' from line #202 in column #6 (Primary) to Integer");
        _listHelper.setColumnType(5, ListHelper.ListColumnType.String); // Row 201 is a string
        clickButton("Begin import");
        waitAndClickButton("Next");
        waitAndClickButton("Save and Finish");
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_XLS));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.css(".labkey-column-header").withText("Role")); // excluded column

        log("Import XLSX GPAT assay");
        clickProject(PROJECT_NAME);
        _fileBrowserHelper.importFile(GPAT_ASSAY_XLSX, "Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_XLSX);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);
        uncheckCheckbox(Locator.gwtCheckBoxOnImportGridByColLabel("Role"));
        click(Locator.gwtNextButtonOnImportGridByColLabel("Score"));
        _extHelper.waitForExtDialog("Score Column Properties");
        _extHelper.clickExtTab("Validators");
        checkCheckbox("required");
        clickButton("OK", 0);
        click(Locator.gwtNextButtonOnImportGridByColLabel("Primary"));
        _extHelper.waitForExtDialog("Primary Column Properties");
        _extHelper.clickExtTab("Advanced");
        checkCheckbox("mvEnabled");
        clickButton("OK", 0);
        assertFormElementEquals("SpecimenID", "SpecimenID");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "VisitID");
        assertFormElementEquals("Date", "DrawDt");
        _listHelper.setColumnType(5, ListHelper.ListColumnType.String); // Row 201 is a string
        clickButton("Begin import");
        waitAndClickButton("Next");
        waitAndClickButton("Save and Finish");
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_XLSX));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.css(".labkey-column-header").withText("Role")); // excluded column

        log("Import TSV GPAT assay");
        clickProject(PROJECT_NAME);
        _fileBrowserHelper.importFile(GPAT_ASSAY_TSV, "Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_TSV);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);
        uncheckCheckbox(Locator.gwtCheckBoxOnImportGridByColLabel("Role"));
        click(Locator.gwtNextButtonOnImportGridByColLabel("Score"));
        _extHelper.waitForExtDialog("Score Column Properties");
        _extHelper.clickExtTab("Validators");
        checkCheckbox("required");
        clickButton("OK", 0);
        click(Locator.gwtNextButtonOnImportGridByColLabel("Primary"));
        _extHelper.waitForExtDialog("Primary Column Properties");
        _extHelper.clickExtTab("Advanced");
        checkCheckbox("mvEnabled");
        clickButton("OK", 0);
        assertFormElementEquals("SpecimenID", "SpecimenID");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "VisitID");
        assertFormElementEquals("Date", "DrawDt");
        _listHelper.setColumnType(5, ListHelper.ListColumnType.String);
        clickButton("Show Assay Designer");

        waitForElement(Locator.xpath( getPropertyXPath(ASSAY_NAME_TSV + " Data Fields")), WAIT_FOR_JAVASCRIPT);
        _listHelper.setColumnLabel(getPropertyXPath(ASSAY_NAME_TSV + " Data Fields"), 4, "Blank");
        _listHelper.setColumnLabel(7, "Result");
        _listHelper.setColumnName(7, "Result");
        click(Locator.xpath(getPropertyXPath(ASSAY_NAME_TSV + " Data Fields") + "//span[contains(@class,'x-tab-strip-text') and text()='" + "Advanced" + "']"));
        setFormElement(Locator.xpath(getPropertyXPath(ASSAY_NAME_TSV + " Data Fields") + "//td/input[@id='importAliases']") , "Score");
        pressTab(Locator.xpath(getPropertyXPath(ASSAY_NAME_TSV + " Data Fields") + "//td/input[@id='importAliases']"));

        clickButton("Save & Close");
        waitAndClickButton("Next");
        waitAndClickButton("Save and Finish");
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_TSV));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.css(".labkey-column-header").withText("Role")); // excluded column

        log("Verify standard column aliases");
        clickProject(PROJECT_NAME);
        _fileBrowserHelper.importFile(ALIASED_ASSAY_1, "Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        assertFormElementEquals("SpecimenID", "specId");
        assertFormElementEquals("ParticipantID", "ParticipantID");
        assertFormElementEquals("VisitID", "visitNo");
        assertFormElementEquals("Date", "draw_date");
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        _fileBrowserHelper.importFile(ALIASED_ASSAY_2, "Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        assertFormElementEquals("SpecimenID", "vialId1");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "visit_no");
        assertFormElementEquals("Date", "drawDate");
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        _fileBrowserHelper.importFile(ALIASED_ASSAY_3, "Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        assertFormElementEquals("SpecimenID", "vialId");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "visitId");
        assertFormElementEquals("Date", "date");
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        _fileBrowserHelper.importFile(ALIASED_ASSAY_4, "Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        assertFormElementEquals("SpecimenID", "guspec");
        assertFormElementEquals("ParticipantID", "ptid");
        assertFormElementEquals("VisitID", "visitId");
        assertFormElementEquals("Date", "date");
        clickButton("Cancel");

        log("Import FASTA GPAT assay");
        clickProject(PROJECT_NAME);
        _fileBrowserHelper.importFile(GPAT_ASSAY_FNA, "Create New General Assay Design");
        waitForText("SpecimenID", WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_FNA);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);

        clickButton("Begin import");
        waitAndClickButton("Next");
        waitAndClickButton("Save and Finish");
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_FNA));

        waitForText("Sequence");
        assertTextPresent("Header");

        assertTextPresent("HCJDRSZ07IVO6P", "HCJDRSZ07IL1GX", "HCJDRSZ07H5SPZ");
        assertTextPresent("CACCAGACAGGTGTTATGGTGTGTGCCTGTAATCCCAGCTACTTGGGAGGGAGCTCAGGT");
    }
}
