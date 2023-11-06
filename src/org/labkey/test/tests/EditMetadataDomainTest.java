package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.query.QueryMetadataEditorPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Category(Daily.class)
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class EditMetadataDomainTest extends BaseWebDriverTest
{
    private static final String listName = "Sample List";

    @BeforeClass
    public static void setupProject() throws IOException, CommandException
    {
        EditMetadataDomainTest init = (EditMetadataDomainTest) getCurrentTest();
        init.doSetup();
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "Edit Metadata Domain Test";
    }

    private void doSetup() throws IOException, CommandException
    {
        _containerHelper.createProject(getProjectName());
        goToProjectHome();
        new PortalHelper(getDriver()).addBodyWebPart("Lists");

        log("Creating a sample list");
        FieldDefinition.LookupInfo lookupInfo = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(lookupInfo)
                .withColumns(List.of(
                        new FieldDefinition("name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("firstCol", FieldDefinition.ColumnType.String)));
        dgen.createDomain(createDefaultConnection(), "IntList", Map.of("keyName", "id"));
    }

    @Test
    public void testAliasField() throws InterruptedException
    {
        goToProjectHome();
        goToSchemaBrowser();
        selectQuery("lists", listName);
        clickAndWait(Locator.linkWithText("edit metadata"));

        QueryMetadataEditorPage metadataPage = new QueryMetadataEditorPage(getDriver());
        DomainFormPanel formPanel = metadataPage.fieldsPanel();

        log("Verifying new alias field is added");
        metadataPage.aliasField("Name");
        checker().verifyTrue("Missing wrapped field", formPanel.fieldNames().contains("Wrappedname"));

        log("Verifying alias field can be deleted");
        formPanel.getField("Wrappedname").clickRemoveField(false);
        metadataPage.clickSave();

        log("Verifying domain saves the alias field when revisiting");
        metadataPage.aliasField("First Col");
        metadataPage.clickSave();
        checker().verifyTrue("Missing wrapped field", formPanel.fieldNames().contains("WrappedfirstCol"));

        metadataPage.resetToDefault();
        checker().verifyFalse("Wrapped field present after reset", formPanel.fieldNames().contains("WrappedfirstCol"));
        metadataPage.clickSave();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
