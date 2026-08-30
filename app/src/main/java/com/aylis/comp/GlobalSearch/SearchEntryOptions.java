

package com.aylis.comp.GlobalSearch;

import com.aylis.comp.Common.IGeneralItemContainerIdentifier;

public class SearchEntryOptions {

    public static SearchEntryOptions refuse = new SearchEntryOptions();

    public boolean enabled = false;
    public String hint = "";
    public IGeneralItemContainerIdentifier containerIdentifier = null;

}
