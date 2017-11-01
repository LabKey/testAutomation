/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.AssayImportOptions;
import org.labkey.test.util.AssayImporter;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyA.class, Assays.class})
public class NabMultiVirusPlateTest extends BaseWebDriverTest
{
    private static final String PLATE_TEMPLATE_NAME = "NabMultiVirusTest Template";

    protected static final String MULTI_VIRUS_ASSAY_NAB = "MultiVirusNab";
    protected static final String MULTI_VIRUS_ASSAY_NAB_DESC = "Description for Multi Virus NAb assay";

    protected final File TEST_ASSAY_NAB_MV_FILE1 = TestFileUtils.getSampleData("Nab/SpectraMax/20140612_0588.txt");
    protected final File FILEMAKER_ASSAY_METADATA = TestFileUtils.getSampleData("Nab/SpectraMax/metadata_success.xls");
    protected final File FILEMAKER_ASSAY_INCOMPLETE_METADATA = TestFileUtils.getSampleData("Nab/SpectraMax/metadata_incomplete.xls");
    protected final File FILEMAKER_ASSAY_EXTRA_METADATA = TestFileUtils.getSampleData("Nab/SpectraMax/metadata_extra.xls");

    protected final List<String> WELLGROUP_NAMES = Arrays.asList(
        "Specimen 01:Virus 2",
        "Specimen 01:Virus 1",
        "Specimen 02:Virus 2",
        "Specimen 02:Virus 1",
        "Specimen 03:Virus 2",
        "Specimen 03:Virus 1",
        "Specimen 04:Virus 2",
        "Specimen 04:Virus 1",
        "Specimen 05:Virus 2",
        "Specimen 05:Virus 1",
        "Specimen 06:Virus 2",
        "Specimen 06:Virus 1",
        "Specimen 07:Virus 2",
        "Specimen 07:Virus 1",
        "Specimen 08:Virus 2",
        "Specimen 08:Virus 1",
        "Specimen 09:Virus 2",
        "Specimen 09:Virus 1",
        "Specimen 10:Virus 2",
        "Specimen 10:Virus 1");

    @Nullable
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @BeforeClass
    public static void initProject()
    {
        NabMultiVirusPlateTest init = (NabMultiVirusPlateTest)getCurrentTest();

        init.doCreateSteps();
    }

