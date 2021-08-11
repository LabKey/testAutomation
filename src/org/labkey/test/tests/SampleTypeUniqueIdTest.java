package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.pages.experiment.CreateSampleTypePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;

import java.io.IOException;
import java.util.List;

@Category({Daily.class})
public class SampleTypeUniqueIdTest extends BaseWebDriverTest
{
    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        SampleTypeUniqueIdTest init = (SampleTypeUniqueIdTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), "Collaboration");

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.enterAdminMode();
        portalHelper.addWebPart("Sample Types");
        portalHelper.exitAdminMode();
    }

    @Test
    public void testSampleTypeCreateUniqueIdMessaging()
    {
        String sampleTypeName = "SampleTypeCreateUniqueIdMessaging";
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        log("Go to the Sample Type creation designer");
        goToProjectHome();
        CreateSampleTypePage createPage = sampleTypeHelper.goToCreateNewSampleType();
        createPage.setName(sampleTypeName);

        log("Verify initial properties panel Barcodes message");
        checker().verifyTrue("Barcodes initial message not shown", createPage.hasUniqueIdMsg());
        checker().verifyFalse("Barcodes initial message should not have check icon", createPage.hasUniqueIdCheckIcon());
        checker().verifyEquals("Barcode initial message text not as expected", "Not currently enabled for this sample type", createPage.getUniqueIdMsg());

        log("Verify the fields panel info message to add a Barcode field");
        createPage.addField(new FieldDefinition("txtField"));
        checker().verifyTrue("Field panel info alert text not as expected",
                createPage.getFieldsPanel().getPanelAlertText().startsWith("Do you want to add a unique ID field to create barcodes for this sample type?"));

        log("Add and remove uniqueId field and verify banner updates");
        createPage.clickUniqueIdAlertAddButton();
        checker().verifyEquals("Field panel info alert didn't disappear", "", createPage.getFieldsPanel().getPanelAlertText());
        DomainFieldRow barcodeField = createPage.getFieldsPanel().getField("Barcode");
        checker().fatal().verifyTrue("Barcode field not found", barcodeField != null);
        checker().verifyEquals("Barcode field type not as expected", FieldDefinition.ColumnType.Barcode, barcodeField.getType());
        barcodeField.clickRemoveField(false);
        checker().verifyTrue("Field panel info alert text not as expected",
                createPage.getFieldsPanel().getPanelAlertText().startsWith("Do you want to add a unique ID field to create barcodes for this sample type?"));

        log("Add the uniqueId field and verify properties panel message updates");
        createPage.clickUniqueIdAlertAddButton();
        checker().verifyTrue("Barcodes updated message not shown", createPage.hasUniqueIdMsg());
        checker().verifyTrue("Barcodes updated message should have check icon", createPage.hasUniqueIdCheckIcon());
        checker().verifyEquals("Barcode updated message text not as expected", "A Unique ID field for barcodes is defined: Barcode", createPage.getUniqueIdMsg());

        log("Add a second uniqueId field and verify the properties panel message updates");
        String secondUniqueIdName = "SecondUniqueId";
        createPage.addField(new FieldDefinition(secondUniqueIdName, FieldDefinition.ColumnType.Barcode));
        checker().verifyEquals("Barcode updated message text not as expected", "2 Unique ID fields are defined: Barcode, " + secondUniqueIdName, createPage.getUniqueIdMsg());

        log("Save sample type and verify barcode fields show in grid");
        createPage.clickSave();
        sampleTypeHelper.goToSampleType(sampleTypeName);
        List<String> actualNames = sampleTypeHelper.getSamplesDataRegionTable().getColumnNames();
        checker().verifyTrue("Grid does not contain expected unique ID field", actualNames.contains("Barcode"));
        checker().verifyTrue("Grid does not contain expected unique ID field", actualNames.contains(secondUniqueIdName));
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "Sample Type UniqueId Test Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);

    }
}
