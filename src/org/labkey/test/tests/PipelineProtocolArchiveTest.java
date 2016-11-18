package org.labkey.test.tests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PipelineAnalysisHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
public class PipelineProtocolArchiveTest extends BaseWebDriverTest
{
    public static final String WEB_PART_NAME_PIPELINE_PROTOCOLS = "Pipeline Protocols";
    public static final String PROTOCOL_NAME_B_R = "brainRadio";
    public static final String PROTOCOL_NAME_A_M = "toBeArchived";
    public static final String PROTOCOL_NAME_NEW_PROTOCOL = "<New Protocol>";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PIPELINE = "pipeline";
    public static final String COLUMN_ARCHIVED = "Archived";
    public static final int INDEX_ARCHIVE_BUTTON = 1;
    public static final int INDEX_UNARCHIVE_BUTTON = 2;
    PortalHelper _portalHelper = new PortalHelper(this);

    private static final String SIMPLETEST_MODULE = "simpletest";
    private static final String PIPELINE_MODULE = "Pipeline";
    private static final String DATA_INTEGRATION_MODULE = "DataIntegration";
    private static final String PIPELINE_TEST_MODULE = "pipelinetest";
    private static final String PIPELINE = "pipeline";
    //Files in /sampledata/pipeline
    private static final String SAMPLE_INPUT_FILE_NAME1 = "sample1.testIn.tsv";
    private static final String SAMPLE_INPUT_FILE_NAME2 = "sample2.testIn.tsv";
    private static final String SAMPLE_INPUT_FILE_NAME3 = "sample3.testIn.tsv";
    private static final String SAMPLE_INPUT_FILE_NAME4 = "sample4.testIn.tsv";
    private static final String SAMPLE_INPUT_FILE_NAME5 = "sample5.testIn.tsv";



    @Nullable
    protected String getProjectName()
    {
        return "Protocol Archive Test";
    }

    @Before
    public void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Study");
        _containerHelper.enableModules(Arrays.asList(DATA_INTEGRATION_MODULE, SIMPLETEST_MODULE,PIPELINE_TEST_MODULE, PIPELINE_MODULE));

