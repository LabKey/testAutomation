package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.DataRegionTable;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class UserTableCustomFieldUpdateTest extends BaseWebDriverTest
{
    private static final String CUSTOM_FIELD1 = "CustomField1";
    private static final String CUSTOM_FIELD2 = "CustomField2";
    private static final String TEST_USER = "testcustomfieldupdate@test.com";

    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @BeforeClass
    public static void initProject()
    {
        UserTableCustomFieldUpdateTest initTest = (UserTableCustomFieldUpdateTest) getCurrentTest();
        initTest.doSetup();
    }

    private void doSetup()
    {
        DomainDesignerPage domainDesignerPage = goToSiteUsers().clickChangeUserProperties();
        DomainFormPanel domainFormPanel = domainDesignerPage.fieldsPanel();
        if (domainFormPanel.getField(CUSTOM_FIELD1) == null)
            domainFormPanel.addField(CUSTOM_FIELD1).setType(FieldDefinition.ColumnType.String);
        if (domainFormPanel.getField(CUSTOM_FIELD2) == null)
            domainFormPanel.addField(CUSTOM_FIELD2).setType(FieldDefinition.ColumnType.String);
        domainDesignerPage.clickFinish();

        _userHelper.createUser(TEST_USER);
    }

    /*
        Regression coverage for Issue 48185: Update of the User table clears any non-specified custom fields
     */
    @Test
    public void testCustomFieldUpdate() throws IOException, CommandException
    {
        log("Extract the userId");
        DataRegionTable userTable = goToSiteUsers().getUsersTable();
        userTable.setFilter("Email", "Equals", TEST_USER);
        String userId = userTable.getDataAsText(0, "UserId");

        log("Update user with custom field value");
        goToHome();
        impersonate(TEST_USER);
        {
            goToMyAccount();
            clickButton("Edit");
            setFormElement(Locator.name("quf_" + CUSTOM_FIELD1), "Value for " + CUSTOM_FIELD1);
            setFormElement(Locator.name("quf_" + CUSTOM_FIELD2), "Value for " + CUSTOM_FIELD2);
            clickButton("Submit");
        }
        stopImpersonating();

        HashMap row = new HashMap<String, String>();
        row.put("UserId", userId);
        row.put(CUSTOM_FIELD1, "Updated value for " + CUSTOM_FIELD1);

        log("Updating the custom field for the user using API");
        UpdateRowsCommand updateUserRow = new UpdateRowsCommand("core", "Users");
        updateUserRow.setRows(Arrays.asList(row));
        updateUserRow.execute(WebTestHelper.getRemoteApiConnection(), "/");

        userTable = goToSiteUsers().getUsersTable();
        userTable.setFilter("Email", "Equals", TEST_USER);

        Assert.assertEquals("Invalid value for " + CUSTOM_FIELD1 + " in User table", "Updated value for " + CUSTOM_FIELD1,
                userTable.getDataAsText(0, CUSTOM_FIELD1));
        Assert.assertEquals("Invalid value for " + CUSTOM_FIELD2 + " in User table", "Value for " + CUSTOM_FIELD2,
                userTable.getDataAsText(0, CUSTOM_FIELD2));
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, TEST_USER);
    }

}
