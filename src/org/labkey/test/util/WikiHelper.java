/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import java.io.File;

public class WikiHelper extends AbstractHelper
{
    public WikiHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    private void create(String name, @Nullable String format, @Nullable String title)
    {
        if (null != format)
            _test.createNewWikiPage(format);
        else
            _test.createNewWikiPage();

        _test.setFormElement(Locator.name("name"), name);
        if (null != title)
            _test.setFormElement(Locator.name("title"), title);
    }

    public void createWikiPage(String name, @Nullable String title, File srcFile)
    {
        create(name, null, title);

        setSourceFromFile(srcFile);

        _test.saveWikiPage();
    }

    public void createWikiPage(String name, @Nullable String format, @Nullable String title, String body, boolean index, @Nullable File attachment, boolean wikiVisualBody)
    {
        create(name, format, title);

        if (wikiVisualBody == true)
            setWikiVisualBody(body);
        else
            setSource(body);

        if(index)
            _test.checkCheckbox("shouldIndex");
        else
            _test.uncheckCheckbox("shouldIndex");

        if (null != attachment)
        {
            _test.click(Locator.linkWithText("Attach a file"));
            _test.setFormElement(Locator.name("formFiles[0]"), attachment);
        }
        _test.saveWikiPage();
    }

    public void createWikiPage(String name, @Nullable String format, @Nullable String title, String body, File file)
    {
        createWikiPage(name, format, title, body, true, file, true);
    }

    private void setSourceFromFile(File file)
    {
        setSource(_test.getFileContents(file));
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
        _test.switchWikiToSourceView();
        _test.setFormElement(Locator.name("body"), srcFragment);
    }
}
