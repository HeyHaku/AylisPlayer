

package christophedelory.atom;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Source extends Common
{

    private final List<Person> _authors = new ArrayList<Person>();

    private final List<Category> _categories = new ArrayList<Category>();

    private final List<Person> _contributors = new ArrayList<Person>();

    private Generator _generator = null;

    private URIContainer _icon = null;

    private URIContainer _id = null;

    private final List<Link> _links = new ArrayList<Link>();

    private URIContainer _logo = null;

    private TextContainer _rights = null;

    private TextContainer _subtitle = null;

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

    public Generator getGenerator()
    {
        return _generator;
    }

    public void setGenerator(final Generator generator)
    {
        _generator = generator;
    }

    public URIContainer getIcon()
    {
        return _icon;
    }

    public void setIcon(final URIContainer icon)
    {
        _icon = icon;
    }

    public URIContainer getId()
    {
        return _id;
    }

    public void setId(final URIContainer id)
    {
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

    public URIContainer getLogo()
    {
        return _logo;
    }

    public void setLogo(final URIContainer logo)
    {
        _logo = logo;
    }

    public TextContainer getRights()
    {
        return _rights;
    }

    public void setRights(final TextContainer rights)
    {
        _rights = rights;
    }

    public TextContainer getSubtitle()
    {
        return _subtitle;
    }

    public void setSubtitle(final TextContainer subtitle)
    {
        _subtitle = subtitle;
    }

    public TextContainer getTitle()
    {
        return _title;
    }

    public void setTitle(final TextContainer title)
    {
        _title = title;
    }

    public Date getUpdated()
    {
        return _updated;
    }

    public void setUpdated(final Date updated)
    {
        _updated = updated;
    }
}
