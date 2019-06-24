package com.pes.treeview.core.persistent;

import com.pes.treeview.core.domain.CacheNode;
import com.pes.treeview.core.domain.Node;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.pes.treeview.core.domain.Nodes.newCacheNodeFromExisted;
import static java.util.stream.Collectors.toList;

@Component
public class CacheTreeStorage {

    private List<CacheNode> cache;

    public CacheTreeStorage() {
        this.cache = new ArrayList<>();
    }

    public void importToChache(Node externalNode, CacheNode parent, List<CacheNode> childs) {
        CacheNode newCacheNode = newCacheNodeFromExisted(
                externalNode.getValue(),
                externalNode.getGuid(),
                childs,
                parent
        );

        if (parent != null) {
            parent.addChild(newCacheNode);
            newCacheNode.setEnable(parent.isEnable());
        } else {
            cache.add(newCacheNode);
        }
    }

    public void removeChildsFromCache(Node externalNode) {
        cache.removeIf(cacheNode -> findChildsGuids(externalNode).contains(cacheNode.getGuid()));
    }

    public List<CacheNode> getChildsFromCache(Node externalNode) {
        return cache.stream()
                .filter(cacheNode -> findChildsGuids(externalNode).contains(cacheNode.getGuid()))
                .collect(toList());
    }

    private List<UUID> findChildsGuids(Node<?> node) {
        return node.getChilds().stream()
                .map(Node::getGuid)
                .collect(toList());
    }

    public void reset() {
        cache = new ArrayList<>();
    }

    public List<CacheNode> getCache() {
        return cache;
    }
}
