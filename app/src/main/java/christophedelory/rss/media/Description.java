

package christophedelory.rss.media;

public class Description
{

    private String _type = null;

    private String _value = null;

    public void setValue(final String value)
    {
        _value = value.trim();
    }

    public String getValue()
    {
        return _value;
    }

    public void setType(final String type)
    {
        _type = type;
    }

    public String getType()
    {
        return _type;
    }
}
