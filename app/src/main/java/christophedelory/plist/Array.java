

package christophedelory.plist;

import java.util.ArrayList;
import java.util.List;

public class Array extends PlistObject
{

    private final List<PlistObject> _objects = new ArrayList<PlistObject>();

    public List<PlistObject> getPlistObjects()
    {
        return _objects;
    }

    public void addPlistObject(final PlistObject object)
    {
        if (object instanceof Key)
        {
            throw new IllegalArgumentException("No dictionary key allowed in an array");
        }

        object.setParent(this);
        _objects.add(object);
    }
}
