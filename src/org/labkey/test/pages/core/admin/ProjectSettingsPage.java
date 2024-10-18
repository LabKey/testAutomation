/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ProjectSettingsPage extends BaseSettingsPage
{
    public ProjectSettingsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ProjectSettingsPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static ProjectSettingsPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("admin", containerPath, "projectSettings"));
        return new ProjectSettingsPage(driver.getDriver());
    }

    public boolean getShouldInherit()
    {
        return elementCache().shouldInherit.isChecked();
    }

    public void setShouldInherit(boolean value)
    {
        elementCache().shouldInherit.set(value);
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

    public boolean getSystemDescriptionInherited()
    {
        return getInherited("systemDescriptionInherited");
    }

    public void getSystemDescriptionInherited(boolean enable)
    {
        setInherited("systemDescriptionInherited", enable);
    }

    public boolean getHeaderShortNameInherited()
    {
        return getInherited("systemShortNameInherited");
    }

    public void setHeaderShortNameInherited(boolean enable)
    {
        setInherited("systemShortNameInherited", enable);
    }

    public boolean getThemeInherited()
    {
        return getInherited("themeNameInherited");
    }

    public void setThemeInherited(boolean enable)
    {
        setInherited("themeNameInherited", enable);
    }

    public boolean getShowNavInherited()
    {
        return getInherited("folderDisplayModeInherited");
    }

    public void setShowNavInherited(boolean enable)
    {
        setInherited("folderDisplayModeInherited", enable);
    }

    public boolean getShowAppNavInherited()
    {
        return getInherited("applicationMenuDisplayModeInherited");
    }

    public void setShowAppNavInherited(boolean enable)
    {
        setInherited("applicationMenuDisplayModeInherited", enable);
    }

    public boolean getHelpMenuInherited()
    {
        return getInherited("helpMenuEnabledInherited");
    }

    public void setHelpMenuInherited(boolean enable)
    {
        setInherited("helpMenuEnabledInherited", enable);
    }

    public boolean getObjectLevelDiscInherited()
    {
        return getInherited("discussionEnabledInherited");
    }

    public void setObjectLevelDiscInherited(boolean enable)
    {
        setInherited("discussionEnabledInherited", enable);
    }

    public boolean getLogoLinkInherited()
    {
        return getInherited("logoHrefInherited");
    }

    public void setLogoLinkInherited(boolean enable)
    {
        setInherited("logoHrefInherited", enable);
    }

    public boolean getSupportLinkInherited()
    {
        return getInherited("reportAProblemPathInherited");
    }

    public void setSupportLinkInherited(boolean enable)
    {
        setInherited("reportAProblemPathInherited", enable);
    }

    public boolean getSupportEmailInherited()
    {
        return getInherited("supportEmailInherited");
    }

    public void setSupportEmailInherited(boolean enable)
    {
        setInherited("supportEmailInherited", enable);
    }

    public boolean getSystemEmailInherited()
    {
        return getInherited("systemEmailAddressInherited");
    }

    public void setSystemEmailInherited(boolean enable)
    {
        setInherited("systemEmailAddressInherited", enable);
    }

    public boolean getOrgNameInherited()
    {
        return getInherited("companyNameInherited");
    }

    public void setOrgNameInherited(boolean enable)
    {
        setInherited("companyNameInherited", enable);
    }

    public boolean getDefaultDateDisplayInherited()
    {
        return getInherited("defaultDateFormatInherited");
    }

    public void setDefaultDateDisplayInherited(boolean enable)
    {
        setInherited("defaultDateFormatInherited", enable);
    }

    public boolean getDefaultDateTimeDisplayInherited()
    {
        return getInherited("defaultDateTimeFormatInherited");
    }

    public void setDefaultDateTimeDisplayInherited(boolean enable)
    {
        setInherited("defaultDateTimeFormatInherited", enable);
    }

    public boolean getDefaultTimeDisplayInherited()
    {
        return getInherited("defaultTimeFormatInherited");
    }

    public void setDefaultTimeDisplayInherited(boolean enable)
    {
        setInherited("defaultTimeFormatInherited", enable);
    }

    public boolean getDefaultNumberDisplayInherited()
    {
        return getInherited("defaultNumberFormatInherited");
    }

    public void setDefaultNumberDisplayInherited(boolean enable)
    {
        setInherited("defaultNumberFormatInherited", enable);
    }

    public boolean getAdditionalParsingPatternDatesInherited()
    {
        return getInherited("extraDateParsingPatternInherited");
    }

    public void setAdditionalParsingPatternDatesInherited(boolean enable)
    {
        setInherited("extraDateParsingPatternInherited", enable);
    }

    public boolean getAdditionalParsingPatternDateAndTimeInherited()
    {
        return getInherited("extraDateTimeParsingPatternInherited");
    }

    public void setAdditionalParsingPatternDateAndTimeInherited(boolean enable)
    {
        setInherited("extraDateTimeParsingPatternInherited", enable);
    }

    public boolean getAdditionalParsingPatternTimesInherited()
    {
        return getInherited("extraTimeParsingPatternInherited");
    }

    public void setAdditionalParsingPatternTimesInherited(boolean enable)
    {
        setInherited("extraTimeParsingPatternInherited", enable);
    }

    public boolean getRestrictChartingColsInherited()
    {
        return getInherited("restrictedColumnsEnabledInherited");
    }

    public void setRestrictChartingColsInherited(boolean enable)
    {
        setInherited("restrictedColumnsEnabledInherited", enable);
    }

    public boolean getAltLoginPageInherited()
    {
        return getInherited("customLoginInherited");
    }

    public void setAltLoginPageInherited(boolean enable)
    {
        setInherited("customLoginInherited", enable);
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

    protected class ElementCache extends BaseSettingsPage.ElementCache
    {
        WebElement inheritedChk(String name)
        {
            return Locator.checkboxByName(name).findWhenNeeded(this);
        }

        protected final Checkbox shouldInherit = Checkbox.Checkbox(Locator.name("shouldInherit")).findWhenNeeded(this);
    }
}