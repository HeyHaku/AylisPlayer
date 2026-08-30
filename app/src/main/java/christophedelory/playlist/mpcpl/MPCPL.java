

package christophedelory.playlist.mpcpl;

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

public class MPCPL implements SpecificPlaylist
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

        writer.write("MPCPLAYLIST");
        writer.newLine();

        int i = 1;

        for (Resource resource : _resources)
        {
            writer.write(Integer.toString(i));
            writer.write(",type,");
            writer.write(resource.getType());
            writer.newLine();

            writer.write(Integer.toString(i));
            writer.write(",filename,");
            writer.write(resource.getFilename());
            writer.newLine();

            if (resource.getSubtitle() != null)
            {
                writer.write(Integer.toString(i));
                writer.write(",subtitle,");
                writer.write(resource.getSubtitle());
                writer.newLine();
            }

            i++;
        }

        writer.flush();
    }

    @Override
    public Playlist toPlaylist()
    {
        final Playlist ret = new Playlist();

        for (Resource resource : _resources)
        {
            if (resource.getFilename() != null)
            {
                final Media media = new Media();
                final Content content = new Content(resource.getFilename());
                media.setSource(content);
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
