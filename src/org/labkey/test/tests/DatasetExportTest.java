package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.util.DataRegionTable;

@Category({DailyB.class})
public class DatasetExportTest extends AssayResultsExportTest
{
    @Override
    protected String getProjectName()
    {
        return "Dataset Download Test";
    }

    protected String getFolderName()
    {
        return "SimpleStudy";
    }

    @Override
    protected int getTestColumnIndex()
    {
        return 0;
    }

    @Override
    protected String getExportedTsvTestColumnHeader()
    {
        return "participantId";
    }

    @Override
    protected String getExportedFilePrefixRegex()
    {
        return ASSAY_NAME;
    }

    @Override
    protected String getDataRegionId()
    {
        return "Dataset";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        DatasetExportTest initTest = new DatasetExportTest();
        initTest.doCleanup(false);

        initTest.setupDataset();

        currentTest = initTest;
    }

    public void setupDataset() throws Exception
    {
        super.setupTestContainers();

        _containerHelper.createSubfolder(getProjectName(), getFolderName(), "Study");
        clickButton("Create Study");
        checkRadioButton(Locator.radioButtonByNameAndValue("timepointType", "DATE"));
        clickButton("Create Study");
        clickProject(getProjectName());
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithText(ASSAY_RUN_FILE.getName()));

        DataRegionTable assayResults = new DataRegionTable(super.getDataRegionId(), this);
        assayResults.checkAll();
        clickButton("Copy to Study");
        selectOptionByText(Locator.name("targetStudy"), "/" + getProjectName() + "/" + getFolderName() + " (" + getFolderName() + " Study)");
        clickButton("Next");
        clickButton("Copy to Study");
    }

    @Override
    protected void goToDataRegionPage()
    {
        clickProject(getProjectName());
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText("1 dataset"));
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }
}
