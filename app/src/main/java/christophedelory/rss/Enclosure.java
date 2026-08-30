

package christophedelory.rss;

import java.net.URI;
import java.net.URISyntaxException;

public class Enclosure
{

    private URI _url = null;

    private long _length = 0L;

    private String _type = "application/octet-stream";

    public void setURLString(final String url) throws URISyntaxException
    {
        _url = new URI(url);
    }

    public String getURLString()
    {
        return _url.toString();
    }

    public void setURL(final URI url)
    {
        if (url == null)
        {
            throw new NullPointerException("No URL");
        }

        _url = url;
    }

    public URI getURL()
    {
        return _url;
    }

    public void setLength(final long length)
    {
        _length = length;
    }

    public long getLength()
    {
        return _length;
    }

    public String getType()
    {
        return _type;
    }

    public void setType(final String type)
    {
        _type = type.trim();
    }
}
