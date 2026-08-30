

package christophedelory.atom;

import christophedelory.lang.StringUtils;

public class Person extends Common
{

    private String _name = null;

    private String _uri = null;

    private String _email = null;

    public String getName()
    {
        return _name;
    }

    public void setName(final String name)
    {
        _name = name.trim();
    }

    public String getURIString()
    {
        return _uri;
    }

    public void setURIString(final String uri)
    {
        _uri = StringUtils.normalize(uri);
    }

    public String getEmail()
    {
        return _email;
    }

    public void setEmail(final String email)
    {
        _email = StringUtils.normalize(email);
    }
}
