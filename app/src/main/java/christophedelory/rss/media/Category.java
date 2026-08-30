

package christophedelory.rss.media;

public class Category
{

    private String _scheme = null;

    private String _label = null;

    private String _value = null;

    public String getValue()
    {
        return _value;
    }

    public void setValue(final String value)
    {
        _value = value.trim();
    }

    public String getScheme()
    {
        return _scheme;
    }

    public void setScheme(final String scheme)
    {
        _scheme = scheme;
    }

    public String getLabel()
    {
        return _label;
    }

    public void setLabel(final String label)
    {
        _label = label;
    }
}
