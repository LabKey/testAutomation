package org.labkey.test.pages.query;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.EscapeUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
Ï
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UpdateQueryRowPage extends LabKeyPage<UpdateQueryRowPage.ElementCache>
{
    public UpdateQueryRowPage(WebDriver driver)
    {
        super(driver);
    }

    public static UpdateQueryRowPage beginAt(WebDriverWrapper webDriverWrapper, String schemaName, String queryName, int rowId)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath(), schemaName, queryName, rowId);
    }

    public static UpdateQueryRowPage beginAt(WebDriverWrapper webDriverWrapper, String schemaName, String queryName, Pair<String, String> rowKey)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath(), schemaName, queryName, rowKey);
    }

    public static UpdateQueryRowPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, String schemaName, String queryName, int rowId)
    {
        return beginAt(webDriverWrapper, containerPath, schemaName, queryName, Pair.of("rowId", String.valueOf(rowId)));
    }

    public static UpdateQueryRowPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, String schemaName, String queryName, Pair<String, String> rowKey)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("query", containerPath, "updateQueryRow",
                Map.of("schemaName", schemaName, "query.queryName", queryName, rowKey.getKey(), rowKey.getValue())));
        return new UpdateQueryRowPage(webDriverWrapper.getDriver());
    }

    public static UpdateQueryRowPage beginAtInsertRowPage(WebDriverWrapper webDriverWrapper, String containerPath, String schemaName, String queryName)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("query", containerPath, "insertQueryRow",
            Map.of("schemaName", schemaName, "query.queryName", queryName)));
        return new UpdateQueryRowPage(webDriverWrapper.getDriver());
    }

    public void update(Map<String, ?> fields)
    {
        setFields(fields);
        submit();
    }

    public void setFields(Map<String, ?> fields)
    {
        for (Map.Entry<String, ?> entry : fields.entrySet())
        {
            Object value = entry.getValue();
            if (value instanceof String s)
            {
                setField(entry.getKey(), s);
            }
            else if (value instanceof Boolean b)
            {
                setField(entry.getKey(), b);
            }
            else if (value instanceof Integer i)
            {
                setField(entry.getKey(), i);
            }
            else if (value instanceof Long l)
            {
                setField(entry.getKey(), l);
            }
            else if (value instanceof File f)
            {
                setField(entry.getKey(), f);
            }
            else
            {
                throw new IllegalArgumentException("Unsupported value type for '" + entry.getKey() + "': " + value.getClass().getName());
            }
        }
    }

    public UpdateQueryRowPage setField(String fieldName, String value)
    {
        WebElement field = elementCache().findField(fieldName);
        if (field.getTagName().equals("select"))
        {
            selectOptionByText(field, value);
        }
        else
        {
            setFormElement(field, value);
        }
        return this;
    }

    public UpdateQueryRowPage setField(String fieldName, Boolean value)
    {
        new Checkbox(elementCache().findField(fieldName)).set(value);
        return this;
    }

    public UpdateQueryRowPage setField(String fieldName, Integer value)
    {
        return setField(fieldName, String.valueOf(value));
    }

    public UpdateQueryRowPage setField(String fieldName, Long value)
    {
        return setField(fieldName, String.valueOf(value));
    }

    public UpdateQueryRowPage setField(String fieldName, File file)
    {
        setFormElement(elementCache().findField(fieldName), file);
        return this;
    }

    public UpdateQueryRowPage setField(String fieldName, OptionSelect.SelectOption option)
    {
        new OptionSelect<>(elementCache().findField(fieldName)).selectOption(option);
        return this;
    }

    public String getTextInputValue(String fieldName)
    {
        var input = new Input(elementCache().findField(fieldName), getDriver());
        return input.getValue();
    }

    public void submit()
    {
        clickAndWait(elementCache().submitButton);
    }

    public UpdateQueryRowPage submitExpectingErrorContaining(String... errors)
    {
        String actualError = submitExpectingError();
        Assertions.assertThat(actualError).as("Error message saving data.").contains(errors);
        return this;
    }

    public String submitExpectingError()
    {
        submit();
        clearCache();
        return waitForErrors();
    }

    private String waitForErrors()
    {
        Mutable<String> error = new MutableObject<>();
        shortWait().until(wd ->
        {
            error.setValue(String.join("\n", getTexts(Locators.labkeyError.withText().findElements(getDriver()))));
            return !error.getValue().isBlank();
        });
        return error.getValue();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        private final Map<String, WebElement> fieldMap = new HashMap<>();

        WebElement findField(String name)
        {
            if (!fieldMap.containsKey(name))
            {
                fieldMap.put(name, Locator.nameContaining(EscapeUtil.getFormFieldName(name)).findElement(this));
            }
            return fieldMap.get(name);
        }

        final WebElement submitButton = Locator.lkButton("Submit").findWhenNeeded(this);
    }
}
