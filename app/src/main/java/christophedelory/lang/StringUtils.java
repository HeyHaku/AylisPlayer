

package christophedelory.lang;

public final class StringUtils
{

    public static String toString(final int i, final int nbDigits)
    {
        final StringBuilder sb = new StringBuilder(Integer.toString(i));

        while (sb.length() < nbDigits)
        {
            sb.insert(0, '0');
        }

        return sb.toString();
    }

    public static String toString(final long i, final int nbDigits)
    {
        final StringBuilder sb = new StringBuilder(Long.toString(i));

        while (sb.length() < nbDigits)
        {
            sb.insert(0, '0');
        }

        return sb.toString();
    }

    public static String normalize(final String str)
    {
        String ret = null;

        if (str != null)
        {
            final String s = str.trim();

            if (!s.isEmpty())
            {
                ret = s;
            }
        }

        return ret;
    }

    private StringUtils()
    {
    }
}
