

package christophedelory.atom;

import christophedelory.lang.StringUtils;

public class Link extends Type
{

    private String _href = null;

    private String _rel = null;

    private String _hreflang = null;

    private String _title = null;

    private Long _length = null;

    public String getHref()
    {
        return _href;
    }

    public void setHref(final String href)
    {
        _href = href.trim();
    }

    public String getRel()
    {
        return _rel;
    }

    public void setRel(final String rel)
    {
        _rel = StringUtils.normalize(rel);
    }

    public String getHrefLang()
    {
        return _hreflang;
    }

    public void setHrefLang(final String hreflang)
    {
        _hreflang = StringUtils.normalize(hreflang);
    }

    public String getTitle()
    {
        return _title;
    }

    public void setTitle(final String title)
    {
        _title = StringUtils.normalize(title);
    }

    public Long getLength()
    {
        return _length;
    }

    public void setLength(final Long length)
    {
        _length = length;
    }
}
