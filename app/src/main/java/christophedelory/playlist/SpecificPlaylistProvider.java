

package christophedelory.playlist;

import android.app.Service;

import java.io.InputStream;
import org.myapache.commons.logging.Log;
import mychristophedelory.content.type.ContentType;

public interface SpecificPlaylistProvider
{

    String getId();

    ContentType[] getContentTypes();

    SpecificPlaylist readFrom(final InputStream in, final String encoding, final Log logger) throws Exception;

    SpecificPlaylist toSpecificPlaylist(final Playlist playlist) throws Exception;
}

