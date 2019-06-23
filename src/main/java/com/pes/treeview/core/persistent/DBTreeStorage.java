package com.pes.treeview.core.persistent;

import com.pes.treeview.core.domain.DbNode;
import com.pes.treeview.core.domain.Node;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DBTreeStorage {

    private DbNode tree;

    public DBTreeStorage() {
        reset();
    }

    public Node getTree() {
        return tree;
    }

    public void reset() {
        tree = buildDefault();
    }

    private DbNode buildDefault(){
        //Root level
        DbNode root = new DbNode("root", null, UUID.randomUUID());
        //Level 1
        DbNode node11 = new DbNode("Node 1", root, UUID.randomUUID());
        DbNode node12 = new DbNode("Node 2", root, UUID.randomUUID());
        root.addChild(node11);
        root.addChild(node12);
        //Level 2
        DbNode node111 = new DbNode("Node 3", node11, UUID.randomUUID());
        DbNode node112 = new DbNode("Node 4", node11, UUID.randomUUID());
        DbNode node113 = new DbNode("Node 5", node11, UUID.randomUUID());
        node11.addChild(node111);
        node11.addChild(node112);
        node11.addChild(node113);
        DbNode node121 = new DbNode("Node 6", node12, UUID.randomUUID());
        node12.addChild(node121);
        //Level 3
        DbNode node1211 = new DbNode("Node 7", node121, UUID.randomUUID());
        DbNode node1212 = new DbNode("Node 8", node121, UUID.randomUUID());
        node121.addChild(node1211);
        node121.addChild(node1212);
        //Level 4
        DbNode node12111 = new DbNode("Node 9", node1211, UUID.randomUUID());
        DbNode node12112 = new DbNode("Node 10", node1211, UUID.randomUUID());
        DbNode node12113 = new DbNode("Node 11", node1211, UUID.randomUUID());
        node1211.addChild(node12111);
        node1211.addChild(node12112);
        node1211.addChild(node12113);

        return root;
    }
}
