

package mychristophedelory.content;

import java.util.Date;

import android.os.Handler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

public class Content
{

    private final String _urlString;

    private transient URI _uri = null;

    private transient URL _url = null;

    private String _encoding = null;

    private long _length = -1L;

    private String _type = null;

    private long _lastModified = 0L;

    private long _duration = -1L;

    private int _width = -1;

    private int _height = -1;

    private transient Boolean _connected = null;

    public Content(final String url)
    {
        _urlString = url.trim().replace('\\', '/');
    }

    public Content(final URI uri)
    {
        _uri = uri.normalize();
        _urlString = uri.toString();
    }

    public Content(final URL url)
    {
        _urlString = url.toString();
        _url = url;
    }

    public URI getURI() throws URISyntaxException
    {
        synchronized(this)
        {
            if (_uri == null)
            {
                URI uri = null;

                if (_url == null)
                {
                    try
                    {
                        uri = new URI(_urlString);
                    }
                    catch (URISyntaxException e)
                    {
                        uri = null;
                    }

                    if ((uri == null) || !uri.isAbsolute())
                    {
                        uri = new File(_urlString).toURI();
                    }
                }
                else
                {
                    uri = _url.toURI();
                }

                _uri = uri.normalize();
            }
        }

        return _uri;
    }

    public URL getURL() throws MalformedURLException
    {
        synchronized(this)
        {
            if (_url == null)
            {
                try
                {
                    _url = new URL(_urlString);
                }
                catch (MalformedURLException e)
                {
                    _uri = new File(_urlString).toURI().normalize();
                    _url = _uri.toURL();
                }
            }
        }

        return _url;
    }

    public String getEncoding()
    {
        return _encoding;
    }

    public void setEncoding(final String encoding)
    {
        _encoding = encoding;
    }

    public long getLength()
    {
        return _length;
    }

    public void setLength(final long length)
    {
        _length = length;
    }

    public String getType()
    {
        return _type;
    }

    public void setType(final String type)
    {
        _type = type;
    }

    public long getLastModified()
    {
        return _lastModified;
    }

    public void setLastModified(final long lastModified)
    {
        _lastModified = lastModified;
    }

    public long getDuration()
    {
        return _duration;
    }

    public void setDuration(final long duration)
    {
        _duration = duration;
    }

    public int getWidth()
    {
        return _width;
    }

    public void setWidth(final int width)
    {
        _width = width;
    }

    public int getHeight()
    {
        return _height;
    }

    public void setHeight(final int height)
    {
        _height = height;
    }

    public boolean isValid()
    {
        return (_connected == null) ? false : _connected.booleanValue();
    }

    public void connect() throws IOException
    {
        boolean connect = false;

        synchronized(this)
        {
            if (_connected == null)
            {
                _connected = Boolean.FALSE;
                connect = true;
            }
        }

        if (connect)
        {
            final URL url = getURL();

            final URLConnection conn = url.openConnection();

            conn.setAllowUserInteraction(false);
            conn.setDoInput(true);
            conn.setDoOutput(false);

            conn.setUseCaches(true);

            conn.connect();

            final String encoding = conn.getContentEncoding();
            final long length = (long) conn.getContentLength();
            final String type = conn.getContentType();
            final long lastModified = conn.getLastModified();

            if (encoding != null)
            {
                _encoding = encoding;
            }

            if (length >= 0L)
            {
                _length = length;
            }

            if ((type != null) && !"content/unknown".equals(type))
            {
                _type = type;
            }

            if (lastModified > 0L)
            {
                _lastModified = lastModified;
            }

            _connected = Boolean.TRUE;
        }
    }

    @Override
    public boolean equals(final Object obj)
    {
        return (obj == null) ? false : _urlString.equals(obj.toString());
    }

    @Override
    public int hashCode()
    {
        return _urlString.hashCode();
    }

    @Override
    public String toString()
    {
        return _urlString;
    }
}

