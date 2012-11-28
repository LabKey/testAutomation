package org.labkey.test.module;

import org.junit.Assert;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.tests.SimpleApiTest;
import org.labkey.test.util.AdvancedSqlTest;
import org.labkey.test.util.EHRTestHelper;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 11/27/12
 * Time: 2:22 PM
 */
abstract public class AbstractEHRTest extends SimpleApiTest implements AdvancedSqlTest
{
    protected String PROJECT_NAME = "EHR_TestProject";
    protected String FOLDER_NAME = "EHR";
    protected String CONTAINER_PATH = PROJECT_NAME + "/" + FOLDER_NAME;
    protected String STUDY_ZIP = "/sampledata/study/EHR Study Anon.zip";
    protected static final String STUDY_ZIP_NO_DATA = "/sampledata/study/EHR Study Anon Small.zip";

    protected static final String PROJECT_ID = "640991"; // project with one participant
    protected static final String DUMMY_PROTOCOL = "dummyprotocol"; // need a protocol to create table entry
    protected static final String PROJECT_MEMBER_ID = "test2312318"; // PROJECT_ID's single participant
    protected static final String ROOM_ID = "6824778"; // room of PROJECT_MEMBER_ID
    protected static final String CAGE_ID = "4434662"; // cage of PROJECT_MEMBER_ID

    protected static final String AREA_ID = "A1/AB190"; // arbitrary area
    protected static final String PROTOCOL_PROJECT_ID = "795644"; // Project with exactly 3 members
    protected static final String PROTOCOL_ID = "protocol101";
    protected static final String[] PROTOCOL_MEMBER_IDS = {"test3997535", "test4551032", "test5904521"}; //{"test2008446", "test3804589", "test4551032", "test5904521", "test6390238"}; // Protocol members, sorted ASC alphabetically
    protected static final String[] MORE_ANIMAL_IDS = {"test1020148","test1099252","test1112911","test727088","test4564246"}; // Some more, distinct, Ids
    protected static final String DEAD_ANIMAL_ID = "test9118022";
    protected static final String TASK_TITLE = "Test weight task";
    protected static final String MPR_TASK_TITLE = "Test MPR task";

    protected static final EHRUser DATA_ADMIN = new EHRUser("admin@ehrstudy.test", "EHR Administrators", EHRRole.DATA_ADMIN);
    protected static final EHRUser REQUESTER = new EHRUser("requester@ehrstudy.test", "EHR Requestors", EHRRole.REQUESTER);
    protected static final EHRUser BASIC_SUBMITTER = new EHRUser("basicsubmitter@ehrstudy.test", "EHR Basic Submitters", EHRRole.BASIC_SUBMITTER);
    protected static final EHRUser FULL_SUBMITTER = new EHRUser("fullsubmitter@ehrstudy.test", "EHR Full Submitters", EHRRole.FULL_SUBMITTER);
    protected static final EHRUser REQUEST_ADMIN = new EHRUser("request_admin@ehrstudy.test", "EHR Request Admins", EHRRole.REQUEST_ADMIN);
    protected static final EHRUser FULL_UPDATER = new EHRUser("full_updater@ehrstudy.test", "EHR Full Updaters", EHRRole.FULL_UPDATER);

    protected static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    //xpath fragment
    public static final String VISIBLE = "not(ancestor-or-self::*[contains(@style,'visibility: hidden') or contains(@class, 'x-hide-display')])";

    protected EHRTestHelper _helper = new EHRTestHelper(this);

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/ehr";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public boolean enableLinkCheck()
    {
        if ( super.enableLinkCheck() )
            log("EHR test has too many hard coded links and special actions to crawl effectively. Skipping crawl.");
        return false;
    }

    @Override
    public void validateQueries(boolean validateSubfolders)
    {
        //NOTE: the queries are also validated as part of study import
        //also, validation takes place on the project root, while the EHR and required datasets are loaded into a subfolder
        log("Skipping query validation.");
    }

    @Override
    protected Pattern[] getIgnoredElements()
    {
        return new Pattern[] {
                Pattern.compile("qcstate", Pattern.CASE_INSENSITIVE),//qcstate IDs aren't predictable
                Pattern.compile("stacktrace", Pattern.CASE_INSENSITIVE)
        };
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[0];
    }

