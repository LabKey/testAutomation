package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class DatasetInsertPage extends InsertPage
{
    public DatasetInsertPage(BaseWebDriverTest test, String datasetName)
    {
        super(test, "Insert new entry: " + datasetName);
    }

    protected void waitForReady()
    {
        _test.waitForElement(elements().title.withText(_title));
        _test.waitForElement(Locator.tag("*").attributeStartsWith("name", "quf_"));
    }

    public void insert(Map<String, String> values)
    {
        for (Map.Entry<String, String> entry : values.entrySet())
        {
            WebElement fieldInput = Locator.name("quf_" + entry.getKey()).findElement(_test.getDriver());
            String type = fieldInput.getAttribute("type");
            switch (type)
            {
                case "text":
                    _test.setFormElement(fieldInput, entry.getValue());
                    break;
                case "checkbox":
                    if (Boolean.valueOf(entry.getValue()))
                        _test.checkCheckbox(fieldInput);
                    else
                        _test.uncheckCheckbox(fieldInput);
                    break;
                default:
                    String tag = fieldInput.getTagName();
                    switch (tag)
                    {
                        case "textarea":
                            _test.setFormElementJS(fieldInput, entry.getValue());
                            break;
                        case "select":
                            _test.selectOptionByText(fieldInput, entry.getValue());
                            break;
                        default:
                            throw new IllegalArgumentException("Update " + getClass().getSimpleName() + "#insert() to support field: " + entry.getKey() + ", tag = " + tag + ", type = " + type);
                    }
            }
        }

        _test.clickButton("Submit");
    }
}
