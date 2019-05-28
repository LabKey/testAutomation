package org.labkey.serverapi.reader;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface DataLoaderFactory
{
    @NotNull DataLoader createLoader(InputStream is, boolean hasColumnHeaders) throws IOException;

    @NotNull DataLoader createLoader(File file, boolean hasColumnHeaders) throws IOException;

    @NotNull FileType getFileType();
}