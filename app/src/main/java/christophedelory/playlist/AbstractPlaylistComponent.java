

package christophedelory.playlist;

public abstract class AbstractPlaylistComponent
{

    private int _repeatCount = 1;

    private transient AbstractTimeContainer _parent = null;

    void setParent(final AbstractTimeContainer parent)
    {
        _parent = parent;
    }

    public AbstractTimeContainer getParent()
    {
        return _parent;
    }

    public int getRepeatCount()
    {
        return _repeatCount;
    }

    public void setRepeatCount(final int repeatCount)
    {

        if (repeatCount < 0)
        {
            _repeatCount = -1;
        }
        else
        {
            _repeatCount = repeatCount;
        }
    }

    public abstract void acceptDown(final PlaylistVisitor visitor) throws Exception;

    public void acceptUp(final PlaylistVisitor visitor) throws Exception
    {
        if (_parent != null)
        {
            _parent.acceptUp(visitor);
        }
    }
}
