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
package org.labkey.test.util.ext4cmp;

import junit.framework.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.openqa.selenium.WebElement;

/**
 * User: bbimber
 * Date: 6/20/12
 * Time: 12:40 PM
 */
public class Ext4FieldRefWD extends Ext4CmpRefWD
{
    public Ext4FieldRefWD(String id, BaseWebDriverTest test)
    {
        super(id, test);
    }

    public Ext4FieldRefWD(WebElement el, BaseWebDriverTest test)
    {
        super(el, test);
    }

    public static Ext4FieldRefWD getForLabel(BaseWebDriverTest test, String label)
    {
        Ext4FieldRefWD ref = test._ext4Helper.queryOne("field[fieldLabel^=\"" + label + "\"]", Ext4FieldRefWD.class);
        Assert.assertNotNull("Unable to locate field with label: " + label, ref);
        return ref;
    }

    public static boolean isFieldPresent(BaseWebDriverTest test, String label)
    {
        return null != test._ext4Helper.queryOne("field[fieldLabel^=\"" + label + "\"]", Ext4FieldRefWD.class);
    }

    public void setValue(String val)
    {
        eval("setValue(arguments[0])", val);
    }

    public void setValue(String[] vals)
    {
        eval("setValue(arguments)", vals);
    }

    public void setChecked(Boolean checked)
    {
        eval("setValue(arguments[0])", checked);
    }

    public String getValue()
    {
        return (String)getEval("getValue()");
    }
}
