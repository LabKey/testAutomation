/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.components.ui.domainproperties.samples;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ui.domainproperties.EntityTypeDesigner;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Automates the LabKey ui component defined in: packages/components/src/components/domainproperties/samples/SampleTypeDesigner.tsx
 * This is a full-page component and should be wrapped by a context-specific page class
 */
public abstract class SampleTypeDesigner<T extends SampleTypeDesigner<T>> extends EntityTypeDesigner<T>
{
    public static final String CURRENT_SAMPLE_TYPE = "(Current Sample Type)";

    public SampleTypeDesigner(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected abstract T getThis();

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return  (ElementCache) super.elementCache();
    }

    public T addParentAlias(String alias)
    {
        return addParentAlias(alias, null);
    }

    public T addParentAlias(String alias, @Nullable String dataType)
    {
        return addParentAlias(alias, dataType, false);
    }

    public T addParentAlias(String alias, @Nullable String dataType, boolean isRequired)
    {
        expandPropertiesPanel();

        WebDriverWrapper.waitFor(elementCache().addParentAliasButton::isDisplayed,
                "'Add a Parent' button is not visible.", 2_500);

        elementCache().addParentAliasButton.click();
        int initialCount = findEmptyAlias();
        if (dataType == null)
        {
            dataType = CURRENT_SAMPLE_TYPE;
        }
        setParentAlias(initialCount, alias, dataType, isRequired);
        return getThis();
    }

    public boolean hasUniqueIdAlert()
    {
        return elementCache().uniqueIdAlert.isDisplayed();
    }

    public T clickUniqueIdAlertAddButton()
    {
        if (hasUniqueIdAlert())
            elementCache().uniqueIdAlertAddButton.click();
        else
            throw new NotFoundException("Unique Id alert is not displayed.");

        return getThis();
    }

    public boolean hasUniqueIdMsg()
    {
        expandPropertiesPanel();
        return elementCache().uniqueIdMsg.isDisplayed();
    }

    public boolean hasUniqueIdCheckIcon()
    {
        expandPropertiesPanel();
        return hasUniqueIdMsg() && elementCache().uniqueIdMsgCheckIcon.isDisplayed();
    }

    public String getUniqueIdMsg()
    {
        expandPropertiesPanel();
        return elementCache().uniqueIdMsg.getText();
    }

    protected class ElementCache extends EntityTypeDesigner<T>.ElementCache
    {
        protected final WebElement uniqueIdAlert = Locator.tagWithClassContaining("div","uniqueid-alert").refindWhenNeeded(this);
        protected final WebElement uniqueIdAlertAddButton = Locator.tagWithClassContaining("div","uniqueid-alert")
                .append(Locator.tag("button")).refindWhenNeeded(this);
        protected final WebElement uniqueIdMsg = Locator.tagWithClass("div","uniqueid-msg").refindWhenNeeded(this);
        protected final WebElement uniqueIdMsgCheckIcon = Locator.tagWithClass("div","uniqueid-msg")
                .append(Locator.tagWithClassContaining("i", "domain-panel-status-icon-green")).refindWhenNeeded(this);

        public final WebElement addParentAliasButton = Locator.tagWithClassContaining("button", "container--action-button")
                .withText("Add a Parent").findWhenNeeded(propertiesPanel);
        public final WebElement addAliasButton = Locator.tagWithClass("i","container--addition-icon").findWhenNeeded(this);
    }
}
