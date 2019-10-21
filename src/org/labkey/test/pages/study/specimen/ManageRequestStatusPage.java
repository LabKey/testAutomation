package org.labkey.test.pages.study.specimen;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import static org.labkey.test.components.html.Input.Input;
import static org.labkey.test.components.html.SelectWrapper.Select;

public class ManageRequestStatusPage extends LabKeyPage<ManageRequestStatusPage.ElementCache>
{
    public ManageRequestStatusPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageRequestStatusPage beginAt(WebDriverWrapper webDriverWrapper, int id)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath(), id);
    }

    public static ManageRequestStatusPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, int id)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("study-samples", containerPath, "manageRequestStatus", Maps.of("id", String.valueOf(id))));
        return new ManageRequestStatusPage(webDriverWrapper.getDriver());
    }

    public ManageRequestStatusPage setStatus(String status)
    {
        elementCache().statusSelect.selectByVisibleText(status);
        return this;
    }

    public String getStatus()
    {
        return elementCache().statusSelect.getFirstSelectedOption().getText();
    }

    public ManageRequestPage clickSave()
    {
        clickAndWait(elementCache().saveButton);

        return new ManageRequestPage(getDriver());
    }

    public ManageRequestPage clickCancel()
    {
        clickAndWait(elementCache().cancelButton);

        return new ManageRequestPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        private final Input descriptionTextArea = Input(Locator.name("requestDescription"), getDriver()).findWhenNeeded(this);
        private final Select statusSelect = Select(Locator.name("status")).findWhenNeeded(this);
        private final Input commentsTextArea = Input(Locator.name("requestComments"), getDriver()).findWhenNeeded(this);

        // TODO: Supporting Documents

        private final WebElement saveButton = Locator.lkButton("Save Changes and Send Notifications").findWhenNeeded(this);
        private final WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}
