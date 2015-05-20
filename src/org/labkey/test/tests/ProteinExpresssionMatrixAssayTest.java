/*
 * Copyright (c) 2015 LabKey Corporation
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
/**
 * Created by binalpatel on 4/28/15.
 */

import com.google.common.base.Function;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Category({DailyB.class})
public class ProteinExpresssionMatrixAssayTest extends BaseWebDriverTest
{
    private static final String ASSAY_NAME = "Test Protein Expression Matrix";
    private static final String FOLDER_TYPE_MS2 = "MS2";
    private static final String FOLDER_NAME = "ProteinExpressionMatrixFolder";

    private static final File FASTA_FILE = new File("/sampledata/xarfiles/ms2pipe/databases/Bovine_mini.fasta");
    private static final File TSV_FILE = new File("/ms2/matrix/MatchesFasta.tsv");
    private static final File EXCEL_FILE = new File("/ms2/matrix/MatchesFasta.xlsx");
    private static final File TSV_FILE_BAD_DATA = new File("/ms2/matrix/DoesNotMatchFasta.tsv");
    private static final File EXCEL_FILE_BAD_DATA = new File("/ms2/matrix/DoesNotMatchFasta.xlsx");
    private static final File TSV_FILE_DUPLICATE = new File("/ms2/matrix/Duplicates.tsv");
    private static final File EXCEL_FILE_DUPLICATE = new File("/ms2/matrix/Duplicates.xlsx");

    private static final List<String> SAMPLE_IDS = Arrays.asList("Condition B", "Condition D", "Condition E", "Condition F", "Condition G", "Condition H");


    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        //delete record of the import
        beginAt("ms2/showProteinAdmin.view?"); //go to Protein Database Admin

        final DataRegionTable annotInsertions = new DataRegionTable("AnnotInsertions", this);

