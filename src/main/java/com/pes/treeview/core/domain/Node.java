package com.pes.treeview.core.domain;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface Node<T extends Node> {

    UUID getGuid();

    T getParent();

    void addChild(T node);

    Collection<T> getChilds();

    void setValue(String value);

    String getValue();

    boolean isEnable();

    boolean isVisited();

    void setVisited(boolean visited);

    void setEnable(boolean enable);

    default Optional<T> findNotVisitedChild() {
        return getChilds()
                .stream()
                .filter(cacheNode -> !cacheNode.isVisited())
                .findFirst();
    }
}
