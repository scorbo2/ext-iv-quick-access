package ca.corbett.imageviewer.extensions.quickaccess;

import ca.corbett.imageviewer.ImageOperationHandler;
import ca.corbett.imageviewer.QuickMoveManager;
import ca.corbett.imageviewer.ui.MainWindow;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
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
 * @author scorbo2
 * @since ImageViewer 1.1
 */
public final class QuickAccessPanel extends JPanel {

    public QuickAccessPanel() {
        this.setBorder(null);
        setLayout(new GridBagLayout());
    }

    public void setNode(QuickMoveManager.TreeNode node) {
        removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        processNode(node, gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        JLabel spacer = new JLabel("");
        add(spacer, gbc);

        refreshUI();
    }

    private void processNode(QuickMoveManager.TreeNode node, GridBagConstraints gbc) {
        List<Component> componentList = new ArrayList<>();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        final JLabel headerLabel = new JLabel(node.getLabel());
        componentList = new ArrayList<>();
        componentList.add(headerLabel);
        headerLabel.setOpaque(true);
        headerLabel.setBackground(Color.BLACK);
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setPreferredSize(new Dimension(200, 30));
        headerLabel.setMaximumSize(new Dimension(220, 30));
        gbc.insets = new Insets(12, 8, 0, 24);
        gbc.ipadx = 0;
        gbc.gridy++;
        add(headerLabel, gbc);

        if (node.getChildCount() == 0) {
            componentList.add(addLinkButton(node, gbc));
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            QuickMoveManager.TreeNode childNode = (QuickMoveManager.TreeNode)node.getChildAt(i);
            if (childNode.getChildCount() == 0) {
                componentList.add(addLinkButton(childNode, gbc));
            }
        }

        JButton button = new JButton("Remove group");
        componentList.add(button);
        button.setPreferredSize(new Dimension(200, 23));
        button.setMaximumSize(new Dimension(220, 23));
        final List<Component> theList = componentList;
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (Component c : theList) {
                    remove(c);
                }
                refreshUI();
            }
        });
        gbc.insets = new Insets(0, 8, 0, 24);
        gbc.ipadx = 20;
        gbc.gridy++;
        add(button, gbc);

        for (int i = 0; i < node.getChildCount(); i++) {
            QuickMoveManager.TreeNode childNode = (QuickMoveManager.TreeNode)node.getChildAt(i);
            if (childNode.getChildCount() > 0) {
                processNode((QuickMoveManager.TreeNode)node.getChildAt(i), gbc);
            }
        }
    }

    private Component addLinkButton(QuickMoveManager.TreeNode node, GridBagConstraints gbc) {
        JButton button = new JButton(node.getLabel());
        button.setPreferredSize(new Dimension(200, 23));
        button.setMaximumSize(new Dimension(220, 23));
        button.addActionListener(e -> ImageOperationHandler.moveImage(node.getDirectory()));
        gbc.insets = new Insets(0, 8, 0, 24);
        gbc.ipadx = 20;
        gbc.gridy++;
        add(button, gbc);
        return button;
    }

    private void refreshUI() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                invalidate();
                revalidate();
                repaint();
                MainWindow.getInstance().redrawImagePanel();
            }
        });
    }
}
