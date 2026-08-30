

package christophedelory.playlist.pls;

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
import christophedelory.playlist.m3u.Resource;

public class PLSProvider implements SpecificPlaylistProvider
{

    private static final ContentType[] FILETYPES =
    {
        new ContentType(new String[] { ".pls" },
                        new String[] { "audio/x-scpls" },
                        new PlayerSupport[]
                        {
                            new PlayerSupport(PlayerSupport.Player.WINAMP, true, null),
                            new PlayerSupport(PlayerSupport.Player.VLC_MEDIA_PLAYER, false, null),
                            new PlayerSupport(PlayerSupport.Player.MEDIA_PLAYER_CLASSIC, true, null),
                            new PlayerSupport(PlayerSupport.Player.FOOBAR2000, false, null),
                            new PlayerSupport(PlayerSupport.Player.MPLAYER, true, null),
                            new PlayerSupport(PlayerSupport.Player.QUICKTIME, true, null),
                            new PlayerSupport(PlayerSupport.Player.ITUNES, true, null),
                            new PlayerSupport(PlayerSupport.Player.REALPLAYER, false, null),
                        },
                        "Winamp PLSv2 Playlist"),
    };

    @Override
    public String getId()
    {
        return "pls";
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

        PLS ret = new PLS();
        ret.setProvider(this);

        String line;
        boolean magicFound = false;
        int numberOfEntries = -1;

        while ((line = reader.readLine()) != null)
        {
            line = line.trim();

            if (line.length() > 0)
            {

                if (!magicFound)
                {
                    if (!line.equalsIgnoreCase("[playlist]"))
                    {
                        throw new IllegalArgumentException("Not a PLS playlist format");
                    }

                    magicFound = true;
                    continue;
                }

                final int idx = line.indexOf('=');

                if (idx <= 0)
                {
                    logger.error("Malformed PLS playlist");
                    ret = null;
                    break;
                }

                String key = line.substring(0, idx).trim().toLowerCase();
                final String value = line.substring(idx + 1).trim();

                if ("numberofentries".equals(key))
                {
                    int tmpValue;

                    try
                    {
                        tmpValue = Integer.parseInt(value);
                    }
                    catch (NumberFormatException e)
                    {
                        logger.error(e.toString());
                        ret = null;
                        break;
                    }

                    if (tmpValue < 0)
                    {
                        logger.warn("Invalid NumberOfEntries in PLS playlist: " + tmpValue);
                        ret = null;
                        break;
                    }

                    if ((numberOfEntries >= 0) && (numberOfEntries != tmpValue))
                    {
                        logger.error("PLS playlist number of entries already specified with a different value");
                        ret = null;
                        break;
                    }

                    numberOfEntries = tmpValue;
                }
                else if (key.startsWith("file"))
                {
                    key = key.substring(4);
                    int resourceIndex;

                    try
                    {
                        resourceIndex = Integer.parseInt(key) - 1;
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
                    resource.setLocation(value);
                }

                else if (key.startsWith("title"))
                {
                    key = key.substring(5);
                    int resourceIndex;

                    try
                    {
                        resourceIndex = Integer.parseInt(key) - 1;
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
                    resource.setName(value);
                }

                else if (key.startsWith("length"))
                {
                    key = key.substring(6);
                    int resourceIndex;

                    try
                    {
                        resourceIndex = Integer.parseInt(key) - 1;
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

                    try
                    {
                        resource.setLength(Long.parseLong(value));
                    }
                    catch (NumberFormatException e)
                    {
                        logger.error(e.toString());
                        ret = null;
                        break;
                    }
                }
                else if ("version".equals(key))
                {

                    if (!"2".equals(value))
                    {
                        logger.error("Unknown PLS version " + value);
                        ret = null;
                        break;
                    }
                }
                else
                {
                    logger.warn("Unknown PLS keyword " + key);
                }
            }
        }

        if (ret != null)
        {
            if (numberOfEntries < 0)
            {
                logger.warn("No number of entries in PLS playlist");
            }
            else
            {

                final int extras = ret.getResources().size() - numberOfEntries;

                if (extras > 0)
                {
                    logger.warn("Ignoring " + extras + " extra resources according to the specified number of entries " + numberOfEntries);
                }

                for (int i = 0; i < extras; i++)
                {
                    ret.getResources().remove(numberOfEntries);
                }
            }
        }

        return ret;
    }

    @Override
    public SpecificPlaylist toSpecificPlaylist(final Playlist playlist) throws Exception
    {
        final PLS ret = new PLS();
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
                throw new IllegalArgumentException("A PLS playlist cannot handle a sequence repeated indefinitely");
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
            throw new IllegalArgumentException("A parallel time container is incompatible with a PLS playlist");
        }
        else if (component instanceof Media)
        {
            final Media media = (Media) component;

            if (media.getDuration() != null)
            {
                throw new IllegalArgumentException("A PLS playlist cannot handle a timed media");
            }

            if (media.getRepeatCount() < 0)
            {
                throw new IllegalArgumentException("A PLS playlist cannot handle a media repeated indefinitely");
            }

            if (media.getSource() != null)
            {
                for (int iter = 0; iter < media.getRepeatCount(); iter++)
                {
                    final Resource resource = new Resource();
                    resource.setLocation(media.getSource().toString());

                    if (media.getSource().getDuration() >= 0L)
                    {
                        resource.setLength((media.getSource().getDuration() + 999L) / 1000L);
                    }

                    resources.add(resource);
                }
            }
        }
    }
}

