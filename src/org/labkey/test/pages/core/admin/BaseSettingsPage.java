package org.labkey.test.pages.core.admin;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.Locator;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;

public class BaseSettingsPage extends LabKeyPage<BaseSettingsPage.ElementCache>
{

    public BaseSettingsPage(WebDriver driver)
    {
        super(driver);
        waitForPage();
    }

    @Override
    protected void waitForPage()
    {
        Locator.waitForAnyElement(shortWait(), Locator.tagWithText("span","Save"), Locator.tagWithText("span","Done"));
    }

    public String getSystemDescription()
    {
        return getFormElement(elementCache().systemDescription);
    }

    public void setSystemDescription(String description)
    {
        setFormElement(elementCache().systemDescription, description);
    }

    public String getHeaderShortName()
    {
        return getFormElement(elementCache().headerShortName);
    }

    public void setHeaderShortName(String shortName)
    {
        setFormElement(elementCache().headerShortName, shortName);
    }

    public String getTheme()
    {
        return getSelectedOptionText(elementCache().theme);
    }

    public void setTheme(String theme)
    {
        selectOptionByText(elementCache().theme, theme);
    }

    public boolean isShowNavAlwaysChecked()
    {
        return new RadioButton(elementCache().showNavAlways).isChecked();
    }

    public void checkShowNavAlways()
    {
        new RadioButton(elementCache().showNavAlways).check();
    }

    public boolean isShowNavAdminChecked()
    {
        return new RadioButton(elementCache().showNavForAdmin).isChecked();
    }

    public void checkShowNavAdmin()
    {
        new RadioButton(elementCache().showNavForAdmin).check();
    }

    public boolean isShowAppNavAlwaysChecked()
    {
        return new RadioButton(elementCache().showAppNavAlways).isChecked();
    }

    public void checkShowAppNavAlways()
    {
        new RadioButton(elementCache().showAppNavAlways).check();
    }

    public boolean isShowAppNavAdminChecked()
    {
        return new RadioButton(elementCache().showAppNavForAdmin).isChecked();
    }

    public void checkShowAppNavAdmin()
    {
        new RadioButton(elementCache().showAppNavForAdmin).check();
    }

    public boolean getHelpMenu()
    {
        return elementCache().helpMenuEnabledChk.isSelected();
    }

    public void setHelpMenu(boolean enable)
    {
        if (enable)
            checkCheckbox(elementCache().helpMenuEnabledChk);
        else
            uncheckCheckbox(elementCache().helpMenuEnabledChk);
    }

    public boolean getObjectLevelDiscussions()
    {
        return elementCache().discussionEnabledChk.isEnabled();
    }

    public void setObjectLevelDiscussions(boolean enable)
    {
        if (enable)
            checkCheckbox(elementCache().discussionEnabledChk);
        else
            uncheckCheckbox(elementCache().discussionEnabledChk);
    }

    public String getLogoLink()
    {
        return getFormElement(elementCache().logoLinkTxt);
    }

    public void setLogoLink(String link)
    {
        setFormElement(elementCache().logoLinkTxt,link);
    }

    public String getSupportLink()
    {
        return getFormElement(elementCache().supportLinkTxt);
    }

    public void setSupportLink(String link)
    {
        setFormElement(elementCache().supportLinkTxt, link);
    }

    public String getSupportEmail()
    {
        return getFormElement(elementCache().supportEmailTxt);
    }

    public void setSupportEmail(String email)
    {
        setFormElement(elementCache().supportEmailTxt, email);
    }

    public String getSystemEmail()
    {
        return getFormElement(elementCache().systemEmailTxt);
    }

    public void setSystemEmail(String email)
    {
        setFormElement(elementCache().systemEmailTxt, email);
    }

    public String getOrgName()
    {
        return getFormElement(elementCache().organizationNameTxt);
    }

    public void setOrgName(String name)
    {
        setFormElement(elementCache().organizationNameTxt, name);
    }

    public String getDefaultDateDisplay()
    {
        return getFormElement(elementCache().defaultDateFormat);
    }

    public void setDefaultDateDisplay(String displayFormat)
    {
        setFormElement(elementCache().defaultDateFormat, displayFormat);
    }

    public String getDefaultDateTimeDisplay()
    {
        return getFormElement(elementCache().defaultDateTimeFormat);
    }

    public void setDefaultDateTimeDisplay(String displayFormat)
    {
        setFormElement(elementCache().defaultDateTimeFormat, displayFormat);
    }

    public String getDefaultTimeDisplay()
    {
        return getFormElement(elementCache().defaultTimeFormat);
    }

    public void setDefaultTimeDisplay(String displayFormat)
    {
        setFormElement(elementCache().defaultTimeFormat, displayFormat);
    }

    public String getDefaultNumberDisplay()
    {
        return getFormElement(elementCache().defaultNumberFormat);
    }

    public void setDefaultNumberDisplay(String numberFormat)
    {
        setFormElement(elementCache().defaultNumberFormat, numberFormat);
    }

    public String getAdditionalParsingPatternDates()
    {
        return getFormElement(elementCache().additionalParsingPatternDates);
    }

    public void setAdditionalParsingPatternDates(String pattern)
    {
        setFormElement(elementCache().additionalParsingPatternDates, pattern);
    }

    public String getAdditionalParsingPatternDateAndTime()
    {
        return getFormElement(elementCache().additionalParsingPatternDateAndTime);
    }

    public void setAdditionalParsingPatternDateAndTime(String pattern)
    {
        setFormElement(elementCache().additionalParsingPatternDateAndTime, pattern);
    }

    public String getAdditionalParsingPatternTimes()
    {
        return getFormElement(elementCache().additionalParsingPatternTimes);
    }

    public void setAdditionalParsingPatternTimes(String pattern)
    {
        setFormElement(elementCache().additionalParsingPatternTimes, pattern);
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

    public void save()
    {
        clickAndWait(elementCache().saveBtn);
    }

    public void reset()
    {
        doAndWaitForPageToLoad(()->
        {
            elementCache().resetBtn.click();
            acceptAlert();
        });

    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement systemDescription = Locator.name("systemDescription").findWhenNeeded(this);
        WebElement headerShortName = Locator.name("systemShortName").findWhenNeeded(this);
        WebElement theme = Locator.name("themeName").findWhenNeeded(this);
        WebElement showNavAlways = Locator.xpath("//input[@name='folderDisplayMode' and @value='ALWAYS']").findWhenNeeded(this);
        WebElement showNavForAdmin = Locator.xpath("//input[@name='folderDisplayMode' and @value='ADMIN']").findWhenNeeded(this);
        WebElement showAppNavAlways = Locator.xpath("//input[@name='applicationMenuDisplayMode' and @value='ALWAYS']").findWhenNeeded(this);
        WebElement showAppNavForAdmin = Locator.xpath("//input[@name='applicationMenuDisplayMode' and @value='ADMIN']").findWhenNeeded(this);
        WebElement helpMenuEnabledChk = Locator.name("helpMenuEnabled").findWhenNeeded(this);
        WebElement discussionEnabledChk = Locator.name("discussionEnabled").findWhenNeeded(this);
        WebElement logoLinkTxt = Locator.inputByNameContaining("logoHref").findWhenNeeded(this);
        WebElement supportLinkTxt = Locator.inputByNameContaining("reportAProblemPath").findWhenNeeded(this);
        WebElement supportEmailTxt = Locator.inputByNameContaining("supportEmail").findWhenNeeded(this);
        WebElement systemEmailTxt = Locator.inputByNameContaining("systemEmailAddress").findWhenNeeded(this);
        WebElement organizationNameTxt = Locator.inputByNameContaining("companyName").findWhenNeeded(this);
        WebElement defaultDateFormat = Locator.inputByNameContaining("defaultDateFormat").findWhenNeeded(this);
        WebElement defaultTimeFormat = Locator.inputByNameContaining("defaultTimeFormat").findWhenNeeded(this);
        WebElement defaultDateTimeFormat = Locator.inputByNameContaining("defaultDateTimeFormat").findWhenNeeded(this);
        WebElement defaultNumberFormat = Locator.inputByNameContaining("defaultNumberFormat").findWhenNeeded(this);
        WebElement additionalParsingPatternDates = Locator.inputByNameContaining("extraDateParsingPattern").findElement(this);
        WebElement additionalParsingPatternTimes = Locator.inputByNameContaining("extraTimeParsingPattern").findElement(this);
        WebElement additionalParsingPatternDateAndTime = Locator.inputByNameContaining("extraDateTimeParsingPattern").findElement(this);
        WebElement restrictChartingColsChk = Locator.checkboxByName("restrictedColumnsEnabled").findWhenNeeded(this);
        WebElement saveBtn = Locator.lkButton("Save").findWhenNeeded(this);
        WebElement resetBtn = Locator.lkButton("Reset").findWhenNeeded(this);
    }

    /**
     * Reset the settings for the site or a project/folder using the API (SimplePostCommand).
     * @param cn API Connection
     * @param path Project name/path or a '/' for the site setting.
     */
    public static void resetSettings(Connection cn, String path) throws IOException, CommandException
    {
        new SimplePostCommand("admin", "resetProperties")
                .execute(cn, path);
    }

}
