package org.labkey.test.params.experiment;

import org.apache.commons.lang3.StringUtils;

/**
 * Enum of the various storage amount types.
 */
public enum InventoryMetricUnit
{
    // If you add a value here you will also need to update SMSampleTypeDefinition.getInventoryMetricUnit.
    G("g", "g (grams)"),
    MG("mg", "mg (milligrams)"),
    KG("kg", "kg (kilograms)"),
    ML("mL", "mL (milliliters)"),
    UL("uL", "uL (microliters)"),
    L("L", "L (liters)"),
    UNIT("unit", "unit"),
    UNKNOWN("unknown", "unknown"); // Used by getInventoryMetricUnit. Return if an unknown value is in the UI.

    private final String _value; // Used when creating a sample type with the API and setting this property.
    private final String _label; // Used when setting the property through the UI.

    InventoryMetricUnit(String value, String label)
    {
        _value = value;
        _label = label;
    }

    public String getLabel()
    {
        return _label;
    }

    public String getValue()
    {
        return _value;
    }

    public static String getStandardUnit(String unitString)
    {
        if (StringUtils.isEmpty(unitString))
            return unitString;
        for (InventoryMetricUnit unit : values())
        {
            if (unit.getValue().toLowerCase().equals(unitString))
                return unit.getValue();
        }
        return unitString;
    }
}
