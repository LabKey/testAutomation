package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.experiment.UpdateSampleTypePage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SampleTypeHelper;

import java.io.File;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class AttachmentFieldTest extends BaseWebDriverTest
{
    private final File SAMPLE_PDF = new File(TestFileUtils.getSampleData("fileTypes"), "pdf_sample.pdf");

    @BeforeClass
    public static void setupProject()
    {
        AttachmentFieldTest init = (AttachmentFieldTest) getCurrentTest();
        init.doSetup();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return getCurrentTestClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.addBodyWebPart("Sample Types");
        portalHelper.addBodyWebPart("Lists");
    }

    @Test
    public void testAttachmentFieldInSampleType()
    {
        String sampleTypeName = "Sample type with attachment";
        String fieldName = "pdfFile";
        SampleTypeHelper sampleTypeHelper = new SampleTypeHelper(this);

        log("Create a sample type with attachment field");
        sampleTypeHelper.createSampleType(new SampleTypeDefinition(sampleTypeName)
                .setFields(List.of(
                        new FieldDefinition(fieldName, FieldDefinition.ColumnType.File)
                )));

        log("Inserting samples in sample Type");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(sampleTypeName));
        DataRegionTable samplesTable = DataRegionTable.DataRegion(getDriver()).withName("Material").waitFor();
        samplesTable.clickInsertNewRow();
        setFormElement(Locator.name("quf_Name"), "S1");
        setFormElement(Locator.name("quf_" + fieldName), SAMPLE_PDF);
        clickButton("Submit");

        log("Verifying view in browser works");
        Locator.linkContainingText(SAMPLE_PDF.getName()).findElement(getDriver()).click();
        log("Relative URL : " + getCurrentRelativeURL());
        checker().verifyTrue("Incorrect PDF file displayed", getCurrentRelativeURL().contains("core-downloadFileLink.view"));

        goToProjectHome();
        UpdateSampleTypePage updatePage = sampleTypeHelper.goToEditSampleType(sampleTypeName);
        updatePage.getFieldsPanel().getField(fieldName).expand().setAttachmentBehavior("Download File");
        updatePage.clickSave();

        File downloadedFile = doAndWaitForDownload(() -> Locator.linkContainingText(SAMPLE_PDF.getName()).findElement(getDriver()).click());
        checker().verifyTrue("Downloaded file is empty", downloadedFile.length() > 0);
    }

    @Test
    public void testAttachmentFieldInLists()
    {
        String listName = "List with attachment field";
        String fieldName = "pdfFile";
        goToProjectHome();
        log("Creating the list");
        _listHelper.createList(getProjectName(), listName, ListHelper.ListColumnType.AutoInteger, "id");

        log("Adding a attachment field with Show attachment in Browser");
        DomainDesignerPage domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        DomainFormPanel panel = domainDesignerPage.fieldsPanel();
        DomainFieldRow stringRow = panel
                .addField(fieldName)
                .setType(FieldDefinition.ColumnType.Attachment);
        stringRow.setAttachmentBehavior("Show Attachment in Browser");
        domainDesignerPage.clickFinish();

        log("Insert row in list");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        DataRegionTable listTable = new DataRegionTable("query", getDriver());
        listTable.clickInsertNewRow();
        setFormElement(Locator.name("quf_" + fieldName), SAMPLE_PDF);
        clickButton("Submit");

        log("Verify file opened in browser");
        Locator.linkWithText(SAMPLE_PDF.getName()).findElement(getDriver()).click();
        switchToWindow(1);
        checker().verifyTrue("Incorrect PDF file displayed", getCurrentRelativeURL().contains(SAMPLE_PDF.getName()));
        switchToMainWindow();

        log("Verify file is downloaded");
        domainDesignerPage = DomainDesignerPage.beginAt(this, getProjectName(), "lists", listName);
        panel = domainDesignerPage.fieldsPanel();
        panel.getField(fieldName).setAttachmentBehavior("Download Attachment");
        domainDesignerPage.clickFinish();

        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        File downloadedFile = doAndWaitForDownload(() -> Locator.linkWithText(SAMPLE_PDF.getName()).findElement(getDriver()).click());
        checker().verifyTrue("Downloaded file is empty", downloadedFile.length() > 0);
    }
}
