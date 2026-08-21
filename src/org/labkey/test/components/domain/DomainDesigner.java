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
package org.labkey.test.components.domain;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * A simple domain designer with a properties panel and a single field panel.
 */
public abstract class DomainDesigner<EC extends DomainDesigner<?>.ElementCache> extends BaseDomainDesigner<EC>
{
    public DomainDesigner(WebDriver driver)
    {
        super(driver);
    }

    protected void expandPropertiesPanel()
    {
        elementCache().propertiesPanel.expand();
    }

    public DomainFormPanel getFieldsPanel()
    {
        return elementCache().fieldsPanel.expand();
    }

    @Override
    public Object clickSave()
    {
        try
        {
            return super.clickSave();
        }
        catch (TestTimeoutException ex)
        {
            BaseWebDriverTest.getCurrentTest().getArtifactCollector().dumpPageSnapshot("domainSave");
            TestLogger.log("Failed to save domain. Opening properties panel for screenshot.");
            expandPropertiesPanel();
            throw ex;
        }
    }

    public class ElementCache extends BaseDomainDesigner<?>.ElementCache
    {
        protected final DomainPanel<?, ?> propertiesPanel = new DomainPanel.DomainPanelFinder(getDriver()).index(0)
                .timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);
        protected final DomainFormPanel fieldsPanel = new DomainFormPanel.DomainFormPanelFinder(getDriver())
                .index(getFieldPanelIndex()).timeout(2_000).findWhenNeeded();

        protected int getFieldPanelIndex()
        {
            return 1;
        }
    }
}
