

package christophedelory.playlist;

import org.myapache.commons.logging.Log;
import mychristophedelory.logging.LogFactory;

public class Playlist
{

    private final static Log _logger = LogFactory.getLog(Playlist.class);

    private static final PlaylistVisitor NORMALIZATION = new Normalization();

    private final Sequence _rootSequence;

    public Playlist()
    {
        _rootSequence = new Sequence();
    }

    public Sequence getRootSequence()
    {
        return _rootSequence;
    }

    public void normalize()
    {
        try
        {

            _rootSequence.acceptDown(NORMALIZATION);

            _rootSequence.acceptDown(NORMALIZATION);
        }
        catch (Exception e)
        {

            _logger.error("Unexpected error condition", e);
        }
    }

    private static class Normalization extends BasePlaylistVisitor
    {
        @Override
        public void endVisitMedia(final Media target)
        {

            if (target.getSource() == null)
            {
                _logger.info("Removing media with no source: " + target);
                target.getParent().removeComponent(target);
            }
        }

        @Override
        public void endVisitParallel(final Parallel target) throws Exception
        {
            endVisitTimeContainer(target);
        }

        @Override
        public void endVisitSequence(final Sequence target) throws Exception
        {
            endVisitTimeContainer(target);

            if ((target.getParent() == null) && (target.getComponentsNumber() == 1))
            {
                final AbstractPlaylistComponent[] targetComponents = target.getComponents();

                if (targetComponents[0] instanceof Sequence)
                {
                    final Sequence sequence = (Sequence) targetComponents[0];

                    _logger.info("Merging root sequence " + target + " with its single child sequence " + sequence);
                    target.setRepeatCount(target.getRepeatCount() * sequence.getRepeatCount());
                    final AbstractPlaylistComponent[] components = sequence.getComponents();
                    target.removeComponent(sequence);

                    for (AbstractPlaylistComponent component : components)
                    {
                        target.addComponent(component);
                    }
                }
            }

            mergeConsecutiveIdenticalMedia(target);
            mergeConsecutiveSequences(target);
        }

        private void endVisitTimeContainer(final AbstractTimeContainer target)
        {
            final AbstractTimeContainer targetParent = target.getParent();

            if (targetParent != null)
            {
                final int componentsNumber = target.getComponentsNumber();

                if (componentsNumber == 0)
                {

                    _logger.info("Removing empty time container " + target);
                    targetParent.removeComponent(target);

                }
                else if (componentsNumber == 1)
                {

                    final AbstractPlaylistComponent[] targetComponents = target.getComponents();
                    _logger.info("Replacing time container " + target + " with its single child component " + targetComponents[0]);
                    targetComponents[0].setRepeatCount(targetComponents[0].getRepeatCount() * target.getRepeatCount());
                    target.removeComponent(targetComponents[0]);
                    targetParent.removeComponent(target);

                    targetParent.addComponent(targetComponents[0]);

                }
            }
        }

        private void mergeConsecutiveIdenticalMedia(final Sequence target)
        {
            final AbstractPlaylistComponent[] targetComponents = target.getComponents();

            for (int i = 0; i < (targetComponents.length - 1); i++)
            {
                if (targetComponents[i] instanceof Media)
                {
                    final Media media1 = (Media) targetComponents[i];
                    int upTo = i;

                    for (int j = (i + 1); j < targetComponents.length; j++)
                    {

                        if (!(targetComponents[j] instanceof Media))
                        {
                            break;
                        }

                        final Media media2 = (Media) targetComponents[j];

                        if ((media2.getSource() == null) || !media2.getSource().equals(media1.getSource()))
                        {
                            break;
                        }

                        if (((media2.getDuration() == null) && (media1.getDuration() != null)) ||
                            ((media2.getDuration() != null) && !media2.getDuration().equals(media1.getDuration())))
                        {
                            break;
                        }

                        upTo = j;
                    }

                    if (upTo > i)
                    {
                        final Sequence newSequence = new Sequence();
                        newSequence.setRepeatCount(1 + upTo - i);
                        _logger.info("Merging " + newSequence.getRepeatCount() + " identical media in a new sequence");
                        target.addComponent(i, newSequence);

                        for (int j = i; j <= upTo; j++)
                        {
                            target.removeComponent(i + 1);
                            newSequence.addComponent(targetComponents[j]);
                        }

                        i = upTo;
                    }
                }
            }
        }

        private void mergeConsecutiveSequences(final Sequence target)
        {
            final AbstractPlaylistComponent[] targetComponents = target.getComponents();

            for (int i = (targetComponents.length - 1); i > 0 ; i--)
            {
                if ((targetComponents[i - 1] instanceof Sequence) && (targetComponents[i] instanceof Sequence))
                {
                    final Sequence seq1 = (Sequence) targetComponents[i - 1];
                    final Sequence seq2 = (Sequence) targetComponents[i];

                    if (seq1.getRepeatCount() == seq2.getRepeatCount())
                    {

                        _logger.info("Merging sequence " + seq2 + " in sequence " + seq1);
                        final AbstractPlaylistComponent[] components = seq2.getComponents();

                        for (AbstractPlaylistComponent component : components)
                        {
                            seq1.addComponent(component);
                        }

                        target.removeComponent(seq2);
                    }
                }
            }
        }
    }

    public void acceptDown(final PlaylistVisitor visitor) throws Exception
    {
        visitor.beginVisitPlaylist(this);

        _rootSequence.acceptDown(visitor);

        visitor.endVisitPlaylist(this);
    }

    public void acceptUp(final PlaylistVisitor visitor) throws Exception
    {
        visitor.beginVisitPlaylist(this);

        visitor.endVisitPlaylist(this);
    }
}
