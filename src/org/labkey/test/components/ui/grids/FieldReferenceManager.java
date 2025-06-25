package org.labkey.test.components.ui.grids;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.params.FieldKey;
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
    private final List<FieldReference> fieldReferences;
    private final Map<Integer, FieldReference> fieldsByIndex;
    private final Map<FieldKey, FieldReference> fieldKeys = new LinkedHashMap<>();
    private final Map<String, FieldReference> fieldLabels = new LinkedHashMap<>();

    public <T extends FieldReference> FieldReferenceManager(List<T> columnHeaders)
    {
        fieldReferences = List.copyOf(columnHeaders);
        fieldsByIndex = columnHeaders.stream().collect(Collectors.toMap(FieldReference::getDomIndex, Function.identity()));
    }

    public List<FieldReference> getColumnHeaders()
    {
        return fieldReferences;
    }

    public FieldReference getColumnHeader(int index)
    {
        return fieldsByIndex.get(index);
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
        if (fieldKeys.containsKey(fieldIdentifier))
        {
            return fieldKeys.get(fieldIdentifier);
        }
        else if (fieldKeys.size() < fieldReferences.size())
        {
            for (FieldReference header : fieldReferences)
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
        else if (fieldLabels.size() < fieldReferences.size())
        {
            for (FieldReference header : fieldReferences)
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
            return getFieldKey().getName();
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
