

package mychristophedelory.content.type;

import java.io.File;
import java.util.Locale;
import christophedelory.player.PlayerSupport;

public class ContentType implements Cloneable
{

    private final String[] _extensions;

    private final String[] _mimeTypes;

    private final PlayerSupport[] _playerSupports;

    private String _description;

    public ContentType(final String[] extensions, final String[] mimeTypes, final PlayerSupport[] playerSupports, final String description)
    {
        super();

        if (extensions.length <= 0)
        {
            throw new IllegalArgumentException("Empty extension array");
        }

        if (mimeTypes.length <= 0)
        {
            throw new IllegalArgumentException("Empty MIME type array");
        }

        _extensions = new String[extensions.length];

        for (int i = 0; i < extensions.length; i++)
        {
            _extensions[i] = extensions[i].toLowerCase(Locale.ENGLISH);
        }

        _mimeTypes = new String[mimeTypes.length];

        for (int i = 0; i < mimeTypes.length; i++)
        {
            _mimeTypes[i] = mimeTypes[i].toLowerCase(Locale.ENGLISH);
        }

        _description = description;
        _playerSupports = (playerSupports == null) ? new PlayerSupport[0] : playerSupports.clone();
    }

    public String[] getExtensions()
    {
        return _extensions.clone();
    }

    public String[] getMimeTypes()
    {
        return _mimeTypes.clone();
    }

    public PlayerSupport[] getPlayerSupports()
    {
        return _playerSupports.clone();
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(final String description)
    {
        _description = description;
    }

    public boolean matchExtension(final String pattern)
    {
        final String p = pattern.toLowerCase(Locale.ENGLISH);
        boolean ret = false;

        for (String extension : _extensions)
        {
            ret = ret || p.endsWith(extension);
        }

        return ret;
    }

    public boolean accept(final File f)
    {
        return (f.isDirectory()) ? true : matchExtension(f.getName());
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
