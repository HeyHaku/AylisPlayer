

package christophedelory.rss;

import java.net.URI;
import java.net.URISyntaxException;

public class TextInput
{

    private String _title = null;

    private String _description = null;

    private String _name = null;

    private URI _link = null;

    public void setTitle(final String title)
    {
        _title = title.trim();
    }

    public String getTitle()
    {
        return _title;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(final String description)
    {
        _description = description.trim();
    }

    public void setName(final String name)
    {
        _name = name.trim();
    }

    public String getName()
    {
        return _name;
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
}
