/*
 * Copyright (c) 2008-2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

import java.io.File;

/**
 * User: brittp
 * Date: Nov 15, 2005
 * Time: 1:55:56 PM
 */
public class WikiLongTest extends BaseSeleniumWebTest
{
    private static final String PROJECT_NAME = "WikiVerifyProject";
    private static final String PROJECT2_NAME = "WikiCopied";
    private static final String PROJECT3_NAME = "WikiBvt Public Project";
    private static final String PROJECT4_NAME = "WikiBvt Fourth Project";
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
    private static final String USER1 = "user1@wikibvt.test";
    private static final String USER2 = "user2@wikibvt.test";
    private static final String WIKI_PAGE3_WEBPART_TEST = "Best Gene Name";
    private static final String WIKI_NAVTREE_TITLE = "NavTree";
    private static final String WIKI_TERMS_TITLE = "Terms of Use";
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
                    "{labkey:tree|name=core.folderAdmin}\n" +
                    "{labkey:tree|name=core.projectAdmin}\n" +
                    "{labkey:tree|name=core.siteAdmin}\n";

    private static final String HEADER_CONTENT =
            "Yo! This is the header!";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/wiki";
    }


    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void doTestSteps()
    {
        enableEmailRecorder();
        createProject(PROJECT2_NAME);
        setPermissions("Users", "Editor");
        clickNavButton("Save and Finish");
        createProject(PROJECT_NAME);
        createPermissionsGroup("testers");
        setPermissions("testers", "Editor");
        setPermissions("Users", "Editor");
        clickNavButton("Save and Finish");
        clickLinkWithText("Folder Settings");
        checkCheckbox(Locator.checkboxByTitle("Wiki"));
        submit();

        clickLinkWithText(PROJECT2_NAME);
        clickLinkWithText("Folder Settings");
        checkCheckbox(Locator.checkboxByTitle("Wiki"));
        submit();

        clickLinkWithText(PROJECT_NAME);
        addWebPart("Wiki");
        addWebPart("Search");

        log("Test new wiki page");
        createNewWikiPage("RADEOX");

        setFormElement("name", WIKI_PAGE1_NAME);
        setFormElement("title", WIKI_PAGE1_TITLE);
        setFormElement("body", WIKI_PAGE1_CONTENT);
        saveWikiPage();

        searchFor(PROJECT_NAME, "normal normal normal", 1, WIKI_PAGE1_TITLE);

        log("Test add content to link page");
        assertLinkPresentWithText(WIKI_PAGE2_NAME);
        clickLinkWithText(WIKI_PAGE2_NAME);
        assertTextPresent("page has no content");
        clickLinkWithText("add content");
        convertWikiFormat("RADEOX");

        setFormElement("title", WIKI_PAGE2_TITLE);
        setFormElement("body", WIKI_PAGE2_CONTENT);
        saveWikiPage();

        clickLinkWithText("Welcome");
        assertLinkNotPresentWithText(WIKI_PAGE2_NAME);

        searchFor(PROJECT_NAME, "Page AAA", 1, WIKI_PAGE2_TITLE);

        log("test create new html page with a webpart");
        createNewWikiPage("HTML");

        setFormElement("name", WIKI_PAGE3_NAME_TITLE);
        setFormElement("title", WIKI_PAGE3_NAME_TITLE);
        selectOptionByText("parent", WIKI_PAGE2_TITLE + " (" + WIKI_PAGE2_NAME + ")");
        setWikiBody(WIKI_PAGE3_CONTENT);        
        if (isFileUploadAvailable())
        {
            log("test attachments in wiki");
            File file = new File(getLabKeyRoot() + "/common.properties");
            setFormElement("formFiles[0]", file);
        }
        else
            log("File upload skipped.");
        saveWikiPage();
        if (isFileUploadAvailable())
            assertTextPresent("common.properties");
        assertTextPresent(WIKI_PAGE3_WEBPART_TEST);
        assertTextPresent("Some HTML content");

        searchFor(PROJECT_NAME, "Wiki", 3, WIKI_PAGE3_NAME_TITLE);

        log("test edit");
        clickLinkWithText("Edit");
        setFormElement("title", WIKI_PAGE3_ALTTITLE);
        String wikiPage3ContentEdited =
            "<b>Some HTML content</b><br>\n" +
            "<b>More HTML content</b><br>\n" +
            "<a href='" + getContextPath() + "/wiki/" + PROJECT_NAME + "/page.view?name=PageAAA'>Page AAA</a><br>\n";
        setWikiBody(wikiPage3ContentEdited);
        saveWikiPage();

        assertTextPresent("More HTML content");
        clickLinkWithText("Edit");
        setWikiBody(WIKI_PAGE3_CONTENT_NO_QUERY);
        setFormElement("title", WIKI_PAGE3_NAME_TITLE);
        saveWikiPage();

        log("test change renderer type");
        assertTextPresent("Some HTML content");
        clickLinkWithText("Edit");
        changeFormat("TEXT_WITH_LINKS");
        saveWikiPage();

        assertTextPresent("<b>");
        clickLinkWithText("Edit");
        changeFormat("HTML");
        saveWikiPage();

        log("Check Start Page series works");
        searchFor(PROJECT_NAME, "Some HTML", 1, WIKI_PAGE3_NAME_TITLE);
        assertLinkPresentWithTextCount(WIKI_PAGE2_TITLE, 2);

        log("Check Pages menu works");
        clickLinkWithText(WIKI_PAGE2_TITLE);
        clickLinkWithText("next");
        assertTextPresent("Some HTML content");
        clickLinkWithText("previous");
        assertTextPresent("Welcome");

        log("Check sibling order edit works");
        clickLinkWithText(WIKI_PAGE1_TITLE);
        clickLinkWithText("Manage");
        clickNavButton("Move Down", 0);
        clickNavButton("Save");
        clickLinkWithText(WIKI_PAGE3_NAME_TITLE);
        clickLinkWithText("next");
        assertTextPresent("normal normal normal");
        clickLinkWithText("Manage");
        clickNavButton("Move Up", 0);
        clickNavButton("Save");
        clickLinkWithText("next");
        assertTextPresent("Page AAA");

        log("Check parent reset works");
        clickLinkWithText(WIKI_PAGE3_NAME_TITLE);
        clickLinkWithText("Manage");
        selectOptionByText("parent", WIKI_PAGE1_TITLE + " (" + WIKI_PAGE1_NAME + ")");
        waitForPageToLoad();
        clickNavButton("Save");
        clickLinkWithText(WIKI_PAGE1_TITLE);
        clickLinkWithText("next");
        assertTextPresent("Some HTML content");

        log("Check that discussion board works");
        clickLinkWithText(WIKI_PAGE1_TITLE);
        waitForExtReady();
        clickLinkWithText("discuss this", false);
        waitForElement(Locator.linkWithText("Start new discussion"), defaultWaitForPage);
        clickLinkWithText("Start new discussion");
        setFormElement("title", DISC1_TITLE);
        setFormElement("body", DISC1_BODY);
        submit();
        waitForExtReady();
        clickLinkWithText("see discussions (1)", false);
        clickLinkWithText(DISC1_TITLE);
        assertTextPresent(DISC1_TITLE);
        assertTextPresent(DISC1_BODY);

        log("Check response on discussion board works");
        clickNavButton("Respond");
        setFormElement("title", RESP1_TITLE);
        setFormElement("body", RESP1_BODY);
        submit();
        assertTextPresent(RESP1_TITLE);
        assertTextPresent(RESP1_BODY);
        clickNavButton("Delete Message");
        clickNavButton("Delete");
        assertTextNotPresent(DISC1_TITLE);
        assertTextNotPresent(DISC1_BODY);

        log("test navTree and header");
        createNewWikiPage("RADEOX");
        setFormElement("name", "_navTree");
        setFormElement("title", WIKI_NAVTREE_TITLE);
        setWikiBody(NAVBAR1_CONTENT);
        saveWikiPage();

        assertTextNotPresent("Home");
        assertLinkPresentWithText(PROJECT_NAME);

        clickLinkWithText("Edit");
        setWikiBody(NAVBAR2_CONTENT);
        saveWikiPage();
        assertLinkPresentWithText("Projects");
        assertLinkPresentWithText("Manage Project");
        assertLinkPresentWithText("Manage Site");

        //test deleting via edit page
        clickLinkWithText("Edit");
        deleteWikiPage();
        assertLinkPresentWithText("Home");

        createNewWikiPage("HTML");
        setFormElement("name", "_header");
        setFormElement("title", "Header");
        setWikiBody(HEADER_CONTENT);
        saveWikiPage();

        clickLinkWithText("Header");
        assertTextPresent(HEADER_CONTENT);
        clickLinkWithText("Edit", 0);
        deleteWikiPage();
        assertTextNotPresent(HEADER_CONTENT);

        log("Return to where we were...");
        clickLinkWithText(WIKI_PAGE3_NAME_TITLE);

        log("test versions");
        clickLinkWithText("History");
        clickLinkWithText("2");
        clickNavButton("Make Current");
        assertTextPresent("6");
        clickLinkWithText(WIKI_PAGE1_TITLE);
        clickLinkWithText("next");
        assertTextPresent("More HTML content");
        assertTextPresent(WIKI_PAGE3_ALTTITLE);

        termsOfUseTest();

        log("test copy wiki");
        clickWebpartMenuItem("Pages", "Copy");
        clickLinkWithText(PROJECT2_NAME);
        clickNavButton("Copy Pages");

        log("test wiki customize link");
        clickTab("Portal");
        addWebPart("Wiki");
        clickWebpartMenuItem("Wiki", "Customize");
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
        clickNavButton("Submit");
        clickLinkWithText("Welcome");
        log("make sure it all got copied");
        clickLinkWithText(WIKI_PAGE3_ALTTITLE);
        clickLinkWithText(WIKI_PAGE1_TITLE);

        log("Check Permissions");
        log("Create fake user for permissions check");
        enterPermissionsUI();
        clickManageGroup("Users");
        setFormElement("names", USER1);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");

        log("Check if permissions work");
        enterPermissionsUI();
        setPermissions("User", "Reader");
        clickNavButton("Save and Finish");
        impersonate(USER1);
        clickLinkWithText(PROJECT2_NAME);
        assertTextPresent(WIKI_PAGE2_TITLE);
        clickTab("Wiki");
        assertTextNotPresent("copy pages");
        stopImpersonating();
        clickLinkWithText(PROJECT2_NAME);
        enterPermissionsUI();
        removePermission("User", "Editor");
        removePermission("User", "Reader");
        clickNavButton("Save and Finish");
        impersonate(USER1);
        clickLinkWithText(PROJECT2_NAME);
        assertTextNotPresent(WIKI_PAGE2_TITLE);
        stopImpersonating();

        log("Check if readers can read from other projects");
        clickLinkWithText(PROJECT2_NAME);
        clickWebpartMenuItem(WIKI_PAGE2_TITLE, "Customize");
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
        enterPermissionsUI();
        setPermissions("User", "Project Administrator");
        clickNavButton("Save and Finish");
        impersonate(USER1);
        clickLinkWithText(PROJECT2_NAME);
        assertTextNotPresent("Welcome");
        log("Also check copying permission");
        clickTab("Wiki");
        clickWebpartMenuItem("Pages", "Copy");
        assertTextNotPresent(PROJECT_NAME);
        stopImpersonating();
        clickLinkWithText(PROJECT_NAME);
        enterPermissionsUI();
        clickManageGroup("Users");
        setFormElement("names", USER1);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");

        impersonate(USER1);
        clickLinkWithText(PROJECT2_NAME);
        assertTextPresent(WIKI_PAGE2_TITLE);
        log("Also check copying permission");
        clickTab("Wiki");
        clickWebpartMenuItem("Pages", "Copy");
        assertTextNotPresent(PROJECT_NAME);
        stopImpersonating();
        clickLinkWithText(PROJECT_NAME);
        enterPermissionsUI();
        setPermissions("User", "Project Administrator");
        clickNavButton("Save and Finish");

        log("make sure the changes went through");
        impersonate(USER1);
        clickLinkWithText(PROJECT2_NAME);
        clickTab("Wiki");
        clickWebpartMenuItem("Pages", "Copy");
        assertTextPresent(PROJECT_NAME);
        stopImpersonating();

        log("delete wiki web part");
        clickLinkWithText(PROJECT2_NAME);
        clickTab("Portal");
        clickLinkWithImage(getContextPath() + "/_images/partdelete.png", 0);
        waitForElementToDisappear(Locator.linkWithImage(getContextPath() + "/_images/partdelete.png"), WAIT_FOR_JAVASCRIPT);
        assertLinkNotPresentWithText("Welcome");

        log("test wiki TOC customize link");
        addWebPart("Wiki TOC");
        clickWebpartMenuItem("Pages", "Customize");
        setFormElement("title", "Test Customize TOC");
        log("check that container is set to current project");
        assertOptionEquals("webPartContainer", "/" + PROJECT2_NAME);
        selectOptionByText("webPartContainer", "/" + PROJECT_NAME);
        submit();
        log("check that TOC title is set correctly");
        assertTextPresent("Test Customize TOC");
        assertTextPresent(WIKI_PAGE2_TITLE);

        log("Check that 'Copy Pages' in TOC works");
        clickWebpartMenuItem("Test Customize TOC", "Copy");
        clickLinkWithText(PROJECT_NAME);
        clickNavButton("Copy Pages");
        clickLinkWithText(PROJECT_NAME);
        clickTab("Wiki");
        assertTextPresent(WIKI_PAGE1_TITLE);

        log("Check that 'New Page' works");
        clickLinkWithText(PROJECT2_NAME);
        clickTab("Portal");
        clickWebpartMenuItem("Test Customize TOC", "New");
        convertWikiFormat("HTML");

        setFormElement("name", WIKI_PAGE4_TITLE);
        setWikiBody(WIKI_PAGE4_CONTENT);
        saveWikiPage();
        clickLinkWithText(PROJECT_NAME);
        clickTab("Wiki");
        assertTextPresent(WIKI_PAGE4_TITLE);

        log("test delete");
        clickLinkWithText(WIKI_PAGE2_TITLE);
        clickLinkWithText("Edit");
        deleteWikiPage();
        clickLinkWithText(WIKI_PAGE1_TITLE);
        //add once bug with caching wiki title is fixed
        //assertLinkNotPresentWithText(WIKI_PAGE2_TITLE);

        log("delete project with copied wiki");
        clickLinkWithText("Folders");
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

    private void deleteWikiPage()
    {
        waitForElementToDisappear(Locator.xpath("//a[contains(@class, 'disabled')]/span[text()='Delete Page']"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Delete Page");
        clickNavButton("Delete");
    }

    private void termsOfUseTest()
    {
        log("Create user for terms of use checks");
        clickLinkWithText(PROJECT_NAME);
        enterPermissionsUI();
        clickManageGroup("Users");
        setFormElement("names", USER2);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership");

        log("Test terms of use");
        goToModule("Wiki");
        createNewWikiPage("RADEOX");
        setFormElement("name", "_termsOfUse");
        setFormElement("title", WIKI_TERMS_TITLE);
        setFormElement("body", "The first rule of fight club is do not talk about fight club.");
        saveWikiPage();
        pushLocation(); // For attempting to bypass Terms of Use (1 pop)

        createProject(PROJECT3_NAME); // Public project
        setSiteGroupPermissions("Guests", "Reader");
        setSiteGroupPermissions("All Site Users", "Reader");
        goToModule("Wiki");
        createNewWikiPage("RADEOX");
        setFormElement("name", "_termsOfUse");
        setFormElement("title", WIKI_TERMS_TITLE);
        setFormElement("body", "The second rule of fight club is do not talk about fight club.");
        saveWikiPage();
        pushLocation(); // For attempting to bypass Terms of Use (2 pops)

        createProject(PROJECT4_NAME);
        setSiteGroupPermissions("All Site Users", "Reader");
        goToModule("Wiki");
        createNewWikiPage("RADEOX");
        setFormElement("name", "_termsOfUse");
        setFormElement("title", WIKI_TERMS_TITLE);
        setFormElement("body", "The third rule of fight club is do not talk about fight club.");
        saveWikiPage();
        pushLocation(); // For attempting to bypass Terms of Use (3 pops)
        createSubfolder(PROJECT4_NAME, "subfolder", null);
        pushLocation(); // For attempting to bypass Terms of Use (4 pops)

        log("Terms don't come into play until you log out");
        clickLinkWithText(PROJECT_NAME);
        assertTextNotPresent("fight club");
        signOut();

        log("Access project with guest user");
        clickLinkWithText(PROJECT3_NAME);
        assertTextPresent("fight club");
        checkCheckbox("approvedTermsOfUse");
        clickNavButton("Agree");
        goToHome();
        clickLinkWithText(PROJECT3_NAME);
        assertTextNotPresent("fight club");         

        signIn();
        log("Attempt to bypass terms with saved URLs");
        popLocation();
        assertTextPresent("fight club"); // PROJECT_NAME
        popLocation();
        assertTextPresent("fight club"); // PROJECT3_NAME
        popLocation();
        assertTextPresent("fight club"); // PROJECT4_NAME
        popLocation();
        assertTextPresent("fight club"); // PROJECT4_NAME/subfolder

        goToHome();
        clickLinkWithText(PROJECT_NAME);
        assertTextPresent("fight club");
        log("Submit without agreeing");
        clickNavButton("Agree");

        assertTextPresent("fight club");
        checkCheckbox("approvedTermsOfUse");
        clickNavButton("Agree");

        clickLinkWithText(PROJECT4_NAME);
        assertTextPresent("fight club");
        checkCheckbox("approvedTermsOfUse");
        clickNavButton("Agree");

        log("Check terms with impersonated user");
        impersonate(USER2);

        clickLinkWithText(PROJECT_NAME);
        assertTextPresent("fight club");
        checkCheckbox("approvedTermsOfUse");
        clickNavButton("Agree");
        clickLinkWithText(PROJECT3_NAME);
        assertTextPresent("fight club");
        checkCheckbox("approvedTermsOfUse");
        clickNavButton("Agree");
        clickLinkWithText(PROJECT4_NAME);
        assertTextPresent("fight club");
        checkCheckbox("approvedTermsOfUse");
        clickNavButton("Agree");

        stopImpersonating();            
        clickLinkWithText(PROJECT3_NAME);
        assertTextPresent("fight club");
        checkCheckbox("approvedTermsOfUse");
        clickNavButton("Agree");

        clickLinkWithText(PROJECT_NAME);
        clickTab("Wiki");
        clickLinkWithText("Edit");
        deleteWikiPage();
        assertTextNotPresent(WIKI_TERMS_TITLE);
    }

    private void changeFormat(String format)
    {
        clickNavButton("Convert To...", 0);
        sleep(500);
        selectOptionByValue("wiki-input-window-change-format-to", format);
        clickNavButton("Convert", 0);
        sleep(500);
    }

    protected void selectRenderType(String renderType)
    {
        if (!renderType.equals(getSelectedOptionText("rendererType")))
        {
            selectOptionByText("rendererType", renderType);
        }

        if ("HTML".equals(renderType) && isNavButtonPresent("Use HTML Source Editor"))
            clickNavButton("Use HTML Source Editor");
    }

    protected void doCleanup()
    {
        deleteUser(USER1);
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT2_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT3_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT4_NAME); } catch (Throwable t) {}
    }
}
