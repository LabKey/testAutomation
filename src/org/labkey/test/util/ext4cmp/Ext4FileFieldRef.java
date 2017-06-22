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
package org.labkey.test.util.ext4cmp;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebElement;

import java.io.File;

public class Ext4FileFieldRef extends Ext4CmpRef
{
    public Ext4FileFieldRef(String id, WebDriverWrapper test)
    {
        super(id, test);
    }

    public Ext4FileFieldRef(WebElement el, WebDriverWrapper test)
    {
        super(el, test);
    }

    //often there is only one file field on screen, so we'll just grab that
    public static Ext4FileFieldRef create(WebDriverWrapper test)
    {
        return test._ext4Helper.queryOne("filefield", Ext4FileFieldRef.class);
    }

    public void setToFile(File file)
    {
        _test.setFormElement(Locator.id((String)getEval("fileInputEl.dom.id")), file);
        this.getFnEval("this.onFileChange(this.button, null, this.fileInputEl.dom.value)"); //pass name of the file so the field sets the correct display value
    }
}
