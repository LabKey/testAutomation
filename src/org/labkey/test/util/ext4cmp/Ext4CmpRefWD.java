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

import org.labkey.test.BaseWebDriverTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
* User: markigra
* Date: 5/31/12
* Time: 10:43 PM
*/
public class Ext4CmpRefWD
{
    protected String _id;
    protected BaseWebDriverTest _test;
    protected WebElement _el;

    public Ext4CmpRefWD(String id, BaseWebDriverTest test)
    {
        this._id = id;
        this._test = test;
        this._el = test._driver.findElement(By.id(id));
    }

    public Ext4CmpRefWD(WebElement el, BaseWebDriverTest test)
    {
        this._id = el.getAttribute("id");
        this._test = test;
        this._el = el;
    }

    public String getId()
    {
        return _id;
    }

    public List<Ext4CmpRefWD> query(String selector)
    {
        return _test._ext4Helper.componentQuery(selector, _id, Ext4CmpRefWD.class);
    }

    public void eval(String expr, Object... args)
    {
        String script = "Ext4.getCmp('"+_id+"')." + expr + ";";
        _test.executeScript(script, args);
    }

    public Object getEval(String expr, Object... args)
    {
        String script = "return Ext4.getCmp('"+_id+"')." + expr + ";";
        return _test.executeScript(script, args);
    }
}
