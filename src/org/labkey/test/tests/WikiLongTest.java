/*
 * Copyright (c) 2008-2017 LabKey Corporation
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

package org.labkey.test.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Wiki;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@Category({DailyA.class, Wiki.class})
public class WikiLongTest extends BaseWebDriverTest
{
    PortalHelper portalHelper = new PortalHelper(this);

    private static final String PROJECT_NAME = "WikiVerifyProject";
    private static final String PROJECT2_NAME = "WikiCopied";
    private static final String WIKI_PAGE1_TITLE = "Page 1 Wiki Title";
    private static final String WIKI_PAGE1_NAME= "Page 1 Wiki Name";
    private static final String WIKI_PAGE2_NAME = "Page 2 Wiki Name";
    private static final String WIKI_PAGE2_TITLE = "Page 2 Wiki Title";
    private static final String WIKI_PAGE3_ALTTITLE = "PageBBB has HTML";
    private static final String WIKI_PAGE3_NAME_TITLE = "Page 3 Wiki";
    private static final String WIKI_PAGE4_TITLE = "New Wiki";
    private static final String WIKI_PAGE5_NAME = "Malformed";
    private static final String WIKI_PAGE5_TITLE = "Malformed JavaScript Elements Should Work";
    private static final String WIKI_PAGE6_NAME = "Index";
    private static final String WIKI_PAGE6_TITLE = "Indexed Wiki Page Test";
    private static final String WIKI_PAGE7_TITLE = "Page 7 Title For Markdown Test";
    private static final String WIKI_PAGE7_NAME= "Page 7 Name For Markdown Test";
    private static final String WIKI_PAGE8_TITLE = "Page 8 Title For Delete Subtree Test";
    private static final String WIKI_PAGE8_NAME= "Page 8 Name For Delete Subtree Test";

    private static final String WIKI_PAGE1_TITLE_LINK = "/labkey/wiki/WikiCopied/page.view?name=Page%201%20Wiki%20Name";
    private static final String WIKI_PAGE1_TITLE_LINK_COPY = "/labkey/wiki/WikiCopied/page.view?name=Page%201%20Wiki%20Name1";

    private static final String DISC1_TITLE = "Let's Talk";
    private static final String DISC1_BODY = "I don't know how normal this wiki is";
    private static final String RESP1_TITLE = "Let's Keep Talking";
    private static final String RESP1_BODY = "I disagree";
    private static final String USER1 = "wikilong_user1@wikilong.test";
    private static final String USER2 = "wikilong_user2@wikilong.test";
    private static final String USERS_GROUP = "Users";
    private static final String WIKI_PAGE3_WEBPART_TEST = "Best Gene Name";
    private static final String WIKI_NAVTREE_TITLE = "NavTree";
    private static final String WIKI_TERMS_TITLE = "Terms of Use";
    private static final String WIKI_SEARCH_TERM = "okapi";
    private static final String WIKI_INDEX_EDIT_CHECKBOX = "wiki-input-shouldIndex";
    private static final String WIKI_INDEX_MANAGE_CHECKBOX = "shouldIndex";
    private static final String WIKI_DELETE_SUBTREE_CHECKBOX = "isDeletingSubtree";
    private final PortalHelper _portalHelper = new PortalHelper(this);
    private WikiHelper _wikiHelper = new WikiHelper(this);

    private static final String WIKI_PAGE1_CONTENT =
            "1 Title\n" +
            "1.1 Subtitle\n" +
            "normal normal normal\n" +
            "\n" +
            "new paragraph\\\\\n" +
            "[" + WIKI_PAGE2_NAME + "]";

    private static final String WIKI_PAGE2_CONTENT =
            "1 Page AAA\n" +
            "[Welcome to the WikiLongTest|" + WIKI_PAGE1_NAME + "]";

    private static final String WIKI_PAGE3_CONTENT =
            "<b>Some HTML content</b>\n" +
                    "<b>${labkey.webPart(partName='Query', title='My Proteins', schemaName='ms2', " +
                    "queryName='Sequences', allowChooseQuery='true', allowChooseView='true')}</b>\n";
    private static final String WIKI_PAGE3_CONTENT_NO_QUERY =
            "<b>Some HTML content</b>\n" +
                    "<b>No query part</b>\n";

    private static final String WIKI_PAGE4_CONTENT =
            "This is wiki page <i>4</i><br/>${labkey.webPart(partName='Wiki TOC')}";

    private static final String WIKI_PAGE5_CONTENT =
            "    <script>\n" +
            "        var foo = \"<form>Test</form>\";\n" +
            "    </script>";

    private static final String WIKI_PAGE6_CONTENT =
            "The " + WIKI_SEARCH_TERM +
            " was called the African unicorn by Europeans and wasn't widely known to exist until 1901.\n";

    private static final String WIKI_PAGE7_CONTENT =
            "# Title MD\n" +
            "## Subtitle MD\n" +
            "*italic text MD*\n";

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

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("wiki");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testSteps()
    {
        enableEmailRecorder();
        _containerHelper.createProject(PROJECT2_NAME, null);
        _containerHelper.enableModule(PROJECT2_NAME, "MS2");
        _securityHelper.setProjectPerm(USERS_GROUP, "Editor");
        clickButton("Save and Finish");
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.enableModule(PROJECT_NAME, "MS2");
        _permissionsHelper.createPermissionsGroup("testers");
        _securityHelper.setProjectPerm("testers", "Editor");
        _securityHelper.setProjectPerm(USERS_GROUP, "Editor");
        clickButton("Save and Finish");
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.checkboxByTitle("Wiki"));
        submit();

        goToAdminConsole().clickFullTextSearch();
        if (isTextPresent("pause crawler"))
            clickButton("pause crawler");
        beginAt(getDriver().getCurrentUrl().replace("admin.view", "waitForIdle.view"), 10 * defaultWaitForPage);

        clickProject(PROJECT2_NAME);
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.checkboxByTitle("Wiki"));
        submit();

        clickProject(PROJECT_NAME);
        _portalHelper.addWebPart("Wiki");
        _portalHelper.addWebPart("Search");

        log("Test new wiki page");
        _wikiHelper.createNewWikiPage("RADEOX");

        setFormElement(Locator.name("name"), WIKI_PAGE1_NAME);
        setFormElement(Locator.name("title"), WIKI_PAGE1_TITLE);
        setFormElement(Locator.name("body"), WIKI_PAGE1_CONTENT);
        _wikiHelper.saveWikiPage();

        searchFor(PROJECT_NAME, "normal normal normal", 1, WIKI_PAGE1_TITLE);

        log("Test add content to link page");
        WebElement wikiLink = Locator.linkWithText(WIKI_PAGE2_NAME).findElement(getDriver());
        assertEquals("Link to other wiki has bad href", WebTestHelper.buildURL("wiki", getProjectName(), "page", Maps.of("name", WIKI_PAGE2_NAME.replace(" ", "%20"))), wikiLink.getAttribute("href"));
        clickAndWait(wikiLink);
        assertTextPresent("page has no content");
        clickAndWait(Locator.linkWithText("add content"));
        _wikiHelper.convertWikiFormat("RADEOX");

        setFormElement(Locator.name("title"), WIKI_PAGE2_TITLE);
        setFormElement(Locator.name("body"), WIKI_PAGE2_CONTENT);
        _wikiHelper.saveWikiPage();

        clickAndWait(Locator.linkWithText("Welcome to the WikiLongTest"));
        assertElementNotPresent(Locator.linkWithText(WIKI_PAGE2_NAME));

        searchFor(PROJECT_NAME, "\"Page AAA\"", 1, WIKI_PAGE2_TITLE);

        log("Test new wiki page using markdown");
        _wikiHelper.createNewWikiPage("MARKDOWN");
        setFormElement(Locator.name("name"), WIKI_PAGE7_NAME);
        setFormElement(Locator.name("title"), WIKI_PAGE7_TITLE);
        setFormElement(Locator.name("body"), WIKI_PAGE7_CONTENT);
        _wikiHelper.saveWikiPage();
        // verify that after saving the markdown that it is rendered as html that does not include the markdown symbols
        assertTextPresent("Title MD");
        assertTextNotPresent("# Title MD");
        clickAndWait(Locator.linkWithText("Edit"));
        _wikiHelper.convertWikiFormat("HTML");
        // verify that after converting the markdown to html that it is rendered as html that does not include the markdown symbols
        _wikiHelper.saveWikiPage();
        assertTextPresent("Title MD");
        assertTextNotPresent("# Title MD");
        searchFor(PROJECT_NAME, "italic text MD", 1, WIKI_PAGE7_TITLE);

        log("test html wiki containing malformed javascript entities... we should allow this, see #12268");
        _wikiHelper.createNewWikiPage();
        setFormElement(Locator.name("name"), WIKI_PAGE5_NAME);
        setFormElement(Locator.name("title"), WIKI_PAGE5_TITLE);
        _wikiHelper.setWikiBody(WIKI_PAGE5_CONTENT);
        _wikiHelper.saveWikiPage();
        assertTextNotPresent("New Page");  // Should not be an error, so should have left the editor

        log("test create new html page with a webpart");
        _wikiHelper.createNewWikiPage("HTML");

        setFormElement(Locator.name("name"), WIKI_PAGE3_NAME_TITLE);
        setFormElement(Locator.name("title"), WIKI_PAGE3_NAME_TITLE);
        selectOptionByText(Locator.name("parent"), WIKI_PAGE2_TITLE + " (" + WIKI_PAGE2_NAME + ")");
        _wikiHelper.setWikiBody(WIKI_PAGE3_CONTENT);
        log("test attachments in wiki");
        click(Locator.linkWithText("Attach a file"));

        File file = _wikiHelper.getSampleFile();
        setFormElement(Locator.name("formFiles[0]"), file);
        _wikiHelper.saveWikiPage();
        waitForElement(Locator.linkContainingText(file.getName()));
        assertTextPresent(WIKI_PAGE3_WEBPART_TEST,
                "Some HTML content");

        searchFor(PROJECT_NAME, "Wiki", 3, WIKI_PAGE3_NAME_TITLE);

        log("test edit");
        clickAndWait(Locator.linkWithText("Edit"));
        setFormElement(Locator.name("title"), WIKI_PAGE3_ALTTITLE);
        String wikiPage3ContentEdited =
                "<b>Some HTML content</b><br>\n" +
                "<b>More HTML content</b><br>\n" +
                "<a href='" + WebTestHelper.getContextPath() + "/wiki/" + PROJECT_NAME + "/page.view?name=PageAAA'>Page AAA</a><br>\n";
        _wikiHelper.setWikiBody(wikiPage3ContentEdited);
        _wikiHelper.saveWikiPage();

        assertTextPresent("More HTML content");
        clickAndWait(Locator.linkWithText("Edit"));
        _wikiHelper.setWikiBody(WIKI_PAGE3_CONTENT_NO_QUERY);
        setFormElement(Locator.name("title"), WIKI_PAGE3_NAME_TITLE);
        _wikiHelper.saveWikiPage();

        log("test change renderer type");
        assertTextPresent("Some HTML content");
        clickAndWait(Locator.linkWithText("Edit"));
        _wikiHelper.convertWikiFormat("TEXT_WITH_LINKS");
        _wikiHelper.saveWikiPage();

        assertTextPresent("<b>");
        clickAndWait(Locator.linkWithText("Edit"));
        _wikiHelper.convertWikiFormat("HTML");
        _wikiHelper.saveWikiPage();

        log("Check Start Page series works");
        searchFor(PROJECT_NAME, "Some HTML", 1, WIKI_PAGE3_NAME_TITLE);
        assertElementPresent(Locator.linkWithText(WIKI_PAGE2_TITLE), 2);

        log("Check Pages menu works");
        clickAndWait(Locator.linkWithText(WIKI_PAGE2_TITLE));
        clickAndWait(Locator.linkWithText("next"));
        assertTextPresent("Some HTML content");
        clickAndWait(Locator.linkWithText("previous"));
        assertTextPresent("Welcome to the WikiLongTest");

        log("Check sibling order edit works");
        clickAndWait(Locator.linkWithText(WIKI_PAGE1_TITLE));
        clickAndWait(Locator.linkWithText("Manage"));
        clickButton("Move Down", 0);
        clickButton("Save");
        clickAndWait(Locator.linkWithText(WIKI_PAGE2_TITLE));
        clickAndWait(Locator.linkWithText("next"));
        assertElementPresent(Locator.css(".labkey-wiki").containing("Some HTML content"));
        clickAndWait(Locator.linkWithText("next"));
        assertElementPresent(Locator.css(".labkey-wiki").containing("normal normal normal"));
        clickAndWait(Locator.linkWithText("Manage"));
        clickButton("Move Up", 0);
        clickButton("Save");
        clickAndWait(Locator.linkWithText("next"));
        assertTextPresent("Page AAA");

        log("Check parent reset works");
        click(Locator.navTreeExpander(WIKI_PAGE2_TITLE));
        clickAndWait(Locator.linkWithText(WIKI_PAGE3_NAME_TITLE));
        clickAndWait(Locator.linkWithText("Manage"));
        doAndWaitForPageToLoad(() -> selectOptionByText(Locator.name("parent"), WIKI_PAGE1_TITLE + " (" + WIKI_PAGE1_NAME + ")"));
        clickButton("Save");
        clickAndWait(Locator.linkWithText(WIKI_PAGE1_TITLE));
        clickAndWait(Locator.linkWithText("next"));
        assertTextPresent("Some HTML content");

        log("Check that discussion board works");
        clickAndWait(Locator.linkWithText(WIKI_PAGE1_TITLE));
        _ext4Helper.waitForOnReady();
        click(Locator.linkWithText("discussions"));
        waitForElement(Locator.linkWithText("Start new discussion"), defaultWaitForPage);
        clickAndWait(Locator.linkWithText("Start new discussion"));
        setFormElement(Locator.name("title"), DISC1_TITLE);
        setFormElement(Locator.id("body"), DISC1_BODY);
        submit();
        _ext4Helper.waitForOnReady();
        clickMenuButton(true, Locator.linkWithText("discussions")
                .findElement(getDriver()), false, DISC1_TITLE);

        assertTextPresent(DISC1_TITLE,
                DISC1_BODY);

        log("Check response on discussion board works");
        clickButton("Respond");
        setFormElement(Locator.name("title"), RESP1_TITLE);
        setFormElement(Locator.id("body"), RESP1_BODY);
        submit();
        assertTextPresent(RESP1_TITLE,
                RESP1_BODY);
        clickButton("Delete Message");
        clickButton("Delete");
        assertTextNotPresent(DISC1_TITLE, DISC1_BODY);

        log("test navTree and header");
        _wikiHelper.createNewWikiPage("RADEOX");
        setFormElement(Locator.name("name"), "_navTree");
        setFormElement(Locator.name("title"), WIKI_NAVTREE_TITLE);
        _wikiHelper.setWikiBody(NAVBAR1_CONTENT);
        _wikiHelper.saveWikiPage();

        assertElementNotPresent(Locator.linkWithText("Home"));
        assertElementPresent(Locator.linkWithText(PROJECT_NAME));

        clickAndWait(Locator.linkWithText("Edit"));
        _wikiHelper.setWikiBody(NAVBAR2_CONTENT);
        _wikiHelper.saveWikiPage();
        assertElementPresent(Locator.linkWithText("Projects"));
        assertElementPresent(Locator.linkWithText("Manage Project"));
        assertElementPresent(Locator.linkWithText("Manage Site"));

        //test deleting via edit page
        clickAndWait(Locator.linkWithText("Edit"));
        deleteWikiPage(false);
        assertElementNotPresent(Locator.linkWithText(WIKI_NAVTREE_TITLE));

        _wikiHelper.createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), "_header");
        setFormElement(Locator.name("title"), "Header");
        _wikiHelper.setWikiBody(HEADER_CONTENT);
        _wikiHelper.saveWikiPage();

        clickAndWait(Locator.linkWithText("Header"));
        assertTextPresent(HEADER_CONTENT);
        clickAndWait(Locator.linkWithText("Edit").index(0));
        deleteWikiPage(false);
        assertTextNotPresent(HEADER_CONTENT);

        log("Return to where we were...");
        click(Locator.navTreeExpander(WIKI_PAGE1_TITLE));
        clickAndWait(Locator.linkWithText(WIKI_PAGE3_NAME_TITLE));

        log("test versions");
        clickAndWait(Locator.linkWithText("History"));
        clickAndWait(Locator.linkWithText("2"));
        clickButton("Make Current");
        assertTextPresent("6");
        clickAndWait(Locator.linkWithText(WIKI_PAGE1_TITLE));
        clickAndWait(Locator.linkWithText("next"));
        assertTextPresent("More HTML content",
                WIKI_PAGE3_ALTTITLE);


        log("test copy wiki");
        portalHelper.clickWebpartMenuItem("Pages", true, "Copy");
        clickAndWait(Locator.linkWithText(PROJECT2_NAME));
        clickButton("Copy Pages");

        log("test wiki customize link");
        clickTab("Portal");
        _portalHelper.addWebPart("Wiki");
        portalHelper.clickWebpartMenuItem("Wiki", true, "Customize");
        log("check that container is set to current project");
        selectOptionByText(Locator.name("webPartContainer"), "/" + PROJECT_NAME);
        click(Locator.linkWithText("restore to this folder's default page."));
        assertOptionEquals(Locator.name("webPartContainer"), "/" + PROJECT2_NAME);
        log("set container and page");

        //page names are fetched via AJAX, so wait for them to be populated
        waitForText(WIKI_PAGE2_NAME + " (" + WIKI_PAGE2_TITLE + ")");

        selectOptionByText(Locator.name("name"), WIKI_PAGE2_NAME + " (" + WIKI_PAGE2_TITLE + ")");
        sleep(500);
        clickButton("Submit");
        clickAndWait(Locator.linkWithText("Welcome to the WikiLongTest"));
        log("make sure it all got copied");
        click(Locator.navTreeExpander(WIKI_PAGE1_TITLE));
        clickAndWait(Locator.linkWithText(WIKI_PAGE3_ALTTITLE));
        clickAndWait(Locator.linkWithText(WIKI_PAGE1_TITLE));

        log("Check Permissions");
        log("Create fake user for permissions check");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.clickManageGroup(USERS_GROUP);
        setFormElement(Locator.name("names"), USER1);
        uncheckCheckbox(Locator.name("sendEmail"));
        clickButton("Update Group Membership");

        log("Check if permissions work");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setPermissions(USERS_GROUP, "Reader");
        clickButton("Save and Finish");
        impersonate(USER1);
        clickProject(PROJECT2_NAME);
        pushLocation();
        assertTextPresent(WIKI_PAGE2_TITLE);
        clickTab("Wiki");
        assertTextNotPresent("copy pages");
        stopImpersonating();
        clickProject(PROJECT2_NAME);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.removePermission(USERS_GROUP, "Editor");
        clickButton("Save and Finish");
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.removePermission(USERS_GROUP, "Reader");
        clickButton("Save and Finish");
        impersonate(USER1);
        assertElementNotPresent(Locator.linkWithText(PROJECT2_NAME));     // Project should not be visible
        popLocation();
        assertTextPresent("User does not have permission to perform this operation");  // Not authorized
        goToHome();
        stopImpersonating();

        log("Check if readers can read from other projects");
        clickProject(PROJECT2_NAME);
        portalHelper.clickWebpartMenuItem(WIKI_PAGE2_TITLE, true, "Customize");
        selectOptionByText(Locator.name("webPartContainer"), "/" + PROJECT_NAME);

        //page names are now fetched via AJAX, so wait for them to be populated
        waitForText(WIKI_PAGE2_NAME + " (" + WIKI_PAGE2_TITLE + ")");

        selectOptionByText(Locator.name("name"), WIKI_PAGE2_NAME + " (" + WIKI_PAGE2_TITLE + ")");
        submit();
        assertTextPresent(WIKI_PAGE2_TITLE);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setPermissions(USERS_GROUP, "Project Administrator");
        clickButton("Save and Finish");
        impersonate(USER1);
        clickProject(PROJECT2_NAME);
        assertTextNotPresent("Welcome to the WikiLongTest");
        log("Also check copying permission");
        clickTab("Wiki");
        portalHelper.clickWebpartMenuItem("Pages", true, "Copy");
        assertTextNotPresent(PROJECT_NAME);
        stopImpersonating();
        clickProject(PROJECT_NAME);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.clickManageGroup(USERS_GROUP);
        setFormElement(Locator.name("names"), USER1);
        uncheckCheckbox(Locator.checkboxByName("sendEmail"));
        clickButton("Update Group Membership");

        impersonate(USER1);
        clickProject(PROJECT2_NAME);
        assertTextPresent(WIKI_PAGE2_TITLE);
        log("Also check copying permission");
        clickTab("Wiki");
        portalHelper.clickWebpartMenuItem("Pages", true, "Copy");
        assertElementNotPresent(Locator.linkWithText(PROJECT_NAME));
        stopImpersonating();
        clickProject(PROJECT_NAME);
        _permissionsHelper.enterPermissionsUI();
        _permissionsHelper.setPermissions(USERS_GROUP, "Project Administrator");
        clickButton("Save and Finish");

        log("make sure the changes went through");
        impersonate(USER1);
        clickProject(PROJECT2_NAME);
        clickTab("Wiki");
        portalHelper.clickWebpartMenuItem("Pages", true, "Copy");
        assertElementPresent(Locator.linkWithText(PROJECT_NAME));
        stopImpersonating();

        log("delete wiki web part");
        clickProject(PROJECT2_NAME);
        clickTab("Portal");
        portalHelper.removeWebPart(WIKI_PAGE2_TITLE);

        log("test wiki TOC customize link");
        _portalHelper.addWebPart("Wiki Table of Contents");
        portalHelper.clickWebpartMenuItem("Pages", true, "Customize");
        setFormElement(Locator.name("title"), "Test Customize TOC");
        log("check that container is set to current project");
        assertOptionEquals(Locator.name("webPartContainer"), "/" + PROJECT2_NAME);
        selectOptionByText(Locator.name("webPartContainer"), "/" + PROJECT_NAME);
        submit();
        log("check that TOC title is set correctly");
        assertTextPresent("Test Customize TOC",
                WIKI_PAGE2_TITLE);

        log("Check that 'Copy Pages' in TOC works");
        portalHelper.clickWebpartMenuItem("Test Customize TOC", true, "Copy");
        clickAndWait(Locator.linkWithText(PROJECT2_NAME));
        clickButton("Copy Pages");
        assertElementPresent(Locator.linkWithText(WIKI_PAGE1_TITLE), 2);

        log("Check that 'New Page' works");
        clickProject(PROJECT2_NAME);
        clickTab("Portal");
        portalHelper.clickWebpartMenuItem("Test Customize TOC", true, "New");
        _wikiHelper.convertWikiFormat("HTML");

        setFormElement(Locator.name("name"), WIKI_PAGE4_TITLE);
        _wikiHelper.setWikiBody(WIKI_PAGE4_CONTENT);
        _wikiHelper.saveWikiPage();
        clickProject(PROJECT_NAME);
        clickTab("Wiki");
        assertTextPresent(WIKI_PAGE4_TITLE);

        log("test delete");
        clickAndWait(Locator.linkWithText(WIKI_PAGE2_TITLE));
        clickAndWait(Locator.linkWithText("Edit"));
        deleteWikiPage(false);
        clickAndWait(Locator.linkWithText(WIKI_PAGE1_TITLE));
        assertElementPresent(Locator.linkWithText(WIKI_PAGE2_NAME), 1);

        /* TODO: re-enable when this blocking issue is resolved:
         https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=31184
        indexTest();
        */

        log("test delete subtree");
        // create child first
        _wikiHelper.createNewWikiPage();
        setFormElement(Locator.name("name"), WIKI_PAGE8_NAME);
        setFormElement(Locator.name("title"), WIKI_PAGE8_TITLE);
        selectOptionByText(Locator.name("parent"), "  " + WIKI_PAGE3_ALTTITLE + " (" + WIKI_PAGE3_NAME_TITLE + ")");
        _wikiHelper.saveWikiPage();

        clickAndWait(Locator.linkWithText(WIKI_PAGE3_ALTTITLE));
        clickAndWait(Locator.linkWithText("Edit"));
        deleteWikiPage(true);
        clickAndWait(Locator.linkWithText(WIKI_PAGE1_TITLE));
        assertElementPresent(Locator.linkWithText(WIKI_PAGE1_TITLE), 1);
        assertElementNotPresent(Locator.linkWithText(WIKI_PAGE3_ALTTITLE));
        assertElementNotPresent(Locator.linkWithText(WIKI_PAGE8_TITLE));

        //extended wiki test -- generate 2000 pages
