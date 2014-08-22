package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;

public class BoxPlotReportHelper extends GenericChartHelper
{
    /**
     * Creating a plot from manage views page
     */
    public BoxPlotReportHelper(BaseWebDriverTest test)
    {
        super(test, null, "Cohort", null);
    }

    public BoxPlotReportHelper(BaseWebDriverTest test, String sourceDataset)
    {
        super(test, null, "Cohort", null);
        _sourceQuery = sourceDataset;
    }

    public BoxPlotReportHelper(BaseWebDriverTest test, String yMeasure, String xMeasure, String title)
    {
        super(test, yMeasure, xMeasure, title);
    }
}
