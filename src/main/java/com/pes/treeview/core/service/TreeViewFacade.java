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

import static java.util.Collections.singletonList;

@RequiredArgsConstructor
@Slf4j
@Service
public class TreeViewFacade {

    private final CacheTreeStorage cacheTreeStorage;
    private final DBTreeStorage dbTreeStorage;

    private List<Node> visitedNodes = new ArrayList<>();

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
        if (!cacheTreeStorage.findNodeInCacheTrees(externalNode).isPresent()) {
            cacheTreeStorage.importToChache(externalNode);
        }
    }

    public void addNewToCache(Node node, String value) {
        log.info("Import to cache: " + value);
        cacheTreeStorage.findNodeInCacheTrees(node)
                .ifPresent(cacheNode -> cacheTreeStorage.addNewToCache(cacheNode, value));
    }

    public void disableInCache(Node node) {
        log.info("Mark as removed: " + node.getValue());
        cacheTreeStorage.disableLeaf(node);
    }

    public void exportCacheToDb() {
        log.info("Push cache to db!");
        List<CacheNode> cache = cacheTreeStorage.getCache();
        cache.forEach(tree -> cacheTreeStorage.traverseCacheNodeTree(tree, exportCacheNodeToDb()));
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



    private void disableDbNode(Node node) {
        findDbNode(node).ifPresent(dbNode -> dbNode.setEnable(false));
    }

    private void addNewDbNode(CacheNode node) {
        DbNode newNodeParent = createDbParentRecursively(node.getParent());
        DbNode newNode = new DbNode(node.getValue(), newNodeParent, node.getGuid());
        newNodeParent.addChild(newNode);
        node.setCopied(true);
        node.setChanged(false);
    }

    private void changeValueForDbNode(CacheNode node) {
        findDbNode(node).ifPresent(dbNode -> dbNode.setValue(node.getValue()));
        node.setChanged(false);
    }

    private DbNode createDbParentRecursively(CacheNode treeParent) {
        Optional<DbNode> newNodeParent = findDbNode(treeParent);

        if (newNodeParent.isPresent()) {
            return newNodeParent.get();
        }

        DbNode parent = createDbParentRecursively(treeParent.getParent());

        DbNode newNode = new DbNode(treeParent.getValue(), parent, treeParent.getGuid());
        parent.addChild(newNode);
        treeParent.setCopied(true);
        treeParent.setChanged(false);
        return newNode;
    }

    private Optional<DbNode> findDbNode(Node node) {
        return Optional.ofNullable(traverseTree(dbTreeStorage.getTree(),
                     currentNode -> {
                         if (Objects.equals(currentNode.getGuid(), node.getGuid())) {
                             return currentNode;
                         }
                         return null;
                     }));
    }

    private <T extends Node<T>> T traverseTree(T tree, Function<T, T> action) {
        T result = null;

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
                    cleanVisited();
                    return result;
                }

                tree.setVisited(true);
                visitedNodes.add(tree);
                tree = null;
            }
        }

        cleanVisited();
        return result;
    }

    private void cleanVisited() {
        visitedNodes.forEach(node -> node.setVisited(false));
        visitedNodes = new ArrayList<>();
    }
}
