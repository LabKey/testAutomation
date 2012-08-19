package org.labkey.test.tests;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.Maps;

public class ContainerContextTest extends BaseSeleniumWebTest
{
    private static final String SUB_FOLDER_A = "A";
    private static final String SUB_FOLDER_B = "B";

    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.AutoInteger;
    private final static String LIST_KEY_NAME = "Key";

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName();
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/query";
    }

    @Override
    protected void doCleanup() throws Exception
    {
        try { deleteProject(getProjectName()) ; } catch (Throwable t) { }
    }


    @Override
    protected void doTestSteps() throws Exception
    {
        doSetup();

        doTestListLookupURL();
        doTestIssue15610();
        doTestIssue15751();
    }

    protected void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);
        createSubfolder(getProjectName(), SUB_FOLDER_A, new String[] {"List", "Study", "ViscStudies"});
        createSubfolder(getProjectName(), SUB_FOLDER_B, new String[] {"List", "Study", "ViscStudies"});
    }

    protected void doTestListLookupURL()
    {
        log("** Creating lookup target list in sub-folder");
        goToProjectHome();
        ListHelper.ListColumn[] lookupTargetCols = {
            new ListHelper.ListColumn("LookupName", "LookupName", ListHelper.ListColumnType.String, "Lookup Name"),
            new ListHelper.ListColumn("LookupAge", "LookupAge", ListHelper.ListColumnType.Integer, "Lookup Age", null, null, null, "fake/action.view?key=${Key}")
        };
        String lookupTargetListName = SUB_FOLDER_A + "-LookupTarget-List";
        ListHelper.createList(this, SUB_FOLDER_A, lookupTargetListName, LIST_KEY_TYPE, LIST_KEY_NAME, lookupTargetCols);
        clickNavButton("Done");

        log("** Insert row into lookup target list");
        goToProjectHome();
        clickLinkWithText(SUB_FOLDER_A);
        clickLinkWithText(lookupTargetListName);
        ListHelper.insertNewRow(this, Maps.<String, String>of(
                "LookupName", "MyLookupItem1",
                "LookupAge", "100"
        ));
        ListHelper.insertNewRow(this, Maps.<String, String>of(
                "LookupName", "MyLookupItem2",
                "LookupAge", "200"
        ));

        log("** Creating list with lookup to list in sub-folder");
        goToProjectHome();
        ListHelper.ListColumn[] cols = {
            new ListHelper.ListColumn("MyName", "MyName", ListHelper.ListColumnType.String, "My Name"),
            new ListHelper.ListColumn("ListLookup", "ListLookup", ListHelper.ListColumnType.String, "List Lookup", new ListHelper.LookupInfo(getProjectName() + "/" + SUB_FOLDER_A, "lists", lookupTargetListName)),
        };
        String lookupSourceListName = "Project-LookupSource-List";
        ListHelper.createList(this, getProjectName(), lookupSourceListName, LIST_KEY_TYPE, LIST_KEY_NAME, cols);
        clickNavButton("Done");

        log("** Insert row into list");
        goToProjectHome();
        clickLinkWithText(lookupSourceListName);
        clickNavButton("Insert New");
        setFormElement("quf_MyName", "MyName");
        selectOptionByText(Locator.name("quf_ListLookup"), "MyLookupItem2");
        clickNavButton("Submit");

        log("** Adding in lookup list columns to grid");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, new String[] { "ListLookup", "LookupAge" });
        CustomizeViewsHelper.saveCustomView(this);

        log("** Checking URLs go to correct container...");
        String href = getAttribute(Locator.linkWithText("edit"), "href");
        assertTrue("Expected [edit] link to go to " + getProjectName() + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/update.view?"));

        href = getAttribute(Locator.linkWithText("details"), "href");
        assertTrue("Expected [details] link to go to " + getProjectName() + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/details.view?"));

        href = getAttribute(Locator.linkWithText("MyName"), "href");
        assertTrue("Expected MyName link to go to " + getProjectName() + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/details.view?"));

        href = getAttribute(Locator.linkWithText("MyLookupItem2"), "href");
        assertTrue("Expected ListLookup link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container, href=" + href,
                href.contains("/list/" + getProjectName() + "/" + SUB_FOLDER_A + "/details.view?"));

        href = getAttribute(Locator.linkWithText("200"), "href");
        assertTrue("Expected ListLookup/LookupAge link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container, href=" + href,
                href.contains("/fake/" + getProjectName() + "/" + SUB_FOLDER_A + "/action.view?key=2"));

    }

    // Issue 15610: viscstudieslist - URLs generated from lookups are broken
    protected void doTestIssue15610()
    {
        log("** Creating study in " + SUB_FOLDER_A);
        goToProjectHome();
        clickLinkWithText(SUB_FOLDER_A);
        goToManageStudy();
        clickNavButton("Create Study");
        setFormElement(Locator.name("label"), SUB_FOLDER_A + "-Study");
        clickNavButton("Create Study");

        log("** Creating study in " + SUB_FOLDER_B);
        goToProjectHome();
        clickLinkWithText(SUB_FOLDER_B);
        goToManageStudy();
        clickNavButton("Create Study");
        setFormElement(Locator.name("label"), SUB_FOLDER_B + "-Study");
        clickNavButton("Create Study");

        log("** Creating list with lookup to viscstudies.studies");
        ListHelper.ListColumn[] cols = {
            new ListHelper.ListColumn("StudyLookup", "StudyLookup", ListHelper.ListColumnType.String, "Study Lookup", new ListHelper.LookupInfo(null, "viscstudies", "studies")),
        };
        ListHelper.createList(this, getProjectName(), "Issue15610-List", LIST_KEY_TYPE, LIST_KEY_NAME, cols);
        clickNavButton("Done");

        log("** Insering row into list");
        goToProjectHome();
        clickLinkWithText("Issue15610-List");
        clickNavButton("Insert New");
        selectOptionByText(Locator.name("quf_StudyLookup"), SUB_FOLDER_A + "-Study");
        clickNavButton("Submit");

        clickNavButton("Insert New");
        selectOptionByText(Locator.name("quf_StudyLookup"), SUB_FOLDER_B + "-Study");
        clickNavButton("Submit");

        log("** Checking URLs go to correct container...");
        String href = getAttribute(Locator.linkWithText(SUB_FOLDER_A + "-Study"), "href");
        assertTrue("Expected 'MyStudy' link to go to " + getProjectName() + "/" + SUB_FOLDER_A + " container: " + href,
                href.contains("/study/" + getProjectName() + "/" + SUB_FOLDER_A + "/studySchedule.view"));

        href = getAttribute(Locator.linkWithText(SUB_FOLDER_B + "-Study"), "href");
        assertTrue("Expected 'MyStudy' link to go to " + getProjectName() + "/" + SUB_FOLDER_B + " container: " + href,
                href.contains("/study/" + getProjectName() + "/" + SUB_FOLDER_B + "/studySchedule.view"));
    }

    // Issue 15751: Pipeline job list generates URLs without correct container
    protected void doTestIssue15751()
    {
        log("** Create pipeline jobs");
        insertJobIntoSubFolder(SUB_FOLDER_A);
        insertJobIntoSubFolder(SUB_FOLDER_B);

        log("** Viewing pipeline status from project container. Sort by Description (report name) and include sub-folders");
        beginAt("/pipeline-status/" + getProjectName() + "/showList.view?StatusFiles.sort=Description&StatusFiles.containerFilterName=CurrentAndSubfolders");

        log("** Checking URLs go to correct container...");
        String href = getAttribute(Locator.linkWithText("COMPLETE", 0), "href");
        assertTrue("Expected 'COMPLETE' link 0 to go to current A container: " + href,
                href.contains("/pipeline-status/" + getProjectName() + "/" + SUB_FOLDER_A + "/details.view"));

        href = getAttribute(Locator.linkWithText("COMPLETE", 1), "href");
        assertTrue("Expected 'COMPLETE' link 1 to go to current B container: " + href,
                href.contains("/pipeline-status/" + getProjectName() + "/" + SUB_FOLDER_B + "/details.view"));
    }

    protected void insertJobIntoSubFolder(String folder)
    {
        goToProjectHome();

        log("** Creating list in folder '" + folder + "'");
        ListHelper.ListColumn[] cols = {
            new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name")
        };
        String listName = folder + "-Issue15751-List";
        ListHelper.createList(this, folder, listName, LIST_KEY_TYPE, LIST_KEY_NAME, cols);
        clickNavButton("Done");

        log("** Creating background R script");
        goToProjectHome();
        clickLinkWithText(folder);
        clickLinkWithText(listName);
        clickMenuButton("Views", "Create", "R View");
        clickCheckboxById("runInBackground");
        clickNavButton("Save", 0);
        setFormElement(Locator.xpath("//input[@class='ext-mb-input']"), folder + "-BackgroundReport");
        ExtHelper.clickExtButton(this, "Save");

        log("** Executing background R script");
        clickMenuButton("Views", folder + "-BackgroundReport");
        waitForElement(Locator.navButton("Start Job"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Start Job", 0);
        waitForText("COMPLETE", WAIT_FOR_PAGE);
    }

}
