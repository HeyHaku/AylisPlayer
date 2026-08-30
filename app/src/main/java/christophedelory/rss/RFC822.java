

package christophedelory.rss;

import java.text.DateFormat;

import java.util.Date;

import java.util.Locale;

import java.text.ParseException;

import java.text.SimpleDateFormat;

final class RFC822
{

    private static final DateFormat FULL_RFC822_DATETIME_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);

    private static final DateFormat FULL_RFC822_DATETIME_FORMAT_2 = new SimpleDateFormat("EEE, d MMM yyyy HH:mm Z", Locale.US);

    private static final DateFormat COMPACT_RFC822_DATETIME_FORMAT = new SimpleDateFormat("d MMM yyyy HH:mm:ss Z", Locale.US);

    private static final DateFormat COMPACT_RFC822_DATETIME_FORMAT_2 = new SimpleDateFormat("d MMM yyyy HH:mm Z", Locale.US);

    public static final DateFormat ISO8601_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

    public static String toString(final Date date)
    {
        synchronized(FULL_RFC822_DATETIME_FORMAT)
        {
            return FULL_RFC822_DATETIME_FORMAT.format(date);
        }
    }

    public static Date valueOf(final String dateString)
    {
        Date ret = null;

        synchronized(FULL_RFC822_DATETIME_FORMAT)
        {
            try
            {
                ret = FULL_RFC822_DATETIME_FORMAT.parse(dateString);
            }
            catch (ParseException e)
            {

                ret = null;
            }
        }

        if (ret == null)
        {
            synchronized(FULL_RFC822_DATETIME_FORMAT_2)
            {
                try
                {
                    ret = FULL_RFC822_DATETIME_FORMAT_2.parse(dateString);
                }
                catch (ParseException e)
                {

                    ret = null;
                }
            }
        }

        if (ret == null)
        {
            synchronized(COMPACT_RFC822_DATETIME_FORMAT)
            {
                try
                {
                    ret = COMPACT_RFC822_DATETIME_FORMAT.parse(dateString);
                }
                catch (ParseException e)
                {

                    ret = null;
                }
            }
        }

        if (ret == null)
        {
            synchronized(COMPACT_RFC822_DATETIME_FORMAT_2)
            {
                try
                {
                    ret = COMPACT_RFC822_DATETIME_FORMAT_2.parse(dateString);
                }
                catch (ParseException e)
                {

                    ret = null;
                }
            }
        }

        return ret;
    }

    private RFC822()
    {
    }
}

