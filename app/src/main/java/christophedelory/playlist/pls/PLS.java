

package christophedelory.playlist.pls;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import mychristophedelory.content.Content;
import christophedelory.playlist.Media;
import christophedelory.playlist.Playlist;
import christophedelory.playlist.SpecificPlaylist;
import christophedelory.playlist.m3u.Resource;
import christophedelory.playlist.SpecificPlaylistProvider;

public class PLS implements SpecificPlaylist
{

    private transient SpecificPlaylistProvider _provider = null;

    private final List<Resource> _resources = new ArrayList<Resource>();

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

        writer.write("[Playlist]");
        writer.newLine();

        writer.write("NumberOfEntries=");

        writer.write(Integer.toString(_resources.size()));
        writer.newLine();

        int i = 1;

        for (Resource resource : _resources)
        {
            writer.write("File");
            writer.write(Integer.toString(i));
            writer.write("=");
            writer.write(resource.getLocation());
            writer.newLine();

            if (resource.getName() != null)
            {
                writer.write("Title");
                writer.write(Integer.toString(i));
                writer.write("=");
                writer.write(resource.getName());
                writer.newLine();
            }

            if (resource.getLength() >= 0L)
            {
                writer.write("Length");
                writer.write(Integer.toString(i));
                writer.write("=");
                writer.write(Long.toString(resource.getLength()));
                writer.newLine();
            }

            i++;
        }

        writer.write("Version=2");
        writer.newLine();

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

    public List<Resource> getResources()
    {
        return _resources;
    }
}