        _portalHelper.addWebPart("Data Pipeline");
        _portalHelper.addWebPart(WEB_PART_NAME_PIPELINE_PROTOCOLS);

    }
        @Test
    public void  testArchiveProtocol() throws Exception
    {
        String PROTOCOL_DEF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bioml>\n" +
                "<note label=\"diseaseGroup\" type=\"input\">brain</note>\n" +
                "<note label=\"docType\" type=\"input\">not_a_value_in_the_xml</note>  \n" +
                "</bioml>";

        DataRegionTable protocols = DataRegionTable.findDataRegionWithinWebpart(this, WEB_PART_NAME_PIPELINE_PROTOCOLS);
        Assert.assertEquals("Expecting empty protocol list",0,protocols.getDataRowCount());

        assertTextPresent("No data to show.");

        runPipeline(PIPELINE, SAMPLE_INPUT_FILE_NAME1, PROTOCOL_NAME_B_R, PROTOCOL_DEF, false);

        clickProject(getProjectName());

        String protocol1_Name = protocols.getDataAsText(0,COLUMN_NAME);
        Assert.assertEquals("Wrong protocol name", PROTOCOL_NAME_B_R,protocol1_Name);
        String protocol1_Pipeline = protocols.getDataAsText(0, COLUMN_PIPELINE);
        Assert.assertEquals("Wrong pipeline name", getClientPipelineLabel(),protocol1_Pipeline);
        String protocol1_Archived = protocols.getDataAsText(0, COLUMN_ARCHIVED);
        Assert.assertEquals("Wrong archived indication", "",protocol1_Archived);


        runPipeline(PIPELINE, SAMPLE_INPUT_FILE_NAME2, PROTOCOL_NAME_A_M, PROTOCOL_DEF, false);

        clickProject(getProjectName());

        String protocol2_Name = protocols.getDataAsText(1,COLUMN_NAME);
        Assert.assertEquals("Wrong protocol name", PROTOCOL_NAME_A_M,protocol2_Name);
        String protocol2_Archived = protocols.getDataAsText(1,COLUMN_ARCHIVED);
        Assert.assertEquals("Wrong archived indication", "",protocol2_Archived);

        List<String> expectedOptions = new ArrayList<>(Arrays.asList(
                PROTOCOL_NAME_NEW_PROTOCOL,
                PROTOCOL_NAME_B_R,
                PROTOCOL_NAME_A_M));

        confirmProtocolSelectList(expectedOptions, SAMPLE_INPUT_FILE_NAME3);

        clickProject(getProjectName());
        protocols.checkCheckbox(1);
        doAndWaitForPageToLoad(()-> clickArchiveAndAcceptAlert(protocols));
        protocol2_Archived = protocols.getDataAsText(1,COLUMN_ARCHIVED);
        Assert.assertNotEquals("Wrong archived indication", "",protocol2_Archived);

        expectedOptions.remove(2);

        confirmProtocolSelectList(expectedOptions, SAMPLE_INPUT_FILE_NAME4);

        clickProject(getProjectName());
        protocols.checkCheckbox(1);
        doAndWaitForPageToLoad(()-> clickUnarchiveAndAcceptAlert(protocols));
        protocol2_Archived = protocols.getDataAsText(1,COLUMN_ARCHIVED);
        Assert.assertEquals("Wrong archived indication", "",protocol2_Archived);

        expectedOptions.add(2,PROTOCOL_NAME_A_M);

        confirmProtocolSelectList(expectedOptions, SAMPLE_INPUT_FILE_NAME5);

    }

    private void confirmProtocolSelectList(List<String> expectedOptions, String fileName)
    {
        Locator.IdLocator protocolSelect = Locator.id("protocolSelect");
        prepareToRunPipeline(PIPELINE, fileName);

        waitForElement(protocolSelect);
        List<String> protocolOptions = getSelectOptions(protocolSelect);
        Assert.assertEquals("Expected protocols " , expectedOptions, protocolOptions);
    }

    private void clickArchiveAndAcceptAlert(DataRegionTable protocols)
    {
        protocols.getHeaderButtons().get(INDEX_ARCHIVE_BUTTON).click();
        assertAlert("Are you sure you want to archive the selected protocol?");
    }

    private void clickUnarchiveAndAcceptAlert(DataRegionTable protocols)
    {
        protocols.getHeaderButtons().get(INDEX_UNARCHIVE_BUTTON).click();
        assertAlert("Are you sure you want to unarchive the selected protocol?");
    }

    protected void runPipeline(@Nullable String dir, @NotNull String fileName, @NotNull String protocolName, @Nullable String protocolDef, boolean expectError)
    {
        PipelineAnalysisHelper _pipelineHelper = prepareToRunPipeline(dir, fileName);

        _pipelineHelper.runProtocol(protocolName, protocolDef, false);

        goToModule("Pipeline");
        assertTrue("Import did not complete.", getPipelineStatusValues().contains(expectError ? "ERROR" : "COMPLETE"));

        checkExpectedErrors(expectError ? 1 : 0);
    }

    @NotNull
    private PipelineAnalysisHelper prepareToRunPipeline(@Nullable String dir, @NotNull String fileName)
    {
        goToProjectHome();
        PipelineAnalysisHelper _pipelineHelper = new PipelineAnalysisHelper(this);
        goToModule("Pipeline"); // this seems more robust than using the button on the home page.
        clickButton("Process and Import Data");
        File testFile = null == dir ? TestFileUtils.getSampleData(fileName) : new File(TestFileUtils.getSampleData(dir), fileName);
        _fileBrowserHelper.uploadFile(testFile);
        _fileBrowserHelper.importFile(fileName, getClientPipelineLabel());
        return _pipelineHelper;
    }

    protected String getClientPipelineLabel()
    {
        return "Simpletest tail";
    }


    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
