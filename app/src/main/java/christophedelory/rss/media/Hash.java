

package christophedelory.rss.media;

public class Hash
{

    private String _algo = null;

    private String _value = null;

    public void setValue(final String value)
    {
        _value = value.trim();
    }

    public String getValue()
    {
        return _value;
    }

    public void setAlgo(final String algo)
    {
        _algo = algo;
    }

    public String getAlgo()
    {
        return _algo;
    }
}
