package org.labkey.test.tests.list;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.list.AdvancedListSettingsDialog;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class ListIndexingTest extends BaseWebDriverTest
{
    private final ListHelper _listHelper = new ListHelper(this);
    private static final String listName = "NIMHDemographics";

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }

    @BeforeClass
    public static void setupProject()
    {
        ListIndexingTest init = (ListIndexingTest) getCurrentTest();
        init.doSetup();
    }

    /*
       Regression test cases for Issue 48056: More list indexing fixes
     */
    private void doSetup()
    {
        log("Setup project and list module");
        _containerHelper.createProject(getProjectName(), null);
        _listHelper.importListArchive(getProjectName(), TestFileUtils.getSampleData("lists/ListDemo.lists.zip"));

        goToProjectHome();
        new PortalHelper(this).addWebPart("Search");
    }

    @Test
    public void testEachItemIndexing()
    {
        goToProjectHome();
        EditListDefinitionPage editListDefinitionPage = _listHelper.goToEditDesign("NIMHSlides");
        editListDefinitionPage.openAdvancedListSettings()
                .indexEachItemAsASeparateDocument(true, "Subject Id: ${SubjectId}", AdvancedListSettingsDialog.SearchIndexOptions.NonPhiFields)
                .clickApply()
                .clickSave();

        searchFor(getProjectName(), "10001", 8, null);
    }

    @Test
    public void testEntireListIndexing()
    {
        /*
            Test coverage for :
            Issue 48188: Selecting 'All non-PHI' or 'Custom template' under 'Index each item as a separate document' doesn't stick
         */
        goToProjectHome();
        EditListDefinitionPage editDesign = _listHelper.goToEditDesign(listName);
        editDesign.openAdvancedListSettings()
                .indexEntireListAsASingleDocument(true, "",
                        AdvancedListSettingsDialog.SearchIncludeOptions.MetadataAndData,
                        AdvancedListSettingsDialog.SearchIndexOptions.CustomTemplate, "${Name}")
                .clickApply()
                .clickSave();

        editDesign = _listHelper.goToEditDesign(listName);
        AdvancedListSettingsDialog settingsDialog = editDesign.openAdvancedListSettings();
        Assert.assertTrue("Search index options did not save",
                settingsDialog.isSearchIndexSelected("Index entire list as a single document",
                        AdvancedListSettingsDialog.SearchIndexOptions.CustomTemplate));

        Assert.assertTrue("Search include option did not save",
                settingsDialog.isSearchIncludeSelected("Index entire list as a single document",
                        AdvancedListSettingsDialog.SearchIncludeOptions.MetadataAndData));
        settingsDialog.clickCancel();
        editDesign.clickCancel();

        log("Verifying search result based on advanced settings(Custom template and metadata+data");
        searchFor(getProjectName(), "Justin", 1, "List " + listName);
        searchFor(getProjectName(), "Child", 0, null);

        _listHelper.goToEditDesign(listName)
                .openAdvancedListSettings()
                .disableEntireListIndex()
                .clickApply()
                .clickSave();

        /*
            Test coverage for : Issue 48182: Custom titles on list search results aren't working
         */

        String customTitle = "Custom title for " + listName;
        editDesign = _listHelper.goToEditDesign(listName);
        editDesign.openAdvancedListSettings()
                .indexEntireListAsASingleDocument(true, customTitle,
                        AdvancedListSettingsDialog.SearchIncludeOptions.DataOnly,
                        AdvancedListSettingsDialog.SearchIndexOptions.NonPhiFields, null)
                .clickApply()
                .clickSave();

        editDesign = _listHelper.goToEditDesign("NIMHSamples");
        editDesign.openAdvancedListSettings()
                .indexEntireListAsASingleDocument(true, "",
                        AdvancedListSettingsDialog.SearchIncludeOptions.MetadataAndData,
                        AdvancedListSettingsDialog.SearchIndexOptions.NonPhiFields, null)
                .clickApply()
                .clickSave();

        log("Verifying search result based on advanced settings(Document title, dataOnly and non phi text fields");
        searchFor(getProjectName(), customTitle, 1, null);
        searchFor(getProjectName(), "Occupation", 0, null);
        searchFor(getProjectName(), "10001", 2, null);

        _listHelper.goToEditDesign(listName)
                .openAdvancedListSettings()
                .disableEntireListIndex()
                .clickApply()
                .clickSave();

        _listHelper.goToEditDesign("NIMHSamples")
                .openAdvancedListSettings()
                .disableEntireListIndex()
                .clickApply()
                .clickSave();

        editDesign = _listHelper.goToEditDesign(listName);
        editDesign.openAdvancedListSettings()
                .indexEntireListAsASingleDocument(true, "",
                        AdvancedListSettingsDialog.SearchIncludeOptions.MetadataOnly,
                        AdvancedListSettingsDialog.SearchIndexOptions.NonPhiText, null)
                .clickApply()
                .clickSave();

        log("Verifying search result based on advanced settings(Metadata only and non phi fields)");
        goToProjectHome();
        searchFor(getProjectName(), "Justin", 0, null);
        searchFor(getProjectName(), "Occupation", 1, "List " + listName);
    }

    @Test
    public void testAttachmentIndexing()
    {
        String attachmentList = "List with Attachment";
        File attachmentFile = TestFileUtils.getSampleData("fileTypes/pdf_sample.pdf");
        _listHelper.createList(getProjectName(), attachmentList, ListHelper.ListColumnType.AutoInteger, "id",
                new FieldDefinition("Name", FieldDefinition.ColumnType.String),
                new FieldDefinition("File", FieldDefinition.ColumnType.Attachment));
        _listHelper.beginAtList(getProjectName(), attachmentList);
        _listHelper.insertNewRow(Map.of("Name", "pdf file",
                "File", attachmentFile.getAbsolutePath()), false);

        searchFor(getProjectName(), attachmentFile.getName(), 0, null);

        EditListDefinitionPage editListDefinitionPage = _listHelper.goToEditDesign(attachmentList);
        editListDefinitionPage.openAdvancedListSettings()
                .setIndexFileAttachments(true)
                .clickApply()
                .clickSave();

        searchFor(getProjectName(), attachmentFile.getName(), 1, null);
    }
}
