

package christophedelory.playlist;

import mychristophedelory.content.Content;

public class Media extends AbstractPlaylistComponent
{

    private Content _source = null;

    private Long _duration = null;

    public Content getSource()
    {
        return _source;
    }

    public void setSource(final Content source)
    {
        if (source == null)
        {
            throw new NullPointerException("No media source");
        }

        _source = source;
    }

    public Long getDuration()
    {
        return _duration;
    }

    public void setDuration(final Long millis)
    {
        if ((millis != null) && (millis.longValue() <= 0L))
        {
            throw new IllegalArgumentException("Negative or null duration " + millis);
        }

        _duration = millis;
    }

    public void setDuration(final long millis)
    {
        if (millis <= 0L)
        {
            throw new IllegalArgumentException("Negative or null duration " + millis);
        }

        _duration = Long.valueOf(millis);
    }

    @Override
    public void acceptDown(final PlaylistVisitor visitor) throws Exception
    {
        visitor.beginVisitMedia(this);

        visitor.endVisitMedia(this);
    }

    @Override
    public void acceptUp(final PlaylistVisitor visitor) throws Exception
    {
        visitor.beginVisitMedia(this);

        super.acceptUp(visitor);

        visitor.endVisitMedia(this);
    }
}
