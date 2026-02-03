package ca.corbett.imageviewer.extensions.quickaccess;

import ca.corbett.imageviewer.ImageOperationHandler;
import ca.corbett.imageviewer.QuickMoveManager;
import ca.corbett.imageviewer.ui.MainWindow;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a way to take one or more QuickMoveTreeNode instances and turn them
 * into buttons arranged vertically in one quick-access panel, which can then be stuck to
 * one side of an ImagePanel. This is meant as a super quick alternative to using
 * the JPopupMenu provided by the ImagePanel.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since ImageViewer 1.1
 */
public final class QuickAccessPanel extends JPanel {

    private final JPanel wrapperPanel;

    /**
     * Creates a new, empty QuickAccessPanel.
     */
    public QuickAccessPanel() {
        this.setBorder(null);

        wrapperPanel = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Discards any existing contents of this panel, and populates it
     * with buttons representing the given node and its children.
     */
    public void setNode(QuickMoveManager.TreeNode node) {
        wrapperPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        processNode(node, gbc);
        gbc.gridy++;
        gbc.weighty = 1; // Force all above contents to the top
        JLabel spacer = new JLabel("");
        wrapperPanel.add(spacer, gbc);

        refreshUI();
    }

    /**
     * Invoked recursively to process a node and its children.
     * The idea is that each node becomes a section header, and its
     * children become buttons below it. If a node has no children,
     * it simply becomes a button itself. This logic is complicated,
     * but the configuration options are pretty powerful when you
     * know how to set it up.
     */
    private void processNode(QuickMoveManager.TreeNode node, GridBagConstraints gbc) {
        List<Component> componentList = new ArrayList<>();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        final JLabel headerLabel = buildHeaderLabel(node.getLabel());
        componentList.add(headerLabel);
        gbc.insets = new Insets(12, 8, 0, 24);
        gbc.ipadx = 0;
        gbc.gridy++;
        wrapperPanel.add(headerLabel, gbc);

        // If there are no children, add this node as a link button:
        if (node.getChildCount() == 0) {
            componentList.add(addLinkButton(node, gbc));
        }

        // But if there are children, go through the list, and
        // for each child that has no children, add it as a link button:
        for (int i = 0; i < node.getChildCount(); i++) {
            QuickMoveManager.TreeNode childNode = (QuickMoveManager.TreeNode)node.getChildAt(i);
            if (childNode.getChildCount() == 0) {
                componentList.add(addLinkButton(childNode, gbc));
            }
        }

        // Add a button that can be used to remove this entire section:
        JButton button = buildRemoveButton(componentList);
        gbc.insets = new Insets(0, 8, 0, 24);
        gbc.ipadx = 20;
        gbc.gridy++;
        wrapperPanel.add(button, gbc);

        // Finally, go through all children again, and for each child that has children,
        // process it recursively using this same method:
        for (int i = 0; i < node.getChildCount(); i++) {
            QuickMoveManager.TreeNode childNode = (QuickMoveManager.TreeNode)node.getChildAt(i);
            if (childNode.getChildCount() > 0) {
                processNode((QuickMoveManager.TreeNode)node.getChildAt(i), gbc);
            }
        }
    }

    /**
     * Builds a section header label.
     */
    private JLabel buildHeaderLabel(String text) {
        JLabel headerLabel = new JLabel(text);
        headerLabel.setOpaque(true);
        headerLabel.setBackground(Color.BLACK); // hard-coded colors, sigh...
        headerLabel.setForeground(Color.WHITE); // this will be fixed when swing-issues #330 is addressed
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setPreferredSize(new Dimension(200, 30));
        headerLabel.setMaximumSize(new Dimension(220, 30));
        return headerLabel;
    }

    /**
     * Builds and returns a button that can be used to remove all the
     * listed components from the panel.
     */
    private JButton buildRemoveButton(List<Component> componentsToRemove) {
        JButton button = new JButton("Remove group");
        componentsToRemove.add(button);
        button.setPreferredSize(new Dimension(200, 23));
        button.setMaximumSize(new Dimension(220, 23));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (Component comp : componentsToRemove) {
                    wrapperPanel.remove(comp);
                }
                refreshUI();
            }
        });
        return button;
    }

    /**
     * Adds a link button for the given node. Clicking the button will invoke
     * an immediate move operation in ImageOperationHandler.
     */
    private Component addLinkButton(QuickMoveManager.TreeNode node, GridBagConstraints gbc) {
        JButton button = new JButton(node.getLabel());
        button.setPreferredSize(new Dimension(200, 23));
        button.setMaximumSize(new Dimension(220, 23));
        button.addActionListener(e -> ImageOperationHandler.moveImage(node.getDirectory()));
        gbc.insets = new Insets(0, 8, 0, 24);
        gbc.ipadx = 20;
        gbc.gridy++;
        wrapperPanel.add(button, gbc);
        return button;
    }

    private void refreshUI() {
        wrapperPanel.invalidate();
        wrapperPanel.revalidate();
        wrapperPanel.repaint();
        MainWindow.getInstance().redrawImagePanel();
    }
}
