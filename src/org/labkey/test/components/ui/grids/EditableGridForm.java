/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Wrapper for components that toggle edit mode
 * @param <T> Component after saving changes or cancelling
 */
public abstract class EditableGridForm<T> extends EditableGrid
{
    private final SearchContext _outerScope;

    protected EditableGridForm(SearchContext outerScope, WebDriver driver)
    {
        super(new EditableGridFinder(driver).findWhenNeeded(outerScope));
        _outerScope = outerScope;
    }

    public T saveChanges()
    {
        Locator.tagWithClass("button", "btn-success").findElement(_outerScope).click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
        return getComponentAfterSave();
    }

    public T cancelChanges()
    {
        Locator.tagWithClass("button", "btn-default").withText("Cancel").findElement(_outerScope).click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
        return getComponentAfterSave();
    }

    protected abstract T getComponentAfterSave();
}
