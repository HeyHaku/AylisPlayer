

package christophedelory.playlist.plp;

import android.app.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import org.myapache.commons.logging.Log;
import mychristophedelory.content.type.ContentType;
import christophedelory.player.PlayerSupport;
import christophedelory.playlist.AbstractPlaylistComponent;
import christophedelory.playlist.Media;
import christophedelory.playlist.Parallel;
import christophedelory.playlist.Playlist;
import christophedelory.playlist.Sequence;
import christophedelory.playlist.SpecificPlaylist;
import christophedelory.playlist.SpecificPlaylistProvider;

public class PLPProvider implements SpecificPlaylistProvider
{

    private static final ContentType[] FILETYPES =
    {
        new ContentType(new String[] { ".plp" },
                        new String[] { "text/plain" },
                        new PlayerSupport[]
                        {
                        },
                        "Sansa Playlist File"),
    };

    @Override
    public String getId()
    {
        return "plp";
    }

    @Override
    public ContentType[] getContentTypes()
    {
        return FILETYPES.clone();
    }

    @Override
    public SpecificPlaylist readFrom(final InputStream in, final String encoding, final Log logger) throws Exception
    {
        String enc = encoding;

        if (enc == null)
        {
            enc = "UTF-16LE";
        }

        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, enc));

        PLP ret = new PLP();
        ret.setProvider(this);

        String line;
        boolean magic1Found = false;
        boolean magic2Found = false;
        String disk = null;

        while ((line = reader.readLine()) != null)
        {
            line = line.trim();

            if (line.length() > 0)
            {

                if (!magic1Found)
                {
                    if (!"PLP PLAYLIST".equals(line))
                    {
                        throw new IllegalArgumentException("Not a PLP playlist format");
                    }

                    magic1Found = true;
                    continue;
                }

                if (!magic2Found)
                {
                    if (!"VERSION 1.20".equals(line))
                    {
                        logger.error("Malformed PLP playlist (no version information)");
                        ret = null;
                        break;
                    }

                    magic2Found = true;
                    continue;
                }

                final int idx = line.indexOf(',');

                if (idx <= 0)
                {
                    logger.error("Malformed PLP playlist (playlist entry line format)");
                    ret = null;
                    break;
                }

                final String tmpDisk = line.substring(0, idx).trim();

                if (disk == null)
                {
                    disk = tmpDisk;
                }
                else if (!disk.equals(tmpDisk))
                {
                    logger.error("Malformed PLP playlist (inconsistent disk specifier)");
                    ret = null;
                    break;
                }

                ret.getFilenames().add(line.substring(idx + 1).trim());
            }
        }

        if ((ret != null) && (disk != null))
        {
            ret.setDiskSpecifier(disk);
        }

        return ret;
    }

    @Override
    public SpecificPlaylist toSpecificPlaylist(final Playlist playlist) throws Exception
    {
        final PLP ret = new PLP();
        ret.setProvider(this);

        addToPlaylist(ret.getFilenames(), playlist.getRootSequence());

        return ret;
    }

    private void addToPlaylist(final List<String> filenames, final AbstractPlaylistComponent component) throws Exception
    {
        if (component instanceof Sequence)
        {
            final Sequence seq = (Sequence) component;

            if (seq.getRepeatCount() < 0)
            {
                throw new IllegalArgumentException("A PLP playlist cannot handle a sequence repeated indefinitely");
            }

            final AbstractPlaylistComponent[] components = seq.getComponents();

            for (int iter = 0; iter < seq.getRepeatCount(); iter++)
            {
                for (AbstractPlaylistComponent c : components)
                {
                    addToPlaylist(filenames, c);
                }
            }
        }
        else if (component instanceof Parallel)
        {
            throw new IllegalArgumentException("A parallel time container is incompatible with a PLP playlist");
        }
        else if (component instanceof Media)
        {
            final Media media = (Media) component;

            if (media.getDuration() != null)
            {
                throw new IllegalArgumentException("A PLP playlist cannot handle a timed media");
            }

            if (media.getRepeatCount() < 0)
            {
                throw new IllegalArgumentException("A PLP playlist cannot handle a media repeated indefinitely");
            }

            if (media.getSource() != null)
            {
                for (int iter = 0; iter < media.getRepeatCount(); iter++)
                {
                    filenames.add(media.getSource().toString());
                }
            }
        }
    }
}

