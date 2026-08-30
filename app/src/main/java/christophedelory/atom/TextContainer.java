

package christophedelory.atom;

public class TextContainer extends Type
{

    private String _text = null;

    public String getText()
    {
        return _text;
    }

    public void setText(final String text)
    {
        _text = text.trim();
    }
}
