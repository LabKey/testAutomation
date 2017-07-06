/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.pages.wiki.EditPage;

import java.io.File;

public class WikiHelper
{
    private BaseWebDriverTest _test;
    
    public WikiHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    private void create(String name, @Nullable String format, @Nullable String title)
    {
        if (null != format)
            createNewWikiPage(format);
        else
            createNewWikiPage();

        _test.setFormElement(Locator.name("name"), name);
        if (null != title)
            _test.setFormElement(Locator.name("title"), title);
    }

    public void createWikiPage(String name, @Nullable String title, File srcFile)
    {
        create(name, null, title);

        setSourceFromFile(srcFile);

        saveWikiPage();
    }

    public void createWikiPage(String name, @Nullable String format, @Nullable String title, String body, boolean index, @Nullable File attachment, boolean wikiVisualBody)
    {
        create(name, format, title);

        if (wikiVisualBody)
            setWikiVisualBody(body);
        else
            setSource(body);

        if(index)
            _test.checkCheckbox(Locator.checkboxByName("shouldIndex"));
        else
            _test.uncheckCheckbox(Locator.checkboxByName("shouldIndex"));

        if (null != attachment)
        {
            _test.click(Locator.linkWithText("Attach a file"));
            _test.setFormElement(Locator.name("formFiles[0]"), attachment);
            Locator.lkButton("remove").waitForElement(_test.shortWait());
        }
        saveWikiPage();
        if (attachment != null)
        {
            _test.assertElementPresent(Locator.linkWithText(" " + attachment.getName()));
        }
    }

    public void createWikiPage(String name, @Nullable String format, @Nullable String title, String body, @Nullable File attachment)
    {
        createWikiPage(name, format, title, body, true, attachment, true);
    }

    public File getSampleFile()
    {
        // Just a random file to test attachments, etc.
        return TestFileUtils.getSampleData("fileTypes/xls_sample.xls");
    }

    private void setSourceFromFile(File file)
    {
        setSource(TestFileUtils.getFileContents(file));
    }

    private void setSource(String srcFragment)
    {
        setWikiSourceTab(srcFragment);
    }

    // assumes on Wiki edit page -- Visual tab
    private void setWikiVisualBody(String body)
    {
        _test.setFormElement(Locator.name("body"), body);
    }

    // assumes on Wiki edit page -- Source tab
    private void setWikiSourceTab(String srcFragment)
    {
        switchWikiToSourceView();
        _test.setFormElement(Locator.name("body"), srcFragment);
    }

    public void saveWikiPage()
    {
        String title = Locator.id("wiki-input-title").findElement(_test.getDriver()).getText();
        if (title.equals("")) title = Locator.id("wiki-input-name").findElement(_test.getDriver()).getText();
        _test.clickButton("Save & Close");
        _test.waitForElement(Locator.linkWithText(title));
    }

    /**
     * Creates a new wiki page
     * @param format The format for the new page. Allowed values are "RADEOX" (for wiki),
     * "HTML", and "TEXT_WITH_LINKS". Note that these are the string names for the
     * WikiRendererType enum values.
     * 
     */
    public void createNewWikiPage(String format)
    {
        if(_test.isElementPresent(Locator.linkWithText("new page")))
            _test.clickAndWait(Locator.linkWithText("new page"));
        else if(_test.isElementPresent(Locator.linkWithText("Create a new wiki page")))
            _test.clickAndWait(Locator.linkWithText("Create a new wiki page"));
        else if(_test.isElementPresent(Locator.linkWithText("add content")))
            _test.clickAndWait(Locator.linkWithText("add content"));
        else if(_test.isTextPresent("Pages"))
        {
            PortalHelper portalHelper = new PortalHelper(_test);
            portalHelper.clickWebpartMenuItem("Pages", "New");
        }
        else
            throw new IllegalStateException("Could not find a link on the current page to create a new wiki page." +
                    " Ensure that you navigate to the wiki controller home page or an existing wiki page" +
                    " before calling this method.");

        convertWikiFormat(format);
    }

    public void createNewWikiPage(WikiRendererType format)
    {
        EditPage.beginAt(_test).convertWikiFormat(format);
    }

