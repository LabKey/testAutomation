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
package org.labkey.test.selenium;

import org.openqa.selenium.WebElement;

public abstract class WebElementDecorator extends WebElementWrapper
{
    private final WebElement _decoratedElement;

    protected WebElementDecorator(WebElement decoratedElement)
    {
        _decoratedElement = decoratedElement;
    }

    @Override
    public final WebElement getWrappedElement()
    {
        return _decoratedElement;
    }

    @Override
    public String toString()
    {
        return getWrappedElement().toString();
    }
}
