package org.labkey.test.pages;

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
        _test.sleep(1000); // TODO: can't repro TeamCity failure locally. Seems like it is clicking submit befor the query text has been pasted?
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
                assertEquals(results[i][j], _test.getTableCellText(table, i, j));
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
