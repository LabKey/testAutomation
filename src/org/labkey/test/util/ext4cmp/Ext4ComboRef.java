/*
 * Copyright (c) 2013-2017 LabKey Corporation
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

import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

public class Ext4ComboRef extends Ext4FieldRef
{
    public Ext4ComboRef(String id, WebDriverWrapper test)
    {
        super(id, test);
    }

    public Ext4ComboRef(WebElement el, WebDriverWrapper test)
    {
        super(el, test);
    }

    public Ext4ComboRef(Ext4CmpRef cmp, WebDriverWrapper test)
    {
        super(cmp.getId(), test);
    }

    private Object getRawValueFromDisplayValue(String displayValue)
    {
        waitForStoreLoad();
        return getFnEval("return this.store.data.get(this.store.find(this.displayField, arguments[0])).get(this.valueField)", displayValue);
    }

    public Object getDisplayValue()
    {
        waitForStoreLoad();
        if (this.getValue() == null)
            return null;

        Long recordIdx = (Long)getFnEval("return this.store.find(this.valueField, this.getValue())");
        assert recordIdx != -1 && recordIdx.intValue() != -1 : "Unable to find record with value: " + getValue();

        return getFnEval("return this.store.getAt(arguments[0]).get(this.displayField)", recordIdx);
    }

    public void waitForStoreLoad()
    {
        _test.waitFor(() -> (Boolean)getFnEval("if(this.store){return (this.store.getCount() > 0);} else return false;"),
                "No records loaded in store", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public void setComboByDisplayValue(String displayValue)
    {
        Object value = getRawValueFromDisplayValue(displayValue);
        eval("setValue(arguments[0]);", value);
    }

    public static Ext4ComboRef getForLabel(WebDriverWrapper test, String label)
    {
        Ext4ComboRef ref = test._ext4Helper.queryOne("field.combobox[fieldLabel^=\"" + label + "\"]", Ext4ComboRef.class);
        if (ref == null)
            throw new NoSuchElementException("Unable to locate combo with label: " + label);
        return ref;
    }
}
