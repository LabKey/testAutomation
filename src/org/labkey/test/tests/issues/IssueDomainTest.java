package org.labkey.test.tests.issues;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.domain.DomainCommand;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.GetDomainCommand;
import org.labkey.remoteapi.domain.SaveDomainCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Issues;
import org.labkey.test.util.IssuesHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Category({Issues.class, DailyA.class, Data.class})
@BaseWebDriverTest.ClassTimeout(minutes = 20)
public class IssueDomainTest extends BaseWebDriverTest
{
    private IssuesHelper _issuesHelper = new IssuesHelper(this);
    private final String DOMAIN_KIND = "IssueDefinition";
    private final String DOMAIN_NAME = "issues";//schemaName
    private final String ISSUES_NAME = "testdomainissue";
    private final String MISSING_MANDATORY_FIELD_ERROR_MSG = "Mandatory field 'notifylist' not found, it may have been removed or renamed. Unable to update domain.";

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "IssueDomainProject";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("issues");
    }

    @BeforeClass
    public static void doSetup()
    {
        IssueDomainTest initTest = (IssueDomainTest) getCurrentTest();
        initTest.setup();
    }

    protected String getFolderName()
    {
        return "My Issue Domain Test";
    }

    private void setup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), getFolderName());
        _issuesHelper.createNewIssuesList(ISSUES_NAME, _containerHelper);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
        clickFolder(getFolderName());
    }

    private String getContainerPath()
    {
        return getProjectName() + "/" + getFolderName();
    }

    @Test
    public void mandatoryFieldTest() throws Exception
    {
        log("Remove mandatory field 'NotifyList'");
        GetDomainCommand getCmd = new GetDomainCommand(DOMAIN_NAME, ISSUES_NAME);
        DomainResponse getDomainResponse = getCmd.execute(this.createDefaultConnection(false), getContainerPath());
        List<Map<String, Object>> getDomainCols = getDomainResponse.getColumns();

        int index = 0;
        for (Map<String, Object> col : getDomainCols)
        {
            String colName = (String) col.get("name");
            if ("notifylist".equalsIgnoreCase(colName))
            {
                getDomainCols.remove(index);
                break;
            }
            index++;
        }

        SaveDomainCommand saveCmd = new SaveDomainCommand(DOMAIN_KIND, ISSUES_NAME);
        long getDomainId = getDomainResponse.getDomainId();
        saveCmd.setDomainId((int) getDomainId);
        saveCmd.setDomainURI(getDomainResponse.getDomainURI());
        saveCmd.setSchemaName(DOMAIN_NAME);
        saveCmd.setColumns(getDomainCols);
        testForExpectedErrorMessage(saveCmd, MISSING_MANDATORY_FIELD_ERROR_MSG, "SaveDomain");
    }

    private void testForExpectedErrorMessage(DomainCommand cmd, String expectedErrorMsg, String domainApiType) throws IOException
    {
        try
        {
            cmd.execute(this.createDefaultConnection(false), getContainerPath());
            fail("Expected " + domainApiType + " API to throw CommandException.");
        }
        catch (CommandException e)
        {
            assertEquals("Error message mismatch.", expectedErrorMsg, e.getMessage());
        }
    }
}