    //must already be on wiki page
    public void setWikiValuesAndSave(String name, String title, String body)
    {

        _test.setFormElement(Locator.name("name"), name);
        _test.setFormElement(Locator.name("title"), title);
        setWikiBody(body);
        _test.clickButtonContainingText("Save & Close");
    }

    /**
     * Converts the current wiki page being edited to the specified format.
     * If the page is already in that format, it will no-op.
     * @param format The desired format ("RADEOX", "HTML", "MARKDOWN", or "TEXT_WITH_LINKS")
     * 
     */
    public void convertWikiFormat(String format)
    {
        new EditPage(_test.getDriver()).convertWikiFormat(WikiRendererType.valueOf(format));
    }

    /**
     * Creates a new wiki page using HTML as the format. See {@link WikiHelper#createNewWikiPage(String)}
     * for more details.
     */
    @LogMethod(quiet = true)public void createNewWikiPage()
    {
        createNewWikiPage("HTML");
    }

    /**
     * Sets the wiki page body, automatically switching to source view if necessary
     * @param body The body text to set
     * 
     */
    @LogMethod(quiet = true)public void setWikiBody(String body)
    {
        switchWikiToSourceView();
        _test.setFormElement(Locator.name("body"), body);
    }

    public String setSource(String srcFragment, String wikiName)
    {
        _test.waitForElement(Locator.linkWithText(wikiName));
        PortalHelper portalHelper = new PortalHelper(_test);
        portalHelper.clickWebpartMenuItem(wikiName, "Edit");

        setWikiBody(srcFragment);
        saveWikiPage();
        return srcFragment;
    }

    /**
     * Given a file name sets the wikiName page contents to a file in server/test/data/api
     * @param fileName file will be found in server/test/data/api
     * @param wikiName Name of the wiki where the source should be placed
     * @return The source found in the file.
     */
    public String setSourceFromFile(String fileName, String wikiName)
    {
        return setSource(TestFileUtils.getFileContents("server/test/data/api/" + fileName), wikiName);
    }

    /**
     * Switches the wiki edit page to source view when the format type is HTML.
     */
    public void switchWikiToSourceView()
    {
        String curFormat = (String) _test.executeScript("return LABKEY._wiki.getProps().rendererType;");
        if (curFormat.equalsIgnoreCase("HTML"))
        {
            if (_test.isElementPresent(Locator.css("#wiki-tab-source.labkey-tab-inactive")))
            {
                _test.click(Locator.css("#wiki-tab-source > a"));
                _test.waitForElement(Locator.css("#wiki-tab-source.labkey-tab-active"));
            }
        }
    }

    public void switchWikiToVisualView()
    {
        String curFormat = (String) _test.executeScript("return LABKEY._wiki.getProps().rendererType;");
        if (curFormat.equalsIgnoreCase("HTML"))
        {
            if (_test.isElementPresent(Locator.css("#wiki-tab-visual.labkey-tab-inactive")))
            {
                _test.click(Locator.css("#wiki-tab-visual > a"));
                _test.waitForElement(Locator.css("#wiki-tab-visual.labkey-tab-active"));
            }
        }
    }

    // need to be on a wiki page in the source container, and destinationFolder needs to be a unique link on the page
    public void copyAllWikiPages(String destinationFolder, boolean copyHistory)
    {
        PortalHelper portalHelper = new PortalHelper(_test);
        portalHelper.clickWebpartMenuItem("Pages", "Copy");
        _test.clickAndWait(Locator.linkWithText(destinationFolder));
        if(copyHistory)
            _test.clickAndWait(Locator.checkboxById("isCopyingHistory"), 0);
        _test.clickButton("Copy Pages");
    }

    public enum WikiRendererType
    {
        RADEOX ("Wiki Page"),
        HTML ("HTML"),
        MARKDOWN ("Markdown"),
        TEXT_WITH_LINKS ("Plain Text");

        final String _displayName;

        WikiRendererType(String displayName)
        {
            _displayName = displayName;
        }

        public String getDisplayName()
        {
            return _displayName;
        }
    }
}
