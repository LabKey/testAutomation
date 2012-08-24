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
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 7/26/12
 * Time: 12:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class WikiHelper
{
    /**
     *
     * @param test
     * @param format
     * @param name
     * @param title
     * @param body
     * @param index
     * @param file
     */

    public static void createWikiPage(BaseSeleniumWebTest test, String format, String name, String title, String body, boolean index, File file)
    {

        test.createNewWikiPage(format);

        test.setFormElement("name", name);
        test.setFormElement("title", title);
        test.setFormElement("body", body);

        if(index)
            test.checkCheckbox("shouldIndex");
        else
            test.uncheckCheckbox("shouldIndex");

        if(file!=null)
        {
            test.setFormElement("formFiles[0]", file);
        }
        test.saveWikiPage();
    }
    public static void createWikiPage(BaseSeleniumWebTest test, String format, String name, String title, String body, File file)
    {
        createWikiPage(test, format, name, title, body, true, file);
    }
}
