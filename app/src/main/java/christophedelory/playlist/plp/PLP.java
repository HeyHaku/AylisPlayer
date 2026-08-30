

package christophedelory.playlist.plp;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import mychristophedelory.content.Content;
import christophedelory.playlist.Media;
import christophedelory.playlist.Playlist;
import christophedelory.playlist.SpecificPlaylist;
import christophedelory.playlist.SpecificPlaylistProvider;

public class PLP implements SpecificPlaylist
{

    private transient SpecificPlaylistProvider _provider = null;

    private final List<String> _filenames = new ArrayList<String>();

    private String _diskSpecifier = "HARP";

    @Override
    public void setProvider(final SpecificPlaylistProvider provider)
    {
        _provider = provider;
    }

    @Override
    public SpecificPlaylistProvider getProvider()
    {
        return _provider;
    }

    @Override
    public void writeTo(final OutputStream out, final String encoding) throws Exception
    {
        String enc = encoding;

        if (enc == null)
        {
            enc = "UTF-16LE";
        }

        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, enc));

        writer.write("PLP PLAYLIST");
        writer.write('\r');
        writer.write('\n');
        writer.write("VERSION 1.20");
        writer.write('\r');
        writer.write('\n');
        writer.write('\r');
        writer.write('\n');

        for (String filename : _filenames)
        {
            writer.write(_diskSpecifier);
            writer.write(", ");
            writer.write(filename);
            writer.write('\r');
            writer.write('\n');
        }

        writer.flush();
    }

    @Override
    public Playlist toPlaylist()
    {
        final Playlist ret = new Playlist();

        for (String filename : _filenames)
        {
            final Media media = new Media();
            final Content content = new Content(filename);
            media.setSource(content);

            ret.getRootSequence().addComponent(media);
        }

        ret.normalize();

        return ret;
    }

    public List<String> getFilenames()
    {
        return _filenames;
    }

    public String getDiskSpecifier()
    {
        return _diskSpecifier;
    }

    public void setDiskSpecifier(final String diskSpecifier)
    {
        _diskSpecifier = diskSpecifier.trim();
    }
}
