

package christophedelory.playlist;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTimeContainer extends AbstractPlaylistComponent
{

    private final List<AbstractPlaylistComponent> _components = new ArrayList<AbstractPlaylistComponent>();

    public AbstractPlaylistComponent[] getComponents()
    {
        final AbstractPlaylistComponent[] ret = new AbstractPlaylistComponent[_components.size()];
        _components.toArray(ret);

        return ret;
    }

    public void addComponent(final AbstractPlaylistComponent component)
    {
        component.setParent(this);
        _components.add(component);
    }

    public void addComponent(final int index, final AbstractPlaylistComponent component)
    {
        component.setParent(this);
        _components.add(index, component);
    }

    public boolean removeComponent(final AbstractPlaylistComponent component)
    {
        component.setParent(null);

        return _components.remove(component);
    }

    public AbstractPlaylistComponent removeComponent(final int index)
    {
        final AbstractPlaylistComponent component = _components.remove(index);
        component.setParent(null);

        return component;
    }

    public int getComponentsNumber()
    {
        return _components.size();
    }

    @Override
    public void acceptDown(final PlaylistVisitor visitor) throws Exception
    {

        final AbstractPlaylistComponent[] components = getComponents();

        for (AbstractPlaylistComponent component : components)
        {
            component.acceptDown(visitor);
        }
    }
}
