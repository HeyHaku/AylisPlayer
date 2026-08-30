

package christophedelory.atom;

public class URIContainer extends Common
{

    private String _uri = null;

    public String getURIString()
    {
        return _uri;
    }

    public void setURIString(final String uri)
    {
        _uri = uri.trim();
    }
}
