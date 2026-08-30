

package christophedelory.playlist.kpl;

public class Entry
{

    private String _fileName = null;

    private Tag _tag = null;

    public String getFilename()
    {
        return _fileName;
    }

    public void setFilename(final String fileName)
    {
        _fileName = fileName.trim();
    }

    public Tag getTag()
    {
        return _tag;
    }

    public void setTag(final Tag tag)
    {
        _tag = tag;
    }
}
