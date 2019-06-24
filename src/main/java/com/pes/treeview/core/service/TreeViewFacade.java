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
                Node disabledNode = cacheTreeStorage.getCachedExternalNodes().get(node.getGuid());
                disabledNode.setEnable(false);
            }

            if (node.isEnable() && !node.isCopied()) {
                DbNode newNodeParent = createDbParentRecursively(node.getParent());
                DbNode newNode = new DbNode(node.getValue(), newNodeParent, node.getGuid());
                newNodeParent.addChild(newNode);
                cacheTreeStorage.getCachedExternalNodes().putIfAbsent(newNode.getGuid(), newNode);
                node.setCopied(true);
                node.setChanged(false);
            }

            if (node.isEnable() && node.isChanged()) {
                Node changedNode = cacheTreeStorage.getCachedExternalNodes().get(node.getGuid());
                changedNode.setValue(node.getValue());
                node.setChanged(false);
            }

            return null;
        };
    }

    private DbNode createDbParentRecursively(CacheNode treeParent) {
        DbNode newNodeParent = (DbNode) cacheTreeStorage.getCachedExternalNodes().get(treeParent.getGuid());

        if (newNodeParent != null) {
            return newNodeParent;
        }

        newNodeParent = createDbParentRecursively(treeParent.getParent());


        DbNode newNode = new DbNode(treeParent.getValue(), newNodeParent, treeParent.getGuid());
        newNodeParent.addChild(newNode);
        cacheTreeStorage.getCachedExternalNodes().putIfAbsent(newNode.getGuid(), newNode);
        treeParent.setCopied(true);
        treeParent.setChanged(false);

        return newNode;
    }

}
