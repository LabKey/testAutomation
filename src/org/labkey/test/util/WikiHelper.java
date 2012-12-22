/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;

import java.io.File;

/**
 * User: elvan
 * Date: 7/26/12
 * Time: 12:44 PM
 */
public class WikiHelper extends AbstractHelper
{
    public WikiHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    /**
     *
     * @param format
     * @param name
     * @param title
     * @param body
     * @param index
     * @param file
     */

    public void createWikiPage(String format, String name, String title, String body, boolean index, File file)
    {

        _test.createNewWikiPage(format);

        _test.setFormElement("name", name);
        _test.setFormElement("title", title);
        _test.setFormElement("body", body);

        if(index)
            _test.checkCheckbox("shouldIndex");
        else
            _test.uncheckCheckbox("shouldIndex");

        if(file!=null)
        {
            _test.setFormElement("formFiles[0]", file);
        }
        _test.saveWikiPage();
    }

    public void createWikiPage(String format, String name, String title, String body, File file)
    {
        createWikiPage(format, name, title, body, true, file);
    }
}
