package com.dci.intellij.dbn.common.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.jetbrains.annotations.NotNull;

import com.dci.intellij.dbn.common.filter.Filter;

public abstract class AbstractFiltrableList<T> implements FiltrableList<T> {
    private List<T> list;

    public AbstractFiltrableList() {
        list = new ArrayList<T>();
    }

    public AbstractFiltrableList(List<T> list) {
        this.list = list;
    }

    @Override
    public List<T> getFullList() {return list;}

    // update methods should not be affected by filtering
    @Override
    public void sort(Comparator<T> comparator)          {
        Collections.sort(list, comparator);}

    public boolean add(T o)                             {return list.add(o);}

    public boolean addAll(@NotNull Collection<? extends T> c)    {return list.addAll(c);}

    public boolean remove(Object o)                     {return list.remove(o);}

    public boolean removeAll(@NotNull Collection c)              {return list.removeAll(c);}

    public boolean retainAll(@NotNull Collection c)              {return list.retainAll(c);}

    public void clear()                                 {if (!list.isEmpty()) list.clear();}

    public boolean isEmpty()                            {return size() == 0;}

    private boolean isFiltered() {
        return getFilter() != null;
    }

    public int size() {
        Filter<T> filter = getFilter();
        if (filter != null) {
            int count = 0;
            for (T object : list) if (filter.accepts(object)) count++;
            return count;
        } else {
            return list.size();

        }
    }

    @NotNull
    public Iterator<T> iterator(){
        final Filter<T> filter = getFilter();
        if (filter != null) {
            return new Iterator<T>() {
                private Iterator<T> iterator = list.iterator();
                private T next = findNext();
                private T findNext() {
                    while (iterator.hasNext()) {
                        next = iterator.next();
                        if (filter.accepts(next)) return next;
                    }
                    return null;
                }

                public boolean hasNext() { return next != null;}
                public T next() {
                    T result = next;
                    next = findNext();
                    return result;
                }
                public void remove(){
                    throw new UnsupportedOperationException("Iterator remove not implemented in filtrable list");
                }
            };
        } else {
            return list.iterator();

        }
    }

    @NotNull
    public Object[] toArray() {
        Filter<T> filter = getFilter();
        if (filter != null) {
            List<T> result = new ArrayList<T>();
            for (T object : list) if (filter.accepts(object)) result.add(object);
            return result.toArray();
        } else {
            return list.toArray();
        }
    }

    @NotNull
    public <E> E[] toArray(@NotNull E[] e) {
        Filter<T> filter = getFilter();
        if (filter != null) {
            List<T> result = new ArrayList<T>();
            for (T object : list) if (filter.accepts(object)) result.add(object);
            return result.toArray(e);
        } else {
            return list.toArray(e);
        }

    }

    public boolean contains(Object o){
        if (isFiltered()) {
            return indexOf(o) > -1;
        } else {
            return list.contains(o);
        }

    }

    public int indexOf(Object o) {
        Filter<T> filter = getFilter();
        if (filter != null) {
            if (!filter.accepts((T) o)) return -1;

            int index = 0;
            for (T object : list) {
                if (object.equals(o)) return index;
                if (filter.accepts(object)) index++;
            }
            return -1;
        } else {
            return list.indexOf(o);
        }
    }

    public int lastIndexOf(Object o) {
        Filter<T> filter = getFilter();
        if (filter != null) {
            if (!filter.accepts((T) o)) return -1;

            int index = size()-1;
            for (int i = list.size()-1; i > -1; i--) {
                T object = list.get(i);
                if (object.equals(o)) return index;
                if (filter.accepts(object)) index--;
            }
            return -1;
        } else {
            return list.lastIndexOf(o);
        }

    }

    public boolean containsAll(@NotNull Collection c) {
        if (isFiltered()) {
            List list = Arrays.asList(toArray());
            return list.containsAll(c);
        } else {
            return list.containsAll(c);
        }
    }

    public boolean equals(Object o){
        if (isFiltered()) {
            List list = Arrays.asList(toArray());
            return list.equals(o);
        } else {
            return list.equals(o);
        }

    }

    private int findIndex(int index) {
        int count = -1;
        Filter<T> filter = getFilter();
        for (int i = 0; i < list.size(); i++) {
            T object = list.get(i);
            if (filter == null || filter.accepts(object)) count++;
            if (count == index) return i;
        }
        return -1;
    }

    public void add(int index, T element){
        if (isFiltered()) {
            int idx = findIndex(index);
            list.add(idx, element);
        } else {
            list.add(index, element);
        }
    }

    public boolean addAll(int index, @NotNull Collection<? extends T> c){
        if (isFiltered()) {
            int idx = findIndex(index);
            return list.addAll(idx, c);
        } else {
            return list.addAll(index, c);
        }
    }

    public T get(int index){
        if (isFiltered()) {
            int idx = findIndex(index);
            return idx == -1 ? null : list.get(idx);
        } else {
            return list.get(index);
        }
    }

    public T set(int index, T element) {
        if (isFiltered()) {
            int idx = findIndex(index);
            list.set(idx, element);
            return list.get(idx);
        } else {
            return list.set(index, element);
        }
    }

    public T remove(int index) {
        if (isFiltered()) {
            int idx = findIndex(index);
            return list.remove(idx);
        } else {
            return list.remove(index);
        }
    }

    @NotNull
    public ListIterator<T> listIterator()               {throw new UnsupportedOperationException("List iterator not implemented in filtrable list");}

    @NotNull
    public ListIterator<T> listIterator(int index)      {throw new UnsupportedOperationException("List iterator not implemented in filtrable list");}

    @NotNull
    public List<T> subList(int fromIndex, int toIndex)  {throw new UnsupportedOperationException("Sublist not implemented in filtrable list");}

}