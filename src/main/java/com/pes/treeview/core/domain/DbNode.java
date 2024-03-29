package com.pes.treeview.core.domain;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class DbNode implements Node<DbNode> {

    private final UUID guid;
    private final DbNode parent;
    private final List<DbNode> childs;
    private String value;
    private boolean enable;
    private boolean visited;

    DbNode(String value, DbNode parent, UUID guid) {
        this.guid = guid;
        childs = new ArrayList<>();
        this.value = value;
        this.parent = parent;
        this.enable = true;
    }

    public void addChild(DbNode node) {
        childs.add(node);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public void setEnable(boolean enable) {
        if (this.enable) {
            this.enable = enable;
            childs.forEach(node -> node.setEnable(enable));
        }
    }
}
