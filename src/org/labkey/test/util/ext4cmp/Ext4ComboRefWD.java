/*
 * Copyright (c) 2013 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.openqa.selenium.WebElement;

/**
 * User: bimber
 * Date: 1/23/13
 * Time: 5:31 PM
 */
public class Ext4ComboRefWD extends Ext4FieldRefWD
{
    public Ext4ComboRefWD(String id, BaseWebDriverTest test)
    {
        super(id, test);
    }

    public Ext4ComboRefWD(WebElement el, BaseWebDriverTest test)
    {
        super(el, test);
    }

    public Ext4ComboRefWD(Ext4CmpRefWD cmp, BaseWebDriverTest test)
    {
        super(cmp.getId(), test);
    }

    private Object getRawValueFromDisplayValue(String displayValue)
    {
        return getFnEval("return this.store.data.get(this.store.find(this.displayField, arguments[0])).get(this.valueField)", displayValue);
    }

    public void setComboByDisplayValue(String displayValue)
    {
        Object value = getRawValueFromDisplayValue(displayValue);
        eval("setValue(arguments[0]);", value);
    }
}
