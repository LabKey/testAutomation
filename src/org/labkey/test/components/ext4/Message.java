package org.labkey.test.components.ext4;

import org.openqa.selenium.WebDriver;

public class Message extends Window<Window.ElementCache>
{
    private final String _title;

    public Message(String title, WebDriver driver)
    {
        super(title, driver);
        _title = title;
    }

    @Override
    public String getTitle()
    {
        return _title;
    }

    @Override
    public boolean isClosed()
    {
        return super.isClosed() || !elementCache().title.getText().equals(_title);
    }
}
