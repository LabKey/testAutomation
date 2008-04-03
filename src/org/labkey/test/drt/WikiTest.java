package org.labkey.test.drt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;

/**
 * User: brittp
 * Date: Nov 15, 2005
 * Time: 1:55:56 PM
 */
public class WikiTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "WikiVerifyProject";
    private static final String PROJECT2_NAME = "WikiCopied";
    private static final String WIKI_PAGE1_TITLE = "Page 1 Wiki Title";
    private static final String WIKI_PAGE1_NAME= "Page 1 Wiki Name";
    private static final String WIKI_PAGE2_NAME = "Page 2 Wiki Name";
    private static final String WIKI_PAGE2_TITLE = "Page 2 Wiki Title";
    private static final String WIKI_PAGE3_ALTTITLE = "PageBBB has HTML";
    private static final String WIKI_PAGE3_NAME_TITLE = "Page 3 Wiki";
    private static final String WIKI_PAGE4_TITLE = "New Wiki";
    private static final String DISC1_TITLE = "Let's Talk";
    private static final String DISC1_BODY = "I don't know how normal this wiki is";
    private static final String RESP1_TITLE = "Let's Keep Talking";
    private static final String RESP1_BODY = "I disagree";
    private static final String USER1 = "apple@a1b2c1.com";
    private static final String WIKI_PAGE3_WEBPART_TEST = "Best Gene Name";
    private static final int MAX_AJAX_WAIT_CYCLES = 10;

    private static final String WIKI_PAGE1_CONTENT =
            "1 Title\n" +
            "1.1 Subtitle\n" +
            "normal normal normal\n" +
            "\n" +
            "new paragraph\\\\\n" +
            "[" + WIKI_PAGE2_NAME + "]";

    private static final String WIKI_PAGE2_CONTENT =
            "1 Page AAA\n" +
            "[Welcome|" + WIKI_PAGE1_NAME + "]";

    private static final String WIKI_PAGE3_CONTENT =
            "<b>Some HTML content</b>\n" +
                    "<b>${labkey.webPart(partName='Query', title='My Proteins', schemaName='ms2', " +
                    "queryName='Sequences', allowChooseQuery='true', allowChooseView='true')}</b>\n";
    private static final String WIKI_PAGE3_CONTENT_NO_QUERY =
            "<b>Some HTML content</b>\n" +
                    "<b>No query part</b>\n";

    private static final String WIKI_PAGE4_CONTENT =
            "This is wiki page <i>4</i><br/>${labkey.webPart(partName='Wiki TOC')}";

    private static final String NAVBAR1_CONTENT =
            "{labkey:tree|name=core.currentProject}";

    private static final String NAVBAR2_CONTENT =
            "{labkey:tree|name=core.currentProject}\n" +
                    "{labkey:tree|name=core.projects}\n"+
                    "{labkey:tree|name=core.projectAdmin}\n" +
                    "{labkey:tree|name=core.siteAdmin}\n";

    private static final String HEADER_CONTENT =
            "Yo! This is the header!";

    public String getAssociatedModuleDirectory()
    {
        return "wiki";
    }


    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doTestSteps()
    {
        createProject(PROJECT2_NAME);
        setPermissions("Users", "Editor");
        createProject(PROJECT_NAME);
        createPermissionsGroup("testers");
        assertPermissionSetting("Administrators", "Admin (all permissions)");
        assertPermissionSetting("testers", "No Permissions");
        setPermissions("testers", "Editor");
        setPermissions("Users", "Editor");
        clickLinkWithText("Customize Folder");
        checkCheckbox(Locator.checkboxByTitle("Wiki", false));
        submit();

        clickLinkWithText(PROJECT2_NAME);
        clickLinkWithText("Customize Folder");
        checkCheckbox(Locator.checkboxByTitle("Wiki", false));
        submit();
                
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Wiki");
        addWebPart("Search");

        log("Test new wiki page");
        clickLinkWithText("Create a new wiki page");
        selectRenderType("Wiki Page");
        setFormElement("name", WIKI_PAGE1_NAME);
        setFormElement("title", WIKI_PAGE1_TITLE);
        setFormElement("body", WIKI_PAGE1_CONTENT);
        clickNavButton("Submit");

        searchFor(PROJECT_NAME, "normal normal normal", 1, WIKI_PAGE1_TITLE);

        log("Test add content to link page");
        assertLinkPresentWithText(WIKI_PAGE2_NAME);
        clickLinkWithText(WIKI_PAGE2_NAME);
        assertTextPresent("page has no content");
        clickLinkWithText("add content");
        selectRenderType("Wiki Page");
        setFormElement("title", WIKI_PAGE2_TITLE);
        setFormElement("body", WIKI_PAGE2_CONTENT);
        clickNavButton("Submit");
        clickLinkWithText("Welcome");
        //Add once update wiki title bug is fixed
        //assertLinkNotPresentWithText(WIKI_PAGE2_NAME);

        searchFor(PROJECT_NAME, "Page AAA", 1, WIKI_PAGE2_TITLE);

        log("test create new html page with a webpart");
        clickLinkWithText("new page");
        selectRenderType("HTML");
        setFormElement("name", WIKI_PAGE3_NAME_TITLE);
        setFormElement("title", WIKI_PAGE3_NAME_TITLE);
        selectOptionByText("parent", WIKI_PAGE2_TITLE + " (" + WIKI_PAGE2_NAME + ")");
        setFormElement("body", WIKI_PAGE3_CONTENT);

        log("test attachments.");
        if (isFileUploadAvailable())
        {
            clickLinkWithText("Attach a file", false);
            File file = new File(getLabKeyRoot() + "/common.properties");
            setFormElement("formFiles[0]", file);
        }
        else
            log("File upload skipped.");
        clickNavButton("Submit");
        if (isFileUploadAvailable())
            assertTextPresent("common.properties");
        assertTextPresent(WIKI_PAGE3_WEBPART_TEST);
        assertTextPresent("Some HTML content");

        searchFor(PROJECT_NAME, "Wiki", 3, WIKI_PAGE3_NAME_TITLE);

        log("test edit");
        clickLinkWithText("edit");
        selectRenderType("HTML");
        setFormElement("title", WIKI_PAGE3_ALTTITLE);
        String wikiPage3ContentEdited =
            "<b>Some HTML content</b><br>\n" +
            "<b>More HTML content</b><br>\n" +
            "<a href='" + getContextPath() + "/wiki/" + PROJECT_NAME + "/page.view?name=PageAAA'>Page AAA</a><br>\n";
        setFormElement("body", wikiPage3ContentEdited);
        clickNavButton("Finished");
        assertTextPresent("More HTML content");
        clickLinkWithText("edit");
        setFormElement("body", WIKI_PAGE3_CONTENT_NO_QUERY);
        setFormElement("title", WIKI_PAGE3_NAME_TITLE);
        clickNavButton("Finished");

        pushLocation();
        //because we replace the body with the content
        searchFor(PROJECT_NAME, "More HTML", 0);
        popLocation();

        log("test change renderer type");
        assertTextPresent("Some HTML content");
        clickLinkWithText("edit");
        selectRenderType("Plain Text");
        clickNavButton("Finished");
        assertTextPresent("<b>");
        clickLinkWithText("edit");
        selectRenderType("HTML");
        clickNavButton("Finished");
        //bvt
        log("Check Start Page series works");
        searchFor(PROJECT_NAME, "Some HTML", 1, WIKI_PAGE3_NAME_TITLE);
        assertLinkPresentWithTextCount(WIKI_PAGE2_TITLE, 2);

        log("Check Pages menu works");
        clickLinkWithText(WIKI_PAGE2_TITLE);
        clickLinkWithText("[next]");
        assertTextPresent("Some HTML content");
        clickLinkWithText("[previous]");
        assertTextPresent("Welcome");

        log("Check sibling order edit works");
        clickLinkWithText(WIKI_PAGE1_TITLE);
        clickLinkWithText("manage");
        waitForPageToLoad();
        clickNavButton("Move Down", 0);
        clickNavButton("Save");
        clickLinkWithText(WIKI_PAGE3_NAME_TITLE);
        clickLinkWithText("[next]");
        assertTextPresent("normal normal normal");
        clickLinkWithText("manage");
        waitForPageToLoad();
        clickNavButton("Move Up", 0);
        clickNavButton("Save");
        clickLinkWithText("[next]");
        assertTextPresent("Page AAA");
        
        log("Check parent reset works");
        clickLinkWithText(WIKI_PAGE3_NAME_TITLE);
        clickLinkWithText("manage");
        selectOptionByText("parent", WIKI_PAGE1_TITLE + " (" + WIKI_PAGE1_NAME + ")");
        waitForPageToLoad();
        clickNavButton("Save");
        clickLinkWithText(WIKI_PAGE1_TITLE);
        clickLinkWithText("[next]");
        assertTextPresent("Some HTML content");
        //endbvt
        log("Check that discussion board works");
        clickLinkWithText(WIKI_PAGE1_TITLE);
        clickLinkWithText("discuss this", false);
        clickLinkWithText("Start new discussion");
        setFormElement("title", DISC1_TITLE);
        setFormElement("body", DISC1_BODY);
        submit();
        assertTextPresent(DISC1_TITLE);
        assertTextPresent(DISC1_BODY);
        assertTextPresent("see discussions (1)");

        log("Check response on discussion board works");
        clickNavButton("Post Response");
        setFormElement("title", RESP1_TITLE);
        setFormElement("body", RESP1_BODY);
        submit();
        assertTextPresent(RESP1_TITLE);
        assertTextPresent(RESP1_BODY);
        clickNavButton("Delete Message");
        clickNavButton("Delete");
        assertTextNotPresent(DISC1_TITLE);
        assertTextNotPresent(DISC1_BODY);
        //bvt
        log("test navTree and header");
        clickLinkWithText("new page");
        selectRenderType("Wiki Page");
        setFormElement("name", "_navTree");
        setFormElement("title", "NavTree");
        setFormElement("body", NAVBAR1_CONTENT);
        clickNavButton("Submit");

        assertTextNotPresent("Home");
        assertLinkPresentWithText(PROJECT_NAME);

        clickLinkWithText("edit");
        setFormElement("body", NAVBAR2_CONTENT);
        clickNavButton("Save");
        assertTextPresent("Projects");
        assertTextPresent("Manage Project");
        assertTextPresent("Manage Site");

        clickNavButton("Finished");
        clickLinkWithText("manage", 0);
        clickNavButton("Delete Page");
        clickNavButton("Delete", "large");
        assertLinkPresentWithText("Home");

        clickLinkWithText("new page");
        selectRenderType("HTML");
        setFormElement("name", "_header");
        setFormElement("title", "Header");
        setFormElement("body", HEADER_CONTENT);
        clickNavButton("Submit");

        clickLinkWithText(WIKI_PAGE3_NAME_TITLE);
        assertTextPresent(HEADER_CONTENT);
        clickLinkWithText("Header");
        clickLinkWithText("manage", 0);
        clickNavButton("Delete Page");
        clickNavButton("Delete", "large");
        assertTextNotPresent(HEADER_CONTENT);

        log("Return to where we were...");
        clickLinkWithText(WIKI_PAGE3_NAME_TITLE);

        log("test versions");
        clickLinkWithText("history");
        //clickLinkWithText("1"); This has the query web part in it
        //assertTextPresent("Some HTML content");
        clickLinkWithText("2");
        clickNavButton("Make Current");
        assertTextPresent("6");
        clickLinkWithText(WIKI_PAGE1_TITLE);
        clickLinkWithText("[next]");
        assertTextPresent("More HTML content");
        assertTextPresent(WIKI_PAGE3_ALTTITLE);

        log("test terms of use");
        clickLinkWithText("new page");
        selectRenderType("Wiki Page");
        setFormElement("name", "_termsOfUse");
        setFormElement("title", "Terms of Use");
        setFormElement("body", "The first rule of fight club is do not talk about fight club.");
        clickNavButton("Submit");

        log("Terms don't come into play until you log out");
        signOut();
        signIn();
        clickLinkWithText(PROJECT_NAME);
        assertTextPresent("fight club");
        log("Submit without agreeing");
        clickNavButton("Agree");

        assertTextPresent("You must agree");
        checkCheckbox("approvedTermsOfUse");
        clickNavButton("Agree");

        clickTab("Wiki");
        clickLinkWithText("Terms of Use");
        searchFor(PROJECT_NAME, "fight club", 1, "Terms of Use");
        clickLinkWithText("manage");
        clickNavButton("Delete Page");
        clickNavButton("Delete", "large");
        assertTextNotPresent("Terms of Use");
        //endbvt
        log("test copy wiki");
        clickLinkWithText("copy pages");
        clickLinkWithText(PROJECT2_NAME);
        clickNavButton("Copy Pages");

        log("test wiki customize link");
        clickTab("Portal");
        addWebPart("Wiki");
        clickLinkWithImage(getContextPath() + "/_images/partedit.gif");
        log("check that container is set to current project");
        selectOptionByText("webPartContainer", "/" + PROJECT_NAME);
        clickLinkWithText("Reset to Folder Default Page", false);
        assertOptionEquals("webPartContainer", "/" + PROJECT2_NAME);
        log("set container and page");

        //page names are now fetched via AJAX, so wait for them to be populated
        int waitCycles = 0;
        while(!isTextPresent(WIKI_PAGE2_NAME + " (" + WIKI_PAGE2_TITLE + ")") && waitCycles < MAX_AJAX_WAIT_CYCLES)
        {
            log("Waiting for page names to be populated...");
            sleep(500);
            ++waitCycles;
        }
        if(waitCycles == MAX_AJAX_WAIT_CYCLES)
            fail("AJAX population of page names in wiki web part customize view took longer than " + (MAX_AJAX_WAIT_CYCLES/2) + " seconds!");

        selectOptionByText("name", WIKI_PAGE2_NAME + " (" + WIKI_PAGE2_TITLE + ")");
        submit("Submit");
        clickLinkWithText("Welcome");
        log("make sure it all got copied");
        clickLinkWithText(WIKI_PAGE3_ALTTITLE);
        clickLinkWithText(WIKI_PAGE1_TITLE);
        assertTextPresent("normal normal normal");
        //bvt
        log("Check Permissions");
        log("Create fake user for permissions check");
        clickLinkWithText("Permissions");
        clickLink("managegroup/" + PROJECT2_NAME + "/Users");
        setFormElement("names", USER1);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");

        log("Check if permissions work");
        clickLinkWithText("Permissions");
        setPermissions("User", "Reader");
        impersonate(USER1);
        clickLinkWithText(PROJECT2_NAME);
        assertTextPresent(WIKI_PAGE2_TITLE);
        clickTab("Wiki");
        assertTextNotPresent("copy pages");
        signOut();
        signIn();
        clickLinkWithText(PROJECT2_NAME);
        clickLinkWithText("Permissions");
        setPermissions("User", "No Permissions");
        impersonate(USER1);
        clickLinkWithText(PROJECT2_NAME);
        assertTextNotPresent(WIKI_PAGE2_TITLE);
        signOut();
        signIn();

        log("Check if readers can read from other projects");
        clickLinkWithText(PROJECT2_NAME);
        clickLinkWithImage(getContextPath() + "/_images/partedit.gif");
        selectOptionByText("webPartContainer", "/" + PROJECT_NAME);

        //page names are now fetched via AJAX, so wait for them to be populated
        waitCycles = 0;
        while(!isTextPresent(WIKI_PAGE2_NAME + " (" + WIKI_PAGE2_TITLE + ")") && waitCycles < MAX_AJAX_WAIT_CYCLES)
        {
            log("Waiting for page names to be populated...");
            sleep(500);
            ++waitCycles;
        }
        if(waitCycles == MAX_AJAX_WAIT_CYCLES)
            fail("AJAX population of page names in wiki web part customize view took longer than " + (MAX_AJAX_WAIT_CYCLES/2) + " seconds!");

        selectOptionByText("name", WIKI_PAGE2_NAME + " (" + WIKI_PAGE2_TITLE + ")");
        submit();
        assertTextPresent(WIKI_PAGE2_TITLE);
        clickLinkWithText("Permissions");
        setPermissions("User", "Admin (all permissions)");
        impersonate(USER1);
        clickLinkWithText(PROJECT2_NAME);
        assertTextNotPresent("Welcome");
        log("Also check copying permission");
        clickTab("Wiki");
        clickLinkWithText("copy pages");
        assertTextNotPresent(PROJECT_NAME);
        signOut();
        signIn();
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Permissions");
        clickLink("managegroup/" + PROJECT_NAME + "/Users");
        setFormElement("names", USER1);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");
        impersonate(USER1);
        clickLinkWithText(PROJECT2_NAME);
        assertTextPresent(WIKI_PAGE2_TITLE);
        log("Also check copying permission");
        clickTab("Wiki");
        clickLinkWithText("copy pages");
        assertTextNotPresent(PROJECT_NAME);
        signOut();
        signIn();
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("Permissions");
        setPermissions("User", "Admin (all permissions)");
        log("make sure the changes went through");
        signOut();
        signIn();
        impersonate(USER1);
        clickLinkWithText(PROJECT2_NAME);
        clickTab("Wiki");
        clickLinkWithText("copy pages");
        assertTextPresent(PROJECT_NAME);
        signOut();
        signIn();
        //endbvt
        log("delete wiki web part");
        clickLinkWithText(PROJECT2_NAME);
        clickTab("Portal");
        clickLinkWithImage(getContextPath() + "/_images/partdelete.gif");
        assertLinkNotPresentWithText("Welcome");

        log("test wiki TOC customize link");
        addWebPart("Wiki TOC");
        clickLinkWithImage(getContextPath() + "/_images/partedit.gif");
        setFormElement("title", "Test Customize TOC");
        log("check that container is set to current project");
        assertOptionEquals("webPartContainer", "/" + PROJECT2_NAME);
        selectOptionByText("webPartContainer", "/" + PROJECT_NAME);
        submit();
        log("check that TOC title is set correctly");
        assertTextPresent("Test Customize TOC");
        assertTextPresent(WIKI_PAGE2_TITLE);

        log("Check that 'Copy Pages' in TOC works");
        clickLinkWithText("copy pages");
        clickLinkWithText(PROJECT_NAME);
        clickNavButton("Copy Pages");
        clickLinkWithText(PROJECT_NAME);
        clickTab("Wiki");
        assertTextPresent(WIKI_PAGE1_TITLE);

        log("Check that 'New Page' works");
        clickLinkWithText(PROJECT2_NAME);
        clickTab("Portal");
        clickLinkWithText("new page");
        setFormElement("name", WIKI_PAGE4_TITLE);
        setFormElement("body", WIKI_PAGE4_CONTENT);
        clickNavButton("Submit");
        clickLinkWithText(PROJECT_NAME);
        clickTab("Wiki");
        assertTextPresent(WIKI_PAGE4_TITLE);

        log("test delete");
        clickLinkWithText(WIKI_PAGE2_TITLE);
        clickLinkWithText("manage", 0);
        clickNavButton("Delete Page");
        clickNavButton("Delete", "large");
        clickLinkWithText(WIKI_PAGE1_TITLE);
        //add once bug with caching wiki title is fixed
        //assertLinkNotPresentWithText(WIKI_PAGE2_TITLE);

        log("delete project with copied wiki");
        clickLinkWithText("Manage Folders");
        clickNavButton("Delete");
        log("confirm delete");
        clickNavButton("Delete");

        //extended wiki test -- generate 2000 pages
//        clickLinkWithText(PROJECT_NAME);
//        clickTab("Wiki");
//        for (int i = 0; i <= 1999; i++)
//        {
//            //test create new html page
//            clickLinkWithText("new page");
//            setFormElement("name", "Page" + Integer.toString(i));
//            setFormElement("title", "Page" + Integer.toString(i));
//
//            if (i > 99)
//            {
//                String parent = Integer.toString(i);
//                parent = parent.substring(2);
//                selectOption("parent", "Page" + parent + " (Page" + parent + ")");
//            }
//            setFormElement("body", "Page" + Integer.toString(i));
//            submit();
//        }
    }

    protected void selectRenderType(String renderType)
    {
        if (!renderType.equals(getSelectedOptionText("rendererType")))
        {
            selectOptionByText("rendererType", renderType);
            waitForPageToLoad();
        }
        if ("HTML".equals(renderType) && isNavButtonPresent("Use HTML Source Editor"))
            clickNavButton("Use HTML Source Editor");
    }
    
    protected void doCleanup()
        {
            deleteUser(USER1);
            try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
            try {deleteProject(PROJECT2_NAME); } catch (Throwable t) {}
        }

}
