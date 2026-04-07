package org.labkey.test.params.experiment;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enum of the various storage amount types.
 */
public enum InventoryMetricUnit
{
    // If you add a value here you will also need to update SMSampleTypeDefinition.getInventoryMetricUnit.
    G("g", "g (grams)"),
    MG("mg", "mg (milligrams)"),
    KG("kg", "kg (kilograms)"),
    UG("ug", "ug (micrograms)"),
    NG("ng", "ng (nanograms)"),
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

    public static List<InventoryMetricUnit> getAllSampleTypeUnits()
    {
        return Arrays.asList(InventoryMetricUnit.G, InventoryMetricUnit.MG, InventoryMetricUnit.KG,
                InventoryMetricUnit.UG, InventoryMetricUnit.NG,
                InventoryMetricUnit.ML, InventoryMetricUnit.UL, InventoryMetricUnit.L,
                InventoryMetricUnit.UNIT);
    }

    public static List<String> getAllSampleTypeUnitNames()
    {
        return getAllSampleTypeUnits().stream().map(InventoryMetricUnit::getValue).toList();
    }

    public static List<String> getAllSampleTypeUnitLabels()
    {
        return getAllSampleTypeUnits().stream().map(InventoryMetricUnit::getLabel).toList();
    }

    public static List<String> getAllSampleUnits()
    {
        List<String> allUnits = new ArrayList<>(getAllSampleTypeUnitNames());
        allUnits.remove(InventoryMetricUnit.UNIT.getLabel());
        allUnits.addAll(getAllCountUnits());
        return allUnits;
    }

    public static List<String> getAllCountUnits()
    {
        return Arrays.asList("blocks", "bottles", "boxes", "cells", "kits", "organisms", "packs", "pieces", "slides", "syringes", "tests", "tubes", "unit", "vials");
    }

    public static List<InventoryMetricUnit> getMassSampleUnits()
    {
        return Arrays.asList(InventoryMetricUnit.G, InventoryMetricUnit.MG, InventoryMetricUnit.KG,
                InventoryMetricUnit.UG, InventoryMetricUnit.NG);
    }

    public static List<String> getMassSampleUnitNames()
    {
        return getMassSampleUnits().stream().map(InventoryMetricUnit::getValue).toList();
    }

    public static List<String> getMassSampleUnitLabels()
    {
        return getMassSampleUnits().stream().map(InventoryMetricUnit::getLabel).toList();
    }
}
