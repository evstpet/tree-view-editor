package com.pes.treeview.core.persistent;

import com.pes.treeview.core.domain.CachedNode;
import com.pes.treeview.core.domain.DbNode;
import com.pes.treeview.core.domain.Node;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.util.stream.Collectors.toList;

@Component
public class CacheTreeStorage {

    private List<CachedNode> cache;
    private List<CachedNode> visitedNodes = new ArrayList<>();

    public CacheTreeStorage() {
        this.cache = new ArrayList<>();
    }

    public void addNew(Node<CachedNode> node, String value) {
        node.addChild(new CachedNode(value, (CachedNode) node, null));
    }

    public void remove(Node node) {
        Optional<CachedNode> found = cache.stream()
                .map(cachedNode -> findParent(cachedNode, node.getGuid()))
                .filter(Objects::nonNull)
                .findAny();

        if (found.isPresent()) {
            CachedNode foundNode = found.get();
            foundNode.setEnable(false);
        }
        cleanVisited();
    }

    public void importToChache(Node node) {
        if (cache.stream().anyMatch(cachedNode -> Objects.equals(cachedNode.getGuid(), node.getGuid()))) {
            return;
        }

        CachedNode parentNode = null;

        if (node.getParent() != null) {
            Optional<CachedNode> parent = cache.stream()
                    .map(cachedNode -> findParent(cachedNode, node.getParent().getGuid()))
                    .filter(Objects::nonNull)
                    .findAny();

            if (parent.isPresent()) {
                parentNode = parent.get();
            }
        }

        List<CachedNode> childs = cache.stream()
                .filter(child -> findChildsGuids(node).contains(child.getGuid()))
                .collect(toList());

        cache.removeIf(cachedNode -> findChildsGuids(node).contains(cachedNode.getGuid()));

        if (parentNode != null) {
            CachedNode newNode = new CachedNode(node.getValue(), parentNode, node.getGuid());
            childs.forEach(newNode::addChild);
            newNode.setEnable(parentNode.isEnable());
            parentNode.addChild(newNode);
        } else {
            CachedNode newNode = new CachedNode(node.getValue(), null, node.getGuid());
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

    private CachedNode findParent(CachedNode tree, UUID parentGuid) {
        Deque<CachedNode> stack = new LinkedList<>();
        while (tree != null || !stack.isEmpty()) {

            if (!stack.isEmpty()) {
                tree = stack.pop();
            }

            while (tree != null) {
                Optional<CachedNode> child = findChild(tree);
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

    private Optional<CachedNode> findChild(CachedNode node) {
        return node.getChilds().stream()
                        .filter(cachedNode -> !cachedNode.isVisited())
                        .findFirst();
    }

    private void cleanVisited() {
        visitedNodes.forEach(node -> node.setVisited(false));
        visitedNodes = new ArrayList<>();
    }

    public void reset(){
        cache = new ArrayList<>();
    }

    public List<Node> getCache(){
        return new ArrayList<>(cache);
    }
}
