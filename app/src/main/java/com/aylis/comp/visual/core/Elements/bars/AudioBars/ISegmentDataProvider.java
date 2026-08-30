

package com.aylis.comp.visual.core.Elements.bars.AudioBars;

import com.aylis.Common.ISimpleListFloat;
import com.aylis.comp.visual.core.Elements.ICustomizable;
import com.aylis.comp.visual.core.InternalVisualizationDataProvider;

public interface ISegmentDataProvider extends ICustomizable {
    float[] getFrameValues();

    float getRms();

    void process(InternalVisualizationDataProvider visualisationData);

    ISimpleListFloat createFrameValuesAccessorList(int reactionDelay, int reactionAccumulatedDelay, int softnessRadius, ISimpleListFloat barVals);
}

