

package com.aylis.comp.ContextualActionBar;

import com.aylis.comp.Common.IGeneralItemContainerIdentifier;
import junit.framework.Assert;
import java.util.ArrayList;
import java.util.List;

public class ItemSelection<T> {

    private Object containerIdentifier;
    private List<T> items = new ArrayList<>();

    public ItemSelection(Object containerIdentifier) {
        Assert.assertNotNull(containerIdentifier);
        this.containerIdentifier = containerIdentifier;
    }

    public Object getContainerIdentifier() {
        return containerIdentifier;
    }

    public boolean containsItem(T item) {
        return items.contains(item);
    }

    public void addSelection(One<T> item) {
        if (!this.containerIdentifier.equals(item.containerIdentifier)) return;

        if (items.contains(item.item)) return;
        items.add(item.item);
    }

    public void subtractSelection(One<T> item) {
        if (!this.containerIdentifier.equals(item.containerIdentifier)) return;

        items.remove(item.item);
    }

    public static class One<T> {

        private IGeneralItemContainerIdentifier containerIdentifier;
        private T item;

        public One(IGeneralItemContainerIdentifier containerIdentifier, T item) {
            Assert.assertNotNull(containerIdentifier);
            Assert.assertNotNull(item);
            this.containerIdentifier = containerIdentifier;
            this.item = item;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof One)) return false;
            One ob = (One) o;
            return this.containerIdentifier.equals(ob.containerIdentifier) && this.item.equals(ob.item);
        }

        @Override
        public int hashCode() {
            return containerIdentifier.hashCode() + item.hashCode();
        }

        public IGeneralItemContainerIdentifier getContainerIdentifier() {
            return containerIdentifier;
        }

        public boolean containsItem(T item) {
            return this.item.equals(item);
        }

        public T getItemIdentifier() {
            return item;
        }
    }

}
