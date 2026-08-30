

package christophedelory.playlist.m3u;

import android.app.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
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

public class M3UProvider implements SpecificPlaylistProvider
{

    private static final ContentType[] FILETYPES =
    {
        new ContentType(new String[] { ".m3u" },
                        new String[] { "audio/x-mpegurl", "audio/mpegurl" },
                        new PlayerSupport[]
                        {
                            new PlayerSupport(PlayerSupport.Player.WINAMP, true, null),
                            new PlayerSupport(PlayerSupport.Player.VLC_MEDIA_PLAYER, true, null),
                            new PlayerSupport(PlayerSupport.Player.WINDOWS_MEDIA_PLAYER, true, null),
                            new PlayerSupport(PlayerSupport.Player.MEDIA_PLAYER_CLASSIC, true, null),
                            new PlayerSupport(PlayerSupport.Player.FOOBAR2000, true, null),
                            new PlayerSupport(PlayerSupport.Player.MPLAYER, true, null),
                            new PlayerSupport(PlayerSupport.Player.QUICKTIME, true, null),
                            new PlayerSupport(PlayerSupport.Player.ITUNES, true, null),
                            new PlayerSupport(PlayerSupport.Player.REALPLAYER, false, null),
                        },
                        "Winamp M3U"),
        new ContentType(new String[] { ".m3u8" },
                        new String[] { "audio/x-mpegurl", "audio/mpegurl" },
                        new PlayerSupport[]
                        {
                            new PlayerSupport(PlayerSupport.Player.WINAMP, true, null),
                            new PlayerSupport(PlayerSupport.Player.FOOBAR2000, true, null),
                        },
                        "Winamp M3U8"),
        new ContentType(new String[] { ".m4u" },
                        new String[] { "video/x-mpegurl" },
                        new PlayerSupport[]
                        {
                        },
                        "M4U Playlist"),
        new ContentType(new String[] { ".ram" },
                        new String[] { "audio/vnd.rn-realaudio", "audio/x-pn-realaudio" },
                        new PlayerSupport[]
                        {
                            new PlayerSupport(PlayerSupport.Player.MEDIA_PLAYER_CLASSIC, false, null),
                            new PlayerSupport(PlayerSupport.Player.REALPLAYER, false, null),
                        },
                        "Real Audio Metadata (RAM)"),
    };

    @Override
    public String getId()
    {
        return "m3u";
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

        final M3U ret = new M3U();
        ret.setProvider(this);

        String line;
        String songName = null;
        String songLength = null;

        while ((line = reader.readLine()) != null)
        {
            line = line.trim();

            if (line.length() > 0)
            {
                final char firstChar = line.charAt(0);

                if ((firstChar == '<') || (firstChar == '['))
                {
                    throw new IllegalArgumentException("Doesn't seem to be a M3U playlist (and related ones)");
                }
                else if (firstChar == '#')
                {
                    if (line.toUpperCase(Locale.ENGLISH).startsWith("#EXTINF"))
                    {
                        final int indA = line.indexOf(',', 0);

                        if (indA >= 0)
                        {
                            songName = line.substring(indA + 1, line.length());
                        }

                        final int indB = line.indexOf(':', 0);

                        if ((indB >= 0) && (indB < indA))
                        {
                            songLength = line.substring(indB + 1, indA).trim();
                        }
                    }

                }
                else
                {
                    final Resource resource = new Resource();
                    resource.setLocation(line);
                    resource.setName(songName);

                    if (songLength != null)
                    {
                        resource.setLength(Long.parseLong(songLength));
                    }

                    ret.getResources().add(resource);

                    songName = null;
                    songLength = null;
                }
            }
        }

        return ret;
    }

    @Override
    public SpecificPlaylist toSpecificPlaylist(final Playlist playlist) throws Exception
    {
        final M3U ret = new M3U();
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
                throw new IllegalArgumentException("A M3U playlist cannot handle a sequence repeated indefinitely");
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
            throw new IllegalArgumentException("A parallel time container is incompatible with a M3U playlist");
        }
        else if (component instanceof Media)
        {
            final Media media = (Media) component;

            if (media.getDuration() != null)
            {
                throw new IllegalArgumentException("A M3U playlist cannot handle a timed media");
            }

            if (media.getRepeatCount() < 0)
            {
                throw new IllegalArgumentException("A M3U playlist cannot handle a media repeated indefinitely");
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

