

package christophedelory.rss;

public class GUID
{

    private boolean _isPermaLink = true;

    private String _value = null;

    public String getValue()
    {
        return _value;
    }

    public void setValue(final String value)
    {
        _value = value.trim();
    }

    public boolean isPermaLink()
    {
        return _isPermaLink;
    }

    public void setPermaLink(final boolean isPermaLink)
    {
        _isPermaLink = isPermaLink;
    }
}
