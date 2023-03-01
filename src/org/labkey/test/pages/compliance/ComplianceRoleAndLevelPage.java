/*
 * Copyright (c) 2017-2018 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.pages.compliance;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ComplianceRoleAndLevelPage extends LabKeyPage
{

    BaseWebDriverTest _test;

    public ComplianceRoleAndLevelPage(BaseWebDriverTest test)
    {
        super(test);
        _test = test;
        waitForText("Select your desired role and PHI level. Note that this choice will be logged.");
    }

    public ComplianceRoleAndLevelPage setPhiLevel(PhiLevel level)
    {
        elementCache().phiLevel.selectComboBoxItem(level.getText());
        return this;
    }

    public String getPhiLevel()
    {
        return getFormElement(Locator.xpath("//input[@name='phi']"));
    }

    public List<String> getEnabledPhiLevels()
    {
        List<String> options = elementCache().phiLevel.getComboBoxEnabledOptions();
        elementCache().arrowTrigger.click();
        return options;
    }

    public List<String> getDisabledPhiLevels()
    {
        List<String> options = elementCache().phiLevel.getComboBoxDisabledOptions();
        elementCache().arrowTrigger.click();
        return options;
    }

    public ComplianceRoleAndLevelPage setRole(ComplianceRole role)
    {
        return setRole(role, null);
    }

    // Code Review Feedback:
    // Duplicate code. Move this switch into the element cache to share with isRoleEnabled.
    // This block would then look like: elementCache().getRoleRadio(role).check();
    public ComplianceRoleAndLevelPage setRole(ComplianceRole role, @Nullable String irbValue)
    {
        switch (role)
        {
            case RESEARCHWITHWAIVER:
                elementCache().researchWithWavier.check();
                break;
            case RESEARCHWITHHIPAA:
                elementCache().researchWithHipaa.check();
                break;
            case RESEARCHOPS:
                elementCache().researchOps.check();
                break;
            case HEALTHCAREOPS:
                elementCache().healthcareOps.check();
                break;
            case QA:
                elementCache().qualityImprovement.check();
                break;
            case PUBLICHEALTHREPORTING:
                elementCache().publicHealthReporting.check();
                break;
        }

        if(null != irbValue)
            setIrb(role, irbValue);

        return this;
    }

    public ComplianceRole getSelectedRole()
    {
        if(elementCache().researchWithWavier.isChecked())
            return ComplianceRole.RESEARCHWITHWAIVER;
        else if(elementCache().researchWithHipaa.isChecked())
            return ComplianceRole.RESEARCHWITHHIPAA;
        else if(elementCache().researchOps.isChecked())
            return ComplianceRole.RESEARCHOPS;
        else if(elementCache().healthcareOps.isChecked())
            return ComplianceRole.HEALTHCAREOPS;
        else if(elementCache().qualityImprovement.isChecked())
            return ComplianceRole.QA;
        else if(elementCache().publicHealthReporting.isChecked())
            return ComplianceRole.PUBLICHEALTHREPORTING;
        else
            return null;
    }

    // Code Review Feedback:
    // See setRole comment
    // This becomes: return elementCache().getRoleRadio(role).isEnabled();
    public boolean isRoleEnabled(ComplianceRole role)
    {
        boolean isEnabled = false;
        switch (role)
        {
            case RESEARCHWITHWAIVER:
                isEnabled = elementCache().researchWithWavier.isEnabled();
                break;
            case RESEARCHWITHHIPAA:
                isEnabled = elementCache().researchWithHipaa.isEnabled();
                break;
            case RESEARCHOPS:
                isEnabled = elementCache().researchOps.isEnabled();
                break;
            case HEALTHCAREOPS:
                isEnabled = elementCache().healthcareOps.isEnabled();
                break;
            case QA:
                isEnabled = elementCache().qualityImprovement.isEnabled();
                break;
            case PUBLICHEALTHREPORTING:
                isEnabled = elementCache().publicHealthReporting.isEnabled();
                break;
        }

        return isEnabled;
    }

    // Code Review Feedback:
    // Similar to my setRole comment. Move this into the element cache: elementCache().getIrbInput(role).set(irbValue);
    // That would also fail if the caller provides an invalid role
    // (though you'd probably want to catch that and throw something more informative than an NPE).
    public ComplianceRoleAndLevelPage setIrb(ComplianceRole role, String irbValue)
    {
        switch (role)
        {
            case RESEARCHWITHWAIVER:
                setFormElement(elementCache().researchWithWavierIrb, irbValue);
                break;
            case RESEARCHWITHHIPAA:
                setFormElement(elementCache().researchWithHipaaIrb, irbValue);
                break;
            case RESEARCHOPS:
                setFormElement(elementCache().researchOpsIrb, irbValue);
                break;
        }

        return this;
    }

    public String getIrb(ComplianceRole role)
    {
        String irbValue = "";

        switch (role)
        {
            case RESEARCHWITHWAIVER:
                irbValue = getFormElement(elementCache().researchWithWavierIrb);
                break;
            case RESEARCHWITHHIPAA:
                irbValue = getFormElement(elementCache().researchWithHipaaIrb);
                break;
            case RESEARCHOPS:
                irbValue = getFormElement(elementCache().researchOpsIrb);
                break;
        }

        return irbValue;
    }

    public ComplianceTermsOfUsePage clickNext()
    {
        // Code Review Feedback:
        // Use clickAndWait(..) if this is expected to trigger a page load.
        click(Locator.linkWithText("Next"));
        return new ComplianceTermsOfUsePage(_test);
    }

    @Override
    protected ComplianceRoleAndLevelPage.ElementCache elementCache()
    {
        return (ComplianceRoleAndLevelPage.ElementCache) super.elementCache();
    }

    @Override
    protected ComplianceRoleAndLevelPage.ElementCache newElementCache()
    {
        return new ComplianceRoleAndLevelPage.ElementCache();
    }

    // Code Review Feedback:
    // Use shared code. The radio button labels are defined in the ComplianceRole enum. Reference that rather than defining the same string twice.
    // Could probably just make a single method to find these radio buttons:
    // RadioButton findRoleRadio(ComplianceRole role)
    // {
    //     return RadioButton.RadioButton().withLabel(role.getText()).find(this);
    // }
    // That would allow you to loop through the roles or find a particular one without a switch like above.
    private class ElementCache extends LabKeyPage.ElementCache
    {
        RadioButton researchWithWavier = RadioButton.RadioButton().withLabel("Research with Waiver of HIPAA Authorization/Consent").findWhenNeeded(this);
        // Code Review Feedback:
        // Avoid finding things by index. Is there a different way to distinguish between the IRB inputs? This might fail in confusing ways if the page layout was changed
        WebElement researchWithWavierIrb = Locator.xpath("(//input[@name='irb'])[1]").findWhenNeeded(this);
        RadioButton researchWithHipaa = RadioButton.RadioButton().withLabel("Research with HIPAA Authorization/Consent").findWhenNeeded(this);
        WebElement researchWithHipaaIrb = Locator.xpath("(//input[@name='irb'])[2]").findWhenNeeded(this);
        RadioButton researchOps = RadioButton.RadioButton().withLabel("Research Operations").findWhenNeeded(this);
        WebElement researchOpsIrb = Locator.xpath("(//input[@name='irb'])[3]").findWhenNeeded(this);
        RadioButton healthcareOps = RadioButton.RadioButton().withLabel("Healthcare Operations").findWhenNeeded(this);
        RadioButton qualityImprovement = RadioButton.RadioButton().withLabel("Quality Improvement/Quality Assurance").findWhenNeeded(this);
        RadioButton publicHealthReporting = RadioButton.RadioButton().withLabel("Public Health Reporting").findWhenNeeded(this);
        ComboBox phiLevel = ComboBox.ComboBox(getDriver()).withLabel("PHI Level").findWhenNeeded(this);
        WebElement arrowTrigger = Locator.xpath("//div[contains(@class,'arrow')]").findWhenNeeded(this);
    }

    public enum PhiLevel
    {
        NOPHI("Coded/No PHI"),
        LIMITED("Limited PHI"),
        FULL("Identified/Full PHI"),
        RESTRICTED("Restricted PHI");

        private String _text;

        PhiLevel(String text)
        {
            _text = text;
        }

        public String getText()
        {
            return _text;
        }
    }

    public enum ComplianceRole
    {
        RESEARCHWITHWAIVER("Research with Waiver of HIPAA Authorization/Consent"),
        RESEARCHWITHHIPAA("Research with HIPAA Authorization/Consent"),
        RESEARCHOPS("Research Operations"),
        HEALTHCAREOPS("Healthcare Operations"),
        QA("Quality Improvement/Quality Assurance"),
        PUBLICHEALTHREPORTING("Public Health Reporting");

        private String _text;

        ComplianceRole(String text)
        {
            _text = text;
        }

        public String getText()
        {
            return _text;
        }
    }
}
