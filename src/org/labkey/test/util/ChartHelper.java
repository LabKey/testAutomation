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
package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import java.util.Map;

/**
 * User: elvan
 * Date: 2/11/13
 * Time: 3:06 PM
 */
public class ChartHelper  extends AbstractHelperWD
{

    public ChartHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    /**
     * on a page with a drt, clicks the (row)th edit button and set the named
     *
     */
    public void editDrtRow(int row, Map<String, String> nameAndValue)
    {
        _test.clickAndWait(Locator.linkWithText("edit", row));
//        _test.waitForElement(Locator.name(nameAndValue.keySet().));
        for(String name : nameAndValue.keySet())
            _test.setFormElement(Locator.xpath("//tr[td[contains(text(),'" + name + "')]]/td//input"), nameAndValue.get(name));
        _test.clickButton("Submit");
    }
}
