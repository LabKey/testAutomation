package org.labkey.test.tests.query;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.query.SourceQueryPage;
import org.labkey.test.util.SchemaHelper;

import java.util.Arrays;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class LinkedSchemaInheritedPermissionsTest extends BaseWebDriverTest
{
    private static final String SUBFOLDER = "subfolderLinkedSchema " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private final SchemaHelper _schemaHelper = new SchemaHelper(this);

    @BeforeClass
    public static void setupProject()
    {
        LinkedSchemaInheritedPermissionsTest init = getCurrentTest();
        init.doSetup();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName());
        _containerHelper.enableModule("simpletest");

        _containerHelper.createSubfolder(getProjectName(), SUBFOLDER);
        _containerHelper.enableModule("simpletest");
    }

    /*
       Regression test coverage for
        Secure Issue 45251: Linked schemas are too permissive when permissions are inherited
     */
    @Test
    public void testInheritedPermissionLinkedSchema()
    {
        log("Create a list in the parent folder");
        String listName = "People";
        goToProjectHome();
        _listHelper.createList(getProjectName(), listName, "List_key");

        log("Create a new query in the child folder");
        String queryName = "InheritedPermissionsTesting";
        goToProjectFolder(getProjectName(), SUBFOLDER);
        goToSchemaBrowser();
        SourceQueryPage sourceQueryPage = createNewQuery("vehicle", "Models")
                .setName(queryName)
                .clickCreate();
        sourceQueryPage.setSource("SELECT * FROM \"/" + getProjectName() + "\".lists." + listName);
        sourceQueryPage.clickSaveAndFinish();

        log("Create a linked schema");
        String linkedSchemaName = "LinkedSchemaInheritedPermissions";
        goToProjectHome();
        _schemaHelper.createLinkedSchema(getProjectName(), linkedSchemaName, "/" + getProjectName() + "/" + SUBFOLDER
                , null, "vehicle", null, null);

        goToSchemaBrowser();
        selectQuery(linkedSchemaName, queryName);
        assertTextPresent("Query or table not found: /" + getProjectName() + ".lists.People");
    }

    @Override
    protected String getProjectName()
    {
        return "LinkedSchemaInheritedPermissionsTest Project" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
