/*
 * Copyright (c) 2019-2026 LabKey Corporation
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
package org.labkey.test;

public abstract class BootstrapLocators
{

    // '@labkey/components/base/LoadingSpinner.tsx'
    public static final Locator.XPathLocator loadingSpinner = Locator.tag("span").withChild(Locator.tagWithClass("i", "fa-spinner"));

    // '@labkey/components/base/ContentGroup.tsx'
    public static Locator.XPathLocator contentGroup(String label)
    {
        return Locator.byClass("content-group").withChild(Locator.byClass("content-group-label").withText(label));
    }

    public static Locator.XPathLocator button()
    {
        return Locator.byClass("btn");
    }

    public static Locator.XPathLocator button(String text)
    {
        return button().withText(text);
    }

    public static final String APP_USER_MENU_CLASS = "user-dropdown";
    // Part of '@labkey/components/navigation/UserMenuGroup.tsx'
    public static final Locator appUserMenu = Locator.byClass(APP_USER_MENU_CLASS);

    // '@labkey/components/base/Alert.tsx'
    public static final Locator infoBanner = Locator.tagWithClass("div", BannerType.INFO.getCss());
    public static final Locator successBanner = Locator.tagWithClass("div", BannerType.SUCCESS.getCss());
    public static final Locator errorBanner = Locator.tagWithClass("div", BannerType.ERROR.getCss());
    public static final Locator warningBanner = Locator.tagWithClass("div", BannerType.WARNING.getCss());

    public enum BannerType
    {
        SUCCESS("alert-success"),
        INFO("alert-info"),
        WARNING("alert-warning"),
        ERROR("alert-danger");

        private final String _css;

        BannerType(String css)
        {
            _css = css;
        }

        public String getCss()
        {
            return _css;
        }
    }

    public static Locator panel(String panelHeading)
    {
        return Locator.byClass("panel").withChild(Locator.byClass("panel-heading").withText(panelHeading));
    }
}
