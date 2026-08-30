

package christophedelory.rss.media;

import java.util.ArrayList;
import java.util.List;

public class Group extends BaseMedia
{

    private final List<Content> _mediaContents = new ArrayList<Content>();

    public void addMediaContent(final Content mediaContent)
    {
        if (mediaContent == null)
        {
            throw new NullPointerException("no media content");
        }

        _mediaContents.add(mediaContent);
    }

    public List<Content> getMediaContents()
    {
        return _mediaContents;
    }
}
