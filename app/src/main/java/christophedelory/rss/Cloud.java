

package christophedelory.rss;

public class Cloud
{

    private String _domain = null;

    private int _port = 0;

    private String _path = null;

    private String _registerProcedure = null;

    private String _protocol = null;

    public void setDomain(final String domain)
    {
        _domain = domain.trim();
    }

    public String getDomain()
    {
        return _domain;
    }

    public void setPort(final int port)
    {
        _port = port;
    }

    public int getPort()
    {
        return _port;
    }

    public String getPath()
    {
        return _path;
    }

    public void setPath(final String path)
    {
        _path = path.trim();
    }

    public String getRegisterProcedure()
    {
        return _registerProcedure;
    }

    public void setRegisterProcedure(final String registerProcedure)
    {
        _registerProcedure = registerProcedure.trim();
    }

    public String getProtocol()
    {
        return _protocol;
    }

    public void setProtocol(final String protocol)
    {
        _protocol = protocol.trim();
    }
}
