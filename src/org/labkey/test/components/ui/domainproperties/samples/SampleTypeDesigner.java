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

    public T addParentAlias(String alias, @Nullable String optionDisplayText)
    {
        expandPropertiesPanel();

        WebDriverWrapper.waitFor(elementCache().addAliasButton::isDisplayed,
                "'Add Parent Alias' button is not visible.", 2_500);

        elementCache().addAliasButton.click();
        int initialCount = findEmptyAlias();
        if (optionDisplayText == null)
        {
            optionDisplayText = CURRENT_SAMPLE_TYPE;
        }
        setParentAlias(initialCount, alias, optionDisplayText);
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

        protected final WebElement addAliasButton = Locator.tagWithClass("i","container--addition-icon").findWhenNeeded(this);
    }
}
