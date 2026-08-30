

package christophedelory.playlist.kpl;

import java.text.ParseException;

import android.app.Service;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.myapache.commons.logging.Log;
import mychristophedelory.content.type.ContentType;
import christophedelory.io.IOUtils;
import christophedelory.player.PlayerSupport;
import christophedelory.playlist.AbstractPlaylistComponent;
import christophedelory.playlist.Media;
import christophedelory.playlist.Parallel;
import christophedelory.playlist.Playlist;
import christophedelory.playlist.Sequence;
import christophedelory.playlist.SpecificPlaylist;
import christophedelory.playlist.SpecificPlaylistProvider;
import mychristophedelory.xml.Version;

public class KplProvider implements SpecificPlaylistProvider
{

    private static final ContentType[] FILETYPES =
    {
        new ContentType(new String[] { ".kpl" },
                        new String[] { "text/xml" },
                        new PlayerSupport[]
                        {
                        },
                        "Kalliope PlayList"),
    };

    @Override
    public String getId()
    {
        return "kpl";
    }

    @Override
    public ContentType[] getContentTypes()
    {
        return FILETYPES.clone();
    }

    @Override
    public SpecificPlaylist readFrom(final InputStream in, final String encoding, final Log logger) throws Exception
    {
        String enc = encoding;

        if (enc == null)
        {
            enc = "UTF-8";
        }

        String str = IOUtils.toString(in, enc);

        str = str.replace("&", "&amp;");

        str = str.replaceAll("&amp;([a-zA-Z0-9#]+;)", "&$1");

        str = str.replaceAll("<([0-9]+) ", "<x$1 ");
        str = str.replaceAll("</([0-9]+)", "</x$1");

        final StringReader reader = new StringReader(str);

        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        documentBuilder.setErrorHandler(new DefaultHandler());
        final Document document = documentBuilder.parse(new InputSource(reader));

        if (!"xml".equals(document.getDocumentElement().getTagName()))
        {
            throw new IllegalArgumentException("Not a Kalliope playlist (root element is not named 'xml')");
        }

        final Xml ret = new Xml();
        ret.setProvider(this);

        int nb = 0;
        NodeList nodeList = document.getElementsByTagName('x' + Integer.toString(nb));

        while (nodeList.getLength() > 0)
        {
            final Entry entry = new Entry();
            final Element element = (Element) nodeList.item(0);
            entry.setFilename(element.getAttribute("filename"));
            ret.getEntries().add(entry);

            final NodeList tagNodeList = element.getElementsByTagName("tag");

            if (tagNodeList.getLength() > 0)
            {
                final Tag tag = new Tag();
                final Element tagElement = (Element) tagNodeList.item(0);
                tag.setArtist(tagElement.getAttribute("artist"));
                tag.setAlbum(tagElement.getAttribute("album"));
                tag.setTitle(tagElement.getAttribute("title"));
                tag.setYear(tagElement.getAttribute("year"));
                tag.setComment(tagElement.getAttribute("comment"));
                tag.setGenre(tagElement.getAttribute("genre"));
                tag.setTrack(tagElement.getAttribute("track"));
                tag.setGid(tagElement.getAttribute("gid"));
                tag.setHasTag(tagElement.getAttribute("has_tag"));
                entry.setTag(tag);
            }

            nb++;
            nodeList = document.getElementsByTagName('x' + Integer.toString(nb));
        }

        nodeList = document.getElementsByTagName("info");

        if (nodeList.getLength() > 0)
        {
            final Element infoElement = (Element) nodeList.item(0);
            ret.getInfo().setCreationDayString(infoElement.getAttribute("creation_day"));
            ret.getInfo().setModifiedDayString(infoElement.getAttribute("modified_day"));
            ret.getInfo().setAuthor(infoElement.getAttribute("author"));
            ret.getInfo().setPlayer(infoElement.getAttribute("player"));
            ret.getInfo().setPlayerVersion(infoElement.getAttribute("player_version"));
            ret.getInfo().setKplVersion(infoElement.getAttribute("kpl_version"));
        }

        return ret;
    }

    @Override
    public SpecificPlaylist toSpecificPlaylist(final Playlist playlist) throws Exception
    {
        final Xml ret = new Xml();
        ret.setProvider(this);

        final Date day = new Date();
        ret.getInfo().setCreationDay(day);
        ret.getInfo().setModifiedDay(day);
        ret.getInfo().setAuthor("Lizzy v" + Version.CURRENT);

        addToPlaylist(ret.getEntries(), playlist.getRootSequence());

        return ret;
    }

    private void addToPlaylist(final List<Entry> entries, final AbstractPlaylistComponent component) throws Exception
    {
        if (component instanceof Sequence)
        {
            final Sequence sequence = (Sequence) component;

            if (sequence.getRepeatCount() < 0)
            {
                throw new IllegalArgumentException("A KPL playlist cannot handle a sequence repeated indefinitely");
            }

            final AbstractPlaylistComponent[] components = sequence.getComponents();

            for (int iter = 0; iter < sequence.getRepeatCount(); iter++)
            {
                for (AbstractPlaylistComponent c : components)
                {
                    addToPlaylist(entries, c);
                }
            }
        }
        else if (component instanceof Parallel)
        {
            throw new IllegalArgumentException("A KPL playlist cannot play different media at the same time");
        }
        else if (component instanceof Media)
        {
            final Media media = (Media) component;

            if (media.getDuration() != null)
            {
                throw new IllegalArgumentException("A KPL playlist cannot handle a timed media");
            }

            if (media.getRepeatCount() < 0)
            {
                throw new IllegalArgumentException("A KPL playlist cannot handle a media repeated indefinitely");
            }

            if (media.getSource() != null)
            {
                for (int iter = 0; iter < media.getRepeatCount(); iter++)
                {
                    final Entry entry = new Entry();
                    entry.setFilename(media.getSource().toString());
                    entries.add(entry);

                    final Tag tag = new Tag();
                    tag.setGid(Integer.toString(System.identityHashCode(entry)));
                    tag.setGenre("Other");
                    tag.setYear("Unknown Year");
                    tag.setTitle(media.getSource().toString());
                    entry.setTag(tag);
                }
            }
        }
    }
}

