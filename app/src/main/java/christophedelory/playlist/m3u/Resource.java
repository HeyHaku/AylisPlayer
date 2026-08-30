

package christophedelory.playlist.m3u;

import christophedelory.lang.StringUtils;

public class Resource
{

    private String _name = null;

    private String _location = null;

    private long _seconds = -1L;

    public String getName()
    {
        return _name;
    }

    public void setName(final String name)
    {
        _name = StringUtils.normalize(name);
    }

    public String getLocation()
    {
        return _location;
    }

    public void setLocation(final String location)
    {
        _location = location.trim().replace('\\', '/');
    }

    public long getLength()
    {
        return _seconds;
    }

    public void setLength(final long seconds)
    {

        if (seconds < 0L)
        {
            _seconds = -1L;
        }
        else
        {
            _seconds = seconds;
        }
    }
}
