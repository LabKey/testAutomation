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
package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class FileTransferConfigurationPage<EC extends FileTransferConfigurationPage.ElementCache> extends LabKeyPage<EC>
{
    public FileTransferConfigurationPage(WebDriver driver)
    {
        super(driver);
    }

    public static FileTransferConfigurationPage beginAt(WebDriverWrapper driver)
    {
        driver.beginAt(WebTestHelper.buildURL("filetransfer", "configuration"));
        return new FileTransferConfigurationPage(driver.getDriver());
    }

    public String getClientId()
    {
        return getFormElement(elementCache().clientId);
    }

    public FileTransferConfigurationPage setClientId(String clientId)
    {
        setFormElement(elementCache().clientId, clientId);
        return this;
    }

    public String getClientIdAlert()
    {
        return getAlertMessage(elementCache().clientId);
    }

    public String getClientSecret()
    {
        return getFormElement(elementCache().clientSecret);
    }

    public FileTransferConfigurationPage setClientSecret(String clientSecret)
    {
        setFormElement(elementCache().clientSecret, clientSecret);
        return this;
    }

    public String getClientSecretAlert()
    {
        return getAlertMessage(elementCache().clientSecret);
    }

    public String getAuthUrlPrefix()
    {
        return getFormElement(elementCache().authUrlPrefix);
    }

    public FileTransferConfigurationPage setAuthUrlPrefix(String authUrlPrefix)
    {
        setFormElement(elementCache().authUrlPrefix, authUrlPrefix);
        return this;
    }

    public String getAuthUrlPrefixAlert()
    {
        return getAlertMessage(elementCache().authUrlPrefix);
    }

    public String getBrowseEndpointUrlPrefix()
    {
        return getFormElement(elementCache().browseEndpointUrlPrefix);
    }

    public FileTransferConfigurationPage setBrowseEndpointUrlPrefix(String browseEndpointUrlPrefix)
    {
        setFormElement(elementCache().browseEndpointUrlPrefix, browseEndpointUrlPrefix);
        return this;
    }

    public String getBrowseEndpointUrlPrefixAlert()
    {
        return getAlertMessage(elementCache().browseEndpointUrlPrefix);
    }

    public String getTransferApiUrlPrefix()
    {
        return getFormElement(elementCache().transferApiUrlPrefix);
    }

    public FileTransferConfigurationPage setTransferApiUrlPrefix(String transferApiUrlPrefix)
    {
        setFormElement(elementCache().transferApiUrlPrefix, transferApiUrlPrefix);
        return this;
    }

    public String getTransferApiUrlPrefixAlert()
    {
        return getAlertMessage(elementCache().transferApiUrlPrefix);
    }

    public String getTransferUiUrlPrefix()
    {
        return getFormElement(elementCache().transferUiUrlPrefix);
    }

    public FileTransferConfigurationPage setTransferUiUrlPrefix(String transferUiUrlPrefix)
    {
        setFormElement(elementCache().transferUiUrlPrefix, transferUiUrlPrefix);
        return this;
    }

    public String getSourceEndpointId()
    {
        return getFormElement(elementCache().sourceEndpointId);
    }

    public FileTransferConfigurationPage setSourceEndpointId(String sourceEndpointId)
    {
        setFormElement(elementCache().sourceEndpointId, sourceEndpointId);
        return this;
    }

    public String getSourceEndpointName()
    {
        return getFormElement(elementCache().sourceEndpointDisplayName);
    }

    public FileTransferConfigurationPage setSourceEndpointName(String sourceEndpointName)
    {
        setFormElement(elementCache().sourceEndpointDisplayName, sourceEndpointName);
        return this;
    }

    public String getSourceEndpointDir()
    {
        return getFormElement(elementCache().sourceEndpointLocalDir);
    }

    public FileTransferConfigurationPage setSourceEndpointDir(String sourceEndpointDir)
    {
        setFormElement(elementCache().sourceEndpointLocalDir, sourceEndpointDir);
        return this;
    }

    protected String getAlertMessage(WebElement input)
    {
        String alertMsg, attValue;
        final String START_MSG = "<li role=\"alert\">";
        final String END_MSG = "</li>";

        try
        {
            attValue = input.getAttribute("data-errorqtip");
            if(!attValue.isEmpty())
            {
                alertMsg = attValue.substring(attValue.indexOf(START_MSG) + START_MSG.length(), attValue.indexOf(END_MSG));
            }
            else
                alertMsg = "";
        }
        catch(NoSuchElementException nse)
        {
            alertMsg = "";
        }

        return alertMsg;
    }

    public void clickSave()
    {
        clickAndWait(elementCache().saveButton);
    }

    public void clickCancel()
    {
        clickAndWait(elementCache().cancelButton);
    }

    @Override
    protected EC newElementCache()
    {
        return (EC) new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected WebElement clientId = new LazyWebElement(Locator.xpath("//input[@name='clientId']"), this);
        protected WebElement clientSecret = new LazyWebElement(Locator.xpath("//input[@name='clientSecret']"),this);
        protected WebElement authUrlPrefix = new LazyWebElement(Locator.xpath("//input[@name='authUrlPrefix']"),this);
        protected WebElement browseEndpointUrlPrefix = new LazyWebElement(Locator.xpath("//input[@name='browseEndpointUrlPrefix']"),this);
        protected WebElement transferApiUrlPrefix = new LazyWebElement(Locator.xpath("//input[@name='transferApiUrlPrefix']"),this);
        protected WebElement transferUiUrlPrefix = new LazyWebElement(Locator.xpath("//input[@name='transferUiUrlPrefix']"),this);
        protected WebElement sourceEndpointId = new LazyWebElement(Locator.xpath("//input[@name='sourceEndpointId']"),this);
        protected WebElement sourceEndpointDisplayName = new LazyWebElement(Locator.xpath("//input[@name='sourceEndpointDisplayName']"),this);
        protected WebElement sourceEndpointLocalDir = new LazyWebElement(Locator.xpath("//input[@name='sourceEndpointLocalDir']"),this);
        protected WebElement saveButton = new LazyWebElement(Locator.xpath(".//a[contains(@class, 'x4-btn')]//span[contains(text(), 'Save')]/ancestor::a"),this);
        protected WebElement cancelButton = new LazyWebElement(Locator.xpath(".//a[contains(@class, 'x4-btn')]//span[contains(text(), 'Cancel')]/ancestor::a"),this);
    }


}
