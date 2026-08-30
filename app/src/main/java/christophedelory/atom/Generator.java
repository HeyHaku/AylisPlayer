

package christophedelory.atom;

import christophedelory.lang.StringUtils;

public class Generator extends Common
{

    private String _value = null;

    private String _uri = null;

    private String _version = null;

    public String getValue()
    {
        return _value;
    }

    public void setValue(final String value)
    {
        _value = value.trim();
    }

    public String getURIString()
    {
        return _uri;
    }

    public void setURIString(final String uri)
    {
        _uri = StringUtils.normalize(uri);
    }

    public String getVersion()
    {
        return _version;
    }

    public void setVersion(final String version)
    {
        _version = StringUtils.normalize(version);
    }
}
