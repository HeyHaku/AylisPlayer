

package christophedelory.plist;

public class Key extends PlistText
{

    public Key()
    {
        super();
    }

    public Key(final java.lang.String value)
    {
        super();

        setValue(value);
    }

    @Override
    public int hashCode()
    {
        int ret = 0;
        final java.lang.String value = getValue();

        if (value != null)
        {
            ret = value.hashCode();
        }

        return ret;
    }

    @Override
    public boolean equals(final Object obj)
    {
        boolean ret = false;

        if ((obj != null) && (obj instanceof Key))
        {
            final Key key = (Key) obj;
            final java.lang.String value = getValue();

            if (value == null)
            {
                ret = (key.getValue() == null);
            }
            else
            {
                ret = value.equals(key.getValue());
            }
        }

        return ret;
    }
}
