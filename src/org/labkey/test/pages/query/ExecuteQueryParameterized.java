package org.labkey.test.pages.query;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class ExecuteQueryParameterized extends LabKeyPage
{
    private final Elements _elements;

    public ExecuteQueryParameterized(BaseWebDriverTest test)
    {
        super(test);
        _elements = new Elements();
    }

    public ExecuteQueryParameterized setParameters(Map<String, String> data)
    {
        for(String key : data.keySet())
        {
            _test.setFormElement(elements().findInputField(key), data.get(key));
        }
        return this;
    }

    public DataRegionTable submit()
    {
        _test.clickAndWait(elements().submitButton);
        return new DataRegionTable("query", _test);
    }

    private Elements elements()
    {
        return _elements;
    }

    private class Elements extends ComponentElements
    {
        protected Elements()
        {
            super(_test.getDriver());
        }

        WebElement findInputField(String fieldKey)
        {
            return Locator.tag("input").attributeEndsWith("name", ".props." + fieldKey).findElement(context);
        }
        WebElement submitButton = new LazyWebElement(Locator.button("Submit"), context);
    }
}