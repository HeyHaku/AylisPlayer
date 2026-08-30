

package christophedelory.playlist.kpl;

import christophedelory.lang.StringUtils;

public class Tag
{

    private String _artist = null;

    private String _album = null;

    private String _title = null;

    private String _year = null;

    private String _comment = null;

    private String _genre = null;

    private String _track = null;

    private String _gid = null;

    private String _has_tag = "True";

    public String getArtist()
    {
        return _artist;
    }

    public void setArtist(final String artist)
    {
        _artist = StringUtils.normalize(artist);
    }

    public String getAlbum()
    {
        return _album;
    }

    public void setAlbum(final String album)
    {
        _album = StringUtils.normalize(album);
    }

    public String getTitle()
    {
        return _title;
    }

    public void setTitle(final String title)
    {
        _title = StringUtils.normalize(title);
    }

    public String getYear()
    {
        return _year;
    }

    public void setYear(final String year)
    {
        _year = StringUtils.normalize(year);
    }

    public String getComment()
    {
        return _comment;
    }

    public void setComment(final String comment)
    {
        _comment = StringUtils.normalize(comment);
    }

    public String getGenre()
    {
        return _genre;
    }

    public void setGenre(final String genre)
    {
        _genre = StringUtils.normalize(genre);
    }

    public String getTrack()
    {
        return _track;
    }

    public void setTrack(final String track)
    {
        _track = StringUtils.normalize(track);
    }

    public String getGid()
    {
        return _gid;
    }

    public void setGid(final String gid)
    {
        _gid = StringUtils.normalize(gid);
    }

    public String getHasTag()
    {
        return _has_tag;
    }

    public void setHasTag(final String hasTag)
    {
        _has_tag = StringUtils.normalize(hasTag);
    }
}