        annotInsertions.setFilter("FileName", "Contains", FASTA_FILE.getName());
        List<String> fileNames = annotInsertions.getColumnDataAsText("FileName");
        if(fileNames.size() > 0)
        {
            annotInsertions.checkAll();
            applyAndWaitForPageToLoad(new Function<Void, Void>()
            {
                @Override
                public Void apply(Void aVoid)
                {
                    annotInsertions.clickHeaderButtonByText("Delete");
                    acceptAlert();
                    return null;
                }
            });
        }
    }

    @BeforeClass
    public static void setupProject()
    {
        ProteinExpresssionMatrixAssayTest init = (ProteinExpresssionMatrixAssayTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), FOLDER_TYPE_MS2);

        //load sample protein database
        beginAt("ms2/showProteinAdmin.view?");

        clickButton("Import Data");
        setFormElement(Locator.id("fname"), TestFileUtils.getLabKeyRoot() + FASTA_FILE);
        selectOptionByText(Locator.name("fileType"), "fasta");
        clickButton("Load Annotations");

        //create a folder of type MS2
        goToProjectHome();
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME);
        goToProjectHome(getProjectName());
        clickFolder(FOLDER_NAME);
        _containerHelper.setFolderType(FOLDER_TYPE_MS2);

        //create New Assay Design of type 'Protein Expression Matrix'
        _assayHelper.createAssayWithDefaults("Protein Expression Matrix", ASSAY_NAME);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testTSVImport()
    {
        importProteinExpressionData("testingTSV", TSV_FILE);
    }

    @Test
    public void testExcelImport()
    {
        importProteinExpressionData("testingExcel", EXCEL_FILE);
    }

    @Test
    public void testBadDataInTSV()
    {
        importProteinExpressionWithBadData(TSV_FILE_BAD_DATA);
    }

    @Test
    public void testBadDataInExcel()
    {
        importProteinExpressionWithBadData(EXCEL_FILE_BAD_DATA);
    }

    @Test
    public void testDuplicatesInTSV()
    {
        importProteinExpressionWithDuplicates(TSV_FILE_DUPLICATE);
    }

    @Test
    public void testDuplicatesInExcel()
    {
        importProteinExpressionWithDuplicates(EXCEL_FILE_DUPLICATE);
    }

    @Test
    public void testCorrectLookupType()
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        _assayHelper.clickEditAssayDesign();
        assertTextPresent("protein.FastaFiles");
        clickAndWait(Locator.linkWithSpan("Cancel"));
    }

    private void importProteinExpressionData(String assayId, File exprDataFile)
    {
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithSpan("Import Data"));
        setFormElement(Locator.name("name"), assayId);
        selectOptionByText(Locator.name("fastaFormatProteinSequences"), TestFileUtils.getLabKeyRoot() + FASTA_FILE.getPath());
        setFormElement(Locator.name("__primaryFile__"), TestFileUtils.getSampleData(exprDataFile.getPath()));
        clickAndWait(Locator.linkWithSpan("Save and Finish"));

        testLinks(assayId);
        ensureEmptyConditionValuesHandled(assayId); //test empty conditions are handled properly
    }

    private void importProteinExpressionWithBadData(File exprBadDataFile)
    {
        pushLocation();

        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithSpan("Import Data"));
        selectOptionByText(Locator.name("fastaFormatProteinSequences"), TestFileUtils.getLabKeyRoot() + FASTA_FILE.getPath());
        setFormElement(Locator.name("__primaryFile__"), TestFileUtils.getSampleData(exprBadDataFile.getPath()));
        clickAndWait(Locator.linkWithSpan("Save and Finish"));

        assertTextPresent("Unable to find protein '30S_ribosomal_pro_NotInFasta' in the selected FASTA file.");

        popLocation();
    }

    private void importProteinExpressionWithDuplicates(File exprDataWithDuplicates)
    {
        pushLocation();

        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithSpan("Import Data"));
        selectOptionByText(Locator.name("fastaFormatProteinSequences"), TestFileUtils.getLabKeyRoot() + FASTA_FILE.getPath());
        setFormElement(Locator.name("__primaryFile__"), TestFileUtils.getSampleData(exprDataWithDuplicates.getPath()));
        clickAndWait(Locator.linkWithSpan("Save and Finish"));

        assertTextPresent("SQLException"); //TODO: Issue 23232: Provide a useful and user readable error message while importing protein expression data with duplicate IDs.

        popLocation();
    }

    private void testLinks(String assayId)
    {
        clickAndWait(Locator.linkWithText(assayId));

        String currentRelativeURL1 = getCurrentRelativeURL();
        pushLocation();
        clickAndWait(Locator.linkWithText(assayId));
        String currentRelativeURL2 = getCurrentRelativeURL();
        assertNotEquals(assayId + " in Run column is not a link.", currentRelativeURL1, currentRelativeURL2);
        assertTextPresent("Standard Properties");
        popLocation();

        currentRelativeURL1 = getCurrentRelativeURL();
        pushLocation();
        clickAndWait(Locator.linkWithText("gi|18071135|gb|AAL58190.1|"));
        currentRelativeURL2 = getCurrentRelativeURL();
        assertNotEquals("'gi|18071135|gb|AAL58190.1|' in Protein column is not a link.", currentRelativeURL1, currentRelativeURL2);
        assertTextPresent("Protein Sequence");
        popLocation();

        currentRelativeURL1 = getCurrentRelativeURL();
        pushLocation();
        clickAndWait(Locator.linkWithText("Condition G"));
        currentRelativeURL2 = getCurrentRelativeURL();
        assertNotEquals("'Condition G' in Sample Id column is not a link.", currentRelativeURL1, currentRelativeURL2);
        assertTextPresent("Sample Condition G");
        popLocation();
    }

    private void ensureEmptyConditionValuesHandled(String assayId)
    {
        final DataRegionTable proteinCol = new DataRegionTable("Data", this);
        proteinCol.setFilter("SeqId", "Contains", "gi|628074|pir||JP0060");
        proteinCol.setSort("SampleId", SortDirection.ASC);
        List<String> columnDataAsText = proteinCol.getColumnDataAsText("Sample Id");
        assertEquals("Condition labels in Sample Id column does not match.", columnDataAsText, SAMPLE_IDS);
        proteinCol.clearFilter("SeqId");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "ProteinExpresssionMatrixAssayTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }

}