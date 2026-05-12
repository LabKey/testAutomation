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
import java.util.HashMap;
import java.util.Map;

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

    public void setDefaultDateTimeDisplay(DATE_FORMAT dateFormat, TIME_FORMAT timeFormat)
    {
        setDefaultDateTimeDateDisplay(dateFormat);
        setDefaultDateTimeTimeDisplay(timeFormat);
    }

    public String getDefaultDateTimeDateDisplay()
    {
        return getSelectedOptionValue(elementCache().defaultDateTimeDateFormat);
    }

    public void setDefaultDateTimeDateDisplay(DATE_FORMAT dateFormat)
    {
        selectOptionByValue(elementCache().defaultDateTimeDateFormat, dateFormat.toString());
    }

    public boolean defaultDateTimeDateDisplayWarning()
    {
        return elementCache().nonStandardWarning(elementCache().defaultDateTimeDateFormat).isDisplayed();
    }

    public String getDefaultDateTimeTimeDisplay()
    {
        return getSelectedOptionValue(elementCache().defaultDateTimeTimeFormat);
    }

    public void setDefaultDateTimeTimeDisplay(TIME_FORMAT timeFormat)
    {
        selectOptionByValue(elementCache().defaultDateTimeTimeFormat, timeFormat.toString());
    }

    public boolean defaultDateTimeTimeDisplayWarning()
    {
        return elementCache().nonStandardWarning(elementCache().defaultDateTimeTimeFormat).isDisplayed();
    }

    public String getDefaultTimeDisplay()
    {
        return getSelectedOptionValue(elementCache().defaultTimeFormat);
    }

    public void setDefaultTimeDisplay(TIME_FORMAT timeFormat)
    {
        selectOptionByValue(elementCache().defaultTimeFormat, timeFormat.toString());
    }

    public boolean defaultTimeDisplayWarning()
    {
        return elementCache().nonStandardWarning(elementCache().defaultTimeFormat).isDisplayed();
    }

    public String getDefaultNumberDisplay()
    {
        return getFormElement(elementCache().defaultNumberFormat);
    }

    public void setDefaultNumberDisplay(String numberFormat)
    {
        setFormElement(elementCache().defaultNumberFormat, numberFormat);
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

    public String getAltLoginPage()
    {
        return getFormElement(elementCache().altLoginPageTxt);
    }

    public void setAltLoginPage(String loginPage)
    {
        setFormElement(elementCache().altLoginPageTxt,loginPage);
    }

    /**
     * If the warning banner is present return the text otherwise return an empty string.
     *
     * @return Text in warning banner, empty string if no banner is present.
     */
    public String getFormatWarningMessage()
    {
        if (elementCache().formatWarningBanner.isDisplayed())
        {
            return elementCache().formatWarningBanner.getText();
        }
        else
        {
            return "";
        }
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
        WebElement helpMenuEnabledChk = Locator.checkboxByName("helpMenuEnabled").findWhenNeeded(this);
        WebElement logoLinkTxt = Locator.inputByNameContaining("logoHref").findWhenNeeded(this);
        WebElement supportLinkTxt = Locator.inputByNameContaining("reportAProblemPath").findWhenNeeded(this);
        WebElement supportEmailTxt = Locator.inputByNameContaining("supportEmail").findWhenNeeded(this);
        WebElement systemEmailTxt = Locator.inputByNameContaining("systemEmailAddress").findWhenNeeded(this);
        WebElement organizationNameTxt = Locator.inputByNameContaining("companyName").findWhenNeeded(this);

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
        WebElement restrictChartingColsChk = Locator.checkboxByName("restrictedColumnsEnabled").findWhenNeeded(this);
        WebElement altLoginPageTxt = Locator.inputById("customLogin").findWhenNeeded(this);
        WebElement saveBtn = Locator.lkButton("Save").findWhenNeeded(this);
        WebElement resetBtn = Locator.lkButton("Inherit all").findWhenNeeded(this);
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

    public enum DATE_FORMAT
    {
        yyyy_MM_dd("yyyy-MM-dd"),
        yyyy_MMM_dd("yyyy-MMM-dd"),
        yyyy_MM("yyyy-MM"),
        dd_MMM_yyyy("dd-MMM-yyyy"),
        dd_MMM_yy("dd-MMM-yy"),
        dd_MM_yyyy("dd-MM-yyyy"),
        ddMMMyyyy("ddMMMyyyy"),
        ddMMMyy("ddMMMyy"),
        MMddyyyy("MM/dd/yyyy"),
        MM_dd_yyyy("MM-dd-yyyy"),
        MMMM_dd_yyyy("MMMM dd yyyy"),
        Default("yyyy-MM-dd"),
        DTDefault("yyyy-MM-dd"),
        DATE("Date"), // Valid only in a domain designer.
        DATETIME("DATETIME"); // Valid only in a domain designer and only for the date part of a DateTime field.

        private final String format;

        DATE_FORMAT(String format)
        {
            this.format = format;
        }

        @Override
        public String toString() {
            return this.format;
        }

        private static final Map<String, DATE_FORMAT> lookup = new HashMap<>();

        static {
            for (DATE_FORMAT d : DATE_FORMAT.values()) {
                lookup.put(d.toString(), d);
            }
        }

        public static DATE_FORMAT get(String format) {
            return lookup.get(format);
        }

    }

    public enum TIME_FORMAT
    {
        HH_mm_ss("HH:mm:ss"),
        HH_mm("HH:mm"),
        HH_mm_ss_SSS("HH:mm:ss.SSS"),
        hh_mm_a("hh:mm a"),
        none("<none>"), // Valid only for a DateTime field.
        Default("HH:mm:ss"),
        DTDefault("HH:mm"),
        TIME("Time"); // Valid only in domain designer.

        private final String format;

        TIME_FORMAT(String format)
        {
            this.format = format;
        }

        @Override
        public String toString() {
            return this.format;
        }

        private static final Map<String, TIME_FORMAT> lookup = new HashMap<>();

        static {
            for (TIME_FORMAT t : TIME_FORMAT.values()) {
                lookup.put(t.toString(), t);
            }
        }

        public static TIME_FORMAT get(String format) {
            return lookup.get(format);
        }

    }
}
