

package christophedelory.rss;

public class RSS
{

    public static final String VERSION_2_0 = "2.0";

    public static final String VERSION_0_92 = "0.92";

    public static final String VERSION_0_91 = "0.91";

    private Channel _channel = new Channel();

    private String _version = VERSION_2_0;

    public RSS()
    {
        _channel.setRSS(this);
    }

    public String getVersion()
    {
        return _version;
    }

    public void setVersion(final String version)
    {
        _version = version.trim();
    }

    public Channel getChannel()
    {
        return _channel;
    }

    public void setChannel(final Channel channel)
    {
        channel.setRSS(this);
        _channel = channel;
    }
}
