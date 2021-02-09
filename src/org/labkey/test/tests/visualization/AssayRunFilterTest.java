package org.labkey.test.tests.visualization;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.assay.GetProtocolCommand;
import org.labkey.remoteapi.assay.ImportRunCommand;
import org.labkey.remoteapi.assay.ImportRunResponse;
import org.labkey.remoteapi.assay.Protocol;
import org.labkey.remoteapi.assay.ProtocolResponse;
import org.labkey.remoteapi.assay.SaveProtocolCommand;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Charting;
import org.labkey.test.categories.DailyC;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.pages.assay.AssayDataPage;
import org.labkey.test.pages.assay.AssayRunsPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({DailyC.class, Charting.class})
public class AssayRunFilterTest extends BaseWebDriverTest
{
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        AssayRunFilterTest init = (AssayRunFilterTest) getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        goToProjectHome();
        new PortalHelper(this).addBodyWebPart("Assay List");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testAssayRunFilters() throws Exception
    {
        String assayName = "test_assay_for_run_filters";
        Protocol newAssay = getProtocol(assayName, "General");

        // add a couple of results fields
        Domain resultsDomain = newAssay.getDomains().get(2);
        List<PropertyDescriptor> resultsFields = resultsDomain.getFields();
        resultsFields.add(new PropertyDescriptor("height", "height", "double"));
        resultsFields.add(new PropertyDescriptor("weight", "weight", "double"));
        resultsDomain.setFields(resultsFields);
        Protocol serverProtocol = saveProtocol(createDefaultConnection(), newAssay);

        // add runs to
        // protocolId comes back as a long, but ImportRunCommand weirdly requires an int
        Integer protocolId = Integer.parseInt(serverProtocol.getProtocolId().toString());

        List<Map<String, Object>> runRecords = new ArrayList<>();
        runRecords.add(Map.of("height", 168, "weight", 83.2));
        runRecords.add(Map.of("height", 178, "weight", 93.2));
        runRecords.add(Map.of("height", 188, "weight", 87.2));
        runRecords.add(Map.of("height", 186, "weight", 94));

        ImportRunCommand importRunCommand = new ImportRunCommand(protocolId, runRecords);
        importRunCommand.setName("firstRun");
        importRunCommand.setBatchId(123);
        importRunCommand.execute(createDefaultConnection(), getProjectName());

        List<Map<String, Object>> run2Records = new ArrayList<>();
        run2Records.add(Map.of("height", 178, "weight", 63.2));
        run2Records.add(Map.of("height", 174, "weight", 65.2));
        run2Records.add(Map.of("height", 171, "weight", 67.2));

        ImportRunCommand importRunCommand2 = new ImportRunCommand(protocolId, run2Records);
        importRunCommand2.setName("secondRun");
        importRunCommand2.setBatchId(124);
        ImportRunResponse execute = importRunCommand2.execute(createDefaultConnection(), getProjectName());

        // create an unfiltered chart
        AssayDataPage resultsPage = goToAssayRunsPage(assayName).clickViewResults();
        DataRegionTable dataTable = resultsPage.getDataTable();
        dataTable.createChart()
                .setChartType(ChartTypeDialog.ChartType.Scatter)
                .setYAxis("height")
                .setXAxis("weight")
                .clickApply()
                .clickSave()
                .setReportName("HeightWeightAll").clickSave();

        // view the unfilteredChart, with a filter on run
        AssayRunsPage runsPage = goToAssayRunsPage(assayName);
        AssayDataPage dataPage = runsPage.clickAssayIdLink("firstRun");
        dataPage.getDataTable().clickReportMenu(false,"HeightWeightAll");
        String expected = "848688909294168170172174176178180182184186188Dataweightheight";
        assertSVG(expected, 0);

        // now filter on the other run and verify expected results
        dataPage = goToAssayRunsPage(assayName).clickAssayIdLink("secondRun");
        String secondRunExpected = "63.56464.56565.56666.567171172173174175176177178Dataweightheight";
        dataPage.getDataTable().clickReportMenu(false,"HeightWeightAll");
        assertSVG(secondRunExpected);

        // now verify filter built into a chart will be applied when also filtering based on run
        // create filter to show only firstRun results
        goToAssayRunsPage(assayName)
                .clickAssayIdLink("firstRun")
                .getDataTable()
                .createChart()
                .setChartType(ChartTypeDialog.ChartType.Scatter)
                .setYAxis("height")
                .setXAxis("weight")
                .clickApply()
                .clickSave()
                .setReportName("HeightWeightFirstRun").clickSave();
        // now filter to see only secondRun results
        goToAssayRunsPage(assayName)
                .clickAssayIdLink("secondRun")
                .getDataTable()
                .clickReportMenu(false, "HeightWeightFirstRun");
        // expect both filters to result in 0 records shown
        waitForText("The response returned 0 rows of data.");
        assertSVG("00.20.40.60.8100.10.20.30.40.50.60.70.80.91Dataweightheight");
    }

    private Protocol getProtocol(String assayName, String providerName) throws Exception
    {
        // get the template from the server
        Connection cn = createDefaultConnection();
        String containerPath = getProjectName();
        GetProtocolCommand getProtocolCommand = new GetProtocolCommand(providerName);
        ProtocolResponse getProtocolResponse = getProtocolCommand.execute(cn, containerPath);

        // modify the protocol according to our needs
        Protocol newAssayProtocol = getProtocolResponse.getProtocol();
        newAssayProtocol.setName(assayName);

        return newAssayProtocol;
    }

    private Protocol saveProtocol(Connection cn, Protocol protocol) throws Exception
    {
        SaveProtocolCommand saveProtocolCommand = new SaveProtocolCommand(protocol);
        ProtocolResponse savedProtocolResponse = saveProtocolCommand.execute(cn, getProjectName());
        return savedProtocolResponse.getProtocol();
    }

    private AssayRunsPage goToAssayRunsPage(String assayName)
    {
        goToProjectHome();
        clickAndWait(Locator.linkWithText("Assay List"));
        clickAndWait(Locator.linkWithText(assayName));
        return new AssayRunsPage(getDriver());
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "AssayRunFilterTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
