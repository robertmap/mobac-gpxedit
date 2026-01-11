package mobac.gui.gpxtree;

import mobac.gui.mapview.layer.GpxLayer;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Tracks the edited/dirty state and visibility of a GPX file in the tree.
 */
public class GpxFileState {
    private boolean dirty = false;
    private boolean visible = true;
    private final GpxLayer layer;
    private final DefaultMutableTreeNode node;

    public GpxFileState(GpxLayer layer, DefaultMutableTreeNode node) {
        this.layer = layer;
        this.node = node;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public GpxLayer getLayer() {
        return layer;
    }

    public DefaultMutableTreeNode getNode() {
        return node;
    }
}
