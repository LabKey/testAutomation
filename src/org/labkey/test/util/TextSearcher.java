package org.labkey.test.util;

import com.google.common.base.Function;
import org.labkey.test.BaseWebDriverTest;

public class TextSearcher
{
    private Function<String, String> sourceTransformer;
    private Function<String, String> searchTransformer;
    private Function<Void, String> sourceGetter;

    private TextSearcher()
    {
        this.sourceTransformer = TextTransformers.IDENTITY;
        this.searchTransformer = TextTransformers.ENCODER;
    }

    public TextSearcher(final Function<Void, String> sourceGetter)
    {
        this();
        this.sourceGetter = sourceGetter;
    }

    public TextSearcher(final BaseWebDriverTest test)
    {
        this(new Function<Void, String>()
        {
            @Override
            public String apply(Void o)
            {
                return test.getHtmlSource();
            }
        });
    }

    public final void setSourceTransformer(Function<String, String> sourceTransformer)
    {
        this.sourceTransformer = sourceTransformer;
    }

    public final void setSearchTransformer(Function<String, String> searchTransformer)
    {
        this.searchTransformer = searchTransformer;
    }

    public void setSourceGetter(Function<Void, String> sourceGetter)
    {
        this.sourceGetter = sourceGetter;
    }

    public final void searchForTexts(TextHandler textHandler, String[] texts)
    {
        if (null == texts || 0 == texts.length)
            return;

        String transformedSource = sourceTransformer.apply(sourceGetter.apply(null));

        for (String text : texts)
        {
            String transformedText = searchTransformer.apply(text);
            if (!textHandler.handle(transformedSource, transformedText))
                return;
        }
    }

    public interface TextHandler
    {
        // Return true to continue searching
        abstract boolean handle(String htmlSource, String text);
    }

    public static abstract class TextTransformers
    {
        public static final Function<String, String> ENCODER = new Function<String, String>()
        {
            @Override
            public String apply(String text)
            {
                return BaseWebDriverTest.encodeText(text);
            }
        };

        private static final Function<String, String> IDENTITY = new Function<String, String>()
        {
            @Override
            public String apply(String text)
            {
                return text;
            }
        };
    }
}