    @Override
    public void doCleanup(boolean afterTest)
    {
        long startTime = System.currentTimeMillis();
        deleteProject(getProjectName(), afterTest);
        if(isTextPresent(PROJECT_NAME))
        {
            log("Wait extra long for folder to finish deleting.");
            while (isTextPresent(PROJECT_NAME) && System.currentTimeMillis() - startTime < 300000) // 5 minutes max.
            {
                sleep(5000);
                refresh();
            }
            if (!isTextPresent(PROJECT_NAME)) log("Test Project deleted in " + (System.currentTimeMillis() - startTime) + "ms");
            else Assert.fail("Test Project not finished deleting after 5 minutes");
        }

        deleteUsers(afterTest,
                DATA_ADMIN.getUser(),
                REQUESTER.getUser(),
                BASIC_SUBMITTER.getUser(),
                REQUEST_ADMIN.getUser(),
                FULL_UPDATER.getUser(),
                FULL_SUBMITTER.getUser());
    }

    protected void initProject()
    {
        enableEmailRecorder();

        _containerHelper.createProject(PROJECT_NAME, "EHR");
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "EHR", null);

        //set dummy values first, to test the admin UI
        String[] dummyProps = {"/" +  PROJECT_NAME, "EHRStudyContainer", "/fakeContainer"};
        setModuleProperties(Collections.singletonMap("EHR", Collections.singletonList(dummyProps)));

        String[] prop = {"/" + PROJECT_NAME, "EHRStudyContainer", "/" + CONTAINER_PATH};
        setModuleProperties(Collections.singletonMap("EHR", Collections.singletonList(prop)));

        clickLinkWithText(FOLDER_NAME);
        beginAt(getBaseURL()+"/ehr/"+CONTAINER_PATH+"/_initEHR.view");
        clickButton("Delete All", 0);
        waitForText("Delete Complete", 120000);
        clickButton("Populate All", 0);
        waitForText("Populate Complete", 120000);

        //these tables do not have a container field, so are not deleted when the test project is deleted
        clickButton("Delete Data From SNOMED", 0);
        waitForText("Delete Complete", 120000);
        clickButton("Populate SNOMED Table", 0);
        waitForText("Populate Complete", 120000);

        goToModule("Study");
        importStudyFromZip(new File(getLabKeyRoot() + STUDY_ZIP).getPath());
        try
        {
            deleteRecords();
            populateRecords();
        }
        catch (Throwable e)
        {
            //ignore for now
            log("There was an error");
        }

        log("Remove all webparts");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        addWebPart("Quick Search");

