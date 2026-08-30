

package christophedelory.player;

public class PlayerSupport implements Cloneable
{

    public enum Player
    {
        FOOBAR2000,
        ITUNES,
        MEDIA_PLAYER_CLASSIC,
        MPLAYER,

        QUICKTIME,
        REALPLAYER,
        VLC_MEDIA_PLAYER,
        WINAMP,
        WINDOWS_MEDIA_PLAYER,
    };

    public static String toString(final Player player)
    {
        final String ret;

        switch (player)
        {
            case FOOBAR2000:
                ret = "Foobar2000";
                break;
            case ITUNES:
                ret = "iTunes";
                break;
            case MEDIA_PLAYER_CLASSIC:
                ret = "Media Player Classic";
                break;
            case MPLAYER:
                ret = "MPlayer";
                break;

            case QUICKTIME:
                ret = "QuickTime";
                break;
            case REALPLAYER:
                ret = "RealPlayer";
                break;
            case VLC_MEDIA_PLAYER:
                ret = "VLC Media Player (VideoLAN)";
                break;
            case WINAMP:
                ret = "Winamp";
                break;
            case WINDOWS_MEDIA_PLAYER:
                ret = "Windows Media Player";
                break;
            default:
                ret = null;
        }

        return ret;
    }

    private final Player _player;

    private final boolean _isSaved;

    private final String _comment;

    public PlayerSupport(final Player player, final boolean isSaved, final String comment)
    {
        if (player == null)
        {
            throw new NullPointerException("no player");
        }

        _player = player;
        _isSaved = isSaved;
        _comment = comment;
    }

    public Player getPlayer()
    {
        return _player;
    }

    public boolean isSaved()
    {
        return _isSaved;
    }

    public String getComment()
    {
        return _comment;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
