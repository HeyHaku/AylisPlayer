

package christophedelory.playlist.kpl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import christophedelory.lang.StringUtils;

public class Info
{

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private Date _creation_day = null;

    private Date _modified_day = null;

    private String _author = null;

    private String _player = null;

    private String _player_version = null;

    private String _kpl_version = "1";

    public String getCreationDayString()
    {
        String ret = null;

        if (_creation_day != null)
        {
            synchronized(DATE_FORMAT)
            {
                ret = DATE_FORMAT.format(_creation_day);
            }
        }

        return ret;
    }

    public void setCreationDayString(final String creationDay) throws ParseException
    {
        final String day = StringUtils.normalize(creationDay);

        if (day == null)
        {
            _creation_day = null;
        }
        else
        {
            synchronized(DATE_FORMAT)
            {
                _creation_day = DATE_FORMAT.parse(day);
            }
        }
    }

    public Date getCreationDay()
    {
        return _creation_day;
    }

    public void setCreationDay(final Date creationDay)
    {
        _creation_day = creationDay;
    }

    public String getModifiedDayString()
    {
        String ret = null;

        if (_modified_day != null)
        {
            synchronized(DATE_FORMAT)
            {
                ret = DATE_FORMAT.format(_modified_day);
            }
        }

        return ret;
    }

    public void setModifiedDayString(final String modifiedDay) throws ParseException
    {
        final String day = StringUtils.normalize(modifiedDay);

        if (day == null)
        {
            _modified_day = null;
        }
        else
        {
            synchronized(DATE_FORMAT)
            {
                _modified_day = DATE_FORMAT.parse(day);
            }
        }
    }

    public Date getModifiedDay()
    {
        return _modified_day;
    }

    public void setModifiedDay(final Date modifiedDay)
    {
        _modified_day = modifiedDay;
    }

    public String getAuthor()
    {
        return _author;
    }

    public void setAuthor(final String author)
    {
        _author = StringUtils.normalize(author);
    }

    public String getPlayer()
    {
        return _player;
    }

    public void setPlayer(final String player)
    {
        _player = StringUtils.normalize(player);
    }

    public String getPlayerVersion()
    {
        return _player_version;
    }

    public void setPlayerVersion(final String playerVersion)
    {
        _player_version = StringUtils.normalize(playerVersion);
    }

    public String getKplVersion()
    {
        return _kpl_version;
    }

    public void setKplVersion(final String kplVersion)
    {
        _kpl_version = StringUtils.normalize(kplVersion);
    }
}
