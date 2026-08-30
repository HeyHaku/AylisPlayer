

package com.aylis.comp.visual.core;

import android.graphics.PointF;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.AlbumArt.ImageLoadedListener;
import com.aylis.comp.visual.core.playback.AudioFrameData;

public interface InternalVisualizationDataProvider {

    AudioFrameData onRequestSoundVisualizationData(AudioFrameData outResult);

    String onRequestsMeasureText(String val);

    PointF onRequestMeasureVec2f(String val, PointF argVec, PointF lastMeasured, Float frameDataRmsValue);

    AlbumArtRequest onRequestsAlbumArtPath();

    void onRequestAlbumArtPathAndBitmap(
            ImageLoadedListener loadedListener,
            Integer targetBoundsWidth,
            Integer targetBoundsHeight,
            AlbumArtRequest albumartRequest);

}

