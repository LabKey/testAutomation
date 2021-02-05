package org.labkey.test.pages.admin;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.components.labkey.LabKeyAlert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Test wrapper for 'AdminController.RConfigurationAction
 */
public class RConfigurationPage extends FolderManagementPage
{
    public RConfigurationPage(WebDriver driver)
    {
        super(driver);
    }

    public static RConfigurationPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static RConfigurationPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("admin", containerPath, "rConfiguration"));
        return new RConfigurationPage(webDriverWrapper.getDriver());
    }

    public FolderRConfig getRConfig()
    {
        if (!isConfigInherited())
        {
            return new FolderRConfig(false,
                elementCache().reportEngineSelect.getFirstSelectedOption().getText(),
                elementCache().pipelineEngineSelect.getFirstSelectedOption().getText()
            );
        }
        else
        {
            String parentLabel = elementCache().parentLabel.getText();
            String[] parentConfigs = parentLabel.split("\n");
            String parentReportEngine = parentConfigs[0].replace("Reports : ", "");
            String parentPipelineEngine = parentConfigs[1].replace("Pipeline Jobs : ", "");
            return new FolderRConfig(true, parentReportEngine, parentPipelineEngine);
        }
    }

    public RConfigurationPage setInheritConfiguration()
    {
        elementCache().parentDefaultRadio.check();
        return this;
    }

    public boolean isConfigInherited()
    {
        return elementCache().parentDefaultRadio.isChecked();
    }

    public RConfigurationPage setEngineOverrides(String reportEngine, String pipelineEngine)
    {
        elementCache().overrideDefaultRadio.check();
        elementCache().reportEngineSelect.selectByVisibleText(reportEngine);
        elementCache().pipelineEngineSelect.selectByVisibleText(pipelineEngine);
        return this;
    }

    public boolean isSaveEnabled()
    {
        return !elementCache().saveButton.getAttribute("class").contains("disabled");
    }

    public void save()
    {
        Assert.assertTrue("Save button is not enabled", isSaveEnabled());
        elementCache().saveButton.click();
        LabKeyAlert confirmation = LabKeyAlert.getFinder(getDriver()).waitFor();
        Assert.assertEquals("Unexpected alert title.", "Override Default R Configuration", confirmation.getTitle());
        confirmation.clickButton("Yes");

    }

    public List<String> getReportEngineOptions()
    {
        return getEnabledOptions(elementCache().reportEngineSelect.getOptions());
    }

    public List<String> getPipelineEngineOptions()
    {
        return getEnabledOptions(elementCache().pipelineEngineSelect.getOptions());
    }

    @NotNull
    private List<String> getEnabledOptions(List<WebElement> options)
    {
        return options.stream()
            .filter(el -> el.getAttribute("disabled") == null) // 'isEnabled' is false if parent <select> is disabled
            .map(WebElement::getText)
            .collect(Collectors.toList());
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends FolderManagementPage.ElementCache
    {
        final WebElement parentLabel = Locator.id("parentConfigLabel").findWhenNeeded(this);
        final RadioButton parentDefaultRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("overrideDefault", "parent")).findWhenNeeded(this);
        final RadioButton overrideDefaultRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("overrideDefault", "override")).findWhenNeeded(this);
        final Select reportEngineSelect = SelectWrapper.Select(Locator.name("reportEngine")).findWhenNeeded(this);
        final Select pipelineEngineSelect = SelectWrapper.Select(Locator.name("pipelineEngine")).findWhenNeeded(this);

        protected final WebElement saveButton = Locator.id("saveBtn").findWhenNeeded(this);
    }

    public static class FolderRConfig
    {
        private final boolean _inherited;
        private final String _reportEngine;
        private final String _pipelineEngine;

        public FolderRConfig(boolean inherited, String reportEngine, String pipelineEngine)
        {
            _inherited = inherited;
            _reportEngine = reportEngine;
            _pipelineEngine = pipelineEngine;
        }

        public boolean isInherited()
        {
            return _inherited;
        }

        public String getReportEngine()
        {
            return _reportEngine;
        }

        public String getPipelineEngine()
        {
            return _pipelineEngine;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            FolderRConfig that = (FolderRConfig) o;
            return isInherited() == that.isInherited() &&
                getReportEngine().equals(that.getReportEngine()) &&
                getPipelineEngine().equals(that.getPipelineEngine());
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(isInherited(), getReportEngine(), getPipelineEngine());
        }

        @Override
        public String toString()
        {
            return "FolderRConfig{" + "_usesParent=" + _inherited + ", _reportEngine='" + _reportEngine + '\'' + ", _pipelineEngine='" + _pipelineEngine + '\'' + '}';
        }
    }
}
