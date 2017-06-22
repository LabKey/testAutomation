/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.components.html;

import org.junit.Assert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

public class FileInput extends Input
{
    public FileInput(WebElement el, WebDriver driver)
    {
        super(el, driver);
    }

    public void set(File file)
    {
        getWrapper().setFormElement(getComponentElement(), file);
    }

    @Override
    protected void assertElementType(WebElement el)
    {
        String type = el.getAttribute("type");
        Assert.assertEquals("Not a file input: " + el.toString(), "file", type);
    }
}
