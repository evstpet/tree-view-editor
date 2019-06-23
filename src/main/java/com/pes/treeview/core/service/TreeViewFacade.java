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

import static java.util.Collections.singletonList;

@RequiredArgsConstructor
@Slf4j
@Service
public class TreeViewFacade {

    private final CacheTreeStorage cacheTreeStorage;
    private final DBTreeStorage dbTreeStorage;

    private List<CacheNode> visitedNodes = new ArrayList<>();

    public List<Node> getCacheTree(){
        return new ArrayList<>(cacheTreeStorage.getCache());
    }

    public List<Node> getDbTree(){
        return singletonList(dbTreeStorage.getTree());
    }

    public void reset() {
        cacheTreeStorage.reset();
        dbTreeStorage.reset();
    }

    public void importToChache(Node externalNode) {
        log.info("Import to cache: " + externalNode.getValue());
        cacheTreeStorage.importToChache(externalNode);
    }

    public void addNewToCache(Node node, String value) {
        log.info("Import to cache: " + value);
        cacheTreeStorage.findNodeInCacheTrees(node)
                .ifPresent(cacheNode -> cacheTreeStorage.addNewToCache(cacheNode, value));
    }

    public void remove(Node node) {
        log.info("Mark as removed: " + node.getValue());
        cacheTreeStorage.remove(node);
    }

    public void exportCacheToDb() {
        log.info("Push cache to db!");
        List<CacheNode> cachedNodes = cacheTreeStorage.getCache();
        cachedNodes.forEach(this::traverseCachedTree);
        cleanVisited();
    }

    private void traverseCachedTree(CacheNode tree) {

        if (!tree.isEnable()) {
            System.out.println("===Disable node & childs for: " + tree.getValue() + "===");
        }

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
                    if (tree.isEnable()) {
                        continue;
                    }
                }

                tree.setVisited(true);

                if (!tree.isEnable() && tree.isCopied()) {
                    Node disabledNode = cacheTreeStorage.getExternalLinksCache().get(tree.getGuid());
                    disabledNode.setEnable(false);
                }
                if (tree.isEnable() && !tree.isCopied()) {
                    Node newNodeParent = createParentRecursively(tree.getParent());
                    DbNode newNode = new DbNode(tree.getValue(), (DbNode) newNodeParent, tree.getGuid());
                    newNodeParent.addChild(newNode);
                    cacheTreeStorage.getExternalLinksCache().putIfAbsent(newNode.getGuid(), newNode);
                    tree.setCopied(true);
                    tree.setChanged(false);
                }
                if (tree.isEnable() && tree.isChanged()) {
                    Node changedNode = cacheTreeStorage.getExternalLinksCache().get(tree.getGuid());
                    changedNode.setValue(tree.getValue());
                    tree.setChanged(false);
                }

                visitedNodes.add(tree);

                tree = null;
            }
        }
    }

    private Node createParentRecursively(CacheNode treeParent) {
        Node newNodeParent = cacheTreeStorage.getExternalLinksCache().get(treeParent.getGuid());

        if (newNodeParent != null) {
            return newNodeParent;
        }

        newNodeParent = createParentRecursively(treeParent.getParent());


        DbNode newNode = new DbNode(treeParent.getValue(), (DbNode) newNodeParent, treeParent.getGuid());
        newNodeParent.addChild(newNode);
        cacheTreeStorage.getExternalLinksCache().putIfAbsent(newNode.getGuid(), newNode);
        treeParent.setCopied(true);
        treeParent.setChanged(false);

        return newNode;
    }

    private void cleanVisited() {
        visitedNodes.forEach(node -> node.setVisited(false));
        visitedNodes = new ArrayList<>();
    }

}
