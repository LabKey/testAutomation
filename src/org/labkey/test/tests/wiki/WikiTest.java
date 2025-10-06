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

import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Wiki;
import org.labkey.test.pages.admin.ExternalSourcesPage;
import org.labkey.test.pages.admin.ExternalSourcesPage.Directive;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.pages.wiki.EditPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.search.SearchAdminAPIHelper;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Category({Daily.class, Wiki.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class WikiTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = TRICKY_CHARACTERS_FOR_PROJECT_NAMES + "WikiVerifyProject";
    private static final String SUBFOLDER_NAME = TRICKY_CHARACTERS_FOR_PROJECT_NAMES + "WikiVerifySubfolder";
    private static final String SUBFOLDER_PATH = String.format("%s/%s", PROJECT_NAME, SUBFOLDER_NAME);
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
        WikiTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.enableModules(Arrays.asList("Wiki"));
        _containerHelper.createSubfolder(PROJECT_NAME, SUBFOLDER_NAME);
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
        goToProjectHome();

        log("test create new html page with a webpart");
        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createNewWikiPage("HTML");
        numberOfWikiCreated++;

        wikiHelper.setWikiName(WIKI_PAGE_TITLE);
        wikiHelper.setWikiTitle(WIKI_PAGE_TITLE);
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
        String videoHost = "https://www.youtube.com";
        String videoUrl = videoHost + "/embed/JEE4807UHN4";
        String wikiName = "Wiki with video";
        String wikiTitle = "Sample finder video";
        String wikiContent = """
                Some random content start : Have fun watching video below
                {video:%s|height:350|width:500}
                Hope you had fun watching the video..!
                """.formatted(videoUrl);

        ExternalSourcesPage.beginAt(this).ensureHost(Directive.Frame, videoHost);

        goToProjectHome();
        log("Creating the wiki with video");
        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createNewWikiPage("RADEOX");
        numberOfWikiCreated++;
        wikiHelper.setWikiName(wikiName);
        wikiHelper.setWikiTitle(wikiTitle);
        wikiHelper.setWikiBody(wikiContent);
        wikiHelper.saveWikiPage();

        Assert.assertEquals("Video is missing", videoUrl,
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
        wikiHelper.setWikiName(WIKI_PAGE_NAME);
        wikiHelper.setWikiTitle(WIKI_PAGE_TITLE);
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
        wikiHelper.setWikiName(wikiName);
        wikiHelper.setWikiTitle(wikiTitle);
        wikiHelper.setWikiBody("<p>" + wikiContent + "</p>");
        wikiHelper.saveWikiPage();
        numberOfWikiCreated++;

        searchFor(PROJECT_NAME, "commas", 1, null);
        Assert.assertEquals("Incorrect result with comma", Arrays.asList(wikiTitle + "\n/" + getProjectName() + "\n" + wikiContent), getTexts(new SearchResultsPage(getDriver()).getResults()));
    }

    // Issue 49321
    @Test
    public void testDeleteUndeleteAttachment() throws IOException
    {
        String wikiName = "Wiki with attachments";
        String wikiTitle = "Attach Delete Undelete file";
        String wikiContent = "Lorem Ipsum something";
        String fileName = "wiki_temp_file_attachment.txt";
        File testAttachment = TestFileUtils.writeTempFile(fileName, "it was a dark and stormy night");
        Locator wikiPageLinkLoc = Locator.linkWithText(wikiTitle);
        Locator editLinkLoc = Locator.linkWithText("Edit");
        Locator.XPathLocator attachmentParentLoc = Locator.id("wiki-ea-name-0");
        Locator.XPathLocator removeLinkLoc = Locator.tag("td").child(Locator.linkContainingText("remove"));
        Locator.XPathLocator deleteLinkLoc = Locator.tag("td").child(Locator.linkContainingText("delete"));
        Locator undeleteLinkLoc = Locator.tag("td").child("a").child("span").containing("un-delete");
        Locator filePickerLinkLoc = Locator.id("filePickerLink");
        Locator fileInputLoc = Locator.tag("input").withAttribute("type", "file")
                .withAttributeContaining("id", "formFile");

        goToProjectHome();
        log("Creating the wiki " + wikiTitle);
        WikiHelper wikiHelper = new WikiHelper(this);
        wikiHelper.createNewWikiPage("HTML");
        wikiHelper.setWikiName(wikiName);
        wikiHelper.setWikiTitle(wikiTitle);
        wikiHelper.setWikiBody("<p>" + wikiContent + "</p>");
        wikiHelper.saveWikiPage();
        numberOfWikiCreated++;
        waitAndClickAndWait(wikiPageLinkLoc);

        log("adding an attachment");
        waitAndClickAndWait(editLinkLoc);
        click(filePickerLinkLoc);
        setFormElement(fileInputLoc, testAttachment);
        waitForElement(removeLinkLoc);  // when just attached, 'remove' will be an option but delete will not be
        assertElementNotPresent(deleteLinkLoc);
        click(removeLinkLoc);   // verify remove removes the file
        waitForElementToDisappear(Locator.linkWithText(fileName));
        click(filePickerLinkLoc);
        setFormElement(fileInputLoc, testAttachment);
        wikiHelper.saveWikiPage();  // save with the attachment
        waitForElement(Locator.linkWithText(fileName));
        clickAndWait(wikiPageLinkLoc);

        log("Deleting attachment");
        waitAndClickAndWait(editLinkLoc);
        click(deleteLinkLoc);
        waitForElement(attachmentParentLoc.withAttributeContaining("style", "text-decoration: line-through"));
        wikiHelper.saveWikiPage();
        // verify save while in deleted state actually deletes the attachment
        assertElementNotPresent(Locator.linkWithText(fileName));
        clickAndWait(wikiPageLinkLoc);

        log("prepare to delete/undelete attachment");
        // re-attach the file and save
        waitAndClickAndWait(editLinkLoc);
        click(filePickerLinkLoc);
        setFormElement(fileInputLoc, testAttachment);
        wikiHelper.saveWikiPage();
        clickAndWait(wikiPageLinkLoc);

        log("Un-Deleting attachment");
        waitAndClickAndWait(editLinkLoc);
        click(deleteLinkLoc);   // delete
        waitForElement(attachmentParentLoc.withAttributeContaining("style", "text-decoration: line-through"));
        waitAndClick(undeleteLinkLoc);
        checker().awaiting(Duration.ofMillis(500), ()-> Assertions.assertThat(attachmentParentLoc.findElement(getDriver()).getAttribute("style"))
                .as("expect strikethrough style not to be present")
                .doesNotContain("text-decoration: line-through"));
        wikiHelper.saveWikiPage();
        // note: attaching the file and leaving it there will create a search result, so increment wikiCreated count here
        numberOfWikiCreated++;

        // verify save after undelete persists the attachment
        assertElementPresent(Locator.linkWithText(fileName));
    }

    // Issue 51382
    @Test
    public void testCreateWikiWithHostileNameAndTitle() throws Exception
    {
        var newLine = '\u0081';
        var stringTerminator = '\u009c';
        String wikiName = "Wiki with " + stringTerminator + TRICKY_CHARACTERS + newLine;
        String wikiTitle = "Title with " + stringTerminator + TRICKY_CHARACTERS + newLine;
        String wikiContent = "<p>Content with " + stringTerminator + TRICKY_CHARACTERS + newLine + "</p>";

        var createCmd = new SimplePostCommand("wiki", "saveWiki");
        JSONObject json = new JSONObject();
        json.put("title", wikiTitle);
        json.put("content", wikiContent);
        json.put("body", wikiContent);
        json.put("pageVersionId", -1);
        json.put("name", wikiName);
        createCmd.setJsonObject(json);
        try
        {
            createCmd.execute(createDefaultConnection(), getProjectName());
            Assert.fail("Create command should have failed");
        }
        catch (CommandException success)
        {
            log("Error creating wiki page: " + success.getMessage());
            checker().wrapAssertion(() -> Assertions.assertThat(success.getMessage())
                    .as("expect error")
                    .contains("Wiki name contains invalid characters"));
            checker().verifyEquals("expect 400 for bad request", 400, success.getStatusCode());
            var jsonProps = new JSONObject(success.getProperties());
            var errors = jsonProps.getJSONArray("errors");

            checker().wrapAssertion(() -> Assertions.assertThat(List.of(errors.getJSONObject(0), errors.getJSONObject(1)))
                    .extracting(a -> a.get("msg"))
                    .as("expect warnings for wiki name and title")
                    .containsOnly("Wiki name contains invalid characters.", "Wiki title contains invalid characters."));
            checker().wrapAssertion(() -> Assertions.assertThat(List.of(errors.getJSONObject(0), errors.getJSONObject(1)))
                    .extracting(a -> a.get("severity"))
                    .as("expect error severity")
                    .containsOnly("Error"));
            checker().wrapAssertion(() -> Assertions.assertThat(List.of(errors.getJSONObject(0), errors.getJSONObject(1)))
                    .extracting(a -> a.get("field"))
                    .as("expect errors for name, title")
                    .containsOnly("name", "title"));
        }
    }

    // Issue 51382
    @Test
    public void testUpdateWikiWithHostileNameAndTitle() throws Exception
    {
        var newLine = '\u0081';
        var stringTerminator = '\u009c';
        String wikiTitle = "Title with " + stringTerminator + TRICKY_CHARACTERS + newLine;
        String wikiContent = "<p>This is my content " + stringTerminator + TRICKY_CHARACTERS + newLine + "</p>";
        String wikiName = "hostileWiki";
        String wikiTitleSafe = "wikiHostile";
        var cn = createDefaultConnection();

        // first, create a straightforward wiki
        var createCmd = new SimplePostCommand("wiki", "saveWiki");
        JSONObject createJson = new JSONObject();
        createJson.put("name", wikiName);
        createJson.put("title", wikiTitleSafe);
        createJson.put("rendererType", "HTML");
        createJson.put("body", "<p> content </p>");
        createJson.put("pageVersionId", -1);
        createCmd.setJsonObject(createJson);

        var createResponse = createCmd.execute(cn, getProjectName());
        var createResponseJson = new JSONObject(createResponse.getParsedData());
        var wikiProps = createResponseJson.getJSONObject("wikiProps");

        // now, update the wiki with hostile inputs, expecting error/failure
        var updateJson = new JSONObject();
        updateJson.put("name", wikiProps.getString("name"));
        updateJson.put("title", wikiTitle);
        updateJson.put("entityId", wikiProps.getString("entityId"));
        updateJson.put("rendererType", wikiProps.getString("rendererType"));
        updateJson.put("body", wikiContent);
        updateJson.put("pageVersionId", wikiProps.getInt("pageVersionId"));
        createCmd.setJsonObject(updateJson);
        try {
            createCmd.execute(cn, getProjectName());
            Assert.fail("Update command should have failed with hostile input");
        } catch (CommandException success)
        {
            checker().wrapAssertion(()-> Assertions.assertThat(success.getMessage())
                    .as("expect error")
                    .contains("Wiki title contains invalid characters"));
            checker().verifyEquals("expect 400 for bad request", 400, success.getStatusCode());
            var jsonProps =new JSONObject(success.getProperties());
            var error = jsonProps.getJSONArray("errors").getJSONObject(0);

            checker().wrapAssertion(()-> Assertions.assertThat(error)
                    .extracting(a-> a.get("msg"))
                    .as("expect warning for wiki title")
                    .isEqualTo("Wiki title contains invalid characters."));
            checker().wrapAssertion(()-> Assertions.assertThat(error)
                    .extracting(a-> a.get("severity"))
                    .as("expect error severity")
                    .isEqualTo("Error"));
            checker().wrapAssertion(()-> Assertions.assertThat(error)
                    .extracting(a-> a.get("field"))
                    .as("expect title field to be the source of the error")
                    .isEqualTo("title"));
        }
    }

    // Issue 52729 Wiki webpart doesn't resolve after wiki rename with alias
    @Test
    public void testRenameWebPartWiki() throws Exception
    {
        var newLine = '\u0081';
        var stringTerminator = '\u009c';
        String wikiContent = "<p>This is my content " + stringTerminator + TRICKY_CHARACTERS + newLine + "</p>";
        String wikiName = "webPartWiki";
        String wikiTitle = "wikiRenameWebPart";
        var cn = createDefaultConnection();

        // first, create a straightforward wiki
        var createCmd = new SimplePostCommand("wiki", "saveWiki");
        JSONObject createJson = new JSONObject();
        createJson.put("name", wikiName);
        createJson.put("title", wikiTitle);
        createJson.put("rendererType", "HTML");
        createJson.put("body", "<p>content for wiki webpart rename</p>");
        createJson.put("pageVersionId", -1);
        createCmd.setJsonObject(createJson);
        createCmd.execute(cn, SUBFOLDER_PATH);

        // give the folder a wikiWebPart
        goToProjectFolder(PROJECT_NAME, SUBFOLDER_NAME);
        var wikiHelper = new WikiHelper(this);
        new PortalHelper(this).addWebPart("Wiki");
        Locator wikiWebPartLoc  = Locator.tagWithClass("div", "panel-portal")
                .withDescendant(Locator.tagWithAttribute("h3", "title", wikiTitle))
                .descendant(Locator.tagWithText("p", "content for wiki webpart rename"));

        // configure the webPart to use the wiki created above
        wikiHelper.clickChooseAPage();
        var selectedPageOption = getSelectedOptionText(Locator.name("name"));
        checker().withScreenshot("unexpected_selected_page")
                        .wrapAssertion(()-> Assertions.assertThat(selectedPageOption)
                                .as("expect our wiki to be selected")
                                .startsWith(wikiName));
        wikiHelper.saveChosenPage();

        // verify the webpart's content is our expected content
        checker().withScreenshot("unexpected_wiki_content")
                .awaiting(Duration.ofSeconds(1), ()-> Assertions.assertThat(wikiWebPartLoc.existsIn(getDriver()))
                        .as("expect our wiki content to be present")
                        .isTrue());

        // Now edit the wiki, give it a new name, with an alias
        var wikiConfigPage = wikiHelper.manageWikiConfiguration();
        wikiConfigPage.rename("webPartNewWikiName", true)
                        .save();

        // verify the expected content is still present
        checker().withScreenshot("unexpected_wiki_content_after_rename")
                .awaiting(Duration.ofSeconds(1), ()-> Assertions.assertThat(wikiWebPartLoc.existsIn(getDriver()))
                        .as("expect our wiki content to be present")
                        .isTrue());
    }

    protected void verifyWikiPagePresent()
    {
        waitForText(WIKI_CHECK_CONTENT);
        assertTextPresent(WIKI_PAGE_ALTTITLE);
    }

    protected void doTestInlineEditor()
    {
        Locator.XPathLocator inlineEditor = Locator.xpath("//div[@class='labkey-inline-editor']")
                .withDescendant(Locator.tagWithClassContaining("div", "tox-edit-area"));

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
        executeScript("if (!tinymce) {throw 'tinymce API is not available'}" +
                "editor = tinymce.get(arguments[0]);" +
                "if (!editor) {throw 'No tinymce instance: ' + arguments[0];}" +
                "editor.setContent(arguments[1]);" +
                "editor.setDirty(true);"         // Explicitly setDirty as the setContent doesn't by default
                , editorId, content);
        log(String.format("Content [%1$s] set on editor: %2$s", content,  editorId));
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
