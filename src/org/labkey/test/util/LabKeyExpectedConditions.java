/*
 * Copyright (c) 2012 LabKey Corporation
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
