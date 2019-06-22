package com.pes.treeview.ui;

import com.pes.treeview.core.domain.Node;
import com.pes.treeview.core.persistent.CacheTreeStorage;
import com.pes.treeview.core.persistent.DBTreeStorage;
import com.pes.treeview.core.service.TreeViewService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.vaadin.flow.component.grid.Grid.SelectionMode.SINGLE;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER;
import static java.util.Collections.singletonList;


@Route
@UIScope
@Component
public class MainView extends VerticalLayout {

    private TreeGrid<Node> cachedTreeGrid;
    private TreeGrid<Node> dbTreeGrid;
    private Button saveBtn;
    private Button cancelBtn;
    private Button editBtn;
    private Button addBtn;
    private Button removeBtn;
    private Button resetBtn;
    private Button importBtn;
    private Button exportBtn;
    private TextField editableField;

    private TreeViewService treeViewService;

    @Autowired
    public MainView(DBTreeStorage dbTreeStorage, CacheTreeStorage cacheTreeStorage, TreeViewService treeViewService) {
        this.treeViewService = treeViewService;
        editableField = new TextField();
        dbTreeGrid = createTreeGrid(singletonList(dbTreeStorage.getTree()));

        HorizontalLayout baseLayout = new HorizontalLayout();
        baseLayout.add(createCacheTreeBlock(cacheTreeStorage, dbTreeStorage));
        baseLayout.add(createImportBtnBlock(cacheTreeStorage));
        baseLayout.add(createDbTreeBlock());
        add(baseLayout);
    }

    private VerticalLayout createCacheTreeBlock(CacheTreeStorage cacheTreeStorage, DBTreeStorage dbTreeStorage) {
        VerticalLayout baseLayout = new VerticalLayout();
        cachedTreeGrid = createTreeGrid(cacheTreeStorage.getCache());
        Binder<Node> binder = new Binder<>(Node.class);
        Editor<Node> editor = cachedTreeGrid.getEditor();
        editor.setBinder(binder);
        editor.setBuffered(true);
        binder.forField(editableField).bind("value");
        cachedTreeGrid.getColumns().get(0).setEditorComponent(editableField);
        baseLayout.add(cachedTreeGrid);
        baseLayout.add(createCacheBtnsBlock(cacheTreeStorage, dbTreeStorage));

        return baseLayout;
    }

    private VerticalLayout createImportBtnBlock(CacheTreeStorage cacheTreeStorage) {
        VerticalLayout baseLayout = new VerticalLayout();
        importBtn = new Button("Import");
        exportBtn = new Button("Export");

        importBtn.addClickListener(event -> {
            if (!dbTreeGrid.getSelectedItems().isEmpty()) {
                cacheTreeStorage.importToChache(dbTreeGrid.getSelectedItems().iterator().next());
                cachedTreeGrid.setItems(cacheTreeStorage.getCache(), Node::getChilds);
                cachedTreeGrid.getDataProvider().refreshAll();
                cachedTreeGrid.expandRecursively(cacheTreeStorage.getCache(), 10);
            }
        });
        exportBtn.addClickListener(event -> {
            treeViewService.exportCacheToDb();
        });

        baseLayout.add(importBtn);
        baseLayout.add(exportBtn);

        return baseLayout;
    }

    private VerticalLayout createDbTreeBlock() {
        return new VerticalLayout(dbTreeGrid);
    }

    private HorizontalLayout createCacheBtnsBlock(CacheTreeStorage cacheTreeStorage, DBTreeStorage dbTreeStorage) {
        HorizontalLayout baseLayout = new HorizontalLayout();

        saveBtn = new Button("Save");
        saveBtn.addClassName("save");
        saveBtn.setEnabled(false);

        cancelBtn = new Button("Cancel");
        cancelBtn.setEnabled(false);
        cancelBtn.addClassName("cancel");

        editBtn = new Button("Edit");
        editBtn.addClassName("edit");

        addBtn = new Button("+");
        removeBtn = new Button("-");
        resetBtn = new Button("Reset");

        Editor<Node> editor = cachedTreeGrid.getEditor();

        saveBtn.addClickListener(e -> {
            if (!editBtn.isEnabled()) {
                editor.save();
                saveBtn.setEnabled(false);
                cancelBtn.setEnabled(false);
                editBtn.setEnabled(true);
            }
        });
        cancelBtn.addClickListener(e -> {
            if (!editBtn.isEnabled()) {
                editor.cancel();
                saveBtn.setEnabled(false);
                cancelBtn.setEnabled(false);
                editBtn.setEnabled(true);
            }
        });
        editBtn.addClickListener(e -> {
            if (!cachedTreeGrid.getSelectedItems().isEmpty()) {
                Node node = cachedTreeGrid.getSelectedItems().iterator().next();
                if (node.isEnable()) {
                    editor.editItem(node);
                    editableField.focus();
                    editBtn.setEnabled(false);
                    saveBtn.setEnabled(true);
                    cancelBtn.setEnabled(true);
                }
            }
        });
        addBtn.addClickListener(e -> {
            if (!cachedTreeGrid.getSelectedItems().isEmpty()) {
                Node node = cachedTreeGrid.getSelectedItems().iterator().next();
                if (node.isEnable()) {
                    cacheTreeStorage.addNew(node, "New node");
                    cachedTreeGrid.setItems(cacheTreeStorage.getCache(), Node::getChilds);
                    cachedTreeGrid.getDataProvider().refreshAll();
                    cachedTreeGrid.expandRecursively(cacheTreeStorage.getCache(), 10);
                }
            }
        });
        removeBtn.addClickListener(e -> {
            if (!cachedTreeGrid.getSelectedItems().isEmpty()) {
                Node node = cachedTreeGrid.getSelectedItems().iterator().next();
                if (node.isEnable()) {
                    cacheTreeStorage.remove(node);
                    cachedTreeGrid.setItems(cacheTreeStorage.getCache(), Node::getChilds);
                    cachedTreeGrid.getDataProvider().refreshAll();
                    cachedTreeGrid.expandRecursively(cacheTreeStorage.getCache(), 10);
                }
            }
        });
        resetBtn.addClickListener(e -> {
            cacheTreeStorage.reset();
            cachedTreeGrid.setItems(cacheTreeStorage.getCache(), Node::getChilds);
            dbTreeStorage.reset();
            dbTreeGrid.setItems(singletonList(dbTreeStorage.getTree()), Node::getChilds);
            dbTreeGrid.expandRecursively(singletonList(dbTreeStorage.getTree()), 10);
            cachedTreeGrid.getDataProvider().refreshAll();
            dbTreeGrid.getDataProvider().refreshAll();
        });

        baseLayout.add(saveBtn);
        baseLayout.add(cancelBtn);
        baseLayout.add(editBtn);
        baseLayout.add(addBtn);
        baseLayout.add(removeBtn);
        baseLayout.add(resetBtn);

        return baseLayout;
    }

    private TreeGrid<Node> createTreeGrid(List<Node> nodes) {
        TreeGrid<Node> grid = new TreeGrid<>();
        grid.setWidth("45em");

        grid.setItems(
                nodes,
                Node::getChilds
        );

        grid.addHierarchyColumn(node -> displayedValue(node.getValue(), node.isEnable()));
        grid.setSelectionMode(SINGLE);
        grid.expandRecursively(nodes, 10);
        return grid;
    }

    private String displayedValue(String value, boolean enabled) {
        if (!enabled) {
            return "Removed(" + value + ")";
        }

        return value;
    }
}
