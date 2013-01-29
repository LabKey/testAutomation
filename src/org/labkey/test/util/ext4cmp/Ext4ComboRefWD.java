package org.labkey.test.util.ext4cmp;

import org.labkey.test.BaseWebDriverTest;
import org.openqa.selenium.WebElement;

/**
 * Created with IntelliJ IDEA.
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
