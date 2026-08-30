

package christophedelory.io;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;

public final class IOUtils
{

    public static String toString(final InputStream in, final String encoding) throws IOException
    {
        final InputStreamReader reader;

        if (encoding == null)
        {
            reader = new InputStreamReader(in);
        }
        else
        {
            reader = new InputStreamReader(in, encoding);
        }

        final StringWriter writer = new StringWriter();
        final char[] buffer = new char[512];
        int nb = 0;

        while (-1 != (nb = reader.read(buffer)))
        {
            writer.write(buffer, 0, nb);
        }

        return writer.toString();
    }

    private IOUtils()
    {
    }
}
