package org.labkey.test.components.ui.grids;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.params.FieldKey;
import org.labkey.test.params.WrapsFieldKey;
import org.labkey.test.util.CachingSupplier;
import org.labkey.test.util.selenium.WebElementUtils;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FieldReferenceManager
{
    private final List<FieldReference> _fieldReferences;
    private final Map<Integer, FieldReference> _fieldsByIndex;
    private final Map<FieldKey, FieldReference> _fieldKeys = new LinkedHashMap<>();
    private final Map<String, FieldReference> _fieldLabels = new LinkedHashMap<>();

    public <T extends FieldReference> FieldReferenceManager(List<T> columnHeaders)
    {
        _fieldReferences = List.copyOf(columnHeaders);
        _fieldsByIndex = columnHeaders.stream().collect(Collectors.toMap(FieldReference::getDomIndex, Function.identity()));
    }

    public List<FieldReference> getColumnHeaders()
    {
        return _fieldReferences;
    }

    public FieldReference getColumnHeader(int index)
    {
        return _fieldsByIndex.get(index);
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
    public final @NotNull FieldReference findFieldReference(CharSequence fieldIdentifier)
    {
        List<Supplier<FieldReference>> options;

        if (fieldIdentifier instanceof WrapsFieldKey fk)
        {
            options = List.of(() -> findColumnHeaderByFieldKey(fk.getFieldKey())); // We know it is a FieldKey
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

    public final @Nullable FieldReference findFieldReferenceOrNull(CharSequence fieldIdentifier)
    {
        try
        {
            return findFieldReference(fieldIdentifier);
        }
        catch (NoSuchElementException e)
        {
            return null;
        }
    }

    private FieldReference findColumnHeaderByFieldKey(FieldKey fieldIdentifier)
    {
        if (_fieldKeys.containsKey(fieldIdentifier))
        {
            return _fieldKeys.get(fieldIdentifier);
        }
        else if (_fieldKeys.size() < _fieldReferences.size())
        {
            for (FieldReference header : _fieldReferences)
            {
                if (!_fieldKeys.containsValue(header))
                {
                    FieldKey fieldKey = header.getFieldKey();
                    _fieldKeys.put(fieldKey, header);
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
        if (_fieldLabels.containsKey(label))
        {
            return _fieldLabels.get(label);
        }
        else if (_fieldLabels.size() < _fieldReferences.size())
        {
            for (FieldReference header : _fieldReferences)
            {
                if (!_fieldLabels.containsValue(header))
                {
                    String columnLabel = header.getLabel();
                    _fieldLabels.put(columnLabel, header);
                    if (columnLabel.equals(label))
                    {
                        return header;
                    }
                }
            }
        }

        String capitalized = StringUtils.capitalize(label);
        if (capitalized.equals(label))
            return null;
        else
            return findColumnHeaderByLabel(capitalized); // Handle domain names that aren't capitalized
    }

    public static class FieldReference
    {
        private final WebElement _element;
        private final int _domIndex;
        private final CachingSupplier<String> _fieldLabel = new CachingSupplier<>(this::labelSupplier);
        private final CachingSupplier<FieldKey> _fieldKey = new CachingSupplier<>(this::fieldKeySupplier);

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
            return _fieldKey.get();
        }

        public String getLabel()
        {
            return _fieldLabel.get();
        }

        public String getName()
        {
            return getFieldKey().getFullName();
        }

        public int getDomIndex()
        {
            return _domIndex;
        }

        protected String labelSupplier()
        {
            return WebElementUtils.getTextContent(getElement()).trim();
        }

        protected FieldKey fieldKeySupplier()
        {
            String path = getElement().getDomAttribute("data-fieldkey");
            if (path == null)
            {
                // Some grids don't have a field key, but have a similar value in the ID attribute
                path = getElement().getDomAttribute("id");
            }

            if (path != null)
            {
                return FieldKey.fromFieldKey(path);
            }
            else
            {
                return FieldKey.EMPTY;
            }
        }
    }
}
