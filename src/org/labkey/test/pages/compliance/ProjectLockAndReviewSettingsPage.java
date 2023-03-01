package org.labkey.test.pages.compliance;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProjectLockAndReviewSettingsPage extends BaseComplianceSettingsPage
{
    public ProjectLockAndReviewSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ProjectLockAndReviewSettingsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("compliance", null, "complianceSettings",
                Map.of("tab", "projectLockAndReview")));
        return new ProjectLockAndReviewSettingsPage(webDriverWrapper.getDriver());
    }

    public ProjectLockAndReviewSettingsPage setAllowProjectLocking(boolean checked)
    {
        elementCache().projectLockCheckbox().set(checked);
        return this;
    }

    public boolean getAllowProjectLocking()
    {
        return elementCache().projectLockCheckbox().get();
    }

    public List<String> getNonExcludedProjects()
    {
        return getTexts(elementCache().nonExcludedProjectsSelect.getOptions());
    }

    public List<String> getExcludedProjects()
    {
        return getTexts(elementCache().excludedProjectsSelect.getOptions());
    }

    public ProjectLockAndReviewSettingsPage excludeProject(String project)
    {
        assertThat("Expect to see project in non-excluded list", getNonExcludedProjects(), hasItem(project));
        elementCache().nonExcludedProjectsSelect.selectByVisibleText(project);
        elementCache().addRightButton.click();
        waitFor(()-> getExcludedProjects().contains(project),
                "the project was not added to the exclusion list", 2000);
        return this;
    }

    public ProjectLockAndReviewSettingsPage includeProject(String project)
    {
        assertThat("Expect to see project in excluded list", getExcludedProjects(), hasItem(project));
        elementCache().excludedProjectsSelect.selectByVisibleText(project);
        elementCache().addLeftButton.click();
        waitFor(()-> getNonExcludedProjects().contains(project),
                "the project was not added to the non-exclusion list", 2000);
        return this;
    }

    public ProjectLockAndReviewSettingsPage setEnableProjectReviewWorkflow(boolean checked)
    {
        elementCache().enableWorkflowCheckBox.set(checked);
        return this;
    }

    public Checkbox getEnableProjectReviewCheckbox()
    {
        return elementCache().enableWorkflowCheckBox;
    }

    public boolean getEnableProjectReviewWorkflowCheckedState()
    {
        return elementCache().enableWorkflowCheckBox.get();
    }

    public ProjectLockAndReviewSettingsPage setExpirationInterval(String months)
    {
        elementCache().intervalCombo.selectComboBoxItem(months);
        return this;
    }

    public String getExpirationInterval()
    {
        return elementCache().intervalCombo.getValue();
    }

    /**
     *
     * @param days the number of days at which to begin sending warning messages
     * @return
     */
    public ProjectLockAndReviewSettingsPage setExpirationWarning(String days)
    {
        elementCache().beginWarningEmailInput.setValue(days);
        return this;
    }

    public String getExpirationWarning()
    {
        return elementCache().beginWarningEmailInput.get();
    }

    public ProjectLockAndReviewSettingsPage setExpirationWarningInterval(String days)
    {
        elementCache().warningFrequencyInput.setValue(days);
        return this;
    }

    public String getProjectReviewMessage()
    {
        return elementCache().projectReviewMessage.getValue();
    }

    public ProjectLockAndReviewSettingsPage setProjectReviewMessage(String message)
    {
        elementCache().projectReviewMessage.set(message);
        return this;
    }

    public ProjectLockAndReviewSettingsPage clickSave()
    {
        clickButton("Save");
        return this;
    }

    public ProjectLockAndReviewSettingsPage clickCancel()
    {
        clickButton("Cancel");
        return this;
    }

    @Override
    protected BaseComplianceSettingsPage.ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends BaseComplianceSettingsPage.ElementCache
    {
        Checkbox projectLockCheckbox()
        {
            return new Checkbox(Locator.checkboxById("projectLockingAllowed").findElement(getDriver()));
        }
        Checkbox enableWorkflowCheckBox = new Checkbox(Locator.checkboxById("projectReviewEnabled").findWhenNeeded(getDriver()));

        WebElement addLeftButton = Locator.id("addLeft").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        WebElement addRightButton = Locator.id("addRight").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        Select nonExcludedProjectsSelect = new Select(Locator.id("nonExcludedProjects").findWhenNeeded(getDriver()));
        Select excludedProjectsSelect = new Select(Locator.id("excludedProjects").findWhenNeeded(getDriver()));

        ComboBox intervalCombo = new ComboBox.ComboBoxFinder(getDriver()).locatedBy(Locator.id("expirationInterval"))
                .findWhenNeeded(getDriver());
        Input beginWarningEmailInput = Input.Input(Locator.inputById("beginWarningEmailDays"), getDriver()).findWhenNeeded();
        Input warningFrequencyInput = Input.Input(Locator.inputById("warningEmailsFrequency"), getDriver()).findWhenNeeded();
        Input projectReviewMessage = Input.Input(Locator.id("projectReviewMessage"), getDriver()).findWhenNeeded();
    }
}
