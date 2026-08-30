

package christophedelory.plist;

public class PlistText extends PlistObject
{

    private java.lang.String _value = null;

    public java.lang.String getValue()
    {
        return _value;
    }

    public void setValue(final java.lang.String value)
    {
        _value = value.trim();
    }
}