        setupEhrPermissions();
        defineQCStates();
    }

    protected void populateRecords() throws Exception
    {
        log("Inserting initial records into EHR hard tables");

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        //first ehr.protocol
        InsertRowsCommand insertCmd = new InsertRowsCommand("ehr", "protocol");
        Map<String,Object> rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", PROTOCOL_ID);
        insertCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", DUMMY_PROTOCOL);
        insertCmd.addRow(rowMap);
        SaveRowsResponse saveResp = insertCmd.execute(cn, CONTAINER_PATH);

        //then ehr.project
        insertCmd = new InsertRowsCommand("ehr", "project");
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROTOCOL_PROJECT_ID);
        rowMap.put("protocol", PROTOCOL_ID);
        insertCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROJECT_ID);
        rowMap.put("protocol", DUMMY_PROTOCOL);
        insertCmd.addRow(rowMap);
        saveResp = insertCmd.execute(cn, CONTAINER_PATH);
    }

    protected void deleteRecords() throws Exception
    {
        log("Deleting initial records from EHR hard tables");

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        //first ehr.protocol
        DeleteRowsCommand deleteCmd = new DeleteRowsCommand("ehr", "protocol");
        Map<String,Object> rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", PROTOCOL_ID);
        deleteCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", DUMMY_PROTOCOL);
        deleteCmd.addRow(rowMap);
        SaveRowsResponse deleteResp = deleteCmd.execute(cn, CONTAINER_PATH);

        //then ehr.project
        deleteCmd = new DeleteRowsCommand("ehr", "project");
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROTOCOL_PROJECT_ID);
        rowMap.put("protocol", PROTOCOL_ID);
        deleteCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROJECT_ID);
        rowMap.put("protocol", DUMMY_PROTOCOL);
        deleteCmd.addRow(rowMap);
        deleteResp = deleteCmd.execute(cn, CONTAINER_PATH);
    }

    protected void assertNoErrorText()
    {
        assertTextNotPresent("error", "Error", "ERROR", "failed", "Failed", "Invalid", "invalid");
    }

    protected void defineQCStates()
    {
        log("Define QC states for EHR study");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        goToManageStudy();
        clickLinkWithText("Manage Dataset QC States");

        for(EHRQCState qcState : EHRQCState.values())
        {
            setFormElement("newLabel", qcState.label);
            setFormElement("newDescription", qcState.description);
            if(!qcState.publicData) uncheckCheckbox("newPublicData");
            clickButton("Save");
        }

        setFormElement("showPrivateDataByDefault", "true");
        clickButton("Done");
    }

    protected void setupEhrPermissions()
    {
        clickFolder(PROJECT_NAME);
        createUserAndNotify(DATA_ADMIN.getUser(), "");
        clickFolder(PROJECT_NAME);
        createUserAndNotify(REQUESTER.getUser(), "");
        clickFolder(PROJECT_NAME);
        createUserAndNotify(BASIC_SUBMITTER.getUser(), "");
        clickFolder(PROJECT_NAME);
        createUserAndNotify(FULL_SUBMITTER.getUser(), "");
        clickFolder(PROJECT_NAME);
        createUserAndNotify(FULL_UPDATER.getUser(), "");
        clickFolder(PROJECT_NAME);
        createUserAndNotify(REQUEST_ADMIN.getUser(), "");
        clickFolder(PROJECT_NAME);

        setInitialPassword(DATA_ADMIN.getUser(), PasswordUtil.getPassword());
        setInitialPassword(REQUESTER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(BASIC_SUBMITTER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(FULL_SUBMITTER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(FULL_UPDATER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(REQUEST_ADMIN.getUser(), PasswordUtil.getPassword());

        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        pushLocation();
        createPermissionsGroup(DATA_ADMIN.getGroup(), DATA_ADMIN.getUser());
        createPermissionsGroup(REQUESTER.getGroup(), REQUESTER.getUser());
        createPermissionsGroup(BASIC_SUBMITTER.getGroup(), BASIC_SUBMITTER.getUser());
        createPermissionsGroup(FULL_SUBMITTER.getGroup(), FULL_SUBMITTER.getUser());
        createPermissionsGroup(FULL_UPDATER.getGroup(), FULL_UPDATER.getUser());
        createPermissionsGroup(REQUEST_ADMIN.getGroup(), REQUEST_ADMIN.getUser());
        popLocation();
        enterPermissionsUI();
        uncheckInheritedPermissions();
        setPermissions(DATA_ADMIN.getGroup(), "Editor");
        setPermissions(REQUESTER.getGroup(), "Editor");
        setPermissions(BASIC_SUBMITTER.getGroup(), "Editor");
        setPermissions(FULL_SUBMITTER.getGroup(), "Editor");
        setPermissions(FULL_UPDATER.getGroup(), "Editor");
        setPermissions(REQUEST_ADMIN.getGroup(), "Editor");
        savePermissions();
        _ext4Helper.clickTabContainingText("Study Security");
        waitAndClickButton("Study Security");

        checkRadioButton(getAnimalHistoryRadioButtonLocator(DATA_ADMIN.getGroup(), "READOWN"));
        checkRadioButton(getAnimalHistoryRadioButtonLocator(REQUESTER.getGroup(), "READOWN"));
        checkRadioButton(getAnimalHistoryRadioButtonLocator(BASIC_SUBMITTER.getGroup(), "READOWN"));
        checkRadioButton(getAnimalHistoryRadioButtonLocator(FULL_SUBMITTER.getGroup(), "READOWN"));
        checkRadioButton(getAnimalHistoryRadioButtonLocator(FULL_UPDATER.getGroup(), "READOWN"));
        checkRadioButton(getAnimalHistoryRadioButtonLocator(REQUEST_ADMIN.getGroup(), "READOWN"));
        clickAndWait(Locator.id("groupUpdateButton"));

        //"set all to..." combo-boxes don't work through _selenium.
        log("Set per-dataset permissions individually");
        setPDP(DATA_ADMIN);
        setPDP(BASIC_SUBMITTER);
        setPDP(FULL_SUBMITTER);
        setPDP(FULL_UPDATER);
        setPDP(REQUESTER);
        setPDP(REQUEST_ADMIN);

        waitFor(new BaseSeleniumWebTest.Checker()
        {
            public boolean check()
            {
                return "EHR Data Admin".equals(getSelectedOptionText(Locator.name("dataset.1061", 0))) &&
                        "EHR Basic Submitter".equals(getSelectedOptionText(Locator.name("dataset.1061", 1))) &&
                        "EHR Full Submitter".equals(getSelectedOptionText(Locator.name("dataset.1061", 2))) &&
                        "EHR Full Updater".equals(getSelectedOptionText(Locator.name("dataset.1061", 3))) &&
                        "EHR Request Admin".equals(getSelectedOptionText(Locator.name("dataset.1061", 4))) &&
                        "EHR Requestor".equals(getSelectedOptionText(Locator.name("dataset.1061", 5)));
            }
        }, "Per-dataset permission not set", WAIT_FOR_JAVASCRIPT);

        clickButton("Save");
    }

    public void setPDP(EHRUser user)
    {
        int col = getWrapper().getXpathCount("//table[@id='datasetSecurityFormTable']//th[.='" + user.getGroup() + "']/preceding-sibling::*").intValue() + 1;
        int rowCt = getTableRowCount("datasetSecurityFormTable");
        for (int i = 3; i <= rowCt; i++) // xpath indexing is 1 based
        {
            selectOptionByText(Locator.xpath("//table[@id='datasetSecurityFormTable']/tbody/tr[" + i + "]/td[" + col + "]//select"), user.getRole().toString());
        }
    }

    public Locator getAnimalHistoryRadioButtonLocator(String groupName, String setting)
    {
        //not sure why the radios are in TH elements, but they are...
        return Locator.xpath("//form[@id='groupUpdateForm']/table/tbody/tr/td[text()='"
                + groupName + "']/../th/input[@value='" + setting + "']");
    }

    public static class EHRUser
    {
        private final String _userId;
        private final String _groupName;
        private final EHRRole _role;

        public EHRUser(String userId, String groupName, EHRRole role)
        {
            _userId = userId;
            _groupName = groupName;
            _role = role;
        }

        public String getUser()
        {
            return _userId;
        }

        public String getGroup()
        {
            return _groupName;
        }

        public EHRRole getRole()
        {
            return _role;
        }
    }

    public static class Permission
    {
        EHRRole role;
        EHRQCState qcState;
        String action;
        public Permission(EHRRole role, EHRQCState qcState, String action)
        {
            this.role = role;
            this.qcState = qcState;
            this.action = action;
        }

        @Override
        public boolean equals(Object other)
        {
            return other.getClass().equals(Permission.class) &&
                    this.role == ((Permission)other).role &&
                    this.qcState == ((Permission)other).qcState &&
                    this.action.equals(((Permission)other).action);
        }
    }

    public static enum EHRRole
    {
        DATA_ADMIN ("EHR Data Admin"),
        REQUESTER ("EHR Requestor"),
        BASIC_SUBMITTER ("EHR Basic Submitter"),
        FULL_SUBMITTER ("EHR Full Submitter"),
        FULL_UPDATER ("EHR Full Updater"),
        REQUEST_ADMIN ("EHR Request Admin");
        private final String name;
        private EHRRole (String name)
        {this.name = name;}
        public String toString()
        {return name;}
    }

    public static enum EHRQCState
    {
        ABNORMAL("Abnormal", "Value is abnormal", true, false, false),
        COMPLETED("Completed", "Data has been approved for public release", true, false, false),
        DELETE_REQUESTED("Delete Requested", "Records are requested to be deleted", true, true, false),
        IN_PROGRESS("In Progress", "Draft Record, not public", false, true, false),
        REQUEST_APPROVED("Request: Approved", "Request has been approved", true, true, true),
        REQUEST_COMPLETE("Request: Complete", "Request has been completed", true, false, true),
        REQUEST_DENIED("Request: Denied", "Request has been denied", true, false, true),
        REQUEST_PENDING("Request: Pending", "Part of a request that has not been approved", false, false, true),
        REVIEW_REQUIRED("Review Required", "Review is required prior to public release", false, false, false),
        SCHEDULED("Scheduled", "Record is scheduled, but not performed", true, true, false);

        public final String label;
        public final String description;
        public final boolean publicData;

        public final boolean draftData;
        public final boolean isRequest;

        EHRQCState(String label, String description, boolean publicData, boolean draftData, boolean isRequest)
        {
            this.label = label;
            this.description = description;
            this.publicData = publicData;
            this.draftData = draftData;
            this.isRequest = isRequest;
        }
    }
}
