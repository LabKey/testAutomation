package org.labkey.test.components.domain;

import org.labkey.test.params.FieldDefinition;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AdvancedFieldSetting implements Consumer<AdvancedSettingsDialog>
{
    private final Consumer<AdvancedSettingsDialog> action;

    public AdvancedFieldSetting(Consumer<AdvancedSettingsDialog> action)
    {
        this.action = action;
    }

    public <T> AdvancedFieldSetting(BiConsumer<AdvancedSettingsDialog, T> action, T value)
    {
        this(dlg -> action.accept(dlg, value));
    }

    @Override
    public void accept(AdvancedSettingsDialog advancedSettingsDialog)
    {
        action.accept(advancedSettingsDialog);
    }

    public static AdvancedFieldSetting showInDefault(Boolean val)
    {
        return new AdvancedFieldSetting(AdvancedSettingsDialog::showInDefaultView, val);
    }
    public static AdvancedFieldSetting shownInUpdateView(Boolean val)
    {
        return new AdvancedFieldSetting(AdvancedSettingsDialog::showInUpdateView, val);
    }
    public static AdvancedFieldSetting shownInInsertView(Boolean val)
    {
        return new AdvancedFieldSetting(AdvancedSettingsDialog::showInInsertView, val);
    }
    public static AdvancedFieldSetting shownInDetailsView(Boolean val)
    {
        return new AdvancedFieldSetting(AdvancedSettingsDialog::showInDetailsView, val);
    }
    public static AdvancedFieldSetting defaultType(FieldDefinition.DefaultType val)
    {
        return new AdvancedFieldSetting(AdvancedSettingsDialog::setDefaultValueType, val);
    }
    public static AdvancedFieldSetting excludeFromShifting(Boolean val)
    {
        return new AdvancedFieldSetting(AdvancedSettingsDialog::excludeFromDateShifting, val);
    }
    public static AdvancedFieldSetting measure(Boolean val)
    {
        return new AdvancedFieldSetting(AdvancedSettingsDialog::setMeasure, val);
    }
    public static AdvancedFieldSetting dimension(Boolean val)
    {
        return new AdvancedFieldSetting(AdvancedSettingsDialog::setDimension, val);
    }
    public static AdvancedFieldSetting recommendedVariable(Boolean val)
    {
        return new AdvancedFieldSetting(AdvancedSettingsDialog::setRecommendedVariable, val);
    }
    public static AdvancedFieldSetting mvEnabled(Boolean val)
    {
        return new AdvancedFieldSetting(AdvancedSettingsDialog::setMissingValuesEnabled, val);
    }
    public static AdvancedFieldSetting PHI(FieldDefinition.PhiSelectType val)
    {
        return new AdvancedFieldSetting(AdvancedSettingsDialog::setPHILevel, val);
    }

}
