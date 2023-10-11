/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

package org.labkey.test.tests.wiki;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Wiki;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.pages.wiki.EditPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.search.SearchAdminAPIHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({Daily.class, Wiki.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class WikiTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = TRICKY_CHARACTERS_FOR_PROJECT_NAMES + "WikiVerifyProject";
    private static final String WIKI_PAGE_ALTTITLE = "PageBBB has HTML";
    private static final String WIKI_PAGE_WEBPART_ID = "qwp999";
    private static final String WIKI_PAGE_TITLE = "_Test Wiki " + BaseWebDriverTest.INJECT_CHARS_1;
    private static final String WIKI_PAGE_NAME = "_Test Wiki Name " + BaseWebDriverTest.INJECT_CHARS_2;
    private static final String WIKI_PAGE_CONTENT =
            "<b>Some HTML content</b>\n" +
                    "<b>${labkey.webPart(partName='Query', title='My Users', schemaName='core', " +
                    "queryName='Users', allowChooseQuery='true', allowChooseView='true', dataRegionName='" + WIKI_PAGE_WEBPART_ID + "')}</b>\n";
    private static final String WIKI_CHECK_CONTENT = "More HTML content";
    private static int numberOfWikiCreated = 0;

    @BeforeClass
    public static void setupProject()
    {
        WikiTest init = (WikiTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.enableModules(Arrays.asList("Wiki"));

        SearchAdminAPIHelper.pauseCrawler(getDriver());

        goToProjectHome();
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addBodyWebPart("Wiki");
        portalHelper.addBodyWebPart("Search");
        portalHelper.addSideWebPart("Wiki Table of Contents");

    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("wiki");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected String getSubfolderName()
    {
        return "Subfolder";
    }

    @Test
    public void testSteps()
    {
        log("test create new html page with a webpart");
        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createNewWikiPage("HTML");
        numberOfWikiCreated++;

        setFormElement(Locator.name("name"), WIKI_PAGE_TITLE);
        setFormElement(Locator.name("title"), WIKI_PAGE_TITLE);
        wikiHelper.setWikiBody(WIKI_PAGE_CONTENT);

        log("test attachments in wiki");
        click(Locator.linkWithText("Attach a file"));
        File file = wikiHelper.getSampleFile();
        setFormElement(Locator.name("formFiles[00]"), file);
        wikiHelper.saveWikiPage();

        DataRegionTable.DataRegion(getDriver()).withName(WIKI_PAGE_WEBPART_ID).waitFor();
        assertTextPresent(file.getName(), "Some HTML content");
        final Locator.XPathLocator wikiTitleLink = Locator.linkContainingText("_Test Wiki").withAttribute("href");
        assertElementPresent(wikiTitleLink);
        impersonateRole("Reader");
        assertElementNotPresent(wikiTitleLink);
        stopImpersonating();

        log("test search wiki");
        searchFor(PROJECT_NAME, "Wiki", numberOfWikiCreated, WIKI_PAGE_TITLE);

        log("test edit wiki");
        clickAndWait(Locator.linkWithText("Edit"));
        setFormElement(Locator.name("title"), WIKI_PAGE_ALTTITLE);
        String wikiPageContentEdited =
                "<b>Some HTML content</b><br>\n" +
                        "<b>" + WIKI_CHECK_CONTENT + "</b><br>\n";
        wikiHelper.setWikiBody(wikiPageContentEdited);
        wikiHelper.switchWikiToVisualView();
        wikiHelper.saveWikiPage();
        verifyWikiPagePresent();

        doTestInlineEditor();

        log("Verify fix for issue 13937: NotFoundException when attempting to display a wiki from a different folder which has been deleted");
        _containerHelper.createSubfolder(getProjectName(), getSubfolderName(), new String[]{});
        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.addWebPart("Wiki");
        portalHelper.clickWebpartMenuItem("Wiki", "Customize");
        selectOptionByText(Locator.name("webPartContainer"), "/" + getProjectName());
        selectOptionByTextContaining(Locator.name("name").findElement(getDriver()), WIKI_PAGE_ALTTITLE);
        clickButton("Submit");
        verifyWikiPagePresent();

        log("test delete wiki");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(WIKI_PAGE_ALTTITLE));
        clickAndWait(Locator.linkWithText("Edit"));
        clickButton("Delete Page");
        clickButton("Delete");
        assertTextNotPresent(WIKI_PAGE_ALTTITLE);

        log("verify second wiki part pointing to first handled delete well");
        clickFolder(getSubfolderName());
        assertTextNotPresent(WIKI_PAGE_ALTTITLE);
    }

    @Test
    public void testEmbeddedVideoInWiki()
    {
        String wikiName = "Wiki with video";
        String wikiTitle = "Sample finder video";
        String wikiContent = "Some random content start : Have fun watching video below\n" +
                "{video:https://www.youtube.com/embed/JEE4807UHN4|height:350|width:500}\n" +
                "Hope you fun watching the video..!\n";

        goToProjectHome();
        log("Creating the wiki with video");
        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createNewWikiPage("RADEOX");
        numberOfWikiCreated++;
        setFormElement(Locator.name("name"), wikiName);
        setFormElement(Locator.name("title"), wikiTitle);
        wikiHelper.setWikiBody(wikiContent);
        wikiHelper.saveWikiPage();

        Assert.assertEquals("Video is missing", "https://www.youtube.com/embed/JEE4807UHN4",
                getAttribute(Locator.tag("iframe"), "src"));
    }

    @Test
    public void testShowPageTreeForWiki()
    {
        goToProjectHome();
        log("Creating the wiki");
        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createNewWikiPage("HTML");
        numberOfWikiCreated++;
        setFormElement(Locator.name("name"), WIKI_PAGE_NAME);
        setFormElement(Locator.name("title"), WIKI_PAGE_TITLE);
        wikiHelper.setWikiBody(WIKI_CHECK_CONTENT);
        wikiHelper.saveWikiPage();

        log("Verifying the tree is displayed correctly");
        goToProjectHome();
        EditPage editWikiPage = wikiHelper.editWikiPage();
        editWikiPage.clickShowPageTree();
        assertElementPresent(Locator.id("wiki-toc-tree").append(Locator.linkContainingText(WIKI_PAGE_TITLE + " (" + WIKI_PAGE_NAME + ")")));
    }

    /*
        Regression coverage for
        https://www.labkey.org/home/Developer/issues/Secure/issues-details.view?issueId=48019

     */
    @Test
    public void testWikiWithComma()
    {
        String wikiName = "Wiki with comma's";
        String wikiTitle = "Comma in the content";
        String wikiContent = "This is my HTML, with commas";

        goToProjectHome();
        log("Creating the wiki " + wikiTitle);
        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), wikiName);
        setFormElement(Locator.name("title"), wikiTitle);
        wikiHelper.setWikiBody("<p>" + wikiContent + "</p>");
        wikiHelper.saveWikiPage();
        numberOfWikiCreated++;


        searchFor(PROJECT_NAME, "commas", 1, wikiTitle);
        Assert.assertEquals("Incorrect result with comma", Arrays.asList(wikiTitle + "\n/" + getProjectName() + "\n" + wikiContent), getTexts(new SearchResultsPage(getDriver()).getResults()));
    }

    protected void verifyWikiPagePresent()
    {
        waitForText(WIKI_CHECK_CONTENT);
        assertTextPresent(WIKI_PAGE_ALTTITLE);
    }

    protected void doTestInlineEditor()
    {
        Locator.XPathLocator inlineEditor = Locator.xpath("//div[@class='labkey-inline-editor']");

        log("** test inline wiki webpart editor");
        goToProjectHome();

        // tinyMCE does not decode URLs properly, re-navigate to this page letting the server re-write the URL.
        // This can be removed once tinyMCE is upgraded to 4.x+ (3.x as of this writing)
        clickTab("Portal");
        click(Locator.tagWithAttribute("span", "title", "Edit Inline"));
        waitForElement(inlineEditor);

        String addedContent = "Inline edited content";
        setInlineEditorContent(getAttribute(inlineEditor.child("textarea"), "id"), addedContent);
        clickButton("Save", 0);
        waitForElementToDisappear(inlineEditor);
        assertTextPresent(addedContent);
        assertTextNotPresent(WIKI_CHECK_CONTENT);
        assertElementNotPresent(Locator.lkButton("Save"));

        log("** test second edit on inline wiki webpart editor");
        click(Locator.tagWithAttribute("span", "title", "Edit Inline"));
        waitForElement(inlineEditor);
        addedContent = "Second inline edited content: " + WIKI_CHECK_CONTENT;
        setInlineEditorContent(getAttribute(inlineEditor.child("textarea"), "id"), addedContent);
        clickButton("Save", 0);
        waitForElementToDisappear(inlineEditor);
        assertTextPresent(addedContent);

        log("** test cancel on inline wiki webpart editor");
        click(Locator.tagWithAttribute("span", "title", "Edit Inline"));
        String unsavedContent = "SHOULD NOT BE SAVED";
        waitForElement(inlineEditor);
        setInlineEditorContent(getAttribute(inlineEditor.child("textarea"), "id"), unsavedContent);
        clickButton("Cancel", 0);
        assertAlert("Cancelling will lose all unsaved changes. Are you sure?");
        waitForElementToDisappear(inlineEditor);
        assertTextPresent(addedContent);
        assertTextNotPresent(unsavedContent);

        // check that the content was actually saved in the previous steps
        log("** check inline wiki webpart edit is persisted");
        refresh();
        assertTextPresent(addedContent);
    }

    protected void setInlineEditorContent(String editorId, String content)
    {
        executeScript("if (!tinyMCE) {throw 'tinyMCE API is not available'}" +
                "editor = tinyMCE.getInstanceById(arguments[0]);" +
                "if (!editor) {throw 'No tinyMCE instance: ' + arguments[0];}" +
                "editor.setContent(arguments[1]);", editorId, content);
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
