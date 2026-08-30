

package christophedelory.plist;

public class Plist
{

    private java.lang.String _version = "1.0";

    private PlistObject _object = null;

    public java.lang.String getVersion()
    {
        return _version;
    }

    public void setVersion(final java.lang.String version)
    {
        _version = version.trim();
    }

    public PlistObject getPlistObject()
    {
        return _object;
    }

    public void setPlistObject(final PlistObject object)
    {
        if (object instanceof Key)
        {
            throw new IllegalArgumentException("No dictionary key allowed in a plist");
        }

        object.setParent(this);
        _object = object;
    }
}
