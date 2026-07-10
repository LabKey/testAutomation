/*
 * Copyright (c) 2021-2026 LabKey Corporation. All rights reserved. No portion of this work may be reproduced
 * in any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.util.ui;

import org.junit.Assert;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BannerHelper
{
    // Use a supplier to allow instantiation in test constructor.
    private final WrapsDriver _driver;

    public BannerHelper(WrapsDriver wrapsDriver)
    {
        _driver = wrapsDriver;
    }

    private WebDriver getDriver()
    {
        return _driver.getWrappedDriver();
    }

    /**
     * Wait one second for a error banner to show up.
     * @return String of the banner text empty if not present.
     */
    public String waitForError()
    {
        return waitForError(1_000);
    }

    /**
     * Wait a given amount of time for a error banner to show up.
     * @param waitTime Amount of time to wait in milliseconds.
     * @return String of the banner text empty if not present.
     */
    public String waitForError(int waitTime)
    {
        return waitForBanner(BootstrapLocators.errorBanner, waitTime);
    }

    /**
     * Get a reference to the error banner element.
     * @return WebElement referencing the error banner.
     */
    public WebElement errorBanner()
    {
        return BootstrapLocators.errorBanner.findOptionalElement(getDriver()).orElse(null);
    }

    /**
     * Get the text of the error banner.
     * @return Text of the banner.
     */
    public String errorBannerText()
    {
        try
        {
            return errorBanner().getText();
        }
        catch(NullPointerException npe)
        {
            return "";
        }
    }

    /**
     * Wait one second for a warning banner to show up.
     * @return String of the banner text empty if not present.
     */
    public String waitForWarning()
    {
        return waitForWarning(1_000);
    }

    /**
     * Wait a given amount of time for a warning banner to show up.
     * @param waitTime Amount of time to wait in milliseconds.
     * @return String of the banner text empty if not present.
     */
    public String waitForWarning(int waitTime)
    {
        return waitForBanner(BootstrapLocators.warningBanner, waitTime);
    }

    /**
     * Get a reference to the warning banner element.
     * @return WebElement referencing the warning banner.
     */
    public WebElement warningBanner()
    {
        return BootstrapLocators.warningBanner.findElementOrNull(getDriver());
    }

    /**
     * Get the text of the warning banner.
     * @return Text of the banner.
     */
    public String warningBannerText()
    {
        try
        {
            return warningBanner().getText();
        }
        catch(NullPointerException npe)
        {
            return "";
        }
    }

    /**
     * Wait one second for a info banner to show up.
     * @return String of the banner text empty if not present.
     */
    public String waitForInfo()
    {
        return waitForInfo(1_000);
    }

    /**
     * Wait a given amount of time for a info banner to show up.
     * @param waitTime Amount of time to wait in milliseconds.
     * @return String of the banner text empty if not present.
     */
    public String waitForInfo(int waitTime)
    {
        return waitForBanner(BootstrapLocators.infoBanner, waitTime);
    }

    /**
     * Get a reference to the info banner element.
     * @return WebElement referencing the info banner.
     */
    public WebElement infoBanner()
    {
        return BootstrapLocators.infoBanner.existsIn(getDriver()) ? BootstrapLocators.infoBanner.findElement(getDriver()) : null;
    }

    /**
     * Get the text of the info banner.
     * @return Text of the banner.
     */
    public String infoBannerText()
    {
        try
        {
            return infoBanner().getText();
        }
        catch(NullPointerException npe)
        {
            return "";
        }
    }

    /**
     * Wait one second for a success banner to show up.
     * @return String of the banner text empty if not present.
     */
    public String waitForSuccess()
    {
        return waitForSuccess(10_000);
    }

    /**
     * Wait a given amount of time for a success banner to show up.
     * @param waitTime Amount of time to wait in milliseconds.
     * @return String of the banner text empty if not present.
     */
    public String waitForSuccess(int waitTime)
    {
        return waitForBanner(BootstrapLocators.successBanner, waitTime);
    }

    /**
     * Get a reference to the success banner element.
     * @return WebElement referencing the success banner.
     */
    public WebElement successBanner()
    {
        return BootstrapLocators.successBanner.existsIn(getDriver()) ? BootstrapLocators.successBanner.findElement(getDriver()) : null;
    }

    /**
     * Get the text of the success banner.
     * @return Text of the banner.
     */
    public String successBannerText()
    {
        try
        {
            return successBanner().getText();
        }
        catch (NullPointerException npe)
        {
            return "";
        }
    }

    /**
     * Click the link or button that appears in the success banner message.
     *
     * @param text text of the link or button
     */
    public void clickSuccessBannerLink(String text)
    {
        WebElement banner = successBanner();
        WebElement link = Locator.linkContainingText(text).findElementOrNull(banner);
        if (link != null)
            link.click();
        else
            banner.findElement(Locator.buttonContainingText(text)).click();
    }

    private String waitForBanner(Locator bannerLocator, int waitTime)
    {
        try
        {
            return bannerLocator.waitForElement(getDriver(), waitTime).getText();
        }
        catch (NoSuchElementException nsee)
        {
            return "";
        }
    }

    /**
     * Wait for any banner to appear regardless of it's type.
     * If no banner appears an empty string is returned.
     * @return Text of the banner if present empty string otherwise.
     */
    public String waitForAnyBanner()
    {
        try
        {
            WebElement alert = Locator.waitForAnyElement(new WebDriverWait(getDriver(), Duration.ofSeconds(10)),
                    BootstrapLocators.errorBanner, BootstrapLocators.infoBanner, BootstrapLocators.warningBanner, BootstrapLocators.successBanner);
            return alert.getText();
        }
        catch(NoSuchElementException nsee)
        {
            return "";
        }
    }

    /**
     * Get a reference to any banner element that might be there.
     * @return WebElement referencing the success banner. Null if nothing there.
     */
    public WebElement anyBanner()
    {
        return Locator.findAnyElementOrNull(getDriver(),
                BootstrapLocators.errorBanner, BootstrapLocators.infoBanner, BootstrapLocators.warningBanner, BootstrapLocators.successBanner);
    }

    /**
     * Get the text of the banner regardless of the banner type.
     * If no banner is present empty string is returned.
     *
     * @return Text from any banner present, empty string if none present.
     */
    public String anyBannerText()
    {
        WebElement alert = Locator.findAnyElementOrNull(getDriver(),
                BootstrapLocators.errorBanner, BootstrapLocators.infoBanner, BootstrapLocators.warningBanner, BootstrapLocators.successBanner);
        if (alert != null)
            return alert.getText();
        else
            return "";
    }

    /**
     * Get the text from all of the banners that are shown regardless of type. If there are no banners an empty
     * collection is returned.
     *
     * @return List of strings containing the text from all of the banners present.
     */
    public List<String> allBannersTexts()
    {
        List<String> bannersTexts = new ArrayList<>();

        List<WebElement> alerts = Locator.findElements(getDriver(),
                BootstrapLocators.errorBanner, BootstrapLocators.infoBanner, BootstrapLocators.warningBanner, BootstrapLocators.successBanner);
        alerts.forEach(a -> bannersTexts.add(a.getText()));

        return bannersTexts;
    }

    /**
     * Dismiss a banner message regardless of its type.
     * If there are multiple banner messages this will delete the first one.
     */
    public void dismissBannerMessage()
    {
        if(anyBanner() != null)
        {
            WebElement alert = Locator.tagWithClass("div" , "alert").findWhenNeeded(getDriver());
            Locator.xpath("//i[contains(@class, 'fa-times-circle')]").findElement(alert).click();
            WebDriverWrapper.waitFor(()->!alert.isDisplayed(), "Banner message was not dismissed.", 2_500);
        }
    }

    /**
     * Dismisses all banner messages currently displayed on the page.
     *
     * This method attempts to remove all banner messages by repeatedly locating and clicking
     * the close button of the banners. The process is repeated up to 25 times or until no banners
     * are present. Each iteration includes a short delay to allow banners to be dismissed properly
     * before the next attempt.
     *
     * If banners remain after 25 attempts, an assertion failure is triggered.
     *
     * Banners are identified using their respective CSS classes, and the click action is performed
     * on the close icon within each banner.
     *
     * Throws:
     * - AssertionError: If all banners are not dismissed after 25 attempts.
     */
    public void dismissAllBannerMessages()
    {
        int tries = 0;
        while ((anyBanner() != null) && (tries < 25))
        {
            WebElement alert = Locator.byClass("alert").findWhenNeeded(getDriver());
            Locator.byClass("fa-times-circle").findElement(alert).click();
            WebDriverWrapper.sleep(250);
            tries++;
        }

        Assert.assertNull("Tried 25 times to delete all banners, but there are still banners present.", anyBanner());
    }

    public WebElement getReleaseBanner()
    {
        return Locator.tagWithClass("div" , "release-note-container").findElementOrNull(getDriver());
    }

    public boolean hasReleaseBanner()
    {
        return getReleaseBanner() != null;
    }

    public void dismissReleaseNoteBanner()
    {
        WebElement releaseBanner = getReleaseBanner();
        if (releaseBanner != null)
            Locator.xpath("//i[contains(@class, 'fa-times-circle')]").findElement(releaseBanner).click();
    }

    public void clickReleaseNoteLink()
    {
        getReleaseBanner().findElement(Locator.linkContainingText("See what's new.")).click();
    }

    public String getReleaseBannerMsg()
    {
        return Locator.tagWithClass("div", "notification-item").findElement(getReleaseBanner()).getText().trim();
    }

}
