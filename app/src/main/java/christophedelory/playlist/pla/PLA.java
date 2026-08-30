

package christophedelory.playlist.pla;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import mychristophedelory.content.Content;
import christophedelory.playlist.Media;
import christophedelory.playlist.Playlist;
import christophedelory.playlist.SpecificPlaylist;
import christophedelory.playlist.SpecificPlaylistProvider;

public class PLA implements SpecificPlaylist
{

    private transient SpecificPlaylistProvider _provider = null;

    private final List<String> _filenames = new ArrayList<String>();

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
        byte[] array = new byte[512];
        Arrays.fill(array, (byte) 0);
        array[4] = 'i';
        array[5] = 'r';
        array[6] = 'i';
        array[7] = 'v';
        array[8] = 'e';
        array[9] = 'r';
        array[10] = ' ';
        array[11] = 'U';
        array[12] = 'M';
        array[13] = 'S';
        array[14] = ' ';
        array[15] = 'P';
        array[16] = 'L';
        array[17] = 'A';

        final int nbSongs = _filenames.size();
        array[3] = (byte)((nbSongs & 0x000000ff) >> 0);
        array[2] = (byte)((nbSongs & 0x0000ff00) >> 8);
        array[1] = (byte)((nbSongs & 0x00ff0000) >> 16);
        array[0] = (byte)((nbSongs & 0xff000000) >> 24);

        out.write(array);

        for (String filename : _filenames)
        {
            Arrays.fill(array, (byte) 0);

            final int slashIndex = filename.lastIndexOf('/');
            final int antislashIndex = filename.lastIndexOf('\\');
            int fileIndex = 0;

            if (slashIndex > antislashIndex)
            {
                fileIndex = slashIndex + 1;
            }
            else if (antislashIndex > slashIndex)
            {
                fileIndex = antislashIndex + 1;
            }

            fileIndex++;
            array[1] = (byte)((fileIndex & 0x000000ff) >> 0);
            array[0] = (byte)((fileIndex & 0x0000ff00) >> 8);

            final byte[] tmp = filename.getBytes("UTF-16BE");
            System.arraycopy(tmp, 0, array, 2, tmp.length);

            out.write(array);
        }

        out.flush();
    }

    @Override
    public Playlist toPlaylist()
    {
        final Playlist ret = new Playlist();

        for (String filename : _filenames)
        {
            final Media media = new Media();
            final Content content = new Content(filename);
            media.setSource(content);

            ret.getRootSequence().addComponent(media);
        }

        ret.normalize();

        return ret;
    }

    public List<String> getFilenames()
    {
        return _filenames;
    }
}
