package org.labkey.test.pages;

import org.labkey.test.components.ui.navigation.SubNavBar;
import org.openqa.selenium.WebDriver;

/**
 * Base page for pages shared across apps.
 * @param <EC> Element Cache type
 */
public abstract class LabKeyAppPage<EC extends LabKeyAppPage<?>.ElementCache> extends LabKeyPage<EC>
{
    public LabKeyAppPage(WebDriver driver)
    {
        super(driver);
    }

    /**
     * Get a reference to the SubNav bar. Sometimes these are referred to as tabs.
     * @return A SubNav bar object.
     */
    public SubNavBar getSubNavBar()
    {
        return elementCache().subNavBar;
    }

    @Override
    protected abstract EC newElementCache();

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        protected SubNavBar subNavBar = SubNavBar.finder(getDriver()).findWhenNeeded();
    }
}
