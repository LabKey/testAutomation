package org.labkey.test.util;


import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;

/**
 * User: tchadick
 * Date: 11/30/12
 * Time: 4:28 PM
 */
public class LabKeyExpectedConditions
{
    private LabKeyExpectedConditions(){}


    /**
     * An expectation for checking that an element has stopped moving
     *
     * @param loc the container element which should have css style, "position: static"
     * @return true when animation is complete
     */
    public static ExpectedCondition<Boolean> animationIsDone(final Locator loc) {
          return new ExpectedCondition<Boolean>() {
              public Boolean apply(WebDriver driver)
              {
                  return loc.findElement(driver).getCssValue("position").equals("static");
              }
          };
    }

}
