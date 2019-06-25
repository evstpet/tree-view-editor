package com.pes.treeview.core.service;

import com.pes.treeview.core.domain.CacheNode;
import com.pes.treeview.core.domain.DbNode;
import com.pes.treeview.core.domain.Node;
import com.pes.treeview.core.persistent.CacheTreeStorage;
import com.pes.treeview.core.persistent.DBTreeStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

import static com.pes.treeview.core.domain.Nodes.newCacheNode;
import static com.pes.treeview.core.domain.Nodes.newDbNodeFromExisted;
import static java.util.Collections.singletonList;

@RequiredArgsConstructor
@Slf4j
@Service
public class TreeViewFacade {

    private final CacheTreeStorage cacheTreeStorage;
    private final DBTreeStorage dbTreeStorage;

    public List<Node> getCacheTree() {
        return new ArrayList<>(cacheTreeStorage.getCache());
    }

    public List<Node> getDbTree() {
        return singletonList(dbTreeStorage.getTree());
    }

    public void reset() {
        cacheTreeStorage.reset();
        dbTreeStorage.reset();
    }

    public void importToChache(Node externalNode) {
        log.info("Import to cache: " + externalNode.getValue());
        if (externalNode.isEnable() && !findCacheNode(externalNode).isPresent()) {
            Optional<CacheNode> parent = externalNode.getParent() != null ?
                    findCacheNode(externalNode.getParent()) :
                    Optional.empty();
            List<CacheNode> childs = cacheTreeStorage.getChildsFromCache(externalNode);

            cacheTreeStorage.importToChache(externalNode, parent.orElse(null), childs);

            cacheTreeStorage.removeChildsFromCache(externalNode);
        }
    }

    public void addNewToCache(Node node, String value) {
        log.info("Import to cache: " + value);
        findCacheNode(node).ifPresent(cacheNode -> cacheNode.addChild(newCacheNode(value, cacheNode)));
    }

    public void disableInCache(Node node) {
        log.info("Mark as removed: " + node.getValue());
        findCacheNode(node).ifPresent(foundNode -> foundNode.setEnable(false));
    }

    public void exportCacheToDb() {
        log.info("Push cache to db!");
        cacheTreeStorage.getCache().forEach(tree -> traverseTree(tree, exportCacheNodeToDb()));
        log.info("Refresh cache from db!");
        cacheTreeStorage.getCache().forEach(tree -> traverseTree(tree, refreshCacheNodeFromDb()));
    }

    private Function<CacheNode, CacheNode> exportCacheNodeToDb() {
        return node -> {

            if (!node.isEnable() && node.isCopied()) {
                disableDbNode(node);
            }

            if (node.isEnable() && !node.isCopied()) {
                addNewDbNode(node);
            }

            if (node.isEnable() && node.isChanged()) {
                changeValueForDbNode(node);
            }

            return null;
        };
    }

    private Function<CacheNode, CacheNode> refreshCacheNodeFromDb() {
        return node -> {
            findDbNode(node).ifPresent(dbNode -> {
                node.setValue(dbNode.getValue());
                node.setEnable(dbNode.isEnable());
            });

            return null;
        };
    }

    private void disableDbNode(Node node) {
        findDbNode(node).ifPresent(dbNode -> dbNode.setEnable(false));
    }

    private void addNewDbNode(CacheNode node) {
        if (isPermittedToAddNewDbNode(node)) {
            DbNode newNodeParent = createDbParentRecursively(node.getParent());
            DbNode newNode = newDbNodeFromExisted(node.getValue(), newNodeParent, node.getGuid());
            newNodeParent.addChild(newNode);
            node.setCopied(true);
            node.setChanged(false);
        }
    }

    private boolean isPermittedToAddNewDbNode(CacheNode node) {
        CacheNode parent = node.getParent();

        while (parent != null) {
            DbNode dbNode = findDbNode(parent).orElse(null);

            if (dbNode == null) {
                parent = parent.getParent();
                continue;
            }

            if (dbNode.isEnable()) {
                return true;
            }

            log.warn("Can't export: " + node.getValue() + ", because ancestor in db: " + dbNode.getValue() + " is disabled");
            return false;
        }

        return true;
    }

    private void changeValueForDbNode(CacheNode node) {
        findDbNode(node).filter(DbNode::isEnable).ifPresent(dbNode -> dbNode.setValue(node.getValue()));
        node.setChanged(false);
    }

    private DbNode createDbParentRecursively(CacheNode treeParent) {
        Optional<DbNode> newNodeParent = findDbNode(treeParent);

        if (newNodeParent.isPresent()) {
            return newNodeParent.get();
        }

        DbNode parent = createDbParentRecursively(treeParent.getParent());

        DbNode newNode = newDbNodeFromExisted(treeParent.getValue(), parent, treeParent.getGuid());
        parent.addChild(newNode);
        treeParent.setCopied(true);
        treeParent.setChanged(false);
        return newNode;
    }

    private Optional<DbNode> findDbNode(Node node) {
        return findNodeByGuid(dbTreeStorage.getTree(), node.getGuid());
    }

    private Optional<CacheNode> findCacheNode(Node node) {
        return cacheTreeStorage.getCache().stream()
                .map(cacheNode -> findNodeByGuid(cacheNode, node.getGuid()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private <T extends Node<T>> Optional<T> findNodeByGuid(T tree, UUID guid) {
        return Optional.ofNullable(traverseTree(tree,
                                                currentNode -> {
                                                    if (Objects.equals(currentNode.getGuid(), guid)) {
                                                        return currentNode;
                                                    }
                                                    return null;
                                                }));
    }

    private <T extends Node<T>> T traverseTree(T tree, Function<T, T> action) {
        T result = null;
        List<T> visitedNodes = new ArrayList<>();

        Deque<T> stack = new LinkedList<>();
        while (tree != null || !stack.isEmpty()) {

            if (!stack.isEmpty()) {
                tree = stack.pop();
            }

            while (tree != null) {
                Optional<T> child = tree.findNotVisitedChild();
                if (child.isPresent()) {
                    stack.push(tree);
                    tree = child.get();
                    continue;
                }

                result = action.apply(tree);

                if (result != null) {
                    visitedNodes.forEach(node -> node.setVisited(false));
                    return result;
                }

                tree.setVisited(true);
                visitedNodes.add(tree);
                tree = null;
            }
        }


        visitedNodes.forEach(node -> node.setVisited(false));
        return result;
    }
}
