package org.labkey.test.pages.reports;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.CodeMirrorHelper;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Map;

import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;
import static org.labkey.test.components.ext4.RadioButton.RadioButton;

/**
 * Wraps `CreateScriptReportAction` aka `AjaxScriptReportView`
 */
public class ScriptReportPage extends LabKeyPage<ScriptReportPage.ElementCache>
{
    public ScriptReportPage(WebDriver driver)
    {
        super(driver);
    }

    public static ScriptReportPage beginAtCreateReport(WebDriverWrapper webDriverWrapper, String containerPath, String reportType)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("reports", containerPath, "createScriptReport", Map.of("reportType", reportType)));
        return new ScriptReportPage(webDriverWrapper.getDriver());
    }

    public static ScriptReportPage beginAtReport(WebDriverWrapper webDriverWrapper, String containerPath, String reportId)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("reports", containerPath, "runReport", Map.of("reportId", reportId)));
        return new ScriptReportPage(webDriverWrapper.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        String activeTab = Locator.byClass("x4-tab-top-active").waitForElement(getDriver(), 10_000).getText();
        switch (activeTab)
        {
            case "Report" ->
                    waitForElement(Locator.tagWithClass("div", "reportView").notHidden().withPredicate("not(ancestor-or-self::*[contains(@class,'mask')])"), BaseWebDriverTest.WAIT_FOR_PAGE);
            case "Source" ->
                    shortWait().until(ExpectedConditions.visibilityOfElementLocated(Locator.byClass("reportSource")));
        }
    }

    public ScriptReportPage clickSourceTab()
    {
        waitAndClick(Ext4Helper.Locators.tab("Source"));
        waitForElement(Locator.tagWithClass("div", "reportSource").notHidden(), WAIT_FOR_PAGE);
        return this;
    }

    public ScriptReportPage setReportSource(String source)
    {
        clickSourceTab();
        getEditor().setCodeMirrorValue(source);
        return this;
    }

    @NotNull
    public CodeMirrorHelper getEditor()
    {
        return new CodeMirrorHelper(this, "script-report-editor");
    }

    public String saveReport(String name, boolean isSaveAs, int wait)
    {
        WebElement saveButton = Ext4Helper.Locators.ext4Button(isSaveAs ? "Save As" : "Save").findElement(getDriver());
        scrollIntoView(saveButton, true);
        clickAndWait(saveButton, wait);
        if (null != name)
        {
            saveReportWithName(name, isSaveAs);
        }
        return getUrlParam("reportId");
    }

    /**
     * Precondition: on save popup window
     */
    public void saveReportWithName(String name, boolean isSaveAs)
    {
        saveReportWithName(name, isSaveAs, false);
    }

    public void saveReportWithName(String name, boolean isSaveAs, boolean isExternal)
    {
        String windowTitle;
        if (isExternal)
        {
            windowTitle = "Create New Report";
        }
        else
        {
            windowTitle = isSaveAs ? "Save Report As" : "Save Report";
        }

        Window<?> window = new Window<>(windowTitle, getWrappedDriver());
        WebElement nameInput = Locator.xpath("//input[contains(@class, 'x4-form-field')]").findElement(window);
        setFormElement(nameInput, name);
        waitForFormElementToEqual(nameInput, name); // Make sure it sticks
        if (isExternal)
            window.clickButton("OK", 0);
        else
            window.clickButton("OK");
    }

    public String saveReport(String name)
    {
        return saveReport(name, false, 0);
    }

    public String saveAsReport(String name)
    {
        return saveReport(name, true, 0);
    }

    public void selectOption(ReportOption option)
    {
        _selectOption(option, true);
    }

    public void clearOption(ReportOption option)
    {
        _selectOption(option, false);
    }

    private void _selectOption(ReportOption option, boolean checked)
    {
        ensureFieldSetExpanded(option.getSection());
        Checkbox checkbox;
        if (option.isCheckbox())
        {
            checkbox = Ext4Checkbox().withLabel(option.getLabel()).waitFor(getDriver());
        }
        else
        {
            if (!checked)
                throw new IllegalArgumentException("Can't uncheck a radio button");
            checkbox = RadioButton().withLabel(option.getLabel()).waitFor(getDriver());
        }
        checkbox.set(checked);
        waitFor(() -> checkbox.isChecked() == checked, 1000);
    }

    public void ensureFieldSetExpanded(String name)
    {
        if (name != null)
        {
            Locator fieldSet = Locator.xpath("//fieldset").withClass("x4-fieldset-collapsed").withDescendant(Locator.xpath("//div").withClass("x4-fieldset-header-text").containing(name)).append("//div/img");

            if (isElementPresent(fieldSet))
            {
                click(fieldSet);
            }
        }
    }

    public ScriptReportPage clickReportTab()
    {
        waitAndClick(Ext4Helper.Locators.tab("Report"));
        // Report view should appear quickly
        shortWait().until(ExpectedConditions.visibilityOfElementLocated(Locator.tagWithClass("div", "reportView")));
        // Actual report might take a while to load
        _ext4Helper.waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_PAGE);
        return this;
    }

    public WebElement findReportElement()
    {
        clickReportTab();
        return Locator.byClass("reportView").findElement(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
    }

    public interface ReportOption
    {
        String getLabel();
        String getSection();
        boolean isCheckbox();
    }
    
    public enum StandardReportOption implements ReportOption
    {
        shareReport("Make this report available to all users", null, true),
        showSourceTab("Show source tab to all users", null, true),
        runInPipeline("Run this report in the background as a pipeline job", null, true),
        ;

        final String _label;
        final boolean _isCheckbox;
        final String _fieldSet;

        StandardReportOption(String label, String section, boolean isCheckbox)
        {
            _label = label;
            _fieldSet = section;
            _isCheckbox = isCheckbox;
        }

        @Override
        public String getLabel()
        {
            return _label;
        }

        @Override
        public boolean isCheckbox()
        {
            return _isCheckbox;
        }

        @Override
        public String getSection()
        {
            return _fieldSet;
        }
    }
}
