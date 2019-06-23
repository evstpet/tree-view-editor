package com.pes.treeview.ui;

import com.pes.treeview.core.domain.Node;
import com.pes.treeview.core.service.TreeViewFacade;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.vaadin.flow.component.grid.Grid.SelectionMode.SINGLE;


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

    private TreeViewFacade treeViewFacade;

    @Autowired
    public MainView(TreeViewFacade treeViewFacade) {
        this.treeViewFacade = treeViewFacade;
        editableField = new TextField();
        dbTreeGrid = createTreeGrid(treeViewFacade.getDbTree());

        HorizontalLayout baseLayout = new HorizontalLayout();
        baseLayout.add(createCacheTreeBlock());
        baseLayout.add(createImportBtnBlock());
        baseLayout.add(createDbTreeBlock());
        add(baseLayout);
    }

    private VerticalLayout createCacheTreeBlock() {
        VerticalLayout baseLayout = new VerticalLayout();
        cachedTreeGrid = createTreeGrid(treeViewFacade.getCacheTree());
        Binder<Node> binder = new Binder<>(Node.class);
        Editor<Node> editor = cachedTreeGrid.getEditor();
        editor.setBinder(binder);
        editor.setBuffered(true);
        binder.forField(editableField).bind("value");
        cachedTreeGrid.getColumns().get(0).setEditorComponent(editableField);
        baseLayout.add(cachedTreeGrid);
        baseLayout.add(createCacheBtnsBlock());

        return baseLayout;
    }

    private VerticalLayout createImportBtnBlock() {
        VerticalLayout baseLayout = new VerticalLayout();
        importBtn = new Button("Import");
        exportBtn = new Button("Export");

        importBtn.addClickListener(event -> {
            if (!dbTreeGrid.getSelectedItems().isEmpty()) {
                treeViewFacade.importToChache(dbTreeGrid.getSelectedItems().iterator().next());
                refreshCacheTreeGrid();
            }
        });
        exportBtn.addClickListener(event -> {
            treeViewFacade.exportCacheToDb();
            refreshDbTreeGrid();
            refreshCacheTreeGrid();
        });

        baseLayout.add(importBtn);
        baseLayout.add(exportBtn);

        return baseLayout;
    }

    private VerticalLayout createDbTreeBlock() {
        return new VerticalLayout(dbTreeGrid);
    }

    private HorizontalLayout createCacheBtnsBlock() {
        HorizontalLayout baseLayout = new HorizontalLayout();
        Editor<Node> editor = cachedTreeGrid.getEditor();

        saveBtn = new Button("Save");
        saveBtn.addClassName("save");
        saveBtn.setEnabled(false);
        saveBtn.addClickListener(e -> {
            if (!editBtn.isEnabled()) {
                editor.save();
                saveBtn.setEnabled(false);
                cancelBtn.setEnabled(false);
                editBtn.setEnabled(true);
            }
        });
        baseLayout.add(saveBtn);

        cancelBtn = new Button("Cancel");
        cancelBtn.setEnabled(false);
        cancelBtn.addClassName("cancel");
        cancelBtn.addClickListener(e -> {
            if (!editBtn.isEnabled()) {
                editor.cancel();
                saveBtn.setEnabled(false);
                cancelBtn.setEnabled(false);
                editBtn.setEnabled(true);
            }
        });
        baseLayout.add(cancelBtn);

        editBtn = new Button("Edit");
        editBtn.addClassName("edit");
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
        baseLayout.add(editBtn);

        addBtn = new Button("+");
        addBtn.addClickListener(e -> {
            if (!cachedTreeGrid.getSelectedItems().isEmpty()) {
                Node node = cachedTreeGrid.getSelectedItems().iterator().next();
                if (node.isEnable()) {
                    treeViewFacade.addNewToCache(node, "New node");
                    refreshCacheTreeGrid();
                }
            }
        });
        baseLayout.add(addBtn);

        removeBtn = new Button("-");
        removeBtn.addClickListener(e -> {
            if (!cachedTreeGrid.getSelectedItems().isEmpty()) {
                Node node = cachedTreeGrid.getSelectedItems().iterator().next();
                if (node.isEnable()) {
                    treeViewFacade.remove(node);
                    refreshCacheTreeGrid();
                }
            }
        });
        baseLayout.add(removeBtn);

        resetBtn = new Button("Reset");
        resetBtn.addClickListener(e -> {
            treeViewFacade.reset();
            refreshCacheTreeGrid();
            refreshDbTreeGrid();
        });
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

    private void refreshCacheTreeGrid() {
        cachedTreeGrid.setItems(treeViewFacade.getCacheTree(), Node::getChilds);
        cachedTreeGrid.getDataProvider().refreshAll();
        cachedTreeGrid.expandRecursively(treeViewFacade.getCacheTree(), 10);
    }

    private void refreshDbTreeGrid() {
        dbTreeGrid.setItems(treeViewFacade.getDbTree(), Node::getChilds);
        dbTreeGrid.expandRecursively(treeViewFacade.getDbTree(), 10);
        dbTreeGrid.getDataProvider().refreshAll();
    }
}
