

package christophedelory.atom;

import christophedelory.lang.StringUtils;

public class Type extends Common
{

    private String _type = null;

    public String getType()
    {
        return _type;
    }

    public void setType(final String type)
    {
        _type = StringUtils.normalize(type);
    }
}
