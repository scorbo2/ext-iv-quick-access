package ca.corbett.imageviewer.extensions.quickaccess;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.imageviewer.QuickMoveManager;
import ca.corbett.imageviewer.ui.MainWindow;
import ca.corbett.imageviewer.ui.dialogs.QuickMoveDialog;

import java.awt.event.ActionEvent;

/**
 * Intended to be integrated onto the QuickMoveDialog, this action will take the selected
 * node and populate it into the QuickAccessPanel(s) that have been created by
 * this extension so far.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class QuickAccessAction extends EnhancedAction {

    private static final String NAME = "Quick Access";
    private final QuickAccessExtension owner;

    public QuickAccessAction(QuickAccessExtension owner) {
        super(NAME);
        this.owner = owner;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        QuickMoveManager.TreeNode selectedNode = QuickMoveDialog.getSelectedNode();
        if (selectedNode == null) {
            MainWindow.getInstance().showMessageDialog(NAME, "Nothing selected.");
            return;
        }

        owner.setNode(selectedNode);
    }
}
