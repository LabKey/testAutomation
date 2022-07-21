package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.exp.SampleTypeAPIHelper;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *     Address <a href="https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=45861">Issue 45861: When sample types are renamed, the custom views associated with the original name are lost</a>.
 * </p>
 * <p>
 *     A more general sample type rename test can be found in {@link SampleTypeTest#testSampleTypeNames}.
 * </p>
 */
@Category({Daily.class})
public class SampleTypeRenameTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "SampleType_Rename_Test";
    private static final String SAMPLE_TYPE_NAME_INITIAL = "Sample Type To Be Renamed";
    private static final String SAMPLE_TYPE_NAME_UPDATED = "A New Name For This Sample Type";
    private static final String FIELD_STR = "Str";
    private static final String FIELD_INT = "Int";
    private static final Locator editButton = Locator.lkButton("Edit Type");

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("experiment");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        SampleTypeRenameTest init = (SampleTypeRenameTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws IOException, CommandException
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        new PortalHelper(this).addWebPart("Sample Types");

        log(String.format("Create sample type named '%s' with fields '%s' and '%s'.", SAMPLE_TYPE_NAME_INITIAL, FIELD_INT, FIELD_STR));

        SampleTypeDefinition sampleTypeDefinition = new SampleTypeDefinition(SAMPLE_TYPE_NAME_INITIAL);
        sampleTypeDefinition.setNameExpression("S-${genId}");
        sampleTypeDefinition.addField(new FieldDefinition(FIELD_STR, FieldDefinition.ColumnType.String));
        sampleTypeDefinition.addField(new FieldDefinition(FIELD_INT, FieldDefinition.ColumnType.Integer));

        log("Give the sample type some data, will be used to create a chart.");
        TestDataGenerator testDataGenerator = SampleTypeAPIHelper.createEmptySampleType(getCurrentContainerPath(), sampleTypeDefinition);
        testDataGenerator.addCustomRow(Map.of(FIELD_STR, "A", FIELD_INT, 50));
        testDataGenerator.addCustomRow(Map.of(FIELD_STR, "A", FIELD_INT, 25));
        testDataGenerator.addCustomRow(Map.of(FIELD_STR, "B", FIELD_INT, 25));
        testDataGenerator.addCustomRow(Map.of(FIELD_STR, "C", FIELD_INT, 30));
        testDataGenerator.insertRows();
    }

    @Test
    public void testCustomViewWithSampleTypeRename()
    {
        goToProjectHome();

        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this).goToSampleType(SAMPLE_TYPE_NAME_INITIAL);

        DataRegionTable dataRegionTable = sampleTypeHelper.getSamplesDataRegionTable();
        CustomizeView customizeView = dataRegionTable.getCustomizeView();

        String customViewName = "No_Int_View";
        String removedColumn = FIELD_INT;

        log(String.format("Create a custom view named '%s' that doesn't have the '%s' field.", customizeView, removedColumn));

        customizeView.openCustomizeViewPanel();
        customizeView.removeColumn(removedColumn);
        customizeView.saveCustomView(customViewName);

        log("Validate that the custom view was created as expected.");

        BootstrapMenu menu = dataRegionTable.getViewsMenu();

        menu.expand();

        List<String> menuItems = menu.findVisibleMenuItems()
                .stream()
                .map(WebElement::getText)
                .toList();

        checker().fatal()
                .verifyTrue(String.format("Doesn't look like custom view '%s' was saved. Fatal error.", customViewName),
                menuItems.contains(customViewName));

        log(String.format("Rename the sample type to '%s'.", SAMPLE_TYPE_NAME_UPDATED));

        clickAndWait(editButton);
        UpdateSampleTypePage updateSampleTypePage = new UpdateSampleTypePage(getDriver());

        updateSampleTypePage.setName(SAMPLE_TYPE_NAME_UPDATED);
        updateSampleTypePage.clickSave();

        verifyCustomView(sampleTypeHelper, customViewName, removedColumn);

        log(String.format("Change name of the sample type back to '%s' and validate view still there.", SAMPLE_TYPE_NAME_INITIAL));

        clickAndWait(editButton);
        updateSampleTypePage = new UpdateSampleTypePage(getDriver());

        updateSampleTypePage.setName(SAMPLE_TYPE_NAME_INITIAL);
        updateSampleTypePage.clickSave();

        verifyCustomView(sampleTypeHelper, customViewName, removedColumn);

    }

    private void verifyCustomView(SampleTypeHelper sampleTypeHelper, String customViewName, String missingColumn)
    {

        log(String.format("Validate that the custom view '%s' is in the menu.", customViewName));

        DataRegionTable dataRegionTable = sampleTypeHelper.getSamplesDataRegionTable();
        BootstrapMenu menu = dataRegionTable.getViewsMenu();

        menu.expand();

        List<String> menuItems = menu.findVisibleMenuItems()
                .stream()
                .map(WebElement::getText)
                .toList();

        if(checker().withScreenshot("CustomView_Missing_Error")
                .verifyTrue(String.format("Custom view '%s' is not in the menu.", customViewName),
                        menuItems.contains(customViewName)))
        {
            log("Validate that clicking the custom view name does indeed show the custom view.");
            menu.clickSubMenu(true, customViewName);

            dataRegionTable = sampleTypeHelper.getSamplesDataRegionTable();
            List<String> columnNames = dataRegionTable.getColumnNames();

            checker().withScreenshot("Column_Headers_Error")
                    .verifyFalse(String.format("Column '%s' should not be present, but it is.", missingColumn),
                            columnNames.contains(missingColumn));
        }
        else
        {
            log("Custom view was not present in the menu so cannot check if the view works.");
        }

    }

    @Test
    public void testSavedChartsWithSampleTypeRename()
    {
        goToProjectHome();

        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this).goToSampleType(SAMPLE_TYPE_NAME_INITIAL);

        String chartName = "My Bar Chart";
        log(String.format("Create a bar chart named '%s' from the data in sample type '%s'.", chartName, SAMPLE_TYPE_NAME_INITIAL));

        DataRegionTable dataRegionTable = sampleTypeHelper.getSamplesDataRegionTable();
        ChartTypeDialog chartDialog = dataRegionTable.createChart();
        chartDialog.setChartType(ChartTypeDialog.ChartType.Bar);
        chartDialog.setXAxis(FIELD_STR);
        chartDialog.setYAxis(FIELD_INT);
        chartDialog.clickApply().saveReport(chartName);

        log("Verify the chart was created as expected.");

        goToProjectHome();
        sampleTypeHelper = new SampleTypeHelper(this).goToSampleType(SAMPLE_TYPE_NAME_INITIAL);

        String formatSvgData = "ABC010203040506070%sStrSumofInt";
        String expectedSVG = String.format(formatSvgData, SAMPLE_TYPE_NAME_INITIAL.replace(" ", ""));

        checker().fatal().verifyTrue("Chart was not created as expected. Fatal error.",
                verifyBarchart(sampleTypeHelper, chartName, expectedSVG));

        log(String.format("Rename the sample type to '%s' and validate that the saved chart is visible.", SAMPLE_TYPE_NAME_UPDATED));

        goToProjectHome();
        sampleTypeHelper = new SampleTypeHelper(this).goToSampleType(SAMPLE_TYPE_NAME_INITIAL);

        clickAndWait(editButton);
        UpdateSampleTypePage updateSampleTypePage = new UpdateSampleTypePage(getDriver());

        updateSampleTypePage.setName(SAMPLE_TYPE_NAME_UPDATED);
        updateSampleTypePage.clickSave();

        verifyBarchart(sampleTypeHelper, chartName, expectedSVG);

        log(String.format("Rename the sample type back to '%s' and again validate the chart is visible.", SAMPLE_TYPE_NAME_INITIAL));

        goToProjectHome();
        sampleTypeHelper = new SampleTypeHelper(this).goToSampleType(SAMPLE_TYPE_NAME_UPDATED);

        clickAndWait(editButton);
        updateSampleTypePage = new UpdateSampleTypePage(getDriver());

        updateSampleTypePage.setName(SAMPLE_TYPE_NAME_INITIAL);
        updateSampleTypePage.clickSave();

        verifyBarchart(sampleTypeHelper, chartName, expectedSVG);

    }

    private boolean verifyBarchart(SampleTypeHelper sampleTypeHelper, String chartName, String expectedSVG)
    {

        boolean pass;

        log(String.format("Validate that the chart '%s' is in the menu.", chartName));

        BootstrapMenu menu = sampleTypeHelper.getSamplesDataRegionTable().getReportMenu();
        menu.expand();

        List<String> menuItems = menu.findVisibleMenuItems().stream().map(WebElement::getText).toList();

        if(checker().withScreenshot("Chart_Menu_Missing")
                .verifyTrue(String.format("Chart '%s' is not in the menu.", chartName), menuItems.contains(chartName)))
        {

            log("Click on the chart name in the menu and validate the chart is shown.");

            menu.clickSubMenu(true, chartName);
            pass = checker().wrapAssertion(()->assertSVG(expectedSVG));
        }
        else
        {
            log("Chart name was not in the menu, cannot validate chart.");
            pass = false;
        }

        return pass;
    }

}
