

package christophedelory.playlist.pla;

import android.app.Service;

import java.io.InputStream;
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

public class PLAProvider implements SpecificPlaylistProvider
{

    private static final ContentType[] FILETYPES =
    {
        new ContentType(new String[] { ".pla" },
                        new String[] { "application/octet-stream" },
                        new PlayerSupport[]
                        {
                        },
                        "iRiver iQuickList File"),
    };

    @Override
    public String getId()
    {
        return "pla";
    }

    @Override
    public ContentType[] getContentTypes()
    {
        return FILETYPES.clone();
    }

    @Override
    public SpecificPlaylist readFrom(final InputStream in, final String encoding, final Log logger) throws Exception
    {
        PLA ret = new PLA();
        ret.setProvider(this);

        final byte[] array = new byte[512];

        if (in.read(array) != 512)
        {
            throw new IllegalArgumentException("Not a PLA playlist format (file too small)");
        }

        final String magic = new String(array, 4, 14, "US-ASCII");

        if (!"iriver UMS PLA".equals(magic))
        {
            throw new IllegalArgumentException("Not a PLA playlist format (bad magic)");
        }

        final int nbSongs =   (((int) array[3] & 0x0ff) << 0) |
                        (((int) array[2] & 0x0ff) << 8) |
                        (((int) array[1] & 0x0ff) << 16) |
                        (((int) array[0] & 0x0ff) << 24);

        for (int i = 0; i < nbSongs; i++)
        {
            if (in.read(array) != 512)
            {
                logger.error("Malformed PLA playlist (file too small)");
                ret = null;
                break;
            }

            final String songFilename = new String(array, 2, 510, "UTF-16BE");

            ret.getFilenames().add(songFilename);
        }

        return ret;
    }

    @Override
    public SpecificPlaylist toSpecificPlaylist(final Playlist playlist) throws Exception
    {
        final PLA ret = new PLA();
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
                throw new IllegalArgumentException("A PLA playlist cannot handle a sequence repeated indefinitely");
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
            throw new IllegalArgumentException("A parallel time container is incompatible with a PLA playlist");
        }
        else if (component instanceof Media)
        {
            final Media media = (Media) component;

            if (media.getDuration() != null)
            {
                throw new IllegalArgumentException("A PLA playlist cannot handle a timed media");
            }

            if (media.getRepeatCount() < 0)
            {
                throw new IllegalArgumentException("A PLA playlist cannot handle a media repeated indefinitely");
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

