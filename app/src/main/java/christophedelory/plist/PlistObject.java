

package christophedelory.plist;

public class PlistObject
{

    private transient Object _parent = null;

    void setParent(final Object parent)
    {
        _parent = parent;
    }

    public Object getParent()
    {
        return _parent;
    }
}
