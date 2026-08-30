

package christophedelory.rss;

public class Category
{

    private String _domain = null;

    private String _value = null;

    public String getValue()
    {
        return _value;
    }

    public void setValue(final String value)
    {
        _value = value.trim();
    }

    public String getDomain()
    {
        return _domain;
    }

    public void setDomain(final String domain)
    {
        _domain = domain;
    }
}
