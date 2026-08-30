

package christophedelory.rss;

import java.net.URI;
import java.net.URISyntaxException;

public class Image
{

    private URI _url = null;

    private String _title = null;

    private URI _link = null;

    private Integer _width = null;

    private Integer _height = null;

    private String _description = null;

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

    public void setTitle(final String title)
    {
        _title = title.trim();
    }

    public String getTitle()
    {
        return _title;
    }

    public String getLinkString()
    {
        return _link.toString();
    }

    public void setLinkString(final String link) throws URISyntaxException
    {
        _link = new URI(link);
    }

    public void setLink(final URI link)
    {
        if (link == null)
        {
            throw new NullPointerException("No link");
        }

        _link = link;
    }

    public URI getLink()
    {
        return _link;
    }

    public Integer getWidth()
    {
        return _width;
    }

    public void setWidth(final Integer width)
    {
        _width = width;
    }

    public Integer getHeight()
    {
        return _height;
    }

    public void setHeight(final Integer height)
    {
        _height = height;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(final String description)
    {
        _description = description;
    }
}
