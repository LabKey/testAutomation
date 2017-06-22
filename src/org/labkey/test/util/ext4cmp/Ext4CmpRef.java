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

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

public class Ext4CmpRef
{
    protected String _id;
    protected WebDriverWrapper _test;
    protected WebElement _el;

    public Ext4CmpRef(String id, WebDriverWrapper test)
    {
        this._id = id;
        this._test = test;
        this._el = test.getDriver().findElement(By.id(id));
    }

    public Ext4CmpRef(WebElement el, WebDriverWrapper test)
    {
        this._id = el.getAttribute("id");
        this._test = test;
        this._el = el;
    }

    public String getId()
    {
        return _id;
    }

    public List<Ext4CmpRef> query(String selector)
    {
        return _test._ext4Helper.componentQuery(selector, _id, Ext4CmpRef.class);
    }

    public void eval(String expr, Object... args)
    {
        String script = "var cmp = Ext4.getCmp('" + _id + "');" +
                        "if (!cmp) cmp = Ext4.ComponentQuery.query('#" + _id + "')[0];" +
                        "cmp." + expr + ";";
        _test.executeScript(script, args);
    }

    public Object getEval(String expr, Object... args)
    {
        String script = "var cmp = Ext4.getCmp('" + _id + "');" +
                "if (!cmp) cmp = Ext4.ComponentQuery.query('#" + _id + "')[0];" +
                "return cmp." + expr + ";";
        return _test.executeScript(script, args);
    }

    public Object getFnEval(String expr, Object... args)
    {
        String script = "var cmp = Ext4.getCmp('"+_id+"');" +
                        "if (!cmp) cmp = Ext4.ComponentQuery.query('#" + _id + "')[0];" +
                        "return (function(){" + expr + "}).apply(cmp, arguments);";
        return _test.executeScript(script, args);
    }

    public void waitForEnabled()
    {
        _test.waitFor(() -> (Boolean)getFnEval("return !this.isDisabled();"),
                "Component was not enabled", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod(quiet = true)
    public static void waitForComponent(final WebDriverWrapper test, @LoggedParam final String query)
    {
        test.waitFor(() -> (Boolean)test.executeScript("return !!Ext4.ComponentQuery.query(\"" + query + "\").length;"),
                "Component did not appear: " + query, WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public <Type extends Ext4CmpRef> Type down(String componentSelector, Class<Type> clazz)
    {
        componentSelector = componentSelector.replaceAll("'", "\"");  //escape single quotes
        String id = (String)getEval("down(arguments[0]).id", componentSelector);
        List<Type> ret = _test._ext4Helper.componentsFromIds(Arrays.asList(id), clazz);

        return ret.isEmpty() ? null : ret.get(0);
    }
}
