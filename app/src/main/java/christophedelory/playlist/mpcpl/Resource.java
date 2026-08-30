

package christophedelory.playlist.mpcpl;

public class Resource
{

    private String _filename = null;

    private String _type = "0";

    private String _subtitle = null;

    public String getFilename()
    {
        return _filename;
    }

    public void setFilename(final String filename)
    {
        _filename = filename.trim();
    }

    public String getType()
    {
        return _type;
    }

    public void setType(final String type)
    {
        _type = type.trim();
    }

    public String getSubtitle()
    {
        return _subtitle;
    }

    public void setSubtitle(final String subtitle)
    {
        _subtitle = subtitle;
    }
}
