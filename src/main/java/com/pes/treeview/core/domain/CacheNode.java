package com.pes.treeview.core.domain;

import lombok.Getter;

import java.util.*;

@Getter
public class CacheNode implements Node<CacheNode> {

    private final UUID guid;
    private final CacheNode parent;
    private final List<CacheNode> childs;
    private String value;
    private boolean enable;
    private boolean visited;
    private boolean changed;
    private boolean copied;

    public CacheNode(String value, CacheNode parent, UUID originalGuid) {
        if (originalGuid != null) {
            copied = true;
            guid = originalGuid;
        } else {
            guid = UUID.randomUUID();
        }
        childs = new ArrayList<>();
        this.value = value;
        this.parent = parent;
        this.enable = true;
    }

    public void addChild(CacheNode node) {
        boolean nodeIsPresented = childs.stream()
                .anyMatch(child -> Objects.equals(child.getGuid(), node.guid));

        if (!nodeIsPresented) {
            childs.add(node);
        }
    }

    public List<CacheNode> getChilds() {
        return new ArrayList<>(childs);
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

    public Optional<CacheNode> findNotVisitedChild() {
        return getChilds()
                .stream()
                .filter(cacheNode -> !cacheNode.isVisited())
                .findFirst();
    }
}
