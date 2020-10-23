package org.labkey.test.pages.study.specimen;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * This wraps the page served by 'org.labkey.query.reports.ReportsController.ManageNotificationsAction'
 * Page layout is defined in '/org/labkey/query/reports/view/manageNotifications.jsp'
 */
public class ManageNotificationsPage extends LabKeyPage<ManageNotificationsPage.ElementCache>
{
    public ManageNotificationsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManageNotificationsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static ManageNotificationsPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("study-samples", containerPath, "manageNotifications"));
        return new ManageNotificationsPage(webDriverWrapper.getDriver());
    }

    public ManageNotificationsPage setReplyTo(@Nullable String email)
    {
        if (email == null)
        {
            elementCache().replyToAdminRadio.check();
        }
        else
        {
            elementCache().replyToUserRadio.check();
            elementCache().replyToUserInput.set(email);
        }
        return this;
    }

    public ManageNotificationsPage setSubjectSuffix(String suffix)
    {
        elementCache().subjectSuffixInput.set(suffix);
        return this;
    }

    public ManageNotificationsPage setNotificationUsers(@Nullable List<String> users)
    {
        if (users == null)
        {
            elementCache().newRequestNotifyCheckbox.uncheck();
        }
        else
        {
            elementCache().newRequestNotifyCheckbox.check();
            shortWait().until(ExpectedConditions.visibilityOf(elementCache().newRequestNotifyInput.getComponentElement()));
            elementCache().newRequestNotifyInput.set(String.join("\n", users));
        }
        return this;
    }

    public ManageNotificationsPage setCcUsers(@Nullable List<String> users)
    {
        if (users == null)
        {
            elementCache().ccCheckbox.uncheck();
        }
        else
        {
            elementCache().ccCheckbox.check();
            shortWait().until(ExpectedConditions.visibilityOf(elementCache().ccInput.getComponentElement()));
            elementCache().ccInput.set(String.join("\n", users));
        }
        return this;
    }

    public ManageNotificationsPage setDefaultEmailNotification(DefaultEmailNotify option)
    {
        scrollIntoView(elementCache().saveButton); // Make sure radio buttons are fully in view
        elementCache().defaultEmailNotifyRadio(option).check();
        return this;
    }

    public ManageNotificationsPage setSpecimenAttachmentType(SpecimensAttachment option)
    {
        scrollIntoView(elementCache().saveButton); // Make sure radio buttons are fully in view
        elementCache().specimensAttachmentRadio(option).check();
        return this;
    }

    public void clickSave()
    {
        clickAndWait(elementCache().saveButton);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        private final RadioButton replyToAdminRadio = RadioButton.finder().withNameAndValue("replyToCurrentUser", "true").findWhenNeeded(this);
        private final RadioButton replyToUserRadio = RadioButton.finder().withNameAndValue("replyToCurrentUser", "false").findWhenNeeded(this);
        private final Input replyToUserInput = Input.Input(Locator.id("replyTo"), getDriver()).findWhenNeeded(this);

        private final Input subjectSuffixInput = Input.Input(Locator.name("subjectSuffix"), getDriver()).findWhenNeeded(this);

        private final Checkbox newRequestNotifyCheckbox = Checkbox.Checkbox(Locator.id("newRequestNotifyCheckbox")).findWhenNeeded(this);
        private final Input newRequestNotifyInput = Input.Input(Locator.name("newRequestNotify"), getDriver()).findWhenNeeded(this);

        private final Checkbox ccCheckbox = Checkbox.Checkbox(Locator.id("ccCheckbox")).findWhenNeeded(this);
        private final Input ccInput = Input.Input(Locator.name("cc"), getDriver()).findWhenNeeded(this);

        RadioButton defaultEmailNotifyRadio(DefaultEmailNotify option)
        {
            return RadioButton.finder().withNameAndValue("defaultEmailNotify", option.getValue()).find(this);
        }

        RadioButton specimensAttachmentRadio(SpecimensAttachment option)
        {
            return RadioButton.finder().withNameAndValue("specimensAttachment", option.getValue()).find(this);
        }

        private final WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(this);
    }

    public enum DefaultEmailNotify
    {
        ALL("All"),
        NONE("None"),
        INVOLVED("ActorsInvolved");

        private final String value;

        DefaultEmailNotify(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }
    }

    public enum SpecimensAttachment
    {
        BODY("InEmailBody"),
        EXCEL("ExcelAttachment"),
        TEXT("TextAttachment"),
        NONE("Never");

        private final String value;

        SpecimensAttachment(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }
    }
}
