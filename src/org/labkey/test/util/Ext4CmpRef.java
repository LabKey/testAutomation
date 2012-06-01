package org.labkey.test.util;

import org.labkey.test.BaseSeleniumWebTest;

import java.util.List;

/**
* Created by IntelliJ IDEA.
* User: markigra
* Date: 5/31/12
* Time: 10:43 PM
* To change this template use File | Settings | File Templates.
*/
public class Ext4CmpRef
{
    private String _id;
    private BaseSeleniumWebTest _test;

    Ext4CmpRef(String id, BaseSeleniumWebTest test)
    {
        this._id = id;
        this._test = test;
    }

    public String getId()
    {
        return _id;
    }

    public List<Ext4CmpRef> query(String selector)
    {
        String res = _test.getWrapper().getEval("selenium.ext4ComponentQuery('" + selector + "', '" + _id + "')");
        return Ext4Helper.componentsFromJson(_test, res);
    }

    public String eval(String expr)
    {
        return _test.getWrapper().getEval("selenium.ext4ComponentEval('" + _id + "', '" + expr + "')");
    }
}
