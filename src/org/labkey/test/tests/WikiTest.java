/*
 * Copyright (c) 2007-2014 LabKey Corporation
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
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Wiki;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.UIContainerHelper;
import org.labkey.test.util.WikiHelper;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({BVT.class, Wiki.class})
public class WikiTest extends BaseWebDriverTest
{
    private static final String PROJECT_NAME = TRICKY_CHARACTERS_FOR_PROJECT_NAMES +  "WikiVerifyProject";

    private static final String WIKI_PAGE_ALTTITLE = "PageBBB has HTML";
    private static final String WIKI_PAGE_WEBPART_ID = "qwp999";
    private static final String WIKI_PAGE_TITLE = "_Test Wiki";
    private static final String WIKI_PAGE_CONTENT =
            "<b>Some HTML content</b>\n" +
                    "<b>${labkey.webPart(partName='Query', title='My Proteins', schemaName='ms2', " +
                    "queryName='Sequences', allowChooseQuery='true', allowChooseView='true', dataRegionName='" + WIKI_PAGE_WEBPART_ID + "')}</b>\n";
    private static final String WIKI_CHECK_CONTENT = "More HTML content";

    public WikiTest()
    {
        setContainerHelper(new UIContainerHelper(this));
    }

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("wiki");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected String getSubolderName()
    {
          return "Subfolder";
    }

    @Test
    public void testSteps()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        WikiHelper wikiHelper = new WikiHelper(this);

        log("Create Project");
        _containerHelper.createProject(PROJECT_NAME, null);
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.checkboxByTitle("Wiki"));
        submit();

        _containerHelper.enableModule(PROJECT_NAME, "MS2");

        goToAdminConsole();
        clickAndWait(Locator.linkWithText("full-text search"));
        if (isTextPresent("pause crawler"))
            clickButton("pause crawler");
        beginAt(getDriver().getCurrentUrl().replace("admin.view", "waitForIdle.view"), 10 * defaultWaitForPage);

        clickProject(PROJECT_NAME);
        portalHelper.addWebPart("Wiki");
        portalHelper.addWebPart("Search");
        portalHelper.addWebPart("Wiki Table of Contents");

        log("test create new html page with a webpart");
        wikiHelper.createNewWikiPage("HTML");

        setFormElement(Locator.name("name"), WIKI_PAGE_TITLE);
        setFormElement(Locator.name("title"), WIKI_PAGE_TITLE);
        wikiHelper.setWikiBody(WIKI_PAGE_CONTENT);

        log("test attachments in wiki");
        click(Locator.linkWithText("Attach a file"));
        File file = new File(TestFileUtils.getLabKeyRoot() + "/common.properties");
        setFormElement(Locator.name("formFiles[0]"), file);
        wikiHelper.saveWikiPage();

        waitForElement(Locator.id(WIKI_PAGE_WEBPART_ID));
        assertTextPresent("common.properties", "Some HTML content");
        assertElementPresent(Locator.linkContainingText("_Test Wiki"));
        impersonateRole("Reader");
        assertElementNotPresent(Locator.linkContainingText("_Test Wiki"));
        stopImpersonatingRole();

        log("test search wiki");
        searchFor(PROJECT_NAME, "Wiki", 1, WIKI_PAGE_TITLE);

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
        createSubfolder(getProjectName(), getSubolderName(), new String[]{});
        portalHelper.addWebPart("Wiki");
        portalHelper.clickWebpartMenuItem("Wiki", "Customize");
        selectOptionByText(Locator.name("webPartContainer"), "/" + getProjectName());
        waitForElement(Locator.xpath("//option[@value='_Test Wiki']"));
        clickButton("Submit");
        verifyWikiPagePresent();

        log("test delete wiki");
        goToProjectHome();
        _extHelper.clickExtMenuButton(true, Locator.tagWithAttribute("img", "title", "More"), "Edit");
        clickButton("Delete Page");
        clickButton("Delete");
        assertTextNotPresent(WIKI_PAGE_ALTTITLE);

        log("verify second wiki part pointing to first handled delete well");
        clickFolder(getSubolderName());
        assertTextPresent("This folder does not currently contain any wiki pages to display");
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
        click(Locator.tagWithAttribute("img", "title", "Edit Inline"));
        waitForElement(inlineEditor);

        String addedContent = "Inline edited content";
        setInlineEditorContent(getAttribute(inlineEditor.child("textarea"), "id"), addedContent);
        clickButton("Save", 0);
        waitForElementToDisappear(inlineEditor);
        assertTextPresent(addedContent);
        assertTextNotPresent(WIKI_CHECK_CONTENT);
        assertButtonNotPresent("Save");

        log("** test second edit on inline wiki webpart editor");
        click(Locator.tagWithAttribute("img", "title", "Edit Inline"));
        waitForElement(inlineEditor);
        addedContent = "Second inline edited content: " + WIKI_CHECK_CONTENT;
        setInlineEditorContent(getAttribute(inlineEditor.child("textarea"), "id"), addedContent);
        clickButton("Save", 0);
        waitForElementToDisappear(inlineEditor);
        assertTextPresent(addedContent);

        log("** test cancel on inline wiki webpart editor");
        click(Locator.tagWithAttribute("img", "title", "Edit Inline"));
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
        Locator.XPathLocator frameId = Locator.id(editorId + "_ifr");
        waitForElement(frameId);

        // swtich to the tinymce iframe
        getDriver().switchTo().frame(getElement(frameId));

        // locate the tinymce body element
        Locator l = Locator.id("tinymce");

        // send keypress to the element
        WebElement el = l.findElement(getDriver());

        // select all then delete previous content (can't seem to get "select all" via Ctrl-A to work)
        el.sendKeys(Keys.HOME);
        for (int i = 0; i < 10; i++)
            el.sendKeys(Keys.chord(Keys.SHIFT, Keys.DOWN));
        el.sendKeys(Keys.BACK_SPACE);

        // enter new content + newline
        el.sendKeys(content);

        // switch back to parent window
        getDriver().switchTo().defaultContent();
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
