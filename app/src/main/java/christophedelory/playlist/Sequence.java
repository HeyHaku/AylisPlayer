

package christophedelory.playlist;

public class Sequence extends AbstractTimeContainer
{
    @Override
    public void acceptDown(final PlaylistVisitor visitor) throws Exception
    {
        visitor.beginVisitSequence(this);

        super.acceptDown(visitor);

        visitor.endVisitSequence(this);
    }

    @Override
    public void acceptUp(final PlaylistVisitor visitor) throws Exception
    {
        visitor.beginVisitSequence(this);

        super.acceptUp(visitor);

        visitor.endVisitSequence(this);
    }
}
