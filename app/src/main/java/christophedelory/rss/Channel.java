

package christophedelory.rss;

import android.app.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import christophedelory.rss.media.BaseMedia;

public class Channel extends BaseMedia
{

    private String _title = null;

    private URI _link = null;

    private String _description = null;

    private String _language = null;

    private String _copyright = null;

    private String _managingEditor = null;

    private String _webMaster = null;

    private Date _pubDate = null;

    private Date _lastBuildDate = null;

    private final List<Category> _categories = new ArrayList<Category>();

    private String _generator = null;

    private String _docs = "http://blogs.law.harvard.edu/tech/rss";

    private Cloud _cloud = null;

    private Integer _ttl = null;

    private Image _image = null;

    private String _rating = null;

    private TextInput _textInput = null;

    private final List<Integer> _skipHours = new ArrayList<Integer>();

    private final List<String> _skipDays = new ArrayList<String>();

    private final List<Item> _items = new ArrayList<Item>();

    private transient RSS _rss = null;

    public String getTitle()
    {
        return _title;
    }

    public void setTitle(final String title)
    {
        _title = title.trim();
    }

    public void setLinkString(final String link) throws URISyntaxException
    {
        _link = new URI(link);
    }

    public String getLinkString()
    {
        return _link.toString();
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

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(final String description)
    {
        _description = description.trim();
    }

    public String getLanguage()
    {
        return _language;
    }

    public void setLanguage(final String language)
    {
        _language = language;
    }

    public String getCopyright()
    {
        return _copyright;
    }

    public void setCopyright(final String copyright)
    {
        _copyright = copyright;
    }

    public String getManagingEditor()
    {
        return _managingEditor;
    }

    public void setManagingEditor(final String managingEditor)
    {
        _managingEditor = managingEditor;
    }

    public String getWebMaster()
    {
        return _webMaster;
    }

    public void setWebMaster(final String webMaster)
    {
        _webMaster = webMaster;
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

    public void setPubDate(final Date pubDate)
    {
        _pubDate = pubDate;
    }

    public Date getPubDate()
    {
        return _pubDate;
    }

    public void setLastBuildDateString(final String lastBuildDate)
    {
        _lastBuildDate = RFC822.valueOf(lastBuildDate);
    }

    public String getLastBuildDateString()
    {
        String ret = null;

        if (_lastBuildDate != null)
        {
            ret = RFC822.toString(_lastBuildDate);
        }

        return ret;
    }

    public void setLastBuildDate(final Date lastBuildDate)
    {
        _lastBuildDate = lastBuildDate;
    }

    public Date getLastBuildDate()
    {
        return _lastBuildDate;
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

    public void setGenerator(final String generator)
    {
        _generator = generator;
    }

    public String getGenerator()
    {
        return _generator;
    }

    public void setDocs(final String docs)
    {
        _docs = docs;
    }

    public String getDocs()
    {
        return _docs;
    }

    public void setCloud(final Cloud cloud)
    {
        _cloud = cloud;
    }

    public Cloud getCloud()
    {
        return _cloud;
    }

    public void setTTL(final Integer ttl)
    {
        _ttl = ttl;
    }

    public Integer getTTL()
    {
        return _ttl;
    }

    public void setImage(final Image image)
    {
        _image = image;
    }

    public Image getImage()
    {
        return _image;
    }

    public void setRating(final String rating)
    {
        _rating = rating;
    }

    public String getRating()
    {
        return _rating;
    }

    public void setTextInput(final TextInput textInput)
    {
        _textInput = textInput;
    }

    public TextInput getTextInput()
    {
        return _textInput;
    }

    public List<Integer> getSkipHours()
    {
        return _skipHours;
    }

    public void addSkipHour(final Integer skipHour)
    {
        if (skipHour == null)
        {
            throw new NullPointerException("no skipHour");
        }

        _skipHours.add(skipHour);
    }

    public List<String> getSkipDays()
    {
        return _skipDays;
    }

    public void addSkipDay(final String skipDay)
    {
        if (skipDay == null)
        {
            throw new NullPointerException("no skipDay");
        }

        _skipDays.add(skipDay);
    }

    public List<Item> getItems()
    {
        return _items;
    }

    public void addItem(final Item item)
    {
        item.setChannel(this);
        _items.add(item);
    }

    void setRSS(final RSS rss)
    {
        _rss = rss;
    }

    public RSS getRSS()
    {
        return _rss;
    }
}

