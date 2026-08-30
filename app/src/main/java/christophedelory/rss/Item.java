

package christophedelory.rss;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import christophedelory.rss.media.BaseMedia;
import christophedelory.rss.media.Content;
import christophedelory.rss.media.Group;

public class Item extends BaseMedia
{

    private String _title = null;

    private URI _link = null;

    private String _description = null;

    private String _author = null;

    private final List<Category> _categories = new ArrayList<Category>();

    private String _comments = null;

    private Enclosure _enclosure = null;

    private GUID _guid = null;

    private Date _pubDate = null;

    private Source _source = null;

    private final List<Content> _mediaContents = new ArrayList<Content>();

    private final List<Group> _mediaGroups = new ArrayList<Group>();

    private transient Channel _channel = null;

    public String getTitle()
    {
        return _title;
    }

    public void setTitle(final String title)
    {
        _title = title;
    }

    public void setLinkString(final String link) throws URISyntaxException
    {
        _link = new URI(link);
    }

    public String getLinkString()
    {
        String ret = null;

        if (_link != null)
        {
            ret = _link.toString();
        }

        return ret;
    }

    public void setLink(final URI link)
    {
        _link = link;
    }

    public URI getLink()
    {
        return _link;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(final String description)
    {
        _description = description;
    }

    public String getAuthor()
    {
        return _author;
    }

    public void setAuthor(final String author)
    {
        _author = author;
    }

    public List<Category> getCategories()
    {
        return _categories;
    }

    public void addCategory(final Category category)
    {
        if (category == null)
        {
            throw new NullPointerException("no category");
        }

        _categories.add(category);
    }

    public String getComments()
    {
        return _comments;
    }

    public void setComments(final String comments)
    {
        _comments = comments;
    }

    public Enclosure getEnclosure()
    {
        return _enclosure;
    }

    public void setEnclosure(final Enclosure enclosure)
    {
        _enclosure = enclosure;
    }

    public GUID getGuid()
    {
        return _guid;
    }

    public void setGuid(final GUID guid)
    {
        _guid = guid;
    }

    public void setPubDateString(final String pubDate)
    {
        _pubDate = RFC822.valueOf(pubDate);
    }

    public String getPubDateString()
    {
        String ret = null;

        if (_pubDate != null)
        {
            ret = RFC822.toString(_pubDate);
        }

        return ret;
    }

    public Date getPubDate()
    {
        return _pubDate;
    }

    public void setPubDate(final Date pubDate)
    {
        _pubDate = pubDate;
    }

    public Source getSource()
    {
        return _source;
    }

    public void setSource(final Source source)
    {
        _source = source;
    }

	public void addMediaContent(final Content mediaContent)
	{
        if (mediaContent == null)
        {
            throw new NullPointerException("no media content");
        }

		_mediaContents.add(mediaContent);
	}

	public List<Content> getMediaContents()
	{
		return _mediaContents;
	}

	public List<Group> getMediaGroups()
	{
		return _mediaGroups;
	}

	public void addMediaGroup(final Group mediaGroup)
	{
        if (mediaGroup == null)
        {
            throw new NullPointerException("no media group");
        }

		_mediaGroups.add(mediaGroup);
	}

    void setChannel(final Channel channel)
    {
        _channel = channel;
    }

    public Channel getChannel()
    {
        return _channel;
    }
}
