

package christophedelory.playlist;

import java.io.OutputStream;

public interface SpecificPlaylist
{

    void setProvider(final SpecificPlaylistProvider provider);

    SpecificPlaylistProvider getProvider();

    void writeTo(final OutputStream out, final String encoding) throws Exception;

    Playlist toPlaylist();
}
