

/*
 * Copyright 2026 Avee Player. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aylis.comp.visual.core.audio.Providers;

import com.aylis.Common.tlog;
import com.aylis.comp.visual.core.Elements.bars.AudioBars.ISegmentDataProvider;

public class SegmentDataProviderFactory {
    public static final String typeNameNone = "None";

    public static final String[] typeNames = new String[]{
            CavaSpectrumProvider.typeName,
            SegmentAudioSpectrumData2.typeName
    };

    public static ISegmentDataProvider create(String typeName, ISegmentDataProvider reuseOld) {

        if(getTypeName(reuseOld).equals(typeName)) return reuseOld;

        if (typeName.equals(typeNameNone)) {
            return null;
        }

        if (SegmentAudioSpectrumData2.typeName.equals(typeName)) {
            return new SegmentAudioSpectrumData2();
        }

        // Return CavaSpectrumProvider for all valid types to maintain preset compatibility
        return new CavaSpectrumProvider();
    }

    public static String getTypeName(ISegmentDataProvider instance)
    {
        if(instance == null) return typeNameNone;

        if(instance instanceof CavaSpectrumProvider)
            return CavaSpectrumProvider.typeName;
        if(instance instanceof SegmentAudioSpectrumData2)
            return SegmentAudioSpectrumData2.typeName;

        tlog.w("unknown instance type");

        return "unk";
    }
}

