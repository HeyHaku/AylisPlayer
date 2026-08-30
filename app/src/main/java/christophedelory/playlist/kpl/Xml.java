

package christophedelory.playlist.kpl;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import mychristophedelory.content.Content;
import christophedelory.playlist.Media;
import christophedelory.playlist.Playlist;
import christophedelory.playlist.SpecificPlaylist;
import christophedelory.playlist.SpecificPlaylistProvider;

public class Xml implements SpecificPlaylist
{

    private transient SpecificPlaylistProvider _provider = null;

    private final List<Entry> _entries = new ArrayList<Entry>();

    private final Info _info = new Info();

    @Override
    public void setProvider(final SpecificPlaylistProvider provider)
    {
        _provider = provider;
    }

    @Override
    public SpecificPlaylistProvider getProvider()
    {
        return _provider;
    }

    @Override
    public void writeTo(final OutputStream out, final String encoding) throws Exception
    {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        final Document document = documentBuilder.newDocument();
        document.setStrictErrorChecking(false);
        final Element xmlElement = document.createElement("xml");
        document.appendChild(xmlElement);
        int nb = 0;

        for (Entry entry : _entries)
        {
            if (entry.getFilename() != null)
            {
                final Element element = document.createElement(Integer.toString(nb));
                element.setAttribute("filename", entry.getFilename());
                xmlElement.appendChild(element);

                if (entry.getTag() != null)
                {
                    final Element tagElement = document.createElement("tag");
                    tagElement.setAttribute("artist", entry.getTag().getArtist());
                    tagElement.setAttribute("album", entry.getTag().getAlbum());
                    tagElement.setAttribute("title", entry.getTag().getTitle());
                    tagElement.setAttribute("year", entry.getTag().getYear());
                    tagElement.setAttribute("comment", entry.getTag().getComment());
                    tagElement.setAttribute("genre", entry.getTag().getGenre());
                    tagElement.setAttribute("track", entry.getTag().getTrack());
                    tagElement.setAttribute("gid", entry.getTag().getGid());
                    tagElement.setAttribute("has_tag", entry.getTag().getHasTag());
                    element.appendChild(tagElement);
                }

                nb++;
            }
        }

        final Element infoElement = document.createElement("info");
        infoElement.setAttribute("creation_day", _info.getCreationDayString());
        infoElement.setAttribute("modified_day", _info.getModifiedDayString());
        infoElement.setAttribute("author", _info.getAuthor());
        infoElement.setAttribute("player", _info.getPlayer());
        infoElement.setAttribute("player_version", _info.getPlayerVersion());
        infoElement.setAttribute("kpl_version", _info.getKplVersion());
        xmlElement.appendChild(infoElement);

        final DOMSource source = new DOMSource(document);
        final StreamResult result = new StreamResult(out);
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        if (encoding != null)
        {
            transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
        }

        transformer.transform(source, result);

        out.flush();
    }

    @Override
    public Playlist toPlaylist()
    {
        final Playlist ret = new Playlist();

        for (Entry entry : _entries)
        {
            if (entry.getFilename() != null)
            {
                final Media media = new Media();
                final Content content = new Content(entry.getFilename());
                media.setSource(content);
                ret.getRootSequence().addComponent(media);
            }
        }

        ret.normalize();

        return ret;
    }

    public List<Entry> getEntries()
    {
        return _entries;
    }

    public Info getInfo()
    {
        return _info;
    }
}
