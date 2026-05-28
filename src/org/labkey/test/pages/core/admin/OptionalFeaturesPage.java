/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

/*
    Wraps Optional Features, Experimental Features, Deprecated Features pages linked
    from Admin Console
 */
public class OptionalFeaturesPage extends LabKeyPage<OptionalFeaturesPage.ElementCache>
{
    public OptionalFeaturesPage(WebDriver driver)
    {
        super(driver);
    }

    public static OptionalFeaturesPage beginAt(WebDriverWrapper webDriverWrapper, OptionalFeatureType featureType)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("admin", "/", "optionalFeatures", Map.of("Type", featureType.toString())));
        return new OptionalFeaturesPage(webDriverWrapper.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        waitFor(()-> elementCache().listGroupLoc.findWhenNeeded(getDriver()).isDisplayed(),
                "The page did not render in time", WAIT_FOR_JAVASCRIPT);
    }

    public ShowAdminPage goToAdminConsole()
    {
        clickAndWait(Locator.linkWithText("Admin Console"));
        return new ShowAdminPage(getDriver());
    }

    public boolean getFeatureStatus(String id)
    {
        return elementCache().getCheckboxById(id).get();
    }

    public OptionalFeaturesPage setFeatureStatus(String id, boolean status)
    {
        elementCache().getCheckboxById(id).set(status);
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        public final Locator.XPathLocator listGroupLoc = Locator.tagWithClass("div", "list-group");
        public final WebElement listGroupElement = listGroupLoc.waitForElement(getDriver(), 1500);
        public final Locator listItemLabelLoc = Locator.tagWithClass("div", "list-group-item")
                .child(Locator.tag("Label"));

        public Checkbox getCheckboxById(String id)
        {
            return Checkbox.Checkbox(Locator.id(id)).waitFor(listGroupElement);
        }
    }

    public enum OptionalFeatureType{
        Experimental,
        Optional,
        Deprecated
    }
}
