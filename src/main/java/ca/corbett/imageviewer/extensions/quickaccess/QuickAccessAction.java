package ca.corbett.imageviewer.extensions.quickaccess;

import ca.corbett.imageviewer.QuickMoveManager;
import ca.corbett.imageviewer.ui.MainWindow;
import ca.corbett.imageviewer.ui.dialogs.QuickMoveDialog;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * Intended to be integrated onto the QuickMoveDialog, this action will take the selected
 * node and populate it into the QuickAccessPanel(s) that have been created by
 * this extension so far.
 *
 * @author scorbo2
 */
public class QuickAccessAction extends AbstractAction {

    private final QuickAccessExtension owner;

    public QuickAccessAction(QuickAccessExtension owner) {
        super("Quick Access");
        this.owner = owner;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        QuickMoveManager.TreeNode selectedNode = QuickMoveDialog.getSelectedNode();
        if (selectedNode == null) {
            MainWindow.getInstance().showMessageDialog("Quick Access", "Nothing selected.");
            return;
        }

        owner.setNode(selectedNode);
    }

}
