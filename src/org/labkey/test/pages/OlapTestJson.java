/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.test.pages;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import static org.junit.Assert.assertEquals;

/**
 * Created by cnathe on 1/30/2015.
 */
public class OlapTestJson
{
    private BaseWebDriverTest _test;

    public OlapTestJson(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void goToPage(String path, String configId, String schemaName, String cubeName)
    {
        _test.beginAt("/olap/" + path + "/testJson.view?configId=" + configId + "&schemaName=" + schemaName + "&cubeName=" + cubeName);
    }

    public void submitQueryAsJson(String query)
    {
        clear();
        setType("json");
        setQuery(query);
        _test.sleep(1000); // TODO: can't repro TeamCity failure locally. Seems like it is clicking submit before the query text has been set.
        submit();

        Locator.XPathLocator table = Locator.tagWithClass("table", "labkey-data-region");
        _test.waitForElement(table);
    }

    public void compareResults(String[][] results)
    {
        Locator.XPathLocator table = Locator.tagWithClass("table", "labkey-data-region");

        for (int i = 0; i < results.length; i++)
        {
            for (int j = 0; j < results[i].length; j++)
            {
                String expected = results[i][j];
                String actual = _test.getTableCellText(table, i, j);
                if (StringUtils.equals(expected,actual))
                    continue;
                try
                {
                    if (Double.parseDouble(expected) == Double.parseDouble(actual))
                        continue;
                }
                catch (NumberFormatException ignore){}
                assertEquals(expected,actual);
            }
        }
    }

    private void setType(String value)
    {
        _test.selectOptionByValue(Locator.name("type"), value);
    }

    private void setQuery(String query)
    {
        _test.setFormElement(Locator.name("query"), query);
    }

    private void submit()
    {
        _test.click(Locator.inputById("submitbtn"));
    }

    private void clear()
    {
        _test.click(Locator.inputById("clearbtn"));
        _test.waitForElementToDisappear(Locator.tagWithClass("table", "labkey-data-region"));
    }
}
