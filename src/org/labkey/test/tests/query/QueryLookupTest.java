package org.labkey.test.tests.query;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.query.QueryHelper;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.TestDataGenerator;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({})
public class QueryLookupTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = "QueryLookupTest" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private static final String USER = "template_user@querylookuptest.test";
    private static final String LIST_NAME = TestDataGenerator.randomFieldName("list", "<>[]{};,`\"~!@#$%^*=|?\\");
    private static final FieldDefinition.ColumnType LIST_KEY_TYPE = FieldDefinition.ColumnType.Integer;
    private static final String LIST_KEY_NAME = TestDataGenerator.randomFieldName("listkey");
    private static final FieldDefinition NAME_COLUMN =
            new FieldDefinition("Name", FieldDefinition.ColumnType.String);
    private static final FieldDefinition TSHIRT_COLUMN =
            new FieldDefinition("TShirt", FieldDefinition.ColumnType.String);

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(afterTest, USER);
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        QueryLookupTest init = getCurrentTest();

        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        _userHelper.createUser(USER);

        // create a list
        var dgen = new IntListDefinition(LIST_NAME)
                .setFields(List.of(
                        new FieldDefinition(LIST_KEY_NAME, LIST_KEY_TYPE),
                        NAME_COLUMN,
                        TSHIRT_COLUMN))
                .create(createDefaultConnection(), PROJECT_NAME);
        dgen.withGeneratedRows(10)
                .insertRows();

        // create a query on the list
        goToSchemaBrowser();
        //var queryPage = navigateToQuery("lists", LIST_NAME);
        var queryPage = createNewQuery("lists", LIST_NAME);

        log("foo");
    }


    // Issue 49511 Setting lookup to custom query shows "Error: Lookup target table does not exist."
    @Test
    public void testSomething()
    {
        assertTrue("Failing stub test", false);
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
