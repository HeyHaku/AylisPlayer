

package christophedelory.rss.media;

import java.net.URI;
import java.net.URISyntaxException;

public class Player
{

    private URI _url = null;

    private Integer _width = null;

    private Integer _height = null;

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

    public void setWidth(final int width)
    {
        _width = Integer.valueOf(width);
    }

    public void setWidth(final Integer width)
    {
        _width = width;
    }

    public Integer getWidth()
    {
        return _width;
    }

    public void setHeight(final int height)
    {
        _height = Integer.valueOf(height);
    }

    public void setHeight(final Integer height)
    {
        _height = height;
    }

    public Integer getHeight()
    {
        return _height;
    }
}
