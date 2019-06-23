package com.pes.treeview.core.service;

import com.pes.treeview.core.domain.CachedNode;
import com.pes.treeview.core.domain.DbNode;
import com.pes.treeview.core.domain.Node;
import com.pes.treeview.core.persistent.CacheTreeStorage;
import com.pes.treeview.core.persistent.DBTreeStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@RequiredArgsConstructor
@Service
public class TreeViewService {

    private final CacheTreeStorage cacheTreeStorage;
    private final DBTreeStorage dbTreeStorage;

    private List<CachedNode> visitedNodes = new ArrayList<>();

    public void exportCacheToDb() {
        List<Node> cachedNodes = cacheTreeStorage.getCache();
        cachedNodes.forEach(this::traverseCachedTree);
        cacheTreeStorage.getInternalCache().forEach((key, val) ->
                System.out.println("Db node: " + val.getValue()));
        cleanVisited();
    }

    public void traverseCachedTree(Node<CachedNode> node) {
        CachedNode tree = (CachedNode) node;

        if (!tree.isEnable()) {
            System.out.println("===Disable node & childs for: " + tree.getValue() + "===");
        }

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
                    if (tree.isEnable()) {
                        continue;
                    }
                }

                tree.setVisited(true);

                //action
                if (!tree.isEnable() && !tree.isCopied()) {
                    System.out.println("===Do nothing for: " + tree.getValue() + "===");
                }
                if (!tree.isEnable() && tree.isCopied()) {
                    System.out.println("===Disable node & childs for: " + tree.getValue() + "===");
                    Node disabledNode = cacheTreeStorage.getInternalCache().get(tree.getGuid());
                    disabledNode.setEnable(false);
                }
                if (tree.isEnable() && !tree.isCopied()) {
                    System.out.println("===Add new node & childs for: " + tree.getValue() + "===");
                    Node newNodeParent = createParentRecursively(tree.getParent());
                    DbNode newNode = new DbNode(tree.getValue(), (DbNode) newNodeParent, tree.getGuid());
                    newNodeParent.addChild(newNode);
                    cacheTreeStorage.getInternalCache().putIfAbsent(newNode.getGuid(), newNode);
                    tree.setCopied(true);
                    tree.setChanged(false);
                }
                if (tree.isEnable() && tree.isChanged()) {
                    System.out.println("===Set new value for node: " + tree.getValue() + "===");
                    Node changedNode = cacheTreeStorage.getInternalCache().get(tree.getGuid());
                    changedNode.setValue(tree.getValue());
                    tree.setChanged(false);
                }

                visitedNodes.add(tree);

                tree = null;
            }
        }
    }

    private Node createParentRecursively(CachedNode treeParent) {
        Node newNodeParent = cacheTreeStorage.getInternalCache().get(treeParent.getGuid());

        if (newNodeParent != null) {
            return newNodeParent;
        }

        newNodeParent = createParentRecursively(treeParent.getParent());


        DbNode newNode = new DbNode(treeParent.getValue(), (DbNode) newNodeParent, treeParent.getGuid());
        newNodeParent.addChild(newNode);
        cacheTreeStorage.getInternalCache().putIfAbsent(newNode.getGuid(), newNode);
        treeParent.setCopied(true);
        treeParent.setChanged(false);

        return newNode;
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

}
