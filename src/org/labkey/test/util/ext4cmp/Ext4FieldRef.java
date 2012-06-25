package org.labkey.test.util.ext4cmp;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 6/20/12
 * Time: 12:40 PM
 */
public class Ext4FieldRef extends Ext4CmpRef
{
    public Ext4FieldRef(String id, BaseSeleniumWebTest test)
    {
        super(id, test);
    }

    public static Ext4FieldRef getForLabel(BaseSeleniumWebTest test, String label)
    {
        return Ext4Helper.queryOne(test, "field[fieldLabel='" + label + "']", Ext4FieldRef.class);
    }

    public String setValue(String val)
    {
        return eval("this.setValue(\"" + val + "\")");
    }

    public String getValue()
    {
        return eval("this.getValue()");
    }

}
