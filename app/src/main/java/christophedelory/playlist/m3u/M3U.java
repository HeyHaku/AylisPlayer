

package christophedelory.playlist.m3u;

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

public class M3U implements SpecificPlaylist
{

    private transient SpecificPlaylistProvider _provider = null;

    private final List<Resource> _resources = new ArrayList<Resource>();

    private boolean _extensionM3U = false;

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
            enc = "UTF-8";
        }

        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, enc));

        if (_extensionM3U)
        {
            writer.write("#EXTM3U");
            writer.newLine();
        }

        for (Resource resource : _resources)
        {
            if (_extensionM3U)
            {
                writer.write("#EXTINF:");
                writer.write(Long.toString(resource.getLength()));
                writer.write(",");

                if (resource.getName() == null)
                {
                    writer.write(resource.getLocation());
                }
                else
                {
                    writer.write(resource.getName());
                }

                writer.newLine();
            }

            writer.write(resource.getLocation());
            writer.newLine();
        }

        writer.flush();
    }

    @Override
    public Playlist toPlaylist()
    {
        final Playlist ret = new Playlist();

        for (Resource resource : _resources)
        {
            if (resource.getLocation() != null)
            {
                final Media media = new Media();
                final Content content = new Content(resource.getLocation());
                media.setSource(content);
                content.setDuration(resource.getLength() * 1000L);
                ret.getRootSequence().addComponent(media);
            }
        }

        ret.normalize();

        return ret;
    }

    public void setExtensionM3U(final boolean extensionM3U)
    {
        _extensionM3U = extensionM3U;
    }

    public boolean isExtensionM3U()
    {
        return _extensionM3U;
    }

    public List<Resource> getResources()
    {
        return _resources;
    }
}
