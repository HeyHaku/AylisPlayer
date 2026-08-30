

package christophedelory.plist;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class Dict extends PlistObject
{

    private final Hashtable<Key,PlistObject> _objects = new Hashtable<Key,PlistObject>();

    private transient Key _tmpKey = null;

    public Hashtable<Key,PlistObject> getDictionary()
    {
        return _objects;
    }

    public PlistObject put(final Key key, final PlistObject object)
    {
        return _objects.put(key, object);
    }

    public PlistObject put(final java.lang.String key, final PlistObject object)
    {
        final Key k = new Key(key);

        return _objects.put(k, object);
    }

    @Deprecated
    public List<PlistObject> getKeysAndObjects()
    {
        final List<PlistObject> ret = new ArrayList<PlistObject>(_objects.size());
        final Enumeration<Key> iter = _objects.keys();

        while (iter.hasMoreElements())
        {
            final Key key = iter.nextElement();
            ret.add(key);
            ret.add(_objects.get(key));
        }

        return ret;
    }

    @Deprecated
    public void addKeyOrObject(final PlistObject object)
    {
        object.setParent(this);

        if (_tmpKey == null)
        {
            if (!(object instanceof Key))
            {
                throw new IllegalArgumentException("A key is expected here");
            }

            _tmpKey = (Key) object;
        }
        else
        {
            if (object instanceof Key)
            {
                throw new IllegalArgumentException("A key is unexpected here");
            }

            put(_tmpKey, object);
            _tmpKey = null;
        }
    }

    public PlistObject findObjectByKey(final java.lang.String keyString)
    {
        final Key key = new Key(keyString);

        return _objects.get(key);
    }
}
