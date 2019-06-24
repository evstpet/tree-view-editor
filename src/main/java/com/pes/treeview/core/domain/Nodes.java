package com.pes.treeview.core.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Nodes {

    public static CacheNode newCacheNodeFromExisted(
            String externalNodeValue,
            UUID externalNodeGuid,
            List<CacheNode> childs,
            CacheNode parentNode
    ) {
        CacheNode newNode = new CacheNode(externalNodeValue, parentNode, externalNodeGuid);
        newNode.setCopied(true);
        childs.forEach(newNode::addChild);
        return newNode;
    }

    public static CacheNode newCacheNode(
            String externalNodeValue,
            CacheNode parentNode
    ) {
        Objects.requireNonNull(parentNode);
        return new CacheNode(externalNodeValue, parentNode, UUID.randomUUID());
    }

    public static DbNode newDefaultDbNode() {
        //Root level
        DbNode root = new DbNode("root", null, UUID.randomUUID());
        //Level 1
        DbNode node11 = newDbNode("Node 1", root);
        DbNode node12 = newDbNode("Node 2", root);
        root.addChild(node11);
        root.addChild(node12);
        //Level 2
        DbNode node111 = newDbNode("Node 3", node11);
        DbNode node112 = newDbNode("Node 4", node11);
        DbNode node113 = newDbNode("Node 5", node11);
        node11.addChild(node111);
        node11.addChild(node112);
        node11.addChild(node113);
        DbNode node121 = newDbNode("Node 6", node12);
        node12.addChild(node121);
        //Level 3
        DbNode node1211 = newDbNode("Node 7", node121);
        DbNode node1212 = newDbNode("Node 8", node121);
        node121.addChild(node1211);
        node121.addChild(node1212);
        //Level 4
        DbNode node12111 = newDbNode("Node 9", node1211);
        DbNode node12112 = newDbNode("Node 10", node1211);
        DbNode node12113 = newDbNode("Node 11", node1211);
        node1211.addChild(node12111);
        node1211.addChild(node12112);
        node1211.addChild(node12113);

        return root;
    }
    private static DbNode newDbNode(
            String value,
            DbNode parentNode
    ) {
        return new DbNode(value, parentNode, UUID.randomUUID());
    }

    public static DbNode newDbNodeFromExisted(
            String value,
            DbNode parentNode,
            UUID cacheNodeGuid
    ) {
        Objects.requireNonNull(parentNode);
        return new DbNode(value, parentNode, cacheNodeGuid);
    }
}
