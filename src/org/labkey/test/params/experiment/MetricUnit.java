package org.labkey.test.params.experiment;

public interface MetricUnit<T extends MetricUnit<T>>
{
    String getLabel(); // For UI
    String getValue(); // For API
}