//        clickProject(PROJECT_NAME);
//        clickTab("Wiki");
//        for (int i = 0; i <= 1999; i++)
//        {
//            //test create new html page
//            clickAndWait(Locator.linkWithText("new page"));
//            setFormElement(Locator.name("name"), "Page" + Integer.toString(i));
//            setFormElement(Locator.name("title"), "Page" + Integer.toString(i));
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

    private void deleteWikiPage(boolean isDeletingSubtree)
    {
        waitForElementToDisappear(Locator.xpath("//a[contains(@class, 'disabled')]/span[text()='Delete Page']"), WAIT_FOR_JAVASCRIPT);
        clickButton("Delete Page");
        if(isDeletingSubtree)
            checkCheckbox(Locator.id(WIKI_DELETE_SUBTREE_CHECKBOX));
        clickButton("Delete");
    }

    //
    // Verify a wiki page can be indexed and unindexed via create, edit, and manage
    // functionality.
    //
    private void indexTest()
    {
        //
        // verify that the default option for a new page
        // is indexed and content on page can be found
        //
        log("test index option: default is indexed");
        createIndexWiki(true);
        searchFor(PROJECT_NAME, WIKI_SEARCH_TERM, 1, null);
        deleteIndexWiki();

        //
        // create a new page without the index option and verify
        // content cannot be found
        //
        log("test index option:  create new page unindexed");
        createIndexWiki(false);
        searchFor(PROJECT_NAME, WIKI_SEARCH_TERM, 0, null);

        //
        // Edit an existing page that was unindexed and turn on the index
        //
        log("test index option:  edit and turn on indexing for a page that was created unindexed");
        editIndexWiki(false);
        checkCheckbox(Locator.id(WIKI_INDEX_EDIT_CHECKBOX));
        _wikiHelper.saveWikiPage();
        searchFor(PROJECT_NAME, WIKI_SEARCH_TERM, 1, null);

        //
        // Edit an existing page that was indexed and turn off the index
        //
        log("test index option:  edit and turn off indexing for a page that was created indexed");
        editIndexWiki(true);
        uncheckCheckbox(Locator.id(WIKI_INDEX_EDIT_CHECKBOX));
        _wikiHelper.saveWikiPage();
        searchFor(PROJECT_NAME, WIKI_SEARCH_TERM, 0, null);

        //
        // manage an existing page: turn on indexing
        //
        log("test index option:  manage and turn on indexing for a page that is unindexed");
        // note that this also tests that the index option persisted from the edit page is loaded correctly
        // into the manage page
        manageIndexWiki(false);
        checkCheckbox(Locator.id(WIKI_INDEX_MANAGE_CHECKBOX));
        clickButton("Save");
        searchFor(PROJECT_NAME, WIKI_SEARCH_TERM, 1, null);

        //
        // manage an existing page: turn off indexing
        //
        log("test index option:  manage and turn off indexing for a page that is indexed");
        manageIndexWiki(true);
        uncheckCheckbox(Locator.id(WIKI_INDEX_MANAGE_CHECKBOX));
        clickButton("Save");
        searchFor(PROJECT_NAME, WIKI_SEARCH_TERM, 0, null);

        //
        // final sanity check: make sure the shouldIndex setting persists correctly and is correct
        // in the edit page
        //
        log("test index option:  edit and turn off indexing for a page that was created indexed");
        editIndexWiki(false);
        clickButton("Cancel");

        //
        // cleanup
        //
        deleteIndexWiki();
    }

    //
    // helper functions used by indexTest() above
    //
    private void createIndexWiki(boolean shouldIndex)
    {
        _wikiHelper.createNewWikiPage("RADEOX");
        //
        // verify that the index option is checked by default
        //
        assertChecked(Locator.id(WIKI_INDEX_EDIT_CHECKBOX));

        setFormElement(Locator.name("name"), WIKI_PAGE6_NAME);
        setFormElement(Locator.name("title"), WIKI_PAGE6_TITLE);
        _wikiHelper.setWikiBody(WIKI_PAGE6_CONTENT);

        if (!shouldIndex)
        {
            uncheckCheckbox(Locator.id(WIKI_INDEX_EDIT_CHECKBOX));
        }

        _wikiHelper.saveWikiPage();
    }

    private void editIndexWiki(boolean expectedShouldIndex)
    {
        clickTab("Wiki");
        clickAndWait(Locator.linkWithText(WIKI_PAGE6_TITLE));
        clickAndWait(Locator.linkWithText("Edit"));

        if (expectedShouldIndex)
        {
            assertChecked(Locator.id(WIKI_INDEX_EDIT_CHECKBOX));
        }
        else
        {
            assertNotChecked(Locator.id(WIKI_INDEX_EDIT_CHECKBOX));
        }
    }

    private void manageIndexWiki(boolean expectedShouldIndex)
    {
        clickTab("Wiki");
        clickAndWait(Locator.linkWithText(WIKI_PAGE6_TITLE));
        clickAndWait(Locator.linkWithText("Manage"));

        if (expectedShouldIndex)
        {
            assertChecked(Locator.id(WIKI_INDEX_MANAGE_CHECKBOX));
        }
        else
        {
            assertNotChecked(Locator.id(WIKI_INDEX_MANAGE_CHECKBOX));
        }
    }

    private void deleteIndexWiki()
    {
        clickAndWait(Locator.linkWithText(WIKI_PAGE6_TITLE));
        clickAndWait(Locator.linkWithText("Edit"));
        deleteWikiPage(false);
    }

    protected void selectRenderType(String renderType)
    {
        if (!renderType.equals(getSelectedOptionText(Locator.name("rendererType"))))
        {
            selectOptionByText(Locator.name("rendererType"), renderType);
        }

        if ("HTML".equals(renderType) && isButtonPresent("Use HTML Source Editor"))
            clickButton("Use HTML Source Editor");
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteUsersIfPresent(USER1);
        _containerHelper.deleteProject(PROJECT2_NAME, afterTest);
        _containerHelper.deleteProject(PROJECT_NAME, afterTest);
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
