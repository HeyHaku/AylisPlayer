

package christophedelory.rss.media;

public class Credit
{

    private String _scheme = null;

    private String _role = null;

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

    public String getRole()
    {
        return _role;
    }

    public void setRole(final String role)
    {
        _role = role;
    }
}
