package org.labkey.test.tests.experiment;

import org.apache.hc.core5.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.params.experiment.DataClassDefinition;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.exp.DataClassAPIHelper;
import org.labkey.test.util.exp.SampleTypeAPIHelper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Category({Daily.class})
public class GetEntitySequenceAPITest extends BaseWebDriverTest
{
    private static final String READABLE_PROJECT = "GetEntitySequenceAPITest Readable";
    private static final String RESTRICTED_PROJECT = "GetEntitySequenceAPITest Restricted";
    private static final String READER_USER = "reader@getentitysequenceapi.test";
    private static final String SAMPLE_TYPE_NAME = "GetEntitySequenceTest_SampleType";
    private static final String DATA_CLASS_NAME = "GetEntitySequenceTest_DataClass";

    private final ApiPermissionsHelper _permissions = new ApiPermissionsHelper(this);

    public GetEntitySequenceAPITest()
    {
        ((APIContainerHelper) _containerHelper).setNavigateToCreatedFolders(false);
    }

    @BeforeClass
    public static void setupProject()
    {
        GetEntitySequenceAPITest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(READABLE_PROJECT, null);
        _containerHelper.createProject(RESTRICTED_PROJECT, null);

        SampleTypeAPIHelper.createEmptySampleType(RESTRICTED_PROJECT, new SampleTypeDefinition(SAMPLE_TYPE_NAME));
        DataClassAPIHelper.createEmptyDataClass(RESTRICTED_PROJECT, new DataClassDefinition(DATA_CLASS_NAME));

        _userHelper.createUser(READER_USER);
        _userHelper.setInitialPassword(READER_USER);
        _permissions.addMemberToRole(READER_USER, "Reader", PermissionsHelper.MemberType.user, READABLE_PROJECT);
        // Intentionally not granting the user any role in RESTRICTED_PROJECT
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(READABLE_PROJECT, afterTest);
        _containerHelper.deleteProject(RESTRICTED_PROJECT, afterTest);
        _userHelper.deleteUsers(false, READER_USER);
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("experiment");
    }

    // Kanban #1924 Verify we prevent getting the sequence value from a container the user does not have access to
    @Test
    public void testGetEntitySequenceSampleTypeAccessControl() throws IOException, CommandException
    {
        // The sample type lives in RESTRICTED_PROJECT. getSampleType(rowId) fetches globally so the lookup
        // succeeds; the fix then checks the user has ReadPermission on the sample type's container.
        SelectRowsCommand cmd = new SelectRowsCommand("exp", "SampleSets");
        cmd.addFilter("Name", SAMPLE_TYPE_NAME, Filter.Operator.EQUAL);
        cmd.setMaxRows(1);
        int sampleTypeRowId = ((Number) cmd.execute(createDefaultConnection(), RESTRICTED_PROJECT)
                .getRows().get(0).get("RowId")).intValue();

        // User has ReadPermission on READABLE_PROJECT (passes @RequiresPermission), but the sample type
        // belongs to RESTRICTED_PROJECT where the user has no access. seqType=genId is the only path
        // that triggers the cross-container check for sample types.
        String url = WebTestHelper.buildURL("experiment", READABLE_PROJECT, "getEntitySequence",
                Map.of("kindName", SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND,
                       "seqType", "genId",
                       "rowId", String.valueOf(sampleTypeRowId)));
        int status = WebTestHelper.getHttpResponse(url, READER_USER, PasswordUtil.getPassword()).getResponseCode();
        assertEquals("Expected 403 when user lacks ReadPermission on the sample type's container",
                HttpStatus.SC_FORBIDDEN, status);
    }

    // Kanban #1924 Verify we prevent getting the sequence value from a container the user does not have access to

    @Test
    public void testGetEntitySequenceDataClassAccessControl() throws IOException, CommandException
    {
        // The data class lives in RESTRICTED_PROJECT. getDataClass(rowId) fetches globally so the lookup
        // succeeds; the fix then checks the user has ReadPermission on the data class's container.
        // seqType=genId is the only value accepted when kindName=DataClass.
        SelectRowsCommand cmd = new SelectRowsCommand("exp", "DataClasses");
        cmd.addFilter("Name", DATA_CLASS_NAME, Filter.Operator.EQUAL);
        cmd.setMaxRows(1);
        int dataClassRowId = ((Number) cmd.execute(createDefaultConnection(), RESTRICTED_PROJECT)
                .getRows().get(0).get("RowId")).intValue();

        // User has ReadPermission on READABLE_PROJECT (passes @RequiresPermission), but the data class
        // belongs to RESTRICTED_PROJECT where the user has no access.
        String url = WebTestHelper.buildURL("experiment", READABLE_PROJECT, "getEntitySequence",
                Map.of("kindName", "DataClass",
                       "seqType", "genId",
                       "rowId", String.valueOf(dataClassRowId)));
        int status = WebTestHelper.getHttpResponse(url, READER_USER, PasswordUtil.getPassword()).getResponseCode();
        assertEquals("Expected 403 when user lacks ReadPermission on the data class's container",
                HttpStatus.SC_FORBIDDEN, status);
    }
}
