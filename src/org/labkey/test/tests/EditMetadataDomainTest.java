package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.pages.query.EditMetadataPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Category(DailyA.class)
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

        EditMetadataPage metadataPage = new EditMetadataPage(getDriver());
        DomainFormPanel formPanel = metadataPage.fieldsPanel();

        log("Verifying new alias field is added");
        metadataPage.clickAliasField("Name");
        checker().verifyEquals("Incorrect domain fields",
                Arrays.asList("diImportHash", "Modified", "container", "CreatedBy", "lastIndexed",
                        "Created", "ModifiedBy", "id", "name", "firstCol", "EntityId", "Wrappedname"), formPanel.fieldNames());

        log("Verifying alias field can be deleted");
        formPanel.getField("Wrappedname").clickRemoveField(false);
        metadataPage.clickSave();

        log("Verifying domain saves the alias field when revisiting");
        metadataPage.clickAliasField("First Col");
        metadataPage.clickSave();
        checker().verifyEquals("Incorrect domain field value after save",
                Arrays.asList("diImportHash", "Modified", "container", "CreatedBy", "lastIndexed",
                        "Created", "ModifiedBy", "id", "name", "firstCol", "EntityId", "WrappedfirstCol"), formPanel.fieldNames());

        metadataPage.clickResetToDefault();
        checker().verifyEquals("Incorrect domain field value after reset to default",
                Arrays.asList("diImportHash", "Modified", "container", "CreatedBy", "lastIndexed",
                        "Created", "ModifiedBy", "id", "name", "firstCol", "EntityId"), formPanel.fieldNames());
        metadataPage.clickSave();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
