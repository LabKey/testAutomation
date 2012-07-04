package org.labkey.test.util.ext4cmp;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;

/**
 * User: elvan
 * Date: 6/28/12
 * Time: 6:20 PM
 */
public class Ext4FileFieldRef extends Ext4CmpRef
{
    public Ext4FileFieldRef(String id, BaseSeleniumWebTest test)
    {
        super(id, test);
    }

    //often there is only one file field on screen, so we'll just grab that
    public static Ext4FileFieldRef create(BaseSeleniumWebTest test)
    {
        return Ext4Helper.queryOne(test, "filefield", Ext4FileFieldRef.class);
    }

    public void setToFile(String file)
    {
        _test.setFormElement(Locator.id(eval("this.fileInputEl.dom.id")), file);
        this.eval("this.onFileChange()");
    }
}
