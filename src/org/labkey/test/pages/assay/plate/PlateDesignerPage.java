package org.labkey.test.pages.assay.plate;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;

/**
 * Stub page class. Limited plate designer functionality is implemented in
 * {@link org.labkey.test.tests.elispotassay.ElispotAssayTest#highlightWells(String, String, String, String)}
 * TODO: Pull that functionality into this class and expand it
 */
public class PlateDesignerPage extends LabKeyPage<PlateDesignerPage.ElementCache>
{
    public PlateDesignerPage(WebDriver driver)
    {
        super(driver);
    }

    public static PlateDesignerPage beginAt(WebDriverWrapper webDriverWrapper, PlateDesignerParams params)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath(), params);
    }

    public static PlateDesignerPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, PlateDesignerParams params)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("plate", containerPath, "designer", params.toUrlParams()));
        return new PlateDesignerPage(webDriverWrapper.getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
    }

    public static class PlateDesignerParams
    {
        private final Integer rowCount;
        private final Integer colCount;
        private String assayType = "blank";
        private String templateType;

        public PlateDesignerParams(int rowCount, int colCount)
        {
            this.rowCount = rowCount;
            this.colCount = colCount;
        }

        public PlateDesignerParams setAssayType(@NotNull String assayType)
        {
            this.assayType = assayType;
            return this;
        }

        public PlateDesignerParams setTemplateType(@NotNull String templateType)
        {
            this.templateType = templateType;
            return this;
        }

        Map<String, String> toUrlParams()
        {
            Map<String, String> params = new HashMap<>();
            params.put("rowCount", rowCount.toString());
            params.put("colCount", colCount.toString());
            params.put("assayType", assayType);
            if (templateType != null)
            {
                params.put("templateType", templateType);
            }

            return params;
        }

        public Locator templateListLocator()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("new ")
                    .append(rowCount * colCount)
                    .append(" well (")
                    .append(rowCount)
                    .append("x")
                    .append(colCount)
                    .append(") ")
                    .append(assayType);
            if (templateType != null)
            {
                sb.append(" ").append(templateType);
            }
            sb.append(" template");

            return Locator.linkWithText(sb.toString());
        }

        public static PlateDesignerParams _96well()
        {
            return new PlateDesignerParams(8, 12);
        }

        public static PlateDesignerParams _384well()
        {
            return new PlateDesignerParams(16, 24);
        }
    }
}
