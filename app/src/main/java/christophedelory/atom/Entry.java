

package christophedelory.atom;

import android.content.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Entry extends Common
{

    private final List<Person> _authors = new ArrayList<Person>();

    private final List<Category> _categories = new ArrayList<Category>();

    private final List<Content> _contents = new ArrayList<Content>();

    private final List<Person> _contributors = new ArrayList<Person>();

    private URIContainer _id = null;

    private final List<Link> _links = new ArrayList<Link>();

    private Date _published = null;

    private TextContainer _rights = null;

    private Source _source = null;

    private TextContainer _summary = null;

    private TextContainer _title = null;

    private Date _updated = null;

    public List<Person> getAuthors()
    {
        return _authors;
    }

    public void addAuthor(final Person author)
    {
        if (author == null)
        {
            throw new NullPointerException("no author");
        }

        _authors.add(author);
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

    public List<Content> getContents()
    {
        return _contents;
    }

    public void addContent(final Content content)
    {
        if (content == null)
        {
            throw new NullPointerException("no content");
        }

        _contents.add(content);
    }

    public List<Person> getContributors()
    {
        return _contributors;
    }

    public void addContributor(final Person contributor)
    {
        if (contributor == null)
        {
            throw new NullPointerException("no contributor");
        }

        _contributors.add(contributor);
    }

    public URIContainer getId()
    {
        return _id;
    }

    public void setId(final URIContainer id)
    {
        if (id == null)
        {
            throw new NullPointerException("no id");
        }

        _id = id;
    }

    public List<Link> getLinks()
    {
        return _links;
    }

    public void addLink(final Link link)
    {
        if (link == null)
        {
            throw new NullPointerException("no link");
        }

        _links.add(link);
    }

    public Date getPublished()
    {
        return _published;
    }

    public void setPublished(final Date published)
    {
        _published = published;
    }

    public TextContainer getRights()
    {
        return _rights;
    }

    public void setRights(final TextContainer rights)
    {
        _rights = rights;
    }

    public Source getSource()
    {
        return _source;
    }

    public void setSource(final Source source)
    {
        _source = source;
    }

    public TextContainer getSummary()
    {
        return _summary;
    }

    public void setSummary(final TextContainer summary)
    {
        _summary = summary;
    }

    public TextContainer getTitle()
    {
        return _title;
    }

    public void setTitle(final TextContainer title)
    {
        if (title == null)
        {
            throw new NullPointerException("no title");
        }

        _title = title;
    }

    public Date getUpdated()
    {
        return _updated;
    }

    public void setUpdated(final Date updated)
    {
        if (updated == null)
        {
            throw new NullPointerException("no updated date");
        }

        _updated = updated;
    }
}

