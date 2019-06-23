package com.pes.treeview.core.domain;

import java.util.List;
import java.util.UUID;

public final class Nodes {

    public static CacheNode newCacheNodeFromExternal(
            String externalNodeValue,
            UUID externalNodeGuid,
            List<CacheNode> childs,
            CacheNode parentNode
    ) {
        CacheNode newNode = new CacheNode(externalNodeValue, parentNode, externalNodeGuid);
        childs.forEach(newNode::addChild);
        return newNode;
    }
}
