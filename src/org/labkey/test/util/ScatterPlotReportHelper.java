package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;

public class ScatterPlotReportHelper extends GenericChartHelper
{
    ScatterPlotReportHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    public ScatterPlotReportHelper(BaseWebDriverTest test, String sourceQuery)
    {
        super(test, sourceQuery);
    }

    public ScatterPlotReportHelper(BaseWebDriverTest test, String yMeasure, String xMeasure, String title)
    {
        super(test, yMeasure, xMeasure, title);
    }
}
