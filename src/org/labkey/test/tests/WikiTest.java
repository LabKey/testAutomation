/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Wiki;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;
import org.labkey.test.util.search.SearchAdminAPIHelper;
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
                    "<b>${labkey.webPart(partName='Query', title='My Users', schemaName='core', " +
                    "queryName='Users', allowChooseQuery='true', allowChooseView='true', dataRegionName='" + WIKI_PAGE_WEBPART_ID + "')}</b>\n";
    private static final String WIKI_CHECK_CONTENT = "More HTML content";

    @BeforeClass
    public static void setupProject()
    {
        WikiTest init = (WikiTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        _containerHelper.enableModules(Arrays.asList("Wiki"));

        SearchAdminAPIHelper.pauseCrawler(getDriver());
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

    protected String getSubfolderName()
    {
          return "Subfolder";
    }

    @Test
    public void testSteps()
    {
        PortalHelper portalHelper = new PortalHelper(this);
        WikiHelper wikiHelper = new WikiHelper(this);

        goToProjectHome();
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
        File file = wikiHelper.getSampleFile();
        setFormElement(Locator.name("formFiles[0]"), file);
        wikiHelper.saveWikiPage();

        DataRegionTable.DataRegion(getDriver()).withName(WIKI_PAGE_WEBPART_ID).waitFor();
        assertTextPresent(file.getName(), "Some HTML content");
        final Locator.XPathLocator wikiTitleLink = Locator.linkContainingText("_Test Wiki").withAttribute("href");
        assertElementPresent(wikiTitleLink);
        impersonateRole("Reader");
        assertElementNotPresent(wikiTitleLink);
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
        _containerHelper.createSubfolder(getProjectName(), getSubfolderName(), new String[]{});
        portalHelper.addWebPart("Wiki");
        portalHelper.clickWebpartMenuItem("Wiki", "Customize");
        selectOptionByText(Locator.name("webPartContainer"), "/" + getProjectName());
        waitForElement(Locator.xpath("//option[@value='_Test Wiki']"));
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
        WebElement frame = waitForElement(Locator.id(editorId + "_ifr"));

        // switch to the tinymce iframe
        getDriver().switchTo().frame(frame);

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

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
