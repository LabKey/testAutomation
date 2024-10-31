package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.core.admin.BaseSettingsPage;
import org.labkey.test.pages.core.admin.BaseSettingsPage.DATE_FORMAT;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class FolderFormatsPage extends FolderManagementPage
{

    public FolderFormatsPage(WebDriver driver)
    {
        super(driver);
    }

    public static FolderFormatsPage beginAt(WebDriverWrapper wrapper)
    {
        return beginAt(wrapper, wrapper.getCurrentContainerPath());
    }

    public static FolderFormatsPage beginAt(WebDriverWrapper wrapper, String containerPath)
    {
        wrapper.beginAt(WebTestHelper.buildURL("admin", containerPath, "folderSettings"));
        return new FolderFormatsPage(wrapper.getDriver());
    }

    private Boolean getInherited(String name)
    {
        return elementCache().inheritedChk(name).isSelected();
    }

    private void setInherited(String name, boolean enable)
    {
        if (enable)
            checkCheckbox(elementCache().inheritedChk(name));
        else
            uncheckCheckbox(elementCache().inheritedChk(name));
    }

    public boolean getDefaultDateDisplayInherited()
    {
        return getInherited("defaultDateFormatInherited");
    }

    public void setDefaultDateDisplayInherited(boolean enable)
    {
        setInherited("defaultDateFormatInherited", enable);
    }

    public String getDefaultDateDisplay()
    {
        return getSelectedOptionValue(elementCache().defaultDateFormat);
    }

    public void setDefaultDateDisplay(DATE_FORMAT dateFormat)
    {
        selectOptionByValue(elementCache().defaultDateFormat, dateFormat.toString());
    }

    public boolean defaultDateDisplayWarning()
    {
        return elementCache().nonStandardWarning(elementCache().defaultDateFormat).isDisplayed();
    }

    public void setDefaultDateTimeDisplay(BaseSettingsPage.DATE_FORMAT dateFormat, BaseSettingsPage.TIME_FORMAT timeFormat)
    {
        setDefaultDateTimeDateDisplay(dateFormat);
        setDefaultDateTimeTimeDisplay(timeFormat);
    }

    public String getDefaultDateTimeDateDisplay()
    {
        return getSelectedOptionValue(elementCache().defaultDateTimeDateFormat);
    }

    public void setDefaultDateTimeDateDisplay(BaseSettingsPage.DATE_FORMAT dateFormat)
    {
        selectOptionByValue(elementCache().defaultDateTimeDateFormat, dateFormat.toString());
    }

    public boolean defaultDateTimeDateDisplayWarning()
    {
        return elementCache().nonStandardWarning(elementCache().defaultDateTimeDateFormat).isDisplayed();
    }

    public boolean getDefaultTimeDisplayInherited()
    {
        return getInherited("defaultTimeFormatInherited");
    }

    public void setDefaultTimeDisplayInherited(boolean enable)
    {
        setInherited("defaultTimeFormatInherited", enable);
    }

    public String getDefaultDateTimeTimeDisplay()
    {
        return getSelectedOptionValue(elementCache().defaultDateTimeTimeFormat);
    }

    public void setDefaultDateTimeTimeDisplay(BaseSettingsPage.TIME_FORMAT timeFormat)
    {
        selectOptionByValue(elementCache().defaultDateTimeTimeFormat, timeFormat.toString());
    }

    public boolean defaultDateTimeTimeDisplayWarning()
    {
        return elementCache().nonStandardWarning(elementCache().defaultDateTimeTimeFormat).isDisplayed();
    }

    public boolean getDefaultDateTimeDisplayInherited()
    {
        return getInherited("defaultDateTimeFormatInherited");
    }

    public void setDefaultDateTimeDisplayInherited(boolean enable)
    {
        setInherited("defaultDateTimeFormatInherited", enable);
    }

    public String getDefaultTimeDisplay()
    {
        return getSelectedOptionValue(elementCache().defaultTimeFormat);
    }

    public void setDefaultTimeDisplay(BaseSettingsPage.TIME_FORMAT timeFormat)
    {
        selectOptionByValue(elementCache().defaultTimeFormat, timeFormat.toString());
    }

    public boolean defaultTimeDisplayWarning()
    {
        return elementCache().nonStandardWarning(elementCache().defaultTimeFormat).isDisplayed();
    }

    public boolean getDefaultNumberDisplayInherited()
    {
        return getInherited("defaultNumberFormatInherited");
    }

    public void setDefaultNumberDisplayInherited(boolean enable)
    {
        setInherited("defaultNumberFormatInherited", enable);
    }

    public String getDefaultNumberDisplay()
    {
        return getFormElement(elementCache().defaultNumberFormat);
    }

    public void setDefaultNumberDisplay(String numberFormat)
    {
        setFormElement(elementCache().defaultNumberFormat, numberFormat);
    }

    public boolean getAdditionalParsingPatternDatesInherited()
    {
        return getInherited("extraDateParsingPatternInherited");
    }

    public void setAdditionalParsingPatternDatesInherited(boolean enable)
    {
        setInherited("extraDateParsingPatternInherited", enable);
    }

    public String getAdditionalParsingPatternDates()
    {
        return getFormElement(elementCache().additionalParsingPatternDates);
    }

    public void setAdditionalParsingPatternDates(String pattern)
    {
        setFormElement(elementCache().additionalParsingPatternDates, pattern);
    }

    public boolean getAdditionalParsingPatternDateAndTimeInherited()
    {
        return getInherited("extraDateTimeParsingPatternInherited");
    }

    public void setAdditionalParsingPatternDateAndTimeInherited(boolean enable)
    {
        setInherited("extraDateTimeParsingPatternInherited", enable);
    }

    public String getAdditionalParsingPatternDateAndTime()
    {
        return getFormElement(elementCache().additionalParsingPatternDateAndTime);
    }

    public void setAdditionalParsingPatternDateAndTime(String pattern)
    {
        setFormElement(elementCache().additionalParsingPatternDateAndTime, pattern);
    }

    public boolean getAdditionalParsingPatternTimesInherited()
    {
        return getInherited("extraTimeParsingPatternInherited");
    }

    public void setAdditionalParsingPatternTimesInherited(boolean enable)
    {
        setInherited("extraTimeParsingPatternInherited", enable);
    }

    public String getAdditionalParsingPatternTimes()
    {
        return getFormElement(elementCache().additionalParsingPatternTimes);
    }

    public void setAdditionalParsingPatternTimes(String pattern)
    {
        setFormElement(elementCache().additionalParsingPatternTimes, pattern);
    }

    public boolean getRestrictChartingColsInherited()
    {
        return getInherited("restrictedColumnsEnabledInherited");
    }

    public void setRestrictChartingColsInherited(boolean enable)
    {
        setInherited("restrictedColumnsEnabledInherited", enable);
    }

    public void setRestrictChartingCols(boolean restrict)
    {
        if (restrict)
            checkCheckbox(elementCache().restrictChartingColsChk);
        else
            uncheckCheckbox(elementCache().restrictChartingColsChk);
    }

    public boolean getRestrictChartingCols()
    {
        return elementCache().restrictChartingColsChk.isSelected();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends FolderManagementPage.ElementCache
    {
        WebElement inheritedChk(String name)
        {
            return Locator.checkboxByName(name).findWhenNeeded(this);
        }

        WebElement defaultDateFormat = Locator.id("defaultDateFormat").findWhenNeeded(this);
        WebElement defaultTimeFormat = Locator.id("defaultTimeFormat").findWhenNeeded(this);
        WebElement defaultDateTimeDateFormat = Locator.id("dateSelect").findWhenNeeded(this);
        WebElement defaultDateTimeTimeFormat = Locator.id("timeSelect").findWhenNeeded(this);
        WebElement formatWarningBanner = Locator.tagWithId("div", "dateFormatWarning").findWhenNeeded(this);

        WebElement nonStandardWarning(WebElement field)
        {
            String id = field.getAttribute("id");
            String xpath = String.format("//select[@id='%s']/following-sibling::span[@class='has-warning']", id);
            return Locator.xpath(xpath).findWhenNeeded(this);
        }

        WebElement defaultNumberFormat = Locator.inputByNameContaining("defaultNumberFormat").findWhenNeeded(this);
        WebElement additionalParsingPatternDates = Locator.inputByNameContaining("extraDateParsingPattern").findElement(this);
        WebElement additionalParsingPatternTimes = Locator.inputByNameContaining("extraTimeParsingPattern").findElement(this);
        WebElement additionalParsingPatternDateAndTime = Locator.inputByNameContaining("extraDateTimeParsingPattern").findElement(this);
        WebElement restrictChartingColsChk = Locator.checkboxByName("restrictedColumnsEnabled").findWhenNeeded(this);
    }

}
