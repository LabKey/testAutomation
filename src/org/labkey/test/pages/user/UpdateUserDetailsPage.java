package org.labkey.test.pages.user;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

public class UpdateUserDetailsPage extends LabKeyPage<UpdateUserDetailsPage.ElementCache>
{
    public UpdateUserDetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public static UpdateUserDetailsPage beginAt(WebDriverWrapper webDriverWrapper, Integer userId)
    {
        Map<String, String> params = Map.of("userId", userId.toString(), "schemaName", "core", "query.queryName", "SiteUsers");
        webDriverWrapper.beginAt(WebTestHelper.buildURL("user", "showUpdate", params));
        return new UpdateUserDetailsPage(webDriverWrapper.getDriver());
    }

    public UpdateUserDetailsPage setDisplayName(String value)
    {
        return setField("DisplayName", value);
    }

    @Override
    public String getDisplayName()
    {
        return getField("DisplayName");
    }

    public UpdateUserDetailsPage setFirstName(String value)
    {
        return setField("FirstName", value);
    }

    public String getFirstName()
    {
        return getField("FirstName");
    }

    public UpdateUserDetailsPage setLastName(String value)
    {
        return setField("LastName", value);
    }

    public String getLastName()
    {
        return getField("LastName");
    }

    public UpdateUserDetailsPage setPhone(String value)
    {
        return setField("Phone", value);
    }

    public String getPhone()
    {
        return getField("Phone");
    }

    public UpdateUserDetailsPage setMobile(String value)
    {
        return setField("Mobile", value);
    }

    public String getMobile()
    {
        return getField("Mobile");
    }

    public UpdateUserDetailsPage setField(String fieldName, String value)
    {
        elementCache().findInput(fieldName).set(value);
        return this;
    }

    public String getField(String fieldName)
    {
        return elementCache().findInput(fieldName).get();
    }

    public void clickSubmit()
    {
        clickAndWait(elementCache().submitButton);
        assertNoLabKeyErrors();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement submitButton = Locator.lkButton("Submit").findWhenNeeded(this);

        Map<String, Input> formElements = new HashMap<>();

        protected Input findInput(String fieldName)
        {
            if (!formElements.containsKey(fieldName))
            {
                Input input = Input.Input(Locator.name("quf_" + fieldName), getDriver()).find();
                formElements.put(fieldName, input);
            }
            return formElements.get(fieldName);
        }
    }
}
