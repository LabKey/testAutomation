/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.params;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.query.Filter;
import org.labkey.test.components.html.OptionSelect;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldDefinition extends PropertyDescriptor
{
    private static final String SNOWMAN = "\u2603";
    private static final String ANGSTROM = "\u00C5";
    private static final String A_UMLAUT = "\u00E4";
    // Non-alphanumeric characters supported for field names
    public static final String TRICKY_CHARACTERS = "><&$,/%\\'}{][ \"" + SNOWMAN + ANGSTROM + A_UMLAUT;

    // for UI helpers
    private ColumnType _type;
    private String _principalConceptSearchSourceOntology;
    private String _principalConceptSearchExpression;
    private ExpSchema.DerivationDataScopeType _aliquotOption;

    // Stash validator collection to avoid having to convert back from JSON Maps
    private List<FieldValidator<?>> _validators;

    // Collection of JSON properties not explicitly known by 'PropertyDescriptor'
    private final Map<String, Object> _extraFieldProperties = new HashMap<>();

    /**
     * Define a non-lookup field of the specified type
     * @param name field name
     * @param type field type
     */
    public FieldDefinition(String name, ColumnType type)
    {
        setName(name);
        setType(type);
        // Clear out default advanced properties to avoid opening advanced field properties dialog
        setMeasure(null);
        setDimension(null);
        setMvEnabled(null);
    }

    /**
     * Define a String field
     * @param name field name
     */
    public FieldDefinition(String name)
    {
        this(name, ColumnType.String);
    }

    @Override
    public Map<String, Object> getAllProperties()
    {
        return _extraFieldProperties;
    }

    public ColumnType getType()
    {
        return _type;
    }

    private void setType(ColumnType type)
    {
        if (type == ColumnType.Lookup)
        {
            throw new IllegalArgumentException("Use IntLookup or StringLookup to create lookup fields");
        }

        _type = type;
        if (type.getLookupInfo() != null)
        {
            super.setLookup(type.getLookupInfo().getSchema(),
                    type.getLookupInfo().getTable(),
                    type.getLookupInfo().getFolder());
        }
        super.setRangeURI(type.getRangeURI());
        setFieldProperty("conceptURI", type.getConceptURI());
    }

    @Override
    public PropertyDescriptor setRangeURI(String rangeURI)
    {
        throw new UnsupportedOperationException("Field type should be set at instantiation time.");
    }

    // Override return type of PropertyDescriptor setters

    @Override
    public FieldDefinition setLabel(String label)
    {
        super.setLabel(label);
        return this;
    }

    @Override
    public FieldDefinition setDescription(String description)
    {
        super.setDescription(description);
        return this;
    }

    @Override
    public FieldDefinition setFormat(String format)
    {
        super.setFormat(format);
        return this;
    }

    @Override
    public FieldDefinition setMvEnabled(Boolean mvEnabled)
    {
        super.setMvEnabled(mvEnabled);
        return this;
    }

    @Override
    public FieldDefinition setRequired(Boolean required)
    {
        super.setRequired(required);
        return this;
    }

    @Override
    public FieldDefinition setHidden(Boolean hidden)
    {
        super.setHidden(hidden);
        return this;
    }

    public LookupInfo getLookup()
    {
        return _type.getLookupInfo();
    }

    @Override
    public FieldDefinition setLookup(String schema, String query, String container)
    {
        throw new UnsupportedOperationException("Lookup info should be set at instantiation time.");
    }

    // Additional field properties, not currently supported by 'PropertyDescriptor'

    private Object getFieldProperty(String propertyName)
    {
        return _extraFieldProperties.get(propertyName);
    }

    private void setFieldProperty(String propertyName, Object value)
    {
        _extraFieldProperties.put(propertyName, value);
    }

    public List<FieldValidator<?>> getValidators()
    {
        return _validators;
    }

    public FieldDefinition setValidators(List<FieldValidator<?>> validators)
    {
        JSONArray propertyValidators = null;
        if (validators != null)
        {
            propertyValidators = new JSONArray();
            validators.stream().map(FieldValidator::toJSONObject).forEachOrdered(propertyValidators::put);
        }
        setFieldProperty("propertyValidators", propertyValidators);
        _validators = validators;
        return this;
    }

    public String getURL()
    {
        return (String) getFieldProperty("URL");
    }

    public FieldDefinition setURL(String url)
    {
        setFieldProperty("URL", url);
        return this;
    }

    public String getImportAliases()
    {
        return (String) getFieldProperty("importAliases");
    }

    public FieldDefinition setImportAliases(String importAliases)
    {
        setFieldProperty("importAliases", importAliases);
        return this;
    }

    public Integer getScale()
    {
        return (Integer) getFieldProperty("scale");
    }

    public FieldDefinition setScale(Integer scale)
    {
        setFieldProperty("scale", scale);
        return this;
    }

    public Boolean isPrimaryKey()
    {
        return (Boolean) getFieldProperty("isPrimaryKey");
    }

    public FieldDefinition setPrimaryKey(Boolean isPrimaryKey)
    {
        setFieldProperty("isPrimaryKey", isPrimaryKey);
        return this;
    }

    public Boolean getLookupValidatorEnabled()
    {
        return (Boolean) getFieldProperty("lookupValidatorEnabled");
    }

    public FieldDefinition setLookupValidatorEnabled(Boolean lookupValidatorEnabled)
    {
        setFieldProperty("lookupValidatorEnabled", lookupValidatorEnabled);
        return this;
    }

    public Boolean getShownInDetailsView()
    {
        return (Boolean) getFieldProperty("shownInDetailsView");
    }

    public FieldDefinition setShownInDetailsView(Boolean shownInDetailsView)
    {
        setFieldProperty("shownInDetailsView", shownInDetailsView);
        return this;
    }

    public Boolean getShownInInsertView()
    {
        return (Boolean) getFieldProperty("shownInInsertView");
    }

    public FieldDefinition setShownInInsertView(Boolean shownInInsertView)
    {
        setFieldProperty("shownInInsertView", shownInInsertView);
        return this;
    }

    public Boolean getShownInUpdateView()
    {
        return (Boolean) getFieldProperty("shownInUpdateView");
    }

    public FieldDefinition setShownInUpdateView(Boolean shownInUpdateView)
    {
        setFieldProperty("shownInUpdateView", shownInUpdateView);
        return this;
    }

    public FieldDefinition setPHI(PhiSelectType phiType)
    {
        super.setPHI(phiType.name());
        return this;
    }

    public PhiSelectType getPhiLevel()
    {
        if (StringUtils.isBlank(getPHI()))
        {
            return PhiSelectType.NotPHI;
        }
        else
        {
            return PhiSelectType.valueOf(getPHI());
        }
    }

    public String getSourceOntology()
    {
        return (String) getFieldProperty("sourceOntology");
    }

    public FieldDefinition setSourceOntology(String sourceOntology)
    {
        setFieldProperty("sourceOntology", sourceOntology);
        return this;
    }

    public String getConceptLabelColumn()
    {
        return (String) getFieldProperty("conceptLabelColumn");
    }

    public FieldDefinition setConceptLabelColumn(String conceptLabelColumn)
    {
        setFieldProperty("conceptLabelColumn", conceptLabelColumn);
        return this;
    }

    public String getConceptImportColumn()
    {
        return (String) getFieldProperty("conceptImportColumn");
    }

    public FieldDefinition setConceptImportColumn(String conceptImportColumn)
    {
        setFieldProperty("conceptImportColumn", conceptImportColumn);
        return this;
    }

    public String getConceptSubTree()
    {
        return (String) getFieldProperty("conceptSubtree");
    }

    public FieldDefinition setConceptSubtree(String subtree)
    {
        setFieldProperty("conceptSubtree", subtree);
        return this;
    }

    public String getPrincipalConceptCode()
    {
        return (String) getFieldProperty("principalConceptCode");
    }

    public FieldDefinition setPrincipalConceptCode(String principalConceptCode)
    {
        setFieldProperty("principalConceptCode", principalConceptCode);
        return this;
    }

    public String getPrincipalConceptSearchSourceOntology()
    {
        return _principalConceptSearchSourceOntology;
    }

    public String getPrincipalConceptSearchExpression()
    {
        return _principalConceptSearchExpression;
    }

    public FieldDefinition setPrincipalConceptSearchExpression(String ontologyName, String searchExpression)
    {
        _principalConceptSearchSourceOntology = ontologyName;
        _principalConceptSearchExpression = searchExpression;
        return this;
    }

    public FieldDefinition setTextChoiceValues(List<String> values)
    {
        Assert.assertEquals("Invalid field type for text choice values.", ColumnType.TextChoice, getType());
        setValidators(List.of(new FieldDefinition.TextChoiceValidator(values)));
        return this;
    }

    public ExpSchema.DerivationDataScopeType getAliquotOption()
    {
        return _aliquotOption;
    }

    public void setAliquotOption(ExpSchema.DerivationDataScopeType aliquotOption)
    {
        super.setDerivationDataScope(aliquotOption.name());
        _aliquotOption = aliquotOption;
    }

    public enum RangeType
    {
        Equals("Equals", Filter.Operator.EQUAL),
        NE("Does Not Equal", Filter.Operator.NEQ),
        GT("Greater than", Filter.Operator.GT),
        GTE("Greater than or Equals", Filter.Operator.GTE),
        LT("Less than", Filter.Operator.LT),
        LTE("Less than or Equals", Filter.Operator.LTE);
        private final String _description;
        private final Filter.Operator _operator;

        RangeType(String description, Filter.Operator operator)
        {
            _description = description;
            _operator = operator;
        }

        public String toString()
        {
            return _description;
        }

        public Filter.Operator getOperator()
        {
            return _operator;
        }
    }

    public static class SampleColumnType implements ColumnType
    {
        private final LookupInfo _lookupInfo;

        public SampleColumnType(String sampleTypeName)
        {
            _lookupInfo = new LookupInfo(null, "samples", sampleTypeName);
        }

        @Override
        public String getLabel()
        {
            throw new IllegalStateException("UI helpers don't support this method of defining sample columns");
        }

        @Override
        public boolean isLookup()
        {
            return false;
        }

        @Override
        public String getRangeURI()
        {
            return ColumnType.Sample.getRangeURI();
        }

        @Override
        public String getConceptURI()
        {
            return ColumnType.Sample.getConceptURI();
        }

        @Override
        public LookupInfo getLookupInfo()
        {
            return _lookupInfo;
        }

    }

    // Temporary, for 'ColumnType.values()'
    private static final List<ColumnType> COLUMN_TYPES = List.of(
            ColumnType.MultiLine, ColumnType.Integer, ColumnType.String, ColumnType.Subject, ColumnType.DateAndTime,
            ColumnType.Boolean, ColumnType.Double, ColumnType.Decimal, ColumnType.File, ColumnType.Flag,
            ColumnType.Attachment, ColumnType.User, ColumnType.Lookup, ColumnType.OntologyLookup, ColumnType.VisitId,
            ColumnType.VisitDate, ColumnType.Sample, ColumnType.Barcode, ColumnType.TextChoice, ColumnType.SMILES
    );
    
    public interface ColumnType
    {
        ColumnType MultiLine = new ColumnTypeImpl("Multi-Line Text", "multiLine");
        ColumnType Integer = new ColumnTypeImpl("Integer", "int");
        ColumnType String = new ColumnTypeImpl("Text", "string");
        ColumnType Subject = new ColumnTypeImpl("Subject/Participant", "string", "http://cpas.labkey.com/Study#ParticipantId", null);
        ColumnType DateAndTime = new ColumnTypeImpl("Date Time", "dateTime");
        ColumnType Boolean = new ColumnTypeImpl("Boolean", "boolean");
        ColumnType Double = new ColumnTypeImpl("Number (Double)", "float");
        ColumnType Decimal = new ColumnTypeImpl("Decimal (floating point)", "double");
        ColumnType File = new ColumnTypeImpl("File", "http://cpas.fhcrc.org/exp/xml#fileLink");
        ColumnType Flag = new ColumnTypeImpl("Flag", "string", "http://www.labkey.org/exp/xml#flag", null);
        ColumnType Attachment = new ColumnTypeImpl("Attachment", "attachment");
        ColumnType User = new ColumnTypeImpl("User", "int", null, new IntLookup("core", "users"));
        @Deprecated(since = "22.10") // 'Lookup' isn't a type outside of the UI
        ColumnType Lookup = new ColumnTypeImpl("Lookup", null);
        ColumnType OntologyLookup = new ColumnTypeImpl("Ontology Lookup", "string", "http://www.labkey.org/types#conceptCode", null);
        ColumnType VisitId = new ColumnTypeImpl("Visit ID", "double", "http://cpas.labkey.com/Study#VisitId", null);
        ColumnType VisitDate = new ColumnTypeImpl("Visit Date", "dateTime", "http://cpas.labkey.com/Study#VisitId", null);
        ColumnType Sample = new ColumnTypeImpl("Sample", "int", "http://www.labkey.org/exp/xml#sample", new IntLookup( "exp", "Materials"));
        ColumnType Barcode = new ColumnTypeImpl("Unique ID", "string", "http://www.labkey.org/types#storageUniqueId", null);
        ColumnType TextChoice = new ColumnTypeImpl("Text Choice", "string", "http://www.labkey.org/types#textChoice", null);
        ColumnType SMILES = new ColumnTypeImpl("SMILES", "string", "http://www.labkey.org/exp/xml#smiles", null);

        /**
         * UI: The Option text for the column type.
         * API: Unused
         */
        String getLabel();

        /**
         * UI: Is this a plain lookup field. (Formerly 'ColumnType.Lookup'). Some column type have lookup info but are
         * defined as lookups in the domain designer.
         * API: Unused
         */
        boolean isLookup();

        /**
         * UI: Unused
         * API: The value used by the server to determine the field's type
         */
        String getRangeURI();

        /**
         * UI: Unused
         * API: For definiting column types that add special functionality.
         */
        default String getConceptURI()
        {
            return null;
        }

        /**
         * UI: Lookup info for plain lookup fields ({@link #isLookup()} == true)
         * API: Lookup info for plain and special (e.g. 'Sample') lookup fields
         */
        default FieldDefinition.LookupInfo getLookupInfo()
        {
            return null;
        }

        /**
         * @deprecated Bridge for converting away from enum
         */
        @Deprecated (since = "22.10")
        static List<ColumnType> values()
        {
            return COLUMN_TYPES;
        }
    }

    public enum ScaleType implements OptionSelect.SelectOption
    {
        LINEAR("Linear"),
        LOG("Log");

        private final String _text;

        ScaleType(String text)
        {
            _text = text;
        }

        @Override
        public String getValue()
        {
            return name();
        }

        @Override
        public String getText()
        {
            return _text;
        }
    }

    public enum DefaultType implements OptionSelect.SelectOption
    {
        FIXED_EDITABLE("Editable default"),
        LAST_ENTERED("Last entered"),
        FIXED_NON_EDITABLE("Fixed value");

        private final String _text;

        DefaultType(String text)
        {
            _text = text;
        }

        @Override
        public String getValue()
        {
            return name();
        }

        @Override
        public String getText()
        {
            return _text;
        }
    }

    /**
     * Represents possible PHI levels for a field
     *
     * @see org.labkey.api.data.PHI
     */
    public enum PhiSelectType implements OptionSelect.SelectOption
    {
        // Ordered from least to most restrictive
        NotPHI("Not PHI", null),
        Limited("Limited PHI", "Limited PHI Reader"),
        PHI("Full PHI", "Full PHI Reader"),
        Restricted("Restricted PHI", "Restricted PHI Reader");

        private final String _text;
        private final String _roleName;

        PhiSelectType(String text, String roleName)
        {
            _text = text;
            _roleName = roleName;
        }

        @Override
        public String getValue()
        {
            return name();
        }

        @Override
        public String getText()
        {
            return _text;
        }

        public int getRank()
        {
            return ordinal();
        }

        public String getRoleName()
        {
            return _roleName;
        }
    }

    public static class LookupInfo implements ColumnType
    {
        private final String _folder;
        private final String _schema;
        private final String _table;
        private ColumnType _tableType;

        /**
         * @deprecated Use {@link IntLookup} or {@link StringLookup}
         */
        @Deprecated (since = "22.10")
        public LookupInfo(@Nullable String folder, String schema, String table)
        {
            if (folder == null || folder.isEmpty())
            {
                _folder = null;
            }
            else if (!folder.startsWith("/"))
            {
                //container must exactly match an item in the dropdown
                _folder = "/" + folder;
            }
            else
            {
                _folder = folder;
            }

            _schema = StringUtils.trimToNull(schema);
            _table = StringUtils.trimToNull(table);
            _tableType = ColumnType.String;
        }

        public String getFolder()
        {
            return _folder;
        }

        public String getSchema()
        {
            return _schema;
        }

        public String getTable()
        {
            return _table;
        }

        public ColumnType getTableType()
        {
            return _tableType;
        }

        /**
         * @deprecated Use {@link IntLookup} or {@link StringLookup}
         */
        @Deprecated (since = "22.10")
        public LookupInfo setTableType(ColumnType tableType)
        {
            _tableType = tableType;
            return this;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            if (_folder != null)
            {
                sb.append("(Current container)");
            }
            else
            {
                sb.append(_folder);
            }
            sb.append(" : ");
            sb.append(getSchema());
            sb.append(".");
            sb.append(getTable());
            return sb.toString();
        }

        @Override
        public String getLabel()
        {
            return ColumnType.Lookup.getLabel();
        }

        @Override
        public String getRangeURI()
        {
            return _tableType.getRangeURI();
        }

        @Override
        public LookupInfo getLookupInfo()
        {
            return this;
        }

        @Override
        public boolean isLookup()
        {
            return true;
        }
    }

    private static abstract class Lookup extends LookupInfo
    {
        public Lookup(@Nullable String folder, String schema, String table, ColumnType lookupType)
        {
            super(folder, schema, table);
            super.setTableType(lookupType);
        }

        @Override
        public LookupInfo setTableType(ColumnType tableType)
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class IntLookup extends Lookup
    {
        public IntLookup(@Nullable String folder, String schema, String table)
        {
            super(folder, schema, table, ColumnType.Integer);
        }

        public IntLookup(String schema, String table)
        {
            this(null, schema, table);
        }
    }

    public static class StringLookup extends Lookup
    {
        public StringLookup(@Nullable String folder, String schema, String table)
        {
            super(folder, schema, table, ColumnType.String);
        }

        public StringLookup(String schema, String table)
        {
            this(null, schema, table);
        }
    }

    public static abstract class FieldValidator<V extends FieldValidator<V>>
    {
        private final String _name;
        private final String _description;
        private final String _message;

        private boolean _failOnMatch = false;

        protected FieldValidator(String name, String description, String message)
        {
            _name = name;
            _description = description;
            _message = message;
        }

        public String getName()
        {
            return _name;
        }

        public String getDescription()
        {
            return _description;
        }

        public String getMessage()
        {
            return _message;
        }

        public V setFailOnMatch(boolean failOnMatch)
        {
            _failOnMatch = failOnMatch;
            return getThis();
        }

        protected abstract V getThis();

        protected abstract String getExpression();

        protected abstract String getType();

        protected JSONObject getProperties()
        {
            JSONObject json = new JSONObject();
            json.put("failOnMatch", _failOnMatch);
            return json;
        }

        // Even with the <pre> tag the & needs to be escaped for a javadoc compile.
        // "expression": "~gt=34&~lt=99", & -> &#38;
        /**
         * JSON for a field validator looks something like this:
         * <pre>
         * {
         *   "description": "description",
         *   "errorMessage": "error message",
         *   "expression": "~gt=34&#38;~lt=99",
         *   "name": "V range 1",
         *   "new": true,
         *   "properties": {
         *     "failOnMatch": false
         *   },
         *   "type": "Range"
         * }
         * </pre>
         * @return Serializable representation of field validator
         */
        protected JSONObject toJSONObject()
        {
            JSONObject json = new JSONObject();
            json.put("name", _name);
            json.put("expression", getExpression());
            if (_description != null)
            {
                json.put("description", _description);
            }
            if (_message != null)
            {
                json.put("errorMessage", _message);
            }
            json.put("new", true);
            json.put("properties", getProperties());
            json.put("type", getType());

            return json;
        }
    }

    public static class RegExValidator extends FieldValidator<RegExValidator>
    {
        private final String _expression;

        public RegExValidator(String name, String description, String message, String expression)
        {
            super(name, description, message);
            _expression = expression;
        }

        @Override
        protected RegExValidator getThis()
        {
            return this;
        }

        @Override
        protected String getType()
        {
            return "RegEx";
        }

        @Override
        public String getExpression()
        {
            return _expression;
        }
    }

    public static class RangeValidator extends FieldValidator<RangeValidator>
    {
        private final RangeType _firstType;
        private final String _firstRange;
        private final RangeType _secondType;
        private final String _secondRange;

        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange)
        {
            this(name, description, message, firstType, firstRange, null, null);
        }

        public RangeValidator(String name, String description, String message, RangeType firstType, String firstRange, RangeType secondType, String secondRange)
        {
            super(name, description, message);
            _firstType = firstType;
            _firstRange = firstRange;
            _secondType = secondType;
            _secondRange = secondRange;
        }

        @Override
        protected RangeValidator getThis()
        {
            return this;
        }

        @Override
        protected String getType()
        {
            return "Range";
        }

        @Override
        protected String getExpression()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("~");
            sb.append(_firstType.getOperator().getUrlKey());
            if (_firstRange != null)
            {
                sb.append("=");
                sb.append(_firstRange);
            }
            if (_secondType != null)
            {
                sb.append("&");
                sb.append("~");
                sb.append(_secondType.getOperator().getUrlKey());
                if (_secondRange != null)
                {
                    sb.append("=");
                    sb.append(_secondRange);
                }
            }
            return sb.toString();
        }

        public RangeType getFirstType()
        {
            return _firstType;
        }

        public String getFirstRange()
        {
            return _firstRange;
        }

        public RangeType getSecondType()
        {
            return _secondType;
        }

        public String getSecondRange()
        {
            return _secondRange;
        }
    }

    /**
     * TextChoice is implemented using a validator, however it is more 'limited' in scope. The user does not name a TextChoice
     * validator or add a description or error message. A TextChoice is a lot like a look-up field, but it is not linked
     * to an external data source. The user only provides the list of (string) values that the field will display in the dropdown.
     * Another difference is that there can only be one TextChoice on a field, whereas you can have multiple validators on a field.
     */
    public static class TextChoiceValidator extends FieldValidator<TextChoiceValidator>
    {
        private final List<String> _values;

        public TextChoiceValidator(List<String> values)
        {
            // The TextChoice validator only has a name and no description or message.
            // And the name is generated (not user defined).
            super("Text Choice Validator", "", "");
            _values = Collections.unmodifiableList(values);
        }

        @Override
        protected TextChoiceValidator getThis()
        {
            return this;
        }

        @Override
        protected String getType()
        {
            return "TextChoice";
        }

        @Override
        protected String getExpression()
        {
            return String.join("|", _values);
        }

        public List<String> getValues()
        {
            return _values;
        }

    }

}

class ColumnTypeImpl implements FieldDefinition.ColumnType
{
    private final String _label; // the display value in the UI for this kind of field
    private final String _rangeURI;     // the key used inside the API
    private final String _conceptURI;
    private final FieldDefinition.LookupInfo _lookupInfo;

    ColumnTypeImpl(String label, String rangeURI, String conceptURI, FieldDefinition.LookupInfo lookupInfo)
    {
        _label = label;
        _rangeURI = rangeURI;
        _conceptURI = conceptURI;
        _lookupInfo = lookupInfo;
    }

    ColumnTypeImpl(String label, String rangeURI)
    {
        this(label, rangeURI, null, null);
    }

    @Override
    public String getLabel()
    {
        return _label;
    }

    @Override
    public boolean isLookup()
    {
        return false;
    }

    @Override
    public String getRangeURI() { return _rangeURI; }

    @Override
    public String getConceptURI()
    {
        return _conceptURI;
    }

    @Override
    public FieldDefinition.LookupInfo getLookupInfo()
    {
        return _lookupInfo;
    }
}
