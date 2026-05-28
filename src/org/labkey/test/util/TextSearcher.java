/*
 * Copyright (c) 2014-2026 LabKey Corporation
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

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebDriverWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class TextSearcher
{
    private Function<String, String> sourceTransformer;
    private Function<String, String> searchTransformer;
    private final Supplier<String> sourceSupplier;
    private String lastSearchedText = null;

    public TextSearcher(final Supplier<String> sourceSupplier)
    {
        this.sourceTransformer = TextTransformers.IDENTITY;
        this.searchTransformer = TextTransformers.IDENTITY;
        this.sourceSupplier = sourceSupplier;
    }

    public TextSearcher(String source)
    {
        this(() -> source);
    }

    public TextSearcher(File source)
    {
        this(()-> TestFileUtils.getFileContents(source));
        if (source.getName().endsWith(".htm") || source.getName().endsWith(".html"))
            this.searchTransformer = TextTransformers.ENCODE_HTML;
    }

    public TextSearcher(final WebDriverWrapper test)
    {
        this(test::getHtmlSource);
        this.searchTransformer = TextTransformers.ENCODE_HTML;
    }

    public final TextSearcher setSourceTransformer(Function<String, String> sourceTransformer)
    {
        this.sourceTransformer = Objects.requireNonNullElse(sourceTransformer, TextTransformers.IDENTITY);
        return this;
    }

    public final TextSearcher clearSourceTransformer()
    {
        return setSourceTransformer(null);
    }

    public final TextSearcher setSearchTransformer(Function<String, String> searchTransformer)
    {
        this.searchTransformer = Objects.requireNonNullElse(searchTransformer, TextTransformers.IDENTITY);
        return this;
    }

    public final TextSearcher clearSearchTransformer()
    {
        return setSearchTransformer(null);
    }

    public final void searchForTexts(TextHandler textHandler, List<String> texts)
    {
        if (texts == null || texts.isEmpty())
            return;

        String transformedSource = sourceTransformer.apply(sourceSupplier.get());
        lastSearchedText = transformedSource;

        for (String text : texts)
        {
            String transformedText = searchTransformer.apply(text);
            if (transformedText.isEmpty())
                continue;
            if (!textHandler.handle(transformedSource, transformedText))
                return;
        }
    }

    public List<String> getMissingTexts(String... texts)
    {
        return getMissingTexts(Arrays.asList(texts));
    }

    public List<String> getMissingTexts(List<String> texts)
    {
        final List<String> missingTexts = new ArrayList<>();

        TextSearcher.TextHandler handler = (textSource, text) -> {
            if (!textSource.contains(text))
                missingTexts.add(text);
            return true;
        };

        searchForTexts(handler, texts);

        return missingTexts;
    }

    /**
     * Checks whether any of the texts are present
     * @return true if any of the specified text is found
     */
    public boolean isAnyTextPresent(String... texts)
    {
        final MutableBoolean found = new MutableBoolean(false);

        TextSearcher.TextHandler handler = (htmlSource, text) -> {
            if (htmlSource.contains(text))
                found.setTrue();

            return !found.get(); // stop searching if any value is found
        };
        searchForTexts(handler, Arrays.asList(texts));

        return found.get();
    }

    /**
     * Checks whether all the specified texts are present
     * @return true if all the specified texts are present on the page
     */
    public boolean areAllTextsPresent(String... texts)
    {
        final MutableBoolean present = new MutableBoolean(true);

        TextSearcher.TextHandler handler = (htmlSource, text) -> {
            // Not found... stop enumerating and return false
            if (htmlSource == null || !htmlSource.contains(text))
                present.setFalse();

            return present.get();
        };

        searchForTexts(handler, Arrays.asList(texts));

        return present.get();
    }

    /**
     * @return source text from the last search attempt
     */
    public String getLastSearchedText()
    {
        return lastSearchedText;
    }

    public interface TextHandler
    {
        // Return true to continue searching
        boolean handle(String textSource, String text);
    }

    public static abstract class TextTransformers
    {
        public static final Function<String, String> ENCODE_HTML = t -> t
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        public static final Function<String, String> IDENTITY = text -> text;

        //Inserts spaces between camel-cased words
        public static final Function<String, String> FIELD_LABEL = (text) ->
        {
            StringBuilder sb = new StringBuilder();
            sb.append(text.charAt(0));
            for (int i = 1; i < text.length(); i++)
            {
                if (Character.isUpperCase(text.charAt(i)) && ' ' != text.charAt(i - 1))
                    sb.append(" ");
                sb.append(text.charAt(i));
            }
            return ENCODE_HTML.apply(sb.toString());
        };
    }
}
