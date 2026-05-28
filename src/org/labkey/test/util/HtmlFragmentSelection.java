/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.util;

import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * Used when you want to put an HTML payload on the clipboard, allowing you to emulate things like copy/pasting from
 * ELN/Google Docs.
 */
public class HtmlFragmentSelection implements Transferable
{
    // You might be tempted to add support for DataFlavor.stringFlavor to this class in addition to
    // DataFlavor.fragmentHtmlFlavor, because that's what the examples show across the Internet do, and that's what the
    // various AI tools will tell you to do, but it simply doesn't work. If you put a text and HTML payload in the Java
    // clipboard it will only paste the text, and not the HTML.
    private static final DataFlavor[] transferDataFlavors = {DataFlavor.fragmentHtmlFlavor};
    private final String html;

    public HtmlFragmentSelection(String html) {
        this.html = html;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return transferDataFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DataFlavor.fragmentHtmlFlavor);
    }

    @Override
    @NotNull
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
    {
        if (flavor == DataFlavor.fragmentHtmlFlavor) {
            return html;
        }
        throw new UnsupportedFlavorException(flavor);
    }
}