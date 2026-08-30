

package christophedelory.atom;

import christophedelory.lang.StringUtils;

public class Content extends Type
{

    private String _text = null;

    private String _src = null;

    public String getText()
    {
        return _text;
    }

    public void setText(final String text)
    {
        _text = StringUtils.normalize(text);
    }

    public String getSrc()
    {
        return _src;
    }

    public void setSrc(final String src)
    {
        _src = StringUtils.normalize(src);
    }
}
