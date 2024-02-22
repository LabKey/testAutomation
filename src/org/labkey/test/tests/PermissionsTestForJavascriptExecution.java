package org.labkey.test.tests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.query.QueryMetadataEditorPage;
import org.labkey.test.pages.query.SourceQueryPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PermissionsHelper;

import java.util.Arrays;
import java.util.List;

@Category({Daily.class})
public class PermissionsTestForJavascriptExecution extends BaseWebDriverTest
{
    private static final String USER = "javascripttestuser@permissionstestforjavascriptexecution.test";
    private static final String XML_METADATA_2 = "<tables xmlns=\"http://labkey.org/data/xml\"> \n" +
            "  <table tableName=\"Models\" tableDbType=\"TABLE\">\n" +
            "    <columns>\n" +
            "      <column columnName=\"editCol\">\n" +
            "            <columnTitle></columnTitle> \n" +
            "            <displayColumnFactory>\n" +
            "                <className>org.labkey.api.data.JavaScriptDisplayColumnFactory</className>\n" +
            "                <properties>\n" +
            "                    <property name=\"dependency\">ehr/window/ManageRecordWindow.js</property>\n" +
            "                    <property name=\"javaScriptEvents\">onclick=\"EHR.window.ManageRecordWindow.buttonHandler(${Id:jsString}, " +
            "                     ${objectid:jsString}, ${queryName:jsString}, '${dataRegionName}');\"</property>\n" +
            "                </properties>\n" +
            "            </displayColumnFactory>\n" +
            "        </column>\n" +
            "    </columns>\n" +
            "  </table>\n" +
            "</tables>\n";
    private static final String XML_METADATA_1 = "<tables xmlns=\"http://labkey.org/data/xml\">\n" +
            "    <table tableName=\"Models\" tableDbType=\"TABLE\">                \n" +
            "        <buttonBarOptions includeStandardButtons=\"true\">\n" +
            "            <item text=\"Custom Dropdown\" insertPosition=\"end\"> \n" +
            "                <item text=\"Say Hello\"> \n" +
            "                     <onClick>alert('Hello');</onClick> \n" +
            "                </item> \n" +
            "                <item text=\"LabKey.com\"> \n" +
            "                     <target>http://www.labkey.com</target> \n" +
            "                </item> \n" +
            "            </item> \n" +
            "        </buttonBarOptions>\n" +
            "    </table>\n" +
            "</tables>\n";
    ApiPermissionsHelper _apiPermissionsHelper = new ApiPermissionsHelper(this);

    @BeforeClass
    public static void setupProject()
    {
        PermissionsTestForJavascriptExecution init = (PermissionsTestForJavascriptExecution) getCurrentTest();
        init.doSetup();
    }

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(afterTest, USER);
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule("simpletest");

        _userHelper.createUser(USER);
        _apiPermissionsHelper.addMemberToRole(USER, "Project Administrator", PermissionsHelper.MemberType.user);
    }

    /*
        Regression coverage for : Secure Issue 48660: SaveSourceQueryAction doesn't check for JavaScriptDisplayColumnFactory and
        Secure Issue 48508: SaveSourceQueryAction doesn't check for JavaScript in XML payload
     */
    @Test
    public void testSteps()
    {
        String schema = "vehicle";
        String query = "Models";

        log("Verify editing the metadata without developer permissions throws error");
        goToProjectHome();
        impersonate(USER);
        clickTab("Query");
        selectQuery(schema, query);
        waitAndClickAndWait(Locator.linkContainingText("edit metadata"));
        QueryMetadataEditorPage metadataPage = new QueryMetadataEditorPage(getDriver());
        SourceQueryPage sourceQueryPage = metadataPage.clickEditSource();
        sourceQueryPage.setMetadataXml(XML_METADATA_2);
        Assert.assertEquals("Incorrect error message",
                "Failed to Save: An exception occurred: For permissions to use JavaScriptDisplayColumn, contact your system administrator",
                sourceQueryPage.clickSaveExpectingError());
        sourceQueryPage.setMetadataXml(XML_METADATA_1);
        Assert.assertEquals("Incorrect error message",
                "Failed to Save: An exception occurred: Illegal element <onClick>. For permissions to use this element, contact your system administrator",
                sourceQueryPage.clickSaveExpectingError());
        stopImpersonating();

        log("Adding developer role to the user");
        _apiPermissionsHelper.setSiteAdminRoleUserPermissions(USER, "Platform Developer");

        log("Verifying editing metadata is success");
        goToProjectHome();
        impersonate(USER);
        editSource(schema, query, XML_METADATA_1);
        editSource(schema, query, XML_METADATA_2);
        stopImpersonating();

        checkExpectedErrors(2);
    }

    private void editSource(String schema, String query, String xml)
    {
        goToSchemaBrowser();
        selectQuery(schema, query);
        waitAndClickAndWait(Locator.linkContainingText("edit metadata"));
        SourceQueryPage sourceQueryPage = new QueryMetadataEditorPage(getDriver()).clickEditSource();
        sourceQueryPage.setMetadataXml(xml).clickSave();
    }

    @Override
    protected String getProjectName()
    {
        return "PermissionsTestForJavascriptExecution Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("simpletest");
    }
}
