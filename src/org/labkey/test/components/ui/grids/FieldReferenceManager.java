package org.labkey.test.components.ui.grids;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.labkey.test.params.FieldKey;
import org.labkey.test.util.selenium.WebElementUtils;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class FieldReferenceManager
{
    private final List<FieldReference> _fieldReferences;
    private final Map<FieldKey, FieldReference> fieldKeys = new LinkedHashMap<>();
    private final Map<String, FieldReference> fieldLabels = new LinkedHashMap<>();

    public <T extends FieldReference> FieldReferenceManager(List<T> columnHeaders)
    {
        this._fieldReferences = Collections.unmodifiableList(columnHeaders);
    }

    public List<FieldReference> getColumnHeaders()
    {
        return _fieldReferences;
    }

    /**
     * Find field by uncertain field identifier in order of precedence:
     * <ol>
     *     <li>FieldKey object</li>
     *     <li>Encoded fieldKey</li>
     *     <li>Unencoded fieldKey</li>
     *     <li>Field Label</li>
     * </ol>
     */
    public final FieldReference findFieldReference(CharSequence fieldIdentifier)
    {
        List<Supplier<FieldReference>> options;

        if (fieldIdentifier instanceof FieldKey fk)
        {
            options = List.of(() -> findColumnHeaderByFieldKey(fk)); // We know it is a FieldKey
        }
        else
        {
            options = List.of(
                () -> findColumnHeaderByFieldKey(FieldKey.fromFieldKey(fieldIdentifier)), // encoded fieldKey
                () -> findColumnHeaderByFieldKey(FieldKey.fromName(fieldIdentifier)), // unencoded fieldKey
                () -> findColumnHeaderByLabel(fieldIdentifier.toString()) // Field label
            );
        }

        return options.stream()
            .map(Supplier::get)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Unable to locate field: " + fieldIdentifier));
    }

    private FieldReference findColumnHeaderByFieldKey(FieldKey fieldIdentifier)
    {
        if (fieldKeys.containsKey(fieldIdentifier))
        {
            return fieldKeys.get(fieldIdentifier);
        }
        else if (fieldKeys.size() < _fieldReferences.size())
        {
            for (FieldReference header : _fieldReferences)
            {
                if (!fieldKeys.containsValue(header))
                {
                    FieldKey fieldKey = header.getFieldKey();
                    fieldKeys.put(fieldKey, header);
                    if (fieldKey.equals(fieldIdentifier))
                    {
                        return header;
                    }
                }
            }
        }

        return null;
    }

    private FieldReference findColumnHeaderByLabel(String label)
    {
        if (fieldLabels.containsKey(label))
        {
            return fieldLabels.get(label);
        }
        else if (fieldLabels.size() < _fieldReferences.size())
        {
            for (FieldReference header : _fieldReferences)
            {
                if (!fieldLabels.containsValue(header))
                {
                    String columnLabel = header.getLabel();
                    fieldLabels.put(columnLabel, header);
                    if (columnLabel.equals(label))
                    {
                        return header;
                    }
                }
            }
        }

        return null;
    }

    public static class FieldReference
    {
        private final WebElement _element;
        private final int _domIndex;
        private final Mutable<String> _fieldLabel = new MutableObject<>();
        private final Mutable<FieldKey> _fieldKey = new MutableObject<>();

        public FieldReference(WebElement element, int domIndex)
        {
            _element = element;
            _domIndex = domIndex;
        }

        public WebElement getElement()
        {
            return _element;
        }

        public FieldKey getFieldKey()
        {
            if (_fieldKey.getValue() == null)
            {
                String path = _element.getDomAttribute("data-fieldkey");
                if (path == null)
                {
                    // Some grids don't have a field key, but have a similar value in the ID attribute
                    path = _element.getDomAttribute("id");
                }

                if (path != null)
                {
                    _fieldKey.setValue(FieldKey.fromFieldKey(path));
                }
                else
                {
                    _fieldKey.setValue(FieldKey.EMPTY);
                }
            }
            return _fieldKey.getValue();
        }

        public String getLabel()
        {
            if (_fieldLabel.getValue() == null)
            {
                _fieldLabel.setValue(WebElementUtils.getTextContent(getElement()).trim());
            }
            return _fieldLabel.getValue();
        }

        public String getName()
        {
            return getFieldKey().getName();
        }

        public int getDomIndex()
        {
            return _domIndex;
        }
    }
}
