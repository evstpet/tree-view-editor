package com.pes.treeview.core.persistent;

import com.pes.treeview.core.domain.CacheNode;
import com.pes.treeview.core.domain.Node;
import org.springframework.stereotype.Component;

import java.util.*;

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

    public void addNew(Node<CacheNode> node, String value) {
        node.addChild(new CacheNode(value, (CacheNode) node, null));
    }

    public void remove(Node node) {
        Optional<CacheNode> found = cache.stream()
                .map(cacheNode -> findParent(cacheNode, node.getGuid()))
                .filter(Objects::nonNull)
                .findAny();

        if (found.isPresent()) {
            CacheNode foundNode = found.get();
            foundNode.setEnable(false);
        }
        cleanVisited();
    }

    public void importToChache(Node node) {
        if (cache.stream().anyMatch(cacheNode -> Objects.equals(cacheNode.getGuid(), node.getGuid()))) {
            return;
        }

        externalLinksCache.putIfAbsent(node.getGuid(), node);

        CacheNode parentNode = null;

        if (node.getParent() != null) {
            Optional<CacheNode> parent = cache.stream()
                    .map(cacheNode -> findParent(cacheNode, node.getParent().getGuid()))
                    .filter(Objects::nonNull)
                    .findAny();

            if (parent.isPresent()) {
                parentNode = parent.get();
            }
        }

        List<CacheNode> childs = cache.stream()
                .filter(child -> findChildsGuids(node).contains(child.getGuid()))
                .collect(toList());

        cache.removeIf(cacheNode -> findChildsGuids(node).contains(cacheNode.getGuid()));

        if (parentNode != null) {
            CacheNode newNode = new CacheNode(node.getValue(), parentNode, node.getGuid());
            childs.forEach(newNode::addChild);
            newNode.setEnable(parentNode.isEnable());
            parentNode.addChild(newNode);
        } else {
            CacheNode newNode = new CacheNode(node.getValue(), null, node.getGuid());
            childs.forEach(newNode::addChild);
            cache.add(newNode);
        }

        cleanVisited();
    }

    private List<UUID> findChildsGuids(Node<?> node) {


        return node.getChilds().stream()
                .map(Node::getGuid)
                .collect(toList());
    }

    private CacheNode findParent(CacheNode tree, UUID parentGuid) {
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
                    return tree;
                }
                tree.setVisited(true);
                visitedNodes.add(tree);
                tree = null;
            }
        }

        return null;
    }

    private void cleanVisited() {
        visitedNodes.forEach(node -> node.setVisited(false));
        visitedNodes = new ArrayList<>();
    }

    public void reset(){
        cache = new ArrayList<>();
        externalLinksCache = new HashMap<>();
    }

    public List<Node> getCache(){
        return new ArrayList<>(cache);
    }

    public Map<UUID, Node> getExternalLinksCache() {
        return externalLinksCache;
    }
}
