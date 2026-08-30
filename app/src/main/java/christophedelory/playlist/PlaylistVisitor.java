

package christophedelory.playlist;

public interface PlaylistVisitor
{

    void beginVisitPlaylist(final Playlist target) throws Exception;

    void endVisitPlaylist(final Playlist target) throws Exception;

    void beginVisitParallel(final Parallel target) throws Exception;

    void endVisitParallel(final Parallel target) throws Exception;

    void beginVisitSequence(final Sequence target) throws Exception;

    void endVisitSequence(final Sequence target) throws Exception;

    void beginVisitMedia(final Media target) throws Exception;

    void endVisitMedia(final Media target) throws Exception;
}
