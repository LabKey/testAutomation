/*
 * Copyright (c) 2011-2017 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({Assays.class, DailyB.class})
public class GpatAssayTest extends BaseWebDriverTest
{
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
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("assay");
    }

    @Override
    protected String getProjectName()
    {
        return "GpatAssayTest Project";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void testSteps()
    {
        _containerHelper.createProject(getProjectName(), "Assay");
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Pipeline Files");
        setPipelineRoot(TestFileUtils.getLabKeyRoot() + "/sampledata/GPAT");
        clickProject(getProjectName());

        log("Import XLS GPAT assay");
        _fileBrowserHelper.importFile(GPAT_ASSAY_XLS, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_XLS);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);
        uncheckCheckbox(Locator.gwtCheckBoxOnImportGridByColLabel("Role"));
        click(Locator.gwtNextButtonOnImportGridByColLabel("Score"));
        _extHelper.waitForExtDialog("Score Column Properties");
        _extHelper.clickExtTab("Validators");
        checkCheckbox(Locator.checkboxByName("required"));
        clickButton("OK", 0);
        click(Locator.gwtNextButtonOnImportGridByColLabel("Primary"));
        _extHelper.waitForExtDialog("Primary Column Properties");
        _extHelper.clickExtTab("Advanced");
        checkCheckbox(Locator.checkboxByName("mvEnabled"));
        clickButton("OK", 0);
        assertEquals("SpecimenID", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("VisitID", getFormElement(Locator.name("VisitID")));
        assertEquals("DrawDt", getFormElement(Locator.name("Date")));
        // Unable to check fail state: Selenium can't handle GWT alert.
        // clickButton("Begin import", 0);
        // assertAlert("Could not convert the value 'text' from line #202 in column #6 (Primary) to Integer");
        _listHelper.setColumnType(5, ListHelper.ListColumnType.String); // Row 201 is a string
        clickButton("Begin import");
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_XLS));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.tagWithClass("*", "labkey-column-header").withText("Role")); // excluded column

        log("Import XLSX GPAT assay");
        clickProject(getProjectName());
        _fileBrowserHelper.importFile(GPAT_ASSAY_XLSX, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_XLSX);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);
        uncheckCheckbox(Locator.gwtCheckBoxOnImportGridByColLabel("Role"));
        click(Locator.gwtNextButtonOnImportGridByColLabel("Score"));
        _extHelper.waitForExtDialog("Score Column Properties");
        _extHelper.clickExtTab("Validators");
        checkCheckbox(Locator.checkboxByName("required"));
        clickButton("OK", 0);
        click(Locator.gwtNextButtonOnImportGridByColLabel("Primary"));
        _extHelper.waitForExtDialog("Primary Column Properties");
        _extHelper.clickExtTab("Advanced");
        checkCheckbox(Locator.checkboxByName("mvEnabled"));
        clickButton("OK", 0);
        assertEquals("SpecimenID", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("VisitID", getFormElement(Locator.name("VisitID")));
        assertEquals("DrawDt", getFormElement(Locator.name("Date")));
        _listHelper.setColumnType(5, ListHelper.ListColumnType.String); // Row 201 is a string
        clickButton("Begin import");
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_XLSX));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.css(".labkey-column-header").withText("Role")); // excluded column

        log("Import TSV GPAT assay");
        clickProject(getProjectName());
        _fileBrowserHelper.importFile(GPAT_ASSAY_TSV, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_TSV);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);
        uncheckCheckbox(Locator.gwtCheckBoxOnImportGridByColLabel("Role"));
        click(Locator.gwtNextButtonOnImportGridByColLabel("Score"));
        _extHelper.waitForExtDialog("Score Column Properties");
        _extHelper.clickExtTab("Validators");
        checkCheckbox(Locator.checkboxByName("required"));
        clickButton("OK", 0);
        click(Locator.gwtNextButtonOnImportGridByColLabel("Primary"));
        _extHelper.waitForExtDialog("Primary Column Properties");
        _extHelper.clickExtTab("Advanced");
        checkCheckbox(Locator.checkboxByName("mvEnabled"));
        clickButton("OK", 0);
        assertEquals("SpecimenID", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("VisitID", getFormElement(Locator.name("VisitID")));
        assertEquals("DrawDt", getFormElement(Locator.name("Date")));
        _listHelper.setColumnType(5, ListHelper.ListColumnType.String);
        clickButton("Show Assay Designer");

        waitForElement(Locator.xpath(getPropertyXPath(ASSAY_NAME_TSV + " Data Fields")), WAIT_FOR_JAVASCRIPT);
        _listHelper.setColumnLabel(getPropertyXPath(ASSAY_NAME_TSV + " Data Fields"), 4, "Blank");
        _listHelper.setColumnLabel(7, "Result");
        _listHelper.setColumnName(7, "Result");
        click(Locator.xpath(getPropertyXPath(ASSAY_NAME_TSV + " Data Fields") + "//span[contains(@class,'x-tab-strip-text') and text()='" + "Advanced" + "']"));
        setFormElement(Locator.xpath(getPropertyXPath(ASSAY_NAME_TSV + " Data Fields") + "//td/input[@id='importAliases']"), "Score");
        pressTab(Locator.xpath(getPropertyXPath(ASSAY_NAME_TSV + " Data Fields") + "//td/input[@id='importAliases']"));

        clickButton("Save & Close");
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_TSV));
        waitForElement(Locator.css(".labkey-pagination").containing("1 - 100 of 201"));
        assertElementNotPresent(Locator.css(".labkey-column-header").withText("Role")); // excluded column

        log("Verify standard column aliases");
        clickProject(getProjectName());
        _fileBrowserHelper.importFile(ALIASED_ASSAY_1, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        assertEquals("specId", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ParticipantID", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visitNo", getFormElement(Locator.name("VisitID")));
        assertEquals("draw_date", getFormElement(Locator.name("Date")));
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        _fileBrowserHelper.importFile(ALIASED_ASSAY_2, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        assertEquals("vialId1", getFormElement(Locator.name("SpecimenID")));
        assertEquals(null, "ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visit_no", getFormElement(Locator.name("VisitID")));
        assertEquals("drawDate", getFormElement(Locator.name("Date")));
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        _fileBrowserHelper.importFile(ALIASED_ASSAY_3, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        assertEquals("vialId", getFormElement(Locator.name("SpecimenID")));
        assertEquals(null, "ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visitId", getFormElement(Locator.name("VisitID")));
        assertEquals("date", getFormElement(Locator.name("Date")));
        clickButton("Cancel");
        refresh(); // avoid file selection timeout
        _fileBrowserHelper.importFile(ALIASED_ASSAY_4, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        assertEquals("guspec", getFormElement(Locator.name("SpecimenID")));
        assertEquals("ptid", getFormElement(Locator.name("ParticipantID")));
        assertEquals("visitId", getFormElement(Locator.name("VisitID")));
        assertEquals("date", getFormElement(Locator.name("Date")));
        clickButton("Cancel");

        log("Import FASTA GPAT assay");
        clickProject(getProjectName());
        _fileBrowserHelper.importFile(GPAT_ASSAY_FNA, "Create New General Assay Design");
        waitForText(WAIT_FOR_JAVASCRIPT, "SpecimenID");
        setFormElement(Locator.name("AssayDesignerName"), ASSAY_NAME_FNA);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);

        clickButton("Begin import");
        clickButton("Next", defaultWaitForPage);
        clickButton("Save and Finish", defaultWaitForPage);
        waitAndClick(Locator.linkWithText(GPAT_ASSAY_FNA));

        waitForText("Sequence");
        assertTextPresent(
                "Header", "HCJDRSZ07IVO6P", "HCJDRSZ07IL1GX", "HCJDRSZ07H5SPZ",
                "CACCAGACAGGTGTTATGGTGTGTGCCTGTAATCCCAGCTACTTGGGAGGGAGCTCAGGT");
    }

    /**
     * @deprecated Use {@link org.labkey.test.components.PropertiesEditor} from _assayHelper or _listHelper
     */
    @Deprecated
    public String getPropertyXPath(String propertyHeading)
    {
        return "//h3[text() = '" + propertyHeading + "']/../..";
    }
}
