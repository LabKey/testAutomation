package org.labkey.test.params;

public class Format
{
    public static final Format BOLD = new Builder().setBold(true).build();
    public static final Format ITALIC = new Builder().setItalics(true).build();
    public static final Format STRIKETHROUGH = new Builder().setStrikethrough(true).build();
    public static final Format NONE = new Builder().build();

    private final boolean _bold;
    private final boolean _italics;
    private final boolean _strikethrough;

    private Format(Builder builder)
    {
        _bold = builder._bold;
        _italics = builder._italics;
        _strikethrough = builder._strikethrough;
    }

    public boolean isBold()
    {
        return _bold;
    }

    public boolean isItalics()
    {
        return _italics;
    }

    public boolean isStrikethrough()
    {
        return _strikethrough;
    }

    public static class Builder
    {
        private boolean _bold;
        private boolean _italics;
        private boolean _strikethrough;

        public Builder()
        {
        }

        public Builder setBold(boolean bold)
        {
            _bold = bold;
            return this;
        }

        public Builder setItalics(boolean italics)
        {
            _italics = italics;
            return this;
        }

        public Builder setStrikethrough(boolean strikethrough)
        {
            _strikethrough = strikethrough;
            return this;
        }

        public Format build()
        {
            return new Format(this);
        }
    }
}
