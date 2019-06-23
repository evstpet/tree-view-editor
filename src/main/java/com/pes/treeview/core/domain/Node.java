package com.pes.treeview.core.domain;

import java.util.Collection;
import java.util.UUID;

public interface Node<T extends Node> {

    UUID getGuid();

    Node getParent();

    void addChild(T node);

    Collection<T> getChilds();

    void setValue(String value);

    String getValue();

    boolean isEnable();

    void setEnable(boolean enable);
}
