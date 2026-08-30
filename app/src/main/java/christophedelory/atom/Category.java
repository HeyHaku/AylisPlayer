

package christophedelory.atom;

public class Category extends Common
{

    private String _term = null;

    private String _scheme = null;

    private String _label = null;

    public String getTerm()
    {
        return _term;
    }

    public void setTerm(final String term)
    {
        _term = term.trim();
    }

    public String getScheme()
    {
        return _scheme;
    }

    public void setScheme(final String scheme)
    {
        _scheme = scheme;
    }

    public String getLabel()
    {
        return _label;
    }

    public void setLabel(final String label)
    {
        _label = label;
    }
}
