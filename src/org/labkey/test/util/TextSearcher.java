/*
 * Copyright (c) 2014-2017 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebDriverWrapper;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;

public class TextSearcher
{
    private Function<String, String> sourceTransformer;
    private Function<String, String> searchTransformer;
    private Supplier<String> sourceSupplier;

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
        if (sourceTransformer == null)
            this.sourceTransformer = TextTransformers.IDENTITY;
        else
            this.sourceTransformer = sourceTransformer;
        return this;
    }

    public final TextSearcher clearSourceTransformer()
    {
        return setSourceTransformer(null);
    }

    public final TextSearcher setSearchTransformer(Function<String, String> searchTransformer)
    {
        if (searchTransformer == null)
            this.searchTransformer = TextTransformers.IDENTITY;
        else
            this.searchTransformer = searchTransformer;
        return this;
    }

    public final TextSearcher clearSearchTransformer()
    {
        return setSearchTransformer(null);
    }

    public final TextSearcher setSourceSupplier(Supplier<String> sourceSupplier)
    {
        this.sourceSupplier = sourceSupplier;
        return this;
    }

    public final void searchForTexts(TextHandler textHandler, String[] texts)
    {
        if (null == texts || 0 == texts.length)
            return;

        String transformedSource = sourceTransformer.apply(sourceSupplier.get());

        for (String text : texts)
        {
            String transformedText = searchTransformer.apply(text);
            if (transformedText.isEmpty())
                continue;
            if (!textHandler.handle(transformedSource, transformedText))
                return;
        }
    }

    public interface TextHandler
    {
        // Return true to continue searching
        boolean handle(String textSource, String text);
    }

    public static abstract class TextTransformers
    {
        public static final Function<String, String> ENCODE_HTML = BaseWebDriverTest::encodeText;
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
