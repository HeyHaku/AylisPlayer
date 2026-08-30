

package christophedelory.rss.media;

public class Text
{

    private String _type = null;

    private String _lang = null;

    private String _start = null;

    private String _end = null;

    private String _value = null;

    public String getValue()
    {
        return _value;
    }

    public void setValue(final String value)
    {
        _value = value.trim();
    }

    public String getType()
    {
        return _type;
    }

    public void setType(final String type)
    {
        _type = type;
    }

    public String getLang()
    {
        return _lang;
    }

    public void setLang(final String lang)
    {
        _lang = lang;
    }

    public String getStart()
    {
        return _start;
    }

    public void setStart(final String start)
    {
        _start = start;
    }

    public String getEnd()
    {
        return _end;
    }

    public void setEnd(final String end)
    {
        _end = end;
    }
}
