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
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Category({InDevelopment.class})
public class ProteinExpresssionMatrixAssayTest extends BaseWebDriverTest
{
    private static final String ASSAY_NAME = "Test Protein Expression Matrix";
    private static final String FASTA_FILENAME = "Bovine_mini.fasta";
    private static final String FOLDER_TYPE_MS2 = "MS2";
    private static final String FOLDER_NAME = "ProteinExpressionMatrixFolder";

    private static final String PATH_TO_DATA_DIR = "/ms2/matrix/";
    private static final String FASTA_FILE_LOCATION = "/sampledata/xarfiles/ms2pipe/databases/";

    private static final String TSV_FILE_NAME = "MatchesFasta.tsv";
    private static final String EXCEL_FILE_NAME = "MatchesFasta.xlsx";
    private static final String TSV_FILE_NAME_BAD_DATA = "DoesNotMatchFasta.tsv";
    private static final String EXCEL_FILE_NAME_BAD_DATA = "DoesNotMatchFasta.xlsx";
    private static final String TSV_FILE_NAME_DUPLICATE = "Duplicates.tsv";
    private static final String EXCEL_FILE_NAME_DUPLICATE = "Duplicates.xlsx";

    private static final List<String> SAMPLE_IDS = Arrays.asList("Condition B", "Condition D", "Condition E", "Condition F", "Condition G", "Condition H");


    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);

        //remove protein database
        beginAt("ms2/showProteinAdmin.view?"); //go to Protein Database Admin

        final DataRegionTable annotInsertions = new DataRegionTable("AnnotInsertions", this);

        annotInsertions.setFilter("FileName", "Contains", FASTA_FILENAME);
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
                    getAlert();
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

        clickButton("Load New Annot File");
        setFormElement(Locator.id("fname"), TestFileUtils.getLabKeyRoot() + FASTA_FILE_LOCATION + FASTA_FILENAME);
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
        importProteinExpressionData("testingTSV", TSV_FILE_NAME);
    }

    @Test
    public void testExcelImport()
    {
        importProteinExpressionData("testingExcel", EXCEL_FILE_NAME);
    }

    @Test
    public void testBadDataInTSV()
    {
        importProteinExpressionWithBadData(TSV_FILE_NAME_BAD_DATA);
    }

    @Test
    public void testBadDataInExcel()
    {
        importProteinExpressionWithBadData(EXCEL_FILE_NAME_BAD_DATA);
    }

    @Test
    public void testDuplicatesInTSV()
    {
        importProteinExpressionWithDuplicates(TSV_FILE_NAME_DUPLICATE);
    }

    @Test
    public void testDuplicatesInExcel()
    {
        importProteinExpressionWithDuplicates(EXCEL_FILE_NAME_DUPLICATE);
    }


    private void importProteinExpressionData(String assayId, String exprDataFileName)
    {
        click(Locator.linkContainingText(ASSAY_NAME));
        click(Locator.linkWithSpan("Import Data"));
        setFormElement(Locator.name("name"), assayId);
        selectOptionByText(Locator.name("fastaFormatProteinSequences"), TestFileUtils.getLabKeyRoot() + FASTA_FILE_LOCATION + FASTA_FILENAME);
        setFormElement(Locator.name("__primaryFile__"), TestFileUtils.getSampleData(PATH_TO_DATA_DIR + exprDataFileName));
        click(Locator.linkWithSpan("Save and Finish"));

        testCorrectLookupType();
        testLinks(assayId);
        ensureEmptyConditionValuesHandled(assayId); //test empty conditions are handled properly
    }

    private void importProteinExpressionWithBadData(String exprBadDataFileName)
    {
        pushLocation();

        click(Locator.linkContainingText(ASSAY_NAME));
        click(Locator.linkWithSpan("Import Data"));
        selectOptionByText(Locator.name("fastaFormatProteinSequences"), TestFileUtils.getLabKeyRoot() + FASTA_FILE_LOCATION + FASTA_FILENAME);
        setFormElement(Locator.name("__primaryFile__"), TestFileUtils.getSampleData(PATH_TO_DATA_DIR + exprBadDataFileName));
        click(Locator.linkWithSpan("Save and Finish"));

        assertTextPresent("Unable to find Protein '30S_ribosomal_pro_NotInFasta' in the selected Fasta/Uniprot file.");

        popLocation();
    }

    private void importProteinExpressionWithDuplicates(String exprDataWithDuplicates)
    {
        pushLocation();

        click(Locator.linkContainingText(ASSAY_NAME));
        click(Locator.linkWithSpan("Import Data"));
        selectOptionByText(Locator.name("fastaFormatProteinSequences"), TestFileUtils.getLabKeyRoot() + FASTA_FILE_LOCATION + FASTA_FILENAME);
        setFormElement(Locator.name("__primaryFile__"), TestFileUtils.getSampleData(PATH_TO_DATA_DIR + exprDataWithDuplicates));
        click(Locator.linkWithSpan("Save and Finish"));

        assertTextPresent("duplicate key value violates unique constraint"); //TODO: This error message may change. Change this error message accordingly.

        popLocation();
    }

    private void testCorrectLookupType()
    {
        _assayHelper.clickEditAssayDesign();
        assertTextPresent("protein.FastaFiles");
        click(Locator.linkWithSpan("Cancel"));
    }

    private void testLinks(String assayId)
    {
        click(Locator.linkContainingText(assayId));

        String currentRelativeURL1 = getCurrentRelativeURL();
        pushLocation();
        click(Locator.linkContainingText(assayId));
        String currentRelativeURL2 = getCurrentRelativeURL();
        assertNotEquals(assayId + " in Run column is not a link.", currentRelativeURL1, currentRelativeURL2);
        popLocation();

        currentRelativeURL1 = getCurrentRelativeURL();
        pushLocation();
        assertTextPresent("gi|18071135|gb|AAL58190.1|");
        click(Locator.linkContainingText("gi|18071135|gb|AAL58190.1|"));
        currentRelativeURL2 = getCurrentRelativeURL();
        assertNotEquals("'gi|18071135|gb|AAL58190.1|' in Protein column is not a link.", currentRelativeURL1, currentRelativeURL2);
        popLocation();

        currentRelativeURL1 = getCurrentRelativeURL();
        pushLocation();
        click(Locator.linkContainingText("Condition G"));
        currentRelativeURL2 = getCurrentRelativeURL();
        assertNotEquals("'Condition G' in Sample Id column is not a link.", currentRelativeURL1, currentRelativeURL2);
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