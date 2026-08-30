

package christophedelory.plist;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Date extends PlistObject
{

    private static final DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    private java.util.Date _value = null;

    public Date()
    {
        super();
    }

    public Date(final java.util.Date value)
    {
        super();

        if (value == null)
        {
            throw new NullPointerException("no date");
        }

        _value = value;
    }

    public java.lang.String getValueString()
    {
        synchronized(DATETIME_FORMAT)
        {
            return DATETIME_FORMAT.format(_value);
        }
    }

    public void setValueString(final java.lang.String value) throws ParseException
    {
        synchronized(DATETIME_FORMAT)
        {
            _value = DATETIME_FORMAT.parse(value);
        }
    }

    public java.util.Date getValue()
    {
        return _value;
    }

    public void setValue(final java.util.Date value)
    {
        if (value == null)
        {
            throw new NullPointerException("no date");
        }

        _value = value;
    }
}

