package com.pes.treeview.core.persistent;

import com.pes.treeview.core.domain.CacheNode;
import com.pes.treeview.core.domain.Node;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.pes.treeview.core.domain.Nodes.newCacheNodeFromExternal;
import static java.util.stream.Collectors.toList;

@Component
public class CacheTreeStorage {

    private List<CacheNode> cache;
    private Map<UUID, Node> externalLinksCache;
    private List<CacheNode> visitedNodes = new ArrayList<>();

    public CacheTreeStorage() {
        this.cache = new ArrayList<>();
        this.externalLinksCache = new HashMap<>();
    }

    public void addNewToCache(CacheNode node, String value) {
        node.addChild(new CacheNode(value, node, null));
    }

    public void remove(Node node) {
        Optional<CacheNode> found = cache.stream()
                .map(cacheNode -> findNodeInTreeByGuid(cacheNode, node.getGuid()))
                .filter(Objects::nonNull)
                .findAny();

        if (found.isPresent()) {
            CacheNode foundNode = found.get();
            foundNode.setEnable(false);
        }
        cleanVisited();
    }

    public void importToChache(Node externalNode) {
        if (externalLinksCache.get(externalNode.getGuid()) != null) {
            return;
        }
        externalLinksCache.put(externalNode.getGuid(), externalNode);

        CacheNode parentNode = findNodeInCacheTrees(externalNode.getParent()).orElse(null);
        List<CacheNode> childs = getChildsFromCache(externalNode);
        removeChildsFromCache(externalNode);

        CacheNode newCacheNode = newCacheNodeFromExternal(
                externalNode.getValue(),
                externalNode.getGuid(),
                childs,
                parentNode
        );

        if (parentNode != null) {
            parentNode.addChild(newCacheNode);
            newCacheNode.setEnable(parentNode.isEnable());
        } else {
            cache.add(newCacheNode);
        }
    }

    private boolean removeChildsFromCache(Node externalNode) {
        return cache.removeIf(cacheNode -> findChildsGuids(externalNode).contains(cacheNode.getGuid()));
    }

    private List<CacheNode> getChildsFromCache(Node externalNode) {
        return cache.stream()
                .filter(cacheNode -> findChildsGuids(externalNode).contains(cacheNode.getGuid()))
                .collect(toList());
    }

    public Optional<CacheNode> findNodeInCacheTrees(Node node) {
        return Optional
                .ofNullable(node)
                .flatMap(parent -> cache
                        .stream()
                        .map(cacheNode -> findNodeInTreeByGuid(cacheNode, parent.getGuid()))
                        .filter(Objects::nonNull)
                        .findAny()
                );
    }

    private List<UUID> findChildsGuids(Node<?> node) {
        return node.getChilds().stream()
                .map(Node::getGuid)
                .collect(toList());
    }

    private CacheNode findNodeInTreeByGuid(CacheNode tree, UUID parentGuid) {
        Deque<CacheNode> stack = new LinkedList<>();
        while (tree != null || !stack.isEmpty()) {

            if (!stack.isEmpty()) {
                tree = stack.pop();
            }

            while (tree != null) {
                Optional<CacheNode> child = tree.findNotVisitedChild();
                if (child.isPresent()) {
                    stack.push(tree);
                    tree = child.get();
                    continue;
                }

                if (parentGuid.equals(tree.getGuid())) {
                    cleanVisited();
                    return tree;
                }
                tree.setVisited(true);
                visitedNodes.add(tree);
                tree = null;
            }
        }

        cleanVisited();
        return null;
    }

    private void cleanVisited() {
        visitedNodes.forEach(node -> node.setVisited(false));
        visitedNodes = new ArrayList<>();
    }

    public void reset() {
        cache = new ArrayList<>();
        externalLinksCache = new HashMap<>();
    }

    public List<CacheNode> getCache() {
        return cache;
    }

    public Map<UUID, Node> getExternalLinksCache() {
        return externalLinksCache;
    }
}
