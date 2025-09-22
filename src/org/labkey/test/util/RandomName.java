package org.labkey.test.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Record for a randomly generated name
 *
 * @param part The test-provided portion of the name
 * @param name The full, randomly generated name
 */
public record RandomName(String part, String name)
{
    public RandomName(String part, String name)
    {
        this.part = part == null ? "" : part; // Don't trim
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public @NotNull String toString()
    {
        return name;
    }
}
