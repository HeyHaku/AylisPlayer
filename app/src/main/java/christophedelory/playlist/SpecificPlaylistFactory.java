

package christophedelory.playlist;

import android.app.Service;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import org.myapache.commons.logging.Log;
import mychristophedelory.logging.LogFactory;
import mychristophedelory.content.type.ContentType;

public final class SpecificPlaylistFactory
{

    private static SpecificPlaylistFactory _instance = null;

    public static SpecificPlaylistFactory getInstance()
    {
        synchronized(SpecificPlaylistFactory.class)
        {
            if (_instance == null)
            {
                _instance = new SpecificPlaylistFactory();
            }
        }

        return _instance;
    }

    private final ServiceLoader<SpecificPlaylistProvider> _serviceLoader;

    private final Log _logger;

    private SpecificPlaylistFactory()
    {
        _logger = LogFactory.getLog(getClass());
        _serviceLoader = ServiceLoader.load(SpecificPlaylistProvider.class);
    }

    public void reloadProviders()
    {
        _serviceLoader.reload();
    }

    public SpecificPlaylist readFrom(final URL url) throws IOException
    {
        SpecificPlaylist ret = null;

        for (SpecificPlaylistProvider service : _serviceLoader)
        {
            final URLConnection urlConnection = url.openConnection();
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);
            urlConnection.setReadTimeout(60000);
            urlConnection.setUseCaches(true);

            urlConnection.connect();

            final String contentEncoding = urlConnection.getContentEncoding();

            final InputStream in = urlConnection.getInputStream();

            try
            {
                ret = service.readFrom(in, contentEncoding, _logger);

                break;
            }
            catch (Exception e)
            {

                if (_logger.isTraceEnabled())
                {
                    _logger.trace("Playlist provider " + service.getId() + " cannot unmarshal <" + url + ">", e);
                }
                else if (_logger.isDebugEnabled())
                {
                    _logger.debug("Playlist provider " + service.getId() + " cannot unmarshal <" + url + ">: " + e);
                }
            }
            finally
            {
                in.close();
            }
        }

        return ret;
    }

    public SpecificPlaylist readFrom(final File file) throws IOException
    {
        return readFrom(file.toURI().toURL());
    }

    public SpecificPlaylistProvider findProviderById(final String id)
    {
        SpecificPlaylistProvider ret = null;

        for (SpecificPlaylistProvider service : _serviceLoader)
        {
            if (id.equalsIgnoreCase(service.getId()))
            {
                ret = service;
                break;
            }
        }

        return ret;
    }

    public SpecificPlaylistProvider findProviderByExtension(final String filename)
    {
        SpecificPlaylistProvider ret = null;
        final String name = filename.toLowerCase(Locale.ENGLISH);

        for (SpecificPlaylistProvider service : _serviceLoader)
        {
            final ContentType[] types = service.getContentTypes();

            for (ContentType type : types)
            {
                if (type.matchExtension(name))
                {
                    ret = service;
                    break;
                }
            }

            if (ret != null)
            {
                break;
            }
        }

        return ret;
    }

    public List<SpecificPlaylistProvider> getProviders()
    {
        final ArrayList<SpecificPlaylistProvider> ret = new ArrayList<SpecificPlaylistProvider>();

        for (SpecificPlaylistProvider service : _serviceLoader)
        {
            ret.add(service);
        }

        return ret;
    }
}

