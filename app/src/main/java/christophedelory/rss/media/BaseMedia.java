

package christophedelory.rss.media;

import java.util.ArrayList;
import java.util.List;

public class BaseMedia
{

    private Title _mediaTitle = null;

    private Description _mediaDescription = null;

    private Player _mediaPlayer = null;

    private final List<Thumbnail> _mediaThumbnails = new ArrayList<Thumbnail>();

    private Boolean _mediaAdult = null;

    private final List<Category> _mediaCategories = new ArrayList<Category>();

    private final List<Rating> _mediaRatings = new ArrayList<Rating>();

    private String _mediaKeywords = null;

    private final List<Hash> _mediaHashes = new ArrayList<Hash>();

    private final List<Credit> _mediaCredits = new ArrayList<Credit>();

    private Copyright _mediaCopyright = null;

    private final List<Text> _mediaTexts = new ArrayList<Text>();

    private final List<Restriction> _mediaRestrictions = new ArrayList<Restriction>();

    public void setMediaPlayer(final Player mediaPlayer)
    {
        _mediaPlayer = mediaPlayer;
    }

    public Player getMediaPlayer()
    {
        return _mediaPlayer;
    }

    public void addMediaThumbnail(final Thumbnail mediaThumbnail)
    {
        if (mediaThumbnail == null)
        {
            throw new NullPointerException("no media thumbnail");
        }

        _mediaThumbnails.add(mediaThumbnail);
    }

    public List<Thumbnail> getMediaThumbnails()
    {
        return _mediaThumbnails;
    }

    public void setMediaTitle(final Title mediaTitle)
    {
        _mediaTitle = mediaTitle;
    }

    public Title getMediaTitle()
    {
        return _mediaTitle;
    }

    public Description getMediaDescription()
    {
        return _mediaDescription;
    }

    public void setMediaDescription(final Description mediaDescription)
    {
        _mediaDescription = mediaDescription;
    }

    public boolean isMediaAdult()
    {
        return (_mediaAdult == null) ? false : _mediaAdult.booleanValue();
    }

    public void setMediaAdult(final boolean mediaAdult)
    {
        _mediaAdult = Boolean.valueOf(mediaAdult);
    }

    public Boolean getMediaAdult()
    {
        return _mediaAdult;
    }

    public void setMediaAdult(final Boolean mediaAdult)
    {
        _mediaAdult = mediaAdult;
    }

    public List<Category> getMediaCategories()
    {
        return _mediaCategories;
    }

    public void addMediaCategory(final Category mediaCategory)
    {
        if (mediaCategory == null)
        {
            throw new NullPointerException("no media category");
        }

        _mediaCategories.add(mediaCategory);
    }

    public List<Rating> getMediaRatings()
    {
        return _mediaRatings;
    }

    public void addMediaRating(final Rating mediaRating)
    {
        if (mediaRating == null)
        {
            throw new NullPointerException("no media rating");
        }

        _mediaRatings.add(mediaRating);
    }

    public String getMediaKeywords()
    {
        return _mediaKeywords;
    }

    public void setMediaKeywords(final String mediaKeywords)
    {
        _mediaKeywords = mediaKeywords;
    }

    public List<Hash> getMediaHashes()
    {
        return _mediaHashes;
    }

    public void addMediaHash(final Hash mediaHash)
    {
        if (mediaHash == null)
        {
            throw new NullPointerException("no media hash");
        }

        _mediaHashes.add(mediaHash);
    }

    public List<Credit> getMediaCredits()
    {
        return _mediaCredits;
    }

    public void addMediaCredit(final Credit mediaCredit)
    {
        if (mediaCredit == null)
        {
            throw new NullPointerException("no media credit");
        }

        _mediaCredits.add(mediaCredit);
    }

    public Copyright getMediaCopyright()
    {
        return _mediaCopyright;
    }

    public void setMediaCopyright(final Copyright mediaCopyright)
    {
        _mediaCopyright = mediaCopyright;
    }

    public List<Text> getMediaTexts()
    {
        return _mediaTexts;
    }

    public void addMediaText(final Text mediaText)
    {
        if (mediaText == null)
        {
            throw new NullPointerException("no media text");
        }

        _mediaTexts.add(mediaText);
    }

    public List<Restriction> getMediaRestrictions()
    {
        return _mediaRestrictions;
    }

    public void addMediaRestriction(final Restriction mediaRestriction)
    {
        if (mediaRestriction == null)
        {
            throw new NullPointerException("no media restriction");
        }

        _mediaRestrictions.add(mediaRestriction);
    }
}
