

package christophedelory.atom;

import christophedelory.lang.StringUtils;

public class Common
{

    private String _base = null;

    private String _lang = null;

    public String getBaseString()
    {
        return _base;
    }

    public void setBaseString(final String base)
    {
        _base = StringUtils.normalize(base);
    }

    public String getLang()
    {
        return _lang;
    }

    public void setLang(final String lang)
    {
        _lang = StringUtils.normalize(lang);
    }
}
