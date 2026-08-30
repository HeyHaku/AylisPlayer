

package christophedelory.rss.media;

import java.net.URI;
import java.net.URISyntaxException;

public class Copyright
{

    private URI _url = null;

    private String _value = null;

    public void setURLString(final String url) throws URISyntaxException
    {
        _url = new URI(url);
    }

    public String getURLString()
    {
        String ret = null;

        if (_url != null)
        {
            ret = _url.toString();
        }

        return ret;
    }

    public void setURL(final URI url)
    {
        _url = url;
    }

    public URI getURL()
    {
        return _url;
    }

    public String getValue()
    {
        return _value;
    }

    public void setValue(final String value)
    {
        _value = value.trim();
    }
}
