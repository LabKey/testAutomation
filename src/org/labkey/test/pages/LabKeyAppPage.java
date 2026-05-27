/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
