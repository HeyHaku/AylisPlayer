

package mychristophedelory.xml;

import java.io.Serializable;

public class Version implements Cloneable, Serializable
{

    private static final long serialVersionUID = 0L;

    public static Version CURRENT = new Version();

    public static Version valueOf(final String name)
    {
        final int k = name.indexOf('.');

        if (k < 0)
        {
            throw new IllegalArgumentException("The format of a version string is <version.revision.step>");
        }

        if (k == 0)
        {
            throw new IllegalArgumentException("No version part in version string '" + name + '\'');
        }

        if ((k + 1) >= name.length())
        {
            throw new IllegalArgumentException("No revision/step part in version string '" + name + '\'');
        }

        final int l = name.indexOf('.', k + 1);

        if (l < 0)
        {
            throw new IllegalArgumentException("The format of a version string is <version.revision.step>");
        }

        if (l == (k + 1))
        {
            throw new IllegalArgumentException("No revision part in version string '" + name + '\'');
        }

        if ((l + 1) >= name.length())
        {
            throw new IllegalArgumentException("No step part in version string '" + name + '\'');
        }

        final String versionString = name.substring(0, k);
        final String revisionString = name.substring(k + 1, l);
        final String stepString = name.substring(l + 1);

        final int version = Integer.parseInt(versionString);
        final int revision = Integer.parseInt(revisionString);
        final int step = Integer.parseInt(stepString);

        return new Version(version, revision, step);
    }

    private int _version;

    private int _revision;

    private int _step;

    private Version()
    {
        _version = 0;
        _revision = 0;
        _step = 0;
    }

    public Version(final int version, final int revision, final int step)
    {
        setVersion(version);
        setRevision(revision);
        setStep(step);
    }

    private void setVersion(final int version)
    {
        if (version < 0)
        {
            throw new IndexOutOfBoundsException("Version number is negative");
        }

        _version = version;
    }

    public int getVersion()
    {
        return _version;
    }

    private void setRevision(final int revision)
    {
        if (revision < 0)
        {
            throw new IndexOutOfBoundsException("Revision number is negative");
        }

        _revision = revision;
    }

    public int getRevision()
    {
        return _revision;
    }

    private void setStep(final int step)
    {
        if (step < 0)
        {
            throw new IndexOutOfBoundsException("Step number is negative");
        }

        _step = step;
    }

    public int getStep()
    {
        return _step;
    }

    public int compareTo(final Object o)
    {
        return hashCode() - ((Version) o).hashCode();
    }

    @Override
    public boolean equals(final Object obj)
    {
        boolean ret = false;

        if ((obj != null) && (obj instanceof Version))
        {
            ret = (hashCode() == obj.hashCode());
        }

        return ret;
    }

    @Override
    public int hashCode()
    {
        return ((_version & 0x000003ff) << 20) | ((_revision & 0x000003ff) << 10) | (_step & 0x000003ff);
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append(_version);
        sb.append('.');
        sb.append(_revision);
        sb.append('.');
        sb.append(_step);

        return sb.toString();
    }
}
