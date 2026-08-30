

package christophedelory.playlist.mpcpl;

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

public class MPCPLProvider implements SpecificPlaylistProvider
{

    private static final ContentType[] FILETYPES =
    {
        new ContentType(new String[] { ".mpcpl" },
                        new String[] { "text/plain" },
                        new PlayerSupport[]
                        {
                            new PlayerSupport(PlayerSupport.Player.MEDIA_PLAYER_CLASSIC, true, null),
                        },
                        "Media Player Classic Playlist"),
    };

    @Override
    public String getId()
    {
        return "mpcpl";
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
            enc = "UTF-8";
        }

        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, enc));

        MPCPL ret = new MPCPL();
        ret.setProvider(this);

        String line;
        boolean magicFound = false;

        while ((line = reader.readLine()) != null)
        {
            line = line.trim();

            if (line.length() > 0)
            {

                if (!magicFound)
                {
                    if (!line.equalsIgnoreCase("MPCPLAYLIST"))
                    {
                        throw new IllegalArgumentException("Not a MPCPL playlist format");
                    }

                    magicFound = true;
                    continue;
                }

                int idx = line.indexOf(',');

                if (idx <= 0)
                {
                    logger.error("Malformed MPCPL playlist entry " + line);
                    ret = null;
                    break;
                }

                final String resourceIndexString = line.substring(0, idx).trim();
                line = line.substring(idx + 1);

                idx = line.indexOf(',');

                if (idx <= 0)
                {
                    logger.error("Malformed MPCPL playlist entry " + line);
                    ret = null;
                    break;
                }

                final String key = line.substring(0, idx).trim().toLowerCase();
                final String value = line.substring(idx + 1).trim();

                int resourceIndex;

                try
                {
                    resourceIndex = Integer.parseInt(resourceIndexString) - 1;
                }
                catch (NumberFormatException e)
                {
                    logger.error(e.toString());
                    ret = null;
                    break;
                }

                for (int i = ret.getResources().size(); i < (resourceIndex + 1); i++)
                {
                    ret.getResources().add(new Resource());
                }

                final Resource resource = ret.getResources().get(resourceIndex);

                if ("filename".equals(key))
                {
                    resource.setFilename(value);
                }
                else if ("type".equals(key))
                {
                    resource.setType(value);
                }
                else if ("subtitle".equals(key))
                {
                    resource.setSubtitle(value);
                }
                else
                {
                    logger.warn("Unknown MPCPL keyword " + key);
                }
            }
        }

        return ret;
    }

    @Override
    public SpecificPlaylist toSpecificPlaylist(final Playlist playlist) throws Exception
    {
        final MPCPL ret = new MPCPL();
        ret.setProvider(this);

        addToPlaylist(ret.getResources(), playlist.getRootSequence());

        return ret;
    }

    private void addToPlaylist(final List<Resource> resources, final AbstractPlaylistComponent component) throws Exception
    {
        if (component instanceof Sequence)
        {
            final Sequence seq = (Sequence) component;

            if (seq.getRepeatCount() < 0)
            {
                throw new IllegalArgumentException("A MPCPL playlist cannot handle a sequence repeated indefinitely");
            }

            final AbstractPlaylistComponent[] components = seq.getComponents();

            for (int iter = 0; iter < seq.getRepeatCount(); iter++)
            {
                for (AbstractPlaylistComponent c : components)
                {
                    addToPlaylist(resources, c);
                }
            }
        }
        else if (component instanceof Parallel)
        {
            throw new IllegalArgumentException("A parallel time container is incompatible with a MPCPL playlist");
        }
        else if (component instanceof Media)
        {
            final Media media = (Media) component;

            if (media.getDuration() != null)
            {
                throw new IllegalArgumentException("A MPCPL playlist cannot handle a timed media");
            }

            if (media.getRepeatCount() < 0)
            {
                throw new IllegalArgumentException("A MPCPL playlist cannot handle a media repeated indefinitely");
            }

            if (media.getSource() != null)
            {
                for (int iter = 0; iter < media.getRepeatCount(); iter++)
                {
                    final Resource resource = new Resource();
                    resource.setFilename(media.getSource().toString());
                    resources.add(resource);
                }
            }
        }
    }
}

