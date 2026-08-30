

package christophedelory.playlist;

public class Parallel extends AbstractTimeContainer
{
    @Override
    public void acceptDown(final PlaylistVisitor visitor) throws Exception
    {
        visitor.beginVisitParallel(this);

        super.acceptDown(visitor);

        visitor.endVisitParallel(this);
    }

    @Override
    public void acceptUp(final PlaylistVisitor visitor) throws Exception
    {
        visitor.beginVisitParallel(this);

        super.acceptUp(visitor);

        visitor.endVisitParallel(this);
    }
}
