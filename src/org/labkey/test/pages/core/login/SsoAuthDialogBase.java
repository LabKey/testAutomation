/*
 * Copyright (c) 2023-2026 LabKey Corporation
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
package org.labkey.test.pages.core.login;

import org.labkey.test.params.login.AuthenticationProvider;
import org.openqa.selenium.WebDriver;

import java.io.File;

public abstract class SsoAuthDialogBase<T extends SsoAuthDialogBase<T>> extends AuthDialogBase<T>
{
    public SsoAuthDialogBase(AuthenticationProvider<?> provider, WebDriver driver)
    {
        super(provider, driver);
    }

    public SsoAuthDialogBase(LoginConfigRow row)
    {
        super(row);
    }

    public T setPageHeaderLogo(File logoFile)
    {
        elementCache().headerLogoPanel.setLogo(logoFile);
        return getThis();
    }

    public T clearPageHeaderLogo()
    {
        elementCache().headerLogoPanel.clearLogo();
        return getThis();
    }

    public T setLoginPageLogo(File logoFile)
    {
        elementCache().loginPageLogoPanel.setLogo(logoFile);
        return getThis();
    }

    public T clearLoginPageLogo()
    {
        elementCache().loginPageLogoPanel.clearLogo();
        return getThis();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends AuthDialogBase<T>.ElementCache
    {
        SsoLogoInputPanel headerLogoPanel = new SsoLogoInputPanel.SsoLogoInputPanelFinder(getDriver())
                .timeout(4000).withLabel("Page Header Logo").findWhenNeeded(this);
        SsoLogoInputPanel loginPageLogoPanel = new SsoLogoInputPanel.SsoLogoInputPanelFinder(getDriver())
                .timeout(4000).withLabel("Login Page Logo").findWhenNeeded(this);

    }
}
