/*
 * Copyright (c) 2018-2019 LabKey Corporation
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
package org.labkey.test.components;

import org.labkey.test.util.FileBrowserHelper;
import org.openqa.selenium.WebDriver;

public class FilesWebPart extends BodyWebPart<FilesWebPart.ElementCache>
{

    public FilesWebPart(WebDriver driver)
    {
        super(driver, "Files");
    }

    static public FilesWebPart getWebPart(WebDriver driver)
    {
        return new FilesWebPart(driver);
    }

    public FileBrowserHelper fileBrowser()
    {
        return new FileBrowserHelper(getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BodyWebPart.ElementCache
    {
    }

}
