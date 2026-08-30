

package christophedelory.rss.media;

public class Restriction
{

    private String _type = null;

    private String _relationship = null;

    private String _value = null;

    public String getValue()
    {
        return _value;
    }

    public void setValue(final String value)
    {
        _value = value;
    }

    public String getType()
    {
        return _type;
    }

    public void setType(final String type)
    {
        _type = type;
    }

    public String getRelationship()
    {
        return _relationship;
    }

    public void setRelationship(final String relationship)
    {
        _relationship = relationship.trim();
    }
}
