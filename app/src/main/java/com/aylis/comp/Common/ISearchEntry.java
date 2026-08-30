

package com.aylis.comp.Common;

public interface ISearchEntry {

    int getIndex();
    String getQuery();
    boolean isEnabled();
    String getHint();
    IGeneralItemContainerIdentifier getContainerIdentifier();

}
