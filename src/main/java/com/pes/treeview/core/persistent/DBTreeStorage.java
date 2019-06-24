package com.pes.treeview.core.persistent;

import com.pes.treeview.core.domain.CacheNode;
import com.pes.treeview.core.domain.DbNode;
import com.pes.treeview.core.domain.Nodes;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

import static com.pes.treeview.core.domain.Nodes.newDefaultDbNode;

@Component
public class DBTreeStorage {

    private DbNode tree;

    public DBTreeStorage() {
        reset();
    }

    public DbNode getTree() {
        return tree;
    }

    public void reset() {
        tree = newDefaultDbNode();
    }

}
