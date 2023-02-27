/*
 * Copyright (c) 2017 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.pages.compliance;

import org.apache.commons.lang3.tuple.Pair;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * Created by RyanS on 2/8/2016.
 */
public class ActivityDialog extends LabKeyPage
{
    public ActivityDialog(WebDriver driver)
    {
        super(driver);
    }

    public enum ActivityRole
    {
        RESEARCH_WAIVER("Research with Waiver of HIPAA Authorization/Consent", "Test terms research with waiver",true, "irb research waiver", false,1),
        RESEARCH_INFORMED("Research with HIPAA Authorization/Consent","Test terms research with consent", true, "irb research informed", false,2),
        RESEARCH_OPS("Research Operations", "Test terms research ops", true, "irb research prep", false,3),
        HEALTHCARE_OPS("Healthcare Operations","Test terms healthcare ops", true, null, false,4),
        QI("Quality Improvement/Quality Assurance","Test terms qa", true, null, false,5),
        PH("Public Health Reporting","Test terms health reporting", true, null, false,6);

        private String _description;
        private String _terms;
        private Integer _sortOrder;
        private boolean _enabled;
        private String _irb;
        private boolean _testRole;

        private ActivityRole(String description, String terms, boolean enabled, String irb, boolean testRole ,Integer sortOrder)
        {
            _description = description;
            _sortOrder = sortOrder;
            _enabled = enabled;
            _irb = irb;
            _testRole = testRole;
            _terms = terms;
        }

        public String getDescription()
        {
            return _description;
        }

        public boolean isEnabled()
        {
            return _enabled;
        }

        public Integer getIRBFieldIndex()
        {
            return _sortOrder;
        }

        public boolean isIRB()
        {
            return _sortOrder != null;
        }

        public String getIrb()
        {
            return _irb;
        }

        public String getTerms(){ return _terms;}

        public boolean isTestRole()
        {
            return _testRole;
        }
    }

    public enum PHILevel
    {
        CODED("Coded/No PHI", "NotPHI", true),
        LIMITED("Limited PHI", "Limited", false),
        IDENTIFIED("Identified/Full PHI", "PHI", false);

        private String _description;
        private String _PHI;
        private boolean _enabled;

        private PHILevel(String description, String PHI, boolean enabled)
        {
            _description = description;
            _PHI = PHI;
            _enabled = enabled;
        }

        public String getDescription()
        {
            return _description;
        }

        public String getPHI()
        {
            return _PHI;
        }

        public boolean isEnabled()
        {
            return _enabled;
        }
    }

    public void insertTerms(List<Pair<ActivityRole, PHILevel>> terms)
    {
        pushLocation();
        waitAndClick(Locator.linkWithText("Terms of Use >"));
        waitForText("TermsOfUse");
        DataRegionTable termsDRT = new DataRegionTable("query", getDriver());
        for (Pair<ActivityDialog.ActivityRole, ActivityDialog.PHILevel> term : terms)
        {
            ActivityDialog.ActivityRole role = term.getLeft();
            ActivityDialog.PHILevel level = term.getRight();
            termsDRT.clickInsertNewRow();
            waitForText("TermsOfUse");
            selectOptionByValue(Locator.name("quf_Activity"), role.name());
            setFormElement(Locator.name("quf_IRB"), role.getIrb());
            setCheckbox(Locator.checkboxByName("quf_PHI"), level.getPHI().equals("PHI"));
            setFormElement(Locator.name("quf_Term"), role.getTerms());
            setFormElement(Locator.name("quf_SortOrder"), role.getIRBFieldIndex().toString());
            clickAndWait(Locator.linkWithSpan("Submit"));
            waitForText("TermsOfUse");
        }
        popLocation();
    }

    public void setActivityDialogOptions(ActivityRole details, PHILevel phi)
    {
        setActivityDialogOptions(details, phi, "x4-");
    }

    public void setActivityDialogOptions(ActivityRole details, PHILevel phi, String cssPrefix)
    {
        waitForText("Choose Activity");
        click(Ext4Helper.Locators.ext4Radio(details.getDescription()));
        if (details.getIrb() != null)
            setFormElement(Locator.name("irb").index(details.getIRBFieldIndex()-1), details.getIrb());
        Ext4Helper.setCssPrefix(cssPrefix);
        _ext4Helper.selectComboBoxItem("PHI Level", phi.getDescription());
        click(Locator.tagWithText("span", "Next"));
    }

    //don't care what, just set the activity and accept terms
    public void setActivityDialogOptions()
    {
        setActivityDialogOptions(ActivityRole.HEALTHCARE_OPS, PHILevel.CODED);
    }

    public boolean isDialogPresent()
    {
        return isTextPresent("Choose Activity");
    }
}