    private void doCreateSteps()
    {
        //create a new test project
        _containerHelper.createProject(getProjectName(), null);

        clickProject(getProjectName());
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Assay List");

        clickButton("Manage Assays");

        clickButton("Configure Plate Templates");
        clickAndWait(Locator.linkWithText("new 384 well (16x24) NAb multi-virus plate template"));

        waitForElement(Locator.xpath("//input[@id='templateName']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//input[@id='templateName']"), PLATE_TEMPLATE_NAME);

        clickButton("Save & Close");

        goToProjectHome();

        _assayHelper.createAssayAndEdit("TZM-bl Neutralization (NAb)", MULTI_VIRUS_ASSAY_NAB)
                .setDescription(MULTI_VIRUS_ASSAY_NAB_DESC)
                .setPlateTemplate(PLATE_TEMPLATE_NAME)
                .saveAndClose();
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testWellgroupNames()
    {
        log("verifying multi plate NAb assay");
        waitAndClick(Locator.linkWithText(MULTI_VIRUS_ASSAY_NAB));
        importPlateData(TEST_ASSAY_NAB_MV_FILE1, "Polynomial");
        clickAndWait(Locator.linkWithText("View Results"));
        verifyWellgroupNamesPresent(WELLGROUP_NAMES);
    }

    private static final String fileBasedMetadataAssay = "NAb File-based Metadata Assay";

    @Test
    public void testFileMakerMetadata()
    {
        String assayName = createFileBasedMetadataAssay();

        clickAndWait(Locator.linkWithText(assayName));
        AssayImporter importer = new AssayImporter(this,
                new AssayImportOptions.ImportOptionsBuilder().
                        assayId("ptid + date").
                        visitResolver(AssayImportOptions.VisitResolverType.ParticipantVisit).
                        cutoff1("50").
                        cutoff2("80").
                        curveFitMethod("Five Parameter").
                        metadataFile(FILEMAKER_ASSAY_METADATA).
                        runFile(TEST_ASSAY_NAB_MV_FILE1).
                        build()
        );

        importer.doImport();

        clickAndWait(Locator.linkWithText("View Results"));
        verifyWellgroupNamesPresent(WELLGROUP_NAMES);
    }

    @Test
    public void testFileMakerIncompleteMetadata()
    {
        String assayName = createFileBasedMetadataAssay();

        clickAndWait(Locator.linkWithText(assayName));
        AssayImporter importer = new AssayImporter(this,
                new AssayImportOptions.ImportOptionsBuilder().
                        assayId("ptid + date").
                        visitResolver(AssayImportOptions.VisitResolverType.ParticipantVisit).
                        cutoff1("50").
                        cutoff2("80").
                        curveFitMethod("Five Parameter").
                        metadataFile(FILEMAKER_ASSAY_INCOMPLETE_METADATA).
                        runFile(TEST_ASSAY_NAB_MV_FILE1).
                        build()
        );

        importer.doImport();

        assertElementPresent(Locators.labkeyError.withText("Virus Wellgroup \"Virus 2\" does not exist in the metadata file. Was the plate template edited?"));
    }

    @Test
    public void testFileMakerExtraMetadata()
    {
        String assayName = createFileBasedMetadataAssay();

        clickAndWait(Locator.linkContainingText(assayName));
        AssayImporter importer = new AssayImporter(this,
                new AssayImportOptions.ImportOptionsBuilder().
                        assayId("ptid + date").
                        visitResolver(AssayImportOptions.VisitResolverType.ParticipantVisit).
                        cutoff1("50").
                        cutoff2("80").
                        curveFitMethod("Five Parameter").
                        metadataFile(FILEMAKER_ASSAY_EXTRA_METADATA).
                        runFile(TEST_ASSAY_NAB_MV_FILE1).
                        build()
        );

        importer.doImport();

        assertElementPresent(Locators.labkeyError.withText("Well group name \"Virus 3\" does not match any Virus well groups defined in plate template \"" + PLATE_TEMPLATE_NAME + "\""));
    }

    private String createFileBasedMetadataAssay()
    {
        if (!isElementPresent(Locator.linkWithText(fileBasedMetadataAssay)))
        {
            clickAndWait(Locator.linkWithText(MULTI_VIRUS_ASSAY_NAB));

            AssayDesignerPage assayDesigner = _assayHelper.copyAssayDesign();
            assayDesigner.setName(fileBasedMetadataAssay);
            assayDesigner.setMetaDataInputFormat(AssayDesignerPage.MetadataInputFormat.FILE_BASED);
            assayDesigner.saveAndClose();
        }

        return fileBasedMetadataAssay;
    }

    private void importPlateData(File dataFile, String curveFitMethod)
    {
        log("Uploading NAb Runs");
        clickButton("Import Data");
        clickButton("Next");

        setFormElement(Locator.name("cutoff1"), "50");
        setFormElement(Locator.name("cutoff2"), "70");
        selectOptionByText(Locator.name("curveFitMethod"), curveFitMethod);
        setFormElement(Locator.name("specimen01_InitialDilution"), "5");
        setFormElement(Locator.name("specimen01_Factor"), "42");
        selectOptionByText(Locator.name("specimen01_Method"), "Dilution");
        checkCheckbox(Locator.name("specimen01_InitialDilutionCheckBox"));
        checkCheckbox(Locator.name("specimen01_FactorCheckBox"));
        checkCheckbox(Locator.name("specimen01_MethodCheckBox"));

        setFormElement(Locator.xpath("//input[@type='file' and @name='__primaryFile__']"), dataFile);

        clickButton("Save and Finish");
    }

    private void verifyWellgroupNamesPresent(List<String> names)
    {
        DataRegionTable resultsTable = new DataRegionTable("Data", this);
        Assert.assertEquals(names, resultsTable.getColumnDataAsText("Wellgroup Name"));
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("Nab");
    }
}
