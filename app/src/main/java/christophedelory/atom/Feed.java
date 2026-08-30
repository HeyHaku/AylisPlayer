

package christophedelory.atom;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Feed extends Source
{

    private final List<Entry> _entries = new ArrayList<Entry>();

    @Override
    public void setId(final URIContainer id)
    {
        if (id == null)
        {
            throw new NullPointerException("no id");
        }

        super.setId(id);
    }

    @Override
    public void setTitle(final TextContainer title)
    {
        if (title == null)
        {
            throw new NullPointerException("no title");
        }

        super.setTitle(title);
    }

    @Override
    public void setUpdated(final Date updated)
    {
        if (updated == null)
        {
            throw new NullPointerException("no updated date");
        }

        super.setUpdated(updated);
    }

    public List<Entry> getEntries()
    {
        return _entries;
    }

    public void addEntry(final Entry entry)
    {
        if (entry == null)
        {
            throw new NullPointerException("no entry");
        }

        _entries.add(entry);
    }
}
