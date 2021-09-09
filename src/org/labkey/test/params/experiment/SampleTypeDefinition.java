package org.labkey.test.params.experiment;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.components.ui.domainproperties.samples.SampleTypeDesigner;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.property.DomainProps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.labkey.test.util.exp.SampleTypeAPIHelper.SAMPLE_TYPE_DOMAIN_KIND;

/**
 * Defines a Sample Type. Suitable for use with UI and API helpers.
 * 'exp.materials'
 */
public class SampleTypeDefinition extends DomainProps
{
    private String _name;
    private String _nameExpression;
    private String _description;
    private String _autoLinkDataToStudy;
    private String _autoLinkedDatasetCategory;
    private List<FieldDefinition> _fields = new ArrayList<>();
    private Map<String, String> _parentAliases = new HashMap<>();
    // Indicates which parent aliases reference 'exp.dataInputs' instead of 'exp.materialInputs'
    private Set<String> _dataParentAliases = new HashSet<>();

    // Currently these values are only used by the SampleManager module.
    private MetricUnit _inventoryMetricUnit;
    private String _labelColor;

    public SampleTypeDefinition(String name)
    {
        _name = name;
    }

    public String getName()
    {
        return _name;
    }

    public SampleTypeDefinition setName(String name)
    {
        _name = name;
        return this;
    }

    public String getNameExpression()
    {
        return _nameExpression;
    }

    public SampleTypeDefinition setNameExpression(String nameExpression)
    {
        _nameExpression = nameExpression;
        return this;
    }

    public String getDescription()
    {
        return _description;
    }

    public SampleTypeDefinition setDescription(String description)
    {
        _description = description;
        return this;
    }

    public String getAutoLinkDataToStudy()
    {
        return _autoLinkDataToStudy;
    }

    public SampleTypeDefinition setAutoLinkDataToStudy(String value)
    {
        _autoLinkDataToStudy = value;
        return this;
    }

    public String getLinkedDatasetCategory()
    {
        return _autoLinkedDatasetCategory;
    }

    public SampleTypeDefinition setLinkedDatasetCategory(String value)
    {
        _autoLinkedDatasetCategory = value;
        return this;
    }

    protected MetricUnit getInventoryMetricUnit()
    {
        return _inventoryMetricUnit;
    }

    protected SampleTypeDefinition setInventoryMetricUnit(MetricUnit inventoryMetricUnit)
    {
        _inventoryMetricUnit = inventoryMetricUnit;
        return this;
    }

    protected String getLabelColor()
    {
        return _labelColor;
    }

    protected SampleTypeDefinition setLabelColor(String color)
    {
        _labelColor = color;
        return this;
    }

    @NotNull
    public List<FieldDefinition> getFields()
    {
        return _fields;
    }

    public SampleTypeDefinition setFields(@NotNull List<FieldDefinition> fields)
    {
        _fields = new ArrayList<>(fields);
        return this;
    }

    public SampleTypeDefinition addField(@NotNull FieldDefinition field)
    {
        _fields.add(field);
        return this;
    }

    @NotNull
    public Map<String, String> getParentAliases()
    {
        return _parentAliases;
    }

    public SampleTypeDefinition setParentAliases(@NotNull Map<String, String> parentAliases)
    {
        _parentAliases = new HashMap<>(parentAliases);
        _dataParentAliases.clear();
        return this;
    }

    /**
     * Add an import alias referencing the specified Data Class ('exp.dataInputs')
     */
    public SampleTypeDefinition addDataParentAlias(@NotNull String columnName, String dataClassName)
    {
        _parentAliases.put(columnName, dataClassName);
        _dataParentAliases.add(columnName);
        return this;
    }

    /**
     * Add an import alias referencing the specified Sample Type ('exp.materialInputs')
     */
    public SampleTypeDefinition addParentAlias(@NotNull String columnName, String sampleTypeName)
    {
        _parentAliases.put(columnName, sampleTypeName);
        _dataParentAliases.remove(columnName);
        return this;
    }

    public SampleTypeDefinition addParentAlias(@NotNull String columnName)
    {
        return addParentAlias(columnName, SampleTypeDesigner.CURRENT_SAMPLE_TYPE);
    }

    /*
    DomainProps
     */

    @NotNull
    @Override
    protected Domain getDomainDesign()
    {
        Domain domain = new Domain(getName());
        ArrayList<PropertyDescriptor> fields = new ArrayList<>(getFields());
        fields.add(0, new PropertyDescriptor("Name", "string"));
        domain.setFields(fields);
        domain.setDescription(getDescription());
        return domain;
    }

    @NotNull
    @Override
    protected String getKind()
    {
        return SAMPLE_TYPE_DOMAIN_KIND;
    }

    @NotNull
    @Override
    protected Map<String, Object> getOptions()
    {
        Map<String, Object> options = new HashMap<>();
        options.put("name", getName());
        options.put("nameExpression", getNameExpression());
        if (!getParentAliases().isEmpty())
        {
            Map<String, String> importAliases = new HashMap<>();
            for (String columnName : getParentAliases().keySet())
            {
                String aliasTarget = getParentAliases().get(columnName);
                if (aliasTarget == null || aliasTarget.equals(SampleTypeDesigner.CURRENT_SAMPLE_TYPE))
                {
                    aliasTarget = getName();
                }
                String inputPrefix = _dataParentAliases.contains(columnName) ? "dataInputs/" : "materialInputs/";
                String aliasTable = inputPrefix + aliasTarget;
                importAliases.put(columnName, aliasTable);
            }
            options.put("importAliases", importAliases);
        }
        if (getAutoLinkDataToStudy() != null)
        {
            options.put("autoLinkTargetContainerId", getAutoLinkDataToStudy());
        }
        if (getLinkedDatasetCategory() != null)
        {

            options.put("autoLinkCategory", getLinkedDatasetCategory());
        }
        if (getInventoryMetricUnit() != null)
        {
            options.put("metricUnit", getInventoryMetricUnit().getValue());
        }
        if (getLabelColor() != null)
        {
            options.put("labelColor", getLabelColor());
        }
        return options;
    }

    @Override
    protected @NotNull String getSchemaName()
    {
        return "exp.materials";
    }

    @Override
    protected @NotNull String getQueryName()
    {
        return getName();
    }
}
