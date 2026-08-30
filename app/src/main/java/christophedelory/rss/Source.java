

package christophedelory.rss;

import java.net.URI;
import java.net.URISyntaxException;

public class Source
{

    private URI _url = null;

    private String _channelName = null;

    public void setChannelName(final String channelName)
    {
        _channelName = channelName.trim();
    }

    public String getChannelName()
    {
        return _channelName;
    }

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
}
