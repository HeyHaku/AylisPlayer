

package christophedelory.rss.media;

import java.net.URI;
import java.net.URISyntaxException;

public class Content extends BaseMedia
{

    private URI _url = null;

    private Integer _duration = null;

    private Long _fileSize = null;

    private Integer _width = null;

    private Integer _height = null;

    private Integer _channels = null;

    private Integer _bitrate = null;

    private Integer _framerate = null;

    private Float _samplingrate = null;

    private String _type = null;

    private String _medium = null;

    private String _expression = null;

    private String _lang = null;

    private Boolean _isDefault = null;

    public void setURLString(final String url) throws URISyntaxException
    {
        _url = new URI(url);
    }

    public String getURLString()
    {
        String ret = null;

        if (_url != null)
        {
            ret = _url.toString();
        }

        return ret;
    }

    public void setURL(final URI url)
    {
        _url = url;
    }

    public URI getURL()
    {
        return _url;
    }

    public void setDuration(final int duration)
    {
        _duration = Integer.valueOf(duration);
    }

    public void setDuration(final Integer duration)
    {
        _duration = duration;
    }

    public Integer getDuration()
    {
        return _duration;
    }

    public void setBitrate(final Integer bitrate)
    {
        _bitrate = bitrate;
    }

    public Integer getBitrate()
    {
        return _bitrate;
    }

    public void setFramerate(final Integer framerate)
    {
        _framerate = framerate;
    }

    public Integer getFramerate()
    {
        return _framerate;
    }

    public void setSamplingrate(final Float samplingrate)
    {
        _samplingrate = samplingrate;
    }

    public Float getSamplingrate()
    {
        return _samplingrate;
    }

    public void setWidth(final Integer width)
    {
        _width = width;
    }

    public Integer getWidth()
    {
        return _width;
    }

    public void setHeight(final Integer height)
    {
        _height = height;
    }

    public Integer getHeight()
    {
        return _height;
    }

    public void setChannels(final Integer channels)
    {
        _channels = channels;
    }

    public Integer getChannels()
    {
        return _channels;
    }

    public void setFileSize(final Long fileSize)
    {
        _fileSize = fileSize;
    }

    public Long getFileSize()
    {
        return _fileSize;
    }

    public String getType()
    {
        return _type;
    }

    public void setType(final String type)
    {
        _type = type;
    }

    public String getMedium()
    {
        return _medium;
    }

    public void setMedium(final String medium)
    {
        _medium = medium;
    }

    public String getExpression()
    {
        return _expression;
    }

    public void setExpression(final String expression)
    {
        _expression = expression;
    }

    public String getLang()
    {
        return _lang;
    }

    public void setLang(final String lang)
    {
        _lang = lang;
    }

    public boolean isDefault()
    {
        return (_isDefault == null) ? false   : _isDefault.booleanValue();
    }

    public void setDefault(final boolean isDefault)
    {
        _isDefault = Boolean.valueOf(isDefault);
    }

    public Boolean getIsDefault()
    {
        return _isDefault;
    }

    public void setIsDefault(final Boolean isDefault)
    {
        _isDefault = isDefault;
    }
}
