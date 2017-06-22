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
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Ext4FieldRef extends Ext4CmpRef
{
    public Ext4FieldRef(String id, WebDriverWrapper test)
    {
        super(id, test);
    }

    public Ext4FieldRef(WebElement el, WebDriverWrapper test)
    {
        super(el, test);
    }

    public static Ext4FieldRef getForLabel(WebDriverWrapper test, String label)
    {
        Ext4FieldRef ref = test._ext4Helper.queryOne("field[fieldLabel^=\"" + label + "\"]", Ext4FieldRef.class);
        if (ref == null)
            throw new NoSuchElementException("Unable to locate field with label: " + label);
        return ref;
    }

    public static Ext4FieldRef getForBoxLabel(WebDriverWrapper test, String boxLabel)
    {
        Ext4FieldRef ref = test._ext4Helper.queryOne("field[boxLabel^=\"" + boxLabel + "\"]", Ext4FieldRef.class);
        if (ref == null)
            throw new NoSuchElementException("Unable to locate field with boxLabel: " + boxLabel);
        return ref;
    }

    public static Ext4FieldRef getForName(WebDriverWrapper test, String name)
    {
        Ext4FieldRef ref = test._ext4Helper.queryOne("field[name^=\"" + name + "\"]", Ext4FieldRef.class);
        if (ref == null)
            throw new NoSuchElementException("Unable to locate field with name: " + name);
        return ref;
    }

    public static boolean isFieldPresent(WebDriverWrapper test, String label)
    {
        return null != test._ext4Helper.queryOne("field[fieldLabel^=\"" + label + "\"]", Ext4FieldRef.class);
    }

    @LogMethod(quiet = true)
    public static void waitForField(final WebDriverWrapper test, @LoggedParam final String label)
    {
        test.waitFor(() -> isFieldPresent(test, label),
                "Field did not appear: " + label, WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public void setValue(Object val)
    {
        eval("setValue(arguments[0])", val);
    }

    public void setValue(Object[] vals)
    {
        eval("setValue(arguments)", vals);
    }

    public void setChecked(Boolean checked)
    {
        eval("setValue(arguments[0])", checked);
    }

    public Object getValue()
    {
        return getEval("getValue()");
    }

    public Double getDoubleValue()
    {
        Object val = getEval("getValue()");
        if (val == null)
        {
            return null;
        }
        else if (val instanceof Long)
        {
            return ((Long)val).doubleValue();
        }
        else if (val instanceof Integer)
        {
            return ((Integer)val).doubleValue();
        }
        else if (val instanceof Double)
        {
            return ((Double)val);
        }

        throw new IllegalArgumentException("Unknown type: " + val.getClass().getName());
    }

    public Date getDateValue()
    {
        try
        {
            String val = (String)getFnEval("return this.getValue() ? this.getValue().format('c') : null");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            return val == null ? null : dateFormat.parse(val);
        }
        catch (ParseException e)
        {
            throw new RuntimeException(e);
        }
    }

    public boolean isVisible()
    {
        WebElement el = _test.getDriver().findElement(By.id(_id));
        return el.isDisplayed();
    }

    public void clickTrigger()
    {
        _test.click(Locator.id(_id).append(Locator.tagWithClass("div", "x4-form-trigger")));
    }

    public Boolean isDisabled()
    {
        return (Boolean)getEval("isDisabled()");
    }
}
