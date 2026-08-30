

package com.aylis.comp.visual.core.Elements.Segment;

import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.Elements.bars.BarsShapes.ISegmentPath;
import com.aylis.comp.visual.core.Elements.bars.BarsShapes.SegmentPathCircle;
import com.aylis.comp.visual.core.Elements.bars.BarsShapes.SegmentPathHorizontalLine;
import com.aylis.comp.visual.core.Elements.bars.BarsShapes.SegmentPathRoundedRect;
import com.aylis.comp.visual.core.Elements.bars.BarsShapes.SegmentPathSided;

public class SegmentPathFactory {

    public static final String typeNameNone = "None";

    public static final String[] typeNames = new String[]{
            SegmentPathHorizontalLine.typeName,
            SegmentPathCircle.typeName,
            SegmentPathSided.typeName,
            SegmentPathRoundedRect.typeName
    };

    public static ISegmentPath create(String typeName, ISegmentPath reuseOld) {

        if(getTypeName(reuseOld).equals(typeName)) return reuseOld;

        switch (typeName) {
            case typeNameNone:
                return null;
            case SegmentPathHorizontalLine.typeName:
                return new SegmentPathHorizontalLine();
            case SegmentPathCircle.typeName:
                return new SegmentPathCircle();
            case SegmentPathSided.typeName:
                return new SegmentPathSided();
            case SegmentPathRoundedRect.typeName:
                return new SegmentPathRoundedRect();
        }

        tlog.w("unknown typeName: "+typeName);

        return reuseOld;
    }

    public static String getTypeName(ISegmentPath instance) {
        if(instance == null) return typeNameNone;

        if(instance instanceof SegmentPathHorizontalLine)
            return SegmentPathHorizontalLine.typeName;
        else if(instance instanceof SegmentPathCircle)
            return SegmentPathCircle.typeName;
        else if(instance instanceof SegmentPathSided)
            return SegmentPathSided.typeName;
        else if(instance instanceof SegmentPathRoundedRect)
            return SegmentPathRoundedRect.typeName;

        tlog.w("unknown instance type");

        return "unk";
    }
}
