package org.labkey.test.components.glassLibrary.components;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;

public abstract class MenuFinder<Menu> extends WebDriverComponent.WebDriverComponentFinder<Menu, MenuFinder<Menu>>
    {
        private Locator _locator;

        public MenuFinder(WebDriver driver)
        {
            super(driver);
            _locator = MultiMenu.Locators.menuContainer();
        }

        public MenuFinder<Menu> withText(String text)
        {
            _locator = MultiMenu.Locators.menuContainer(text);
            return this;
        }

        public MenuFinder<Menu> withButtonId(String id)
        {
            _locator = MultiMenu.Locators.menuContainer().withChild(Locator.id(id));
            return this;
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
