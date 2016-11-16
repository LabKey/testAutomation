package org.labkey.test.tests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.DailyB;
import org.labkey.test.etl.ETLAbstractTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PipelineAnalysisHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
public class PipelineProtocolArchiveTest extends ETLAbstractTest
{
    public static final String WEB_PART_NAME_PIPELINE_PROTOCOLS = "Pipeline Protocols";
    public static final String PROTOCOL_NAME_B_R = "brainRadio";
    public static final String PROTOCOL_NAME_A_M = "toBeArchived";
    public static final String PROTOCOL_NAME_NEW_PROTOCOL = "<New Protocol>";
    PortalHelper _portalHelper = new PortalHelper(this);
    private static final String DATA_INTEGRATION_MODULE = "DataIntegration";
    private static final String PIPELINE_TEST_MODULE = "pipelinetest";
    private static final String PIPELINE = "pipeline";

    private static final String SAMPLE_INPUT_FILE_NAME1 = "sample1.testIn.tsv";
    private static final String SAMPLE_INPUT_FILE_NAME2 = "sample2.testIn.tsv";
    private static final String SAMPLE_INPUT_FILE_NAME3 = "sample3.testIn.tsv";
    private static final String SAMPLE_INPUT_FILE_NAME4 = "sample4.testIn.tsv";



    @Nullable
    @Override
    protected String getProjectName()
    {
        return "Protocol Archive Test";
    }

    @Test
    public void  testArchiveProtocol() throws Exception
    {
        String PROTOCOL_DEF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bioml>\n" +
                "<note label=\"diseaseGroup\" type=\"input\">brain</note>\n" +
                "<note label=\"docType\" type=\"input\">not_a_value_in_the_xml</note>  \n" +
                "</bioml>";
        _containerHelper.createProject(getProjectName(), "Study");
        _containerHelper.enableModules(Arrays.asList(DATA_INTEGRATION_MODULE, "simpletest"));
        _containerHelper.enableModules(Arrays.asList(PIPELINE_TEST_MODULE, "Pipeline"));

        _portalHelper.addWebPart("Data Pipeline");
        _portalHelper.addWebPart(WEB_PART_NAME_PIPELINE_PROTOCOLS);

        DataRegionTable protocols = DataRegionTable.findDataRegionWithinWebpart(this, WEB_PART_NAME_PIPELINE_PROTOCOLS);
        Assert.assertEquals("Expecting empty protocol list",0,protocols.getDataRowCount());

        _portalHelper.assertTextPresent("No data to show.");

        runPipeline(PIPELINE, SAMPLE_INPUT_FILE_NAME1, PROTOCOL_NAME_B_R, PROTOCOL_DEF, false);

        clickProject(getProjectName());

        protocols = DataRegionTable.findDataRegionWithinWebpart(this, WEB_PART_NAME_PIPELINE_PROTOCOLS);
        String protocol1_Name = protocols.getDataAsText(0,0);
        Assert.assertEquals("Wrong protocol name", PROTOCOL_NAME_B_R,protocol1_Name);
        String protocol1_Pipeline = protocols.getDataAsText(0,1);
        Assert.assertEquals("Wrong pipeline name", getClientPipelineLabel(),protocol1_Pipeline);
        String protocol1_Archived = protocols.getDataAsText(0,2);
        Assert.assertEquals("Wrong archived indication", "",protocol1_Archived);


        runPipeline(PIPELINE, SAMPLE_INPUT_FILE_NAME2, PROTOCOL_NAME_A_M, PROTOCOL_DEF, false);

        clickProject(getProjectName());

        protocols = DataRegionTable.findDataRegionWithinWebpart(this, WEB_PART_NAME_PIPELINE_PROTOCOLS);
        String protocol2_Name = protocols.getDataAsText(1,0);
        Assert.assertEquals("Wrong protocol name", PROTOCOL_NAME_A_M,protocol2_Name);
        String protocol2_Archived = protocols.getDataAsText(1,2);
        Assert.assertEquals("Wrong archived indication", "",protocol2_Archived);

        prepareToRunPipeline(PIPELINE, SAMPLE_INPUT_FILE_NAME3);
        Locator.IdLocator protocolSelect = Locator.id("protocolSelect");
        waitForElement(protocolSelect);
        List<String> protocolOptions = getSelectOptions(protocolSelect);
        Assert.assertEquals("Expected protocol count", 3, protocolOptions.size() );
        Assert.assertEquals("Expected protocol " + PROTOCOL_NAME_NEW_PROTOCOL, PROTOCOL_NAME_NEW_PROTOCOL, protocolOptions.get(0) );
        Assert.assertEquals("Expected protocol " + PROTOCOL_NAME_B_R, PROTOCOL_NAME_B_R, protocolOptions.get(1) );
        Assert.assertEquals("Expected protocol " + PROTOCOL_NAME_A_M, PROTOCOL_NAME_A_M, protocolOptions.get(2) );

        clickProject(getProjectName());
        protocols.checkCheckbox(1);
        protocols.getHeaderButtons().get(1).click();
        getAlertIfPresent().accept();
        protocol2_Archived = protocols.getDataAsText(1,2);
        Assert.assertNotEquals("Wrong archived indication", "",protocol2_Archived);


        prepareToRunPipeline(PIPELINE, SAMPLE_INPUT_FILE_NAME4);
        protocolSelect = Locator.id("protocolSelect");
        waitForElement(protocolSelect);
        protocolOptions = getSelectOptions(protocolSelect);
        Assert.assertEquals("Expected protocol count", 2, protocolOptions.size() );
        Assert.assertEquals("Expected protocol " + PROTOCOL_NAME_NEW_PROTOCOL, PROTOCOL_NAME_NEW_PROTOCOL, protocolOptions.get(0) );
        Assert.assertEquals("Expected protocol " + PROTOCOL_NAME_B_R, PROTOCOL_NAME_B_R, protocolOptions.get(1) );

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



}
