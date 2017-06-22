/*
 * Copyright (c) 2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by RyanS on 5/19/2017.
 */
public class LookAndFeelSettingsPage extends LabKeyPage<LookAndFeelSettingsPage.ElementCache>
{
    public LookAndFeelSettingsPage(WebDriver driver)
    {
        super(driver);
        //waitForElement(elementCache().systemDescription);
    }

    public void setTheme(String theme)
    {
        selectOptionByText(elementCache().theme, theme);
    }

    public void setFontSize(String fontSize)
    {
        selectOptionByText(elementCache().fontSize, fontSize);
    }

    public void setShowNavAlways()
    {
        new RadioButton(elementCache().showNavAlways).check();
    }

    public void setShowNavAdmin()
    {
        new RadioButton(elementCache().showNavForAdmin).check();
    }

    public void enableHelp(boolean enable)
    {
        if(enable){checkCheckbox(elementCache().enableHelpChk);}
        else{uncheckCheckbox(elementCache().enableHelpChk);}
    }

    public void enableObjectLevelDiscussions(boolean enable)
    {
        if(enable){checkCheckbox(elementCache().enableDiscussionChk);}
        else{uncheckCheckbox(elementCache().enableDiscussionChk);}
    }

    public void setLogoLink(String link)
    {
        setFormElement(elementCache().logoLinkTxt,link);
    }

    public String getLogoLink()
    {
        return (elementCache().logoLinkTxt).getText();
    }

    public void setSupportLink(String link)
    {
        setFormElement(elementCache().supportLinkTxt, link);
    }

    public String getSupportLink()
    {
        return (elementCache().supportLinkTxt).getText();
    }

    public void setSupportEmail(String email)
    {
        setFormElement(elementCache().supportEmailTxt,email);
    }

    public String getSupportEmail()
    {
        return (elementCache().supportEmailTxt).getText();
    }

    public void setSystemEmail(String email)
    {
        setFormElement(elementCache().systemEmailTxt,email);
    }

    public String getSystemEmail()
    {
        return (elementCache().systemEmailTxt).getText();
    }

    public void setOrgName(String name)
    {
        setFormElement(elementCache().organizationNameTxt, name);
    }

    public String getOrgName()
    {
        return (elementCache().organizationNameTxt).getText();
    }

    public void setDateParsingMode(boolean isMurican)
    {
        if(isMurican){new RadioButton(elementCache().USDateParsingRdio).check();}
        //isCommie
        else{new RadioButton(elementCache().NonUSDateParsingRdio).check();}
    }

    public void setDefaultDateDisplay(String displayFormat)
    {
        setFormElement(elementCache().defaultDateFormatTxt,displayFormat);
    }

    public String getDefaultDateDisplay()
    {
        return (elementCache().defaultDateFormatTxt).getText();
    }

    public void setDefaultDateTimeDisplay(String displayFormat)
    {
        setFormElement(elementCache().defaultDateTimeFormatTxt,displayFormat);
    }

    public String getDefaultDateTimeDisplay()
    {
        return (elementCache().defaultDateTimeFormatTxt).getText();
    }

    public void setDefaultNumberDisplay(String numberFormat)
    {
        setFormElement(elementCache().defaultNumberFormatTxt, numberFormat);
    }

    public String getDefaultNumberDisplay()
    {
        return (elementCache().defaultNumberFormatTxt).getText();
    }

    public void restrictChartingCols(boolean restrict)
    {
        if(restrict){checkCheckbox(elementCache().restrictChartingColsChk);}
        else{uncheckCheckbox(elementCache().restrictChartingColsChk);}
    }

    public void setAltLoginPage(String loginPage)
    {
        setFormElement(elementCache().altLoginPageTxt,loginPage);
    }

    public String getAltLoginPage()
    {
        return (elementCache().altLoginPageTxt).getText();
    }

    public void save(){click(elementCache().saveBtn);}

    public void reset(){click(elementCache().resetBtn);}

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement systemDescription = Locator.name("systemDescription").findWhenNeeded(this);
        WebElement headerShortName = Locator.name("systemShortName").findWhenNeeded(this);
        WebElement theme = Locator.name("themeName").findWhenNeeded(this);
        WebElement fontSize = Locator.name("themeFont").findWhenNeeded(this);
        WebElement showNavAlways = Locator.xpath("//input[@name='folderDisplayMode' and @value='ALWAYS']").findWhenNeeded(this);
        WebElement showNavForAdmin = Locator.xpath("//input[@name='folderDisplayMode' and @value='ADMIHN']").findWhenNeeded(this);
        WebElement enableHelpChk = Locator.name("enableHelpMenu").findWhenNeeded(this);
        WebElement enableDiscussionChk = Locator.name("enableDiscussion").findWhenNeeded(this);
        WebElement logoLinkTxt = Locator.inputByNameContaining("logoHref").findWhenNeeded(this);
        WebElement supportLinkTxt = Locator.inputByNameContaining("reportAProblemPath").findWhenNeeded(this);
        WebElement supportEmailTxt = Locator.inputByNameContaining("supportEmail").findWhenNeeded(this);
        WebElement systemEmailTxt = Locator.inputByNameContaining("systemEmailAddress").findWhenNeeded(this);
        WebElement organizationNameTxt = Locator.inputByNameContaining("systemEmailAddress").findWhenNeeded(this);
        WebElement USDateParsingRdio = Locator.xpath("//input[@name='dateParsingMode' and @value='US']").findWhenNeeded(this);
        WebElement NonUSDateParsingRdio = Locator.xpath("//input[@name='dateParsingMode' and @value='NON_US']").findWhenNeeded(this);
        WebElement defaultDateFormatTxt = Locator.inputByNameContaining("defaultDateFormat").findWhenNeeded(this);
        WebElement defaultDateTimeFormatTxt = Locator.inputByNameContaining("defaultDateTimeFormat").findWhenNeeded(this);
        WebElement defaultNumberFormatTxt = Locator.inputByNameContaining("defaultNumberFormat").findWhenNeeded(this);
        WebElement restrictChartingColsChk = Locator.checkboxByName("restrictedColumnsEnabled").findWhenNeeded(this);
        WebElement altLoginPageTxt = Locator.inputByNameContaining("customLogin").findWhenNeeded(this);
        WebElement saveBtn = Locator.lkButton("Save").findWhenNeeded(this);
        WebElement resetBtn = Locator.lkButton("Reset").findWhenNeeded(this);
    }
}
