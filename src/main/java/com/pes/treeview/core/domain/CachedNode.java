package com.pes.treeview.core.domain;

import lombok.Getter;

import java.util.*;

@Getter
public class CachedNode implements Node<CachedNode> {

    private final UUID guid;
    private String value;
    private boolean enable;
    private final CachedNode parent;
    private final List<CachedNode> childs;
    private boolean visited;
    private boolean changed;
    private boolean copied;

    public CachedNode(String value, CachedNode parent, UUID dbNodeGuid) {
        if (dbNodeGuid != null) {
            copied = true;
            guid = dbNodeGuid;
        } else {
            guid = UUID.randomUUID();
        }
        childs = new ArrayList<>();
        this.value = value;
        this.parent = parent;
        this.enable = true;
    }

    public void addChild(CachedNode node) {
        boolean nodeIsPresented = childs.stream()
                .anyMatch(child -> Objects.equals(child.getGuid(), node.guid));

        if (!nodeIsPresented) {
            childs.add(node);
        }
    }

    public void setValue(String value) {
        this.value = value;
        this.changed = true;
    }

    public void setEnable(boolean enable) {
        if (this.enable) {
            this.enable = enable;
            childs.forEach(node -> node.setEnable(enable));
        }
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public void setCopied(boolean copied) {
        this.copied = copied;
    }


    @Override
    public String toString() {
        return "CachedNode{" +
                "value='" + value + '\'' +
                ", enable=" + enable +
                ", changed=" + changed +
                ", copied=" + copied +
                '}';
    }
}
