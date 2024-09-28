package org.labkey.test.selenium;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Interaction;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.WheelInput;

import java.time.Duration;

public abstract class ActionsWrapper<T extends ActionsWrapper<T>> extends Actions
{
    public ActionsWrapper(WebDriver driver)
    {
        super(driver);
    }

    protected abstract T getThis();
    
    @Override
    public T keyDown(CharSequence key)
    {
        super.keyDown(key);
        return getThis();
    }

    @Override
    public T keyDown(WebElement target, CharSequence key)
    {
        super.keyDown(target, key);
        return getThis();
    }

    @Override
    public T keyUp(CharSequence key)
    {
        super.keyUp(key);
        return getThis();
    }

    @Override
    public T keyUp(WebElement target, CharSequence key)
    {
        super.keyUp(target, key);
        return getThis();
    }

    @Override
    public T sendKeys(CharSequence... keys)
    {
        super.sendKeys(keys);
        return getThis();
    }

    @Override
    public T sendKeys(WebElement target, CharSequence... keys)
    {
        super.sendKeys(target, keys);
        return getThis();
    }

    @Override
    public T clickAndHold(WebElement target)
    {
        super.clickAndHold(target);
        return getThis();
    }

    @Override
    public T clickAndHold()
    {
        super.clickAndHold();
        return getThis();
    }

    @Override
    public T release(WebElement target)
    {
        super.release(target);
        return getThis();
    }

    @Override
    public T scrollToElement(WebElement element)
    {
        super.scrollToElement(element);
        return getThis();
    }

    @Override
    public T scrollByAmount(int deltaX, int deltaY)
    {
        super.scrollByAmount(deltaX, deltaY);
        return getThis();
    }

    @Override
    public T scrollFromOrigin(WheelInput.ScrollOrigin scrollOrigin, int deltaX, int deltaY)
    {
        super.scrollFromOrigin(scrollOrigin, deltaX, deltaY);
        return getThis();
    }

    @Override
    public T release()
    {
        super.release();
        return getThis();
    }

    @Override
    public T click(WebElement target)
    {
        super.click(target);
        return getThis();
    }

    @Override
    public T click()
    {
        super.click();
        return getThis();
    }

    @Override
    public T doubleClick(WebElement target)
    {
        super.doubleClick(target);
        return getThis();
    }

    @Override
    public T doubleClick()
    {
        super.doubleClick();
        return getThis();
    }

    @Override
    public T moveToElement(WebElement target)
    {
        super.moveToElement(target);
        return getThis();
    }

    @Override
    public T moveToElement(WebElement target, int xOffset, int yOffset)
    {
        super.moveToElement(target, xOffset, yOffset);
        return getThis();
    }

    @Override
    public T moveByOffset(int xOffset, int yOffset)
    {
        super.moveByOffset(xOffset, yOffset);
        return getThis();
    }

    @Override
    public T moveToLocation(int xCoordinate, int yCoordinate)
    {
        super.moveToLocation(xCoordinate, yCoordinate);
        return getThis();
    }

    @Override
    public T contextClick(WebElement target)
    {
        super.contextClick(target);
        return getThis();
    }

    @Override
    public T contextClick()
    {
        super.contextClick();
        return getThis();
    }

    @Override
    public T dragAndDrop(WebElement source, WebElement target)
    {
        super.dragAndDrop(source, target);
        return getThis();
    }

    @Override
    public T dragAndDropBy(WebElement source, int xOffset, int yOffset)
    {
        super.dragAndDropBy(source, xOffset, yOffset);
        return getThis();
    }

    @Override
    public T pause(long pause)
    {
        super.pause(pause);
        return getThis();
    }

    @Override
    public T pause(Duration duration)
    {
        super.pause(duration);
        return getThis();
    }

    @Override
    public T tick(Interaction... actions)
    {
        super.tick(actions);
        return getThis();
    }

    @Override
    public T setActiveKeyboard(String name)
    {
        super.setActiveKeyboard(name);
        return getThis();
    }

    @Override
    public T setActivePointer(PointerInput.Kind kind, String name)
    {
        super.setActivePointer(kind, name);
        return getThis();
    }

    @Override
    public T setActiveWheel(String name)
    {
        super.setActiveWheel(name);
        return getThis();
    }
}
