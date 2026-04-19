package ca.corbett.imageviewer.extensions.quickaccess;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.ScrollUtil;
import ca.corbett.extras.actionpanel.ActionComponentType;
import ca.corbett.extras.actionpanel.ActionPanel;
import ca.corbett.extras.actionpanel.ColorOptions;
import ca.corbett.extras.actionpanel.ColorTheme;
import ca.corbett.extras.image.animation.BlurLayerUI;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.imageviewer.AppConfig;
import ca.corbett.imageviewer.ImageOperationHandler;
import ca.corbett.imageviewer.QuickMoveManager;
import ca.corbett.imageviewer.ui.MainWindow;

import javax.swing.JLayer;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Provides a way to take one or more QuickMoveManager.TreeNode instances and turn them
 * into buttons arranged vertically in one quick-access panel, which can then be stuck to
 * one side of an ImagePanel. This is meant as a super quick alternative to using
 * the JPopupMenu provided by the ImagePanel.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since ImageViewer 1.1
 */
public final class QuickAccessPanel extends JPanel {

    private final BlurLayerUI blurLayerUI;
    private final ActionPanel actionPanel;
    private final JScrollPane scrollPane;
    private final QuickAccessExtension extension;

    private int panelMinWidth = 200; // customized via AppConfig

    /**
     * Creates a new, empty QuickAccessPanel.
     */
    public QuickAccessPanel(QuickAccessExtension extension) {
        this.extension = extension;
        this.setBorder(null);
        this.setName("Quick Access"); // in case our component gets added to a JTabbedPane.
        blurLayerUI = new BlurLayerUI();
        actionPanel = buildActionPanel();
        scrollPane = ScrollUtil.buildScrollPane(new JLayer<>(actionPanel, blurLayerUI));
        refreshPreferredWidth();
        setActionPanelColors();

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    public ActionPanel getActionPanel() {
        return actionPanel;
    }

    /**
     * Reports whether this panel is currently blurred. If so, no interactivity
     * is possible until unblur() is invoked.
     *
     * @return true if this panel is currently blurred, false otherwise.
     */
    public boolean isBlurred() {
        return blurLayerUI.isBlurred();
    }

    /**
     * Looks up the currently-configured minimum width for the Quick Access panel
     * from application settings and updates the instance variable.
     */
    public void refreshPreferredWidth() {
        AbstractProperty prop = AppConfig.getInstance().getPropertiesManager()
                                         .getProperty(QuickAccessExtension.panelMinWidthPropName);
        if (prop instanceof IntegerProperty intProp) {
            panelMinWidth = intProp.getValue();
        }
        else {
            panelMinWidth = 200; // fallback to default if something goes wrong
        }
    }

    /**
     * Invoke this when the panel is no longer needed.
     */
    public void dispose() {
        actionPanel.dispose();
    }

    /**
     * If the quick access panel is not valid for the current state (for example,
     * if we are in ImageSet mode), then you can blur out the contents of the
     * panel and display an optional message to the user. The panel is not interactive
     * while in blurred mode.
     *
     * @param blurMessage An optional message to overlay on the blurred panel. Can be null or blank for no message.
     */
    public void blur(String blurMessage) {
        blurLayerUI.setOverlayText(blurMessage);
        blurLayerUI.setOverlayTextColor(Color.RED);
        blurLayerUI.setBlurIntensity(BlurLayerUI.BlurIntensity.STRONG);
        blurLayerUI.setBlurOverlayColor(Color.DARK_GRAY);
        blurLayerUI.setBlurred(true);
        invalidate();
        revalidate();
        repaint();
    }

    /**
     * If the panel is currently blurred, this will unblur it and remove whatever text overlay
     * was being shown. The panel will once again be interactive after this method is called.
     * If the panel was not blurred, this method does nothing.
     */
    public void unblur() {
        blurLayerUI.setBlurred(false); // safe to invoke even if not currently blurred
        invalidate();
        revalidate();
        repaint();
    }

    /**
     * Indicates if this panel has any content.
     */
    public boolean hasContent() {
        return actionPanel.getGroupCount() > 0;
    }

    /**
     * Discards any existing contents of this panel, and populates it
     * with buttons representing the given node and its children.
     */
    public void setNode(QuickMoveManager.TreeNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node cannot be null");
        }
        actionPanel.setAutoRebuildEnabled(false); // don't rebuild on every change while building the panel.
        try {
            actionPanel.clear(true);
            processNode(node);
        }
        finally {
            // Setting this back to true will trigger an immediate rebuild:
            actionPanel.setAutoRebuildEnabled(true);
        }
        refreshUI();
    }

    /**
     * Updates the colors of this panel according to the current AppConfig settings. This should be invoked
     * whenever the user changes their color theme in the settings, to ensure that the quick access panel
     * matches the rest of the UI.
     */
    public void setActionPanelColors() {
        setBackground(AppConfig.getInstance().getDefaultBackground());
        scrollPane.getViewport().setBackground(AppConfig.getInstance().getDefaultBackground());
        scrollPane.getViewport().setOpaque(true);

        // Set custom color theme if user has picked one in AppConfig:
        // (otherwise, we'll let the Look and Feel decide all the colors)
        ColorTheme colorTheme = AppConfig.getInstance().getActionPanelTheme();
        if (colorTheme != null) {
            actionPanel.getColorOptions().setFromTheme(colorTheme);

            // Tweak it just a little - I want the action tray background to be the same as
            // the panel background. Otherwise, it looks odd with our action buttons:
            ColorOptions options = actionPanel.getColorOptions();
            options.setActionBackground(AppConfig.getInstance().getDefaultBackground());
            options.setToolBarButtonBackground(options.getActionButtonBackground());
        }
        else {
            actionPanel.getColorOptions().useSystemDefaults();
        }
    }

    /**
     * Invoked recursively to process a node and its children.
     * Leaf nodes (nodes with no children) become individual buttons.
     * For a non-leaf node, any direct children that are leaves become
     * buttons grouped under a section headed by this node's label.
     * Children that are themselves non-leaf nodes are processed
     * recursively, and if all children are non-leaf then no explicit
     * section is created for the current node (that level is effectively
     * flattened in the UI).
     */
    private void processNode(QuickMoveManager.TreeNode node) {
        final String label = node.getLabel();

        // If there are no children, add this node as a group with itself as the only action:
        if (node.getChildCount() == 0) {
            actionPanel.add(label, new MoveAction(label, node.getDirectory()));
        }

        // But if there are children, go through the list, and
        // for each child that has no children, add it as a link button:
        for (int i = 0; i < node.getChildCount(); i++) {
            QuickMoveManager.TreeNode childNode = (QuickMoveManager.TreeNode)node.getChildAt(i);
            if (childNode.getChildCount() == 0) {
                actionPanel.add(label, new MoveAction(childNode.getLabel(), childNode.getDirectory()));
            }
        }

        // Finally, go through all children again, and for each child that DOES have children,
        // process it recursively using this same method:
        for (int i = 0; i < node.getChildCount(); i++) {
            QuickMoveManager.TreeNode childNode = (QuickMoveManager.TreeNode)node.getChildAt(i);
            if (childNode.getChildCount() > 0) {
                processNode(childNode);
            }
        }
    }

    /**
     * Invoked internally to build an ActionPanel and configure it for our use.
     */
    private ActionPanel buildActionPanel() {
        // This is very weird, but calling setPreferredSize() on the ActionPanel itself
        // prevents the scrollbar from ever showing up, even when the parent container is too
        // small to show all contents. The only way I've found to make this work is
        // to override ActionPanel itself and return the preferred size from there.
        // All of this, by the way, is just so we can set our preferred width. Sigh.
        ActionPanel panel = new ActionPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension superPref = super.getPreferredSize();
                // Return the larger of the two (panel's width may be larger if contents are large):
                return new Dimension(Math.max(panelMinWidth, superPref.width), superPref.height);
            }
        };

        // Now we can customize ActionPanel to get it to look the way we want it to.
        final int iconSize = 18;
        panel.setHeaderIconSize(iconSize);
        panel.getToolBarOptions().setIconSize(iconSize);
        panel.setActionComponentType(ActionComponentType.BUTTONS);
        panel.getActionTrayMargins().setAll(0); // no gaps between anything, no margins either
        panel.getToolBarMargins().setAll(0); // This should be redundant in Stretch mode.
        panel.setButtonPadding(4); // Give the buttons just a bit more padding than the default 2 pixels.
        panel.setToolBarEnabled(true);
        panel.getToolBarOptions().setAllowItemAdd(false);
        panel.getToolBarOptions().setAllowGroupRename(false);
        panel.getToolBarOptions().setAllowItemRemoval(false);
        panel.getToolBarOptions().setAllowItemReorder(false);
        panel.getToolBarOptions().setAllowGroupRemoval(true);
        panel.addGroupRemovedListener((a, g) -> removeGroup());
        panel.getExpandCollapseOptions().setAllowHeaderDoubleClick(true); // convenient!

        return panel;
    }

    /**
     * Invoked from our ActionPanel to let us know that a group was removed.
     * Generally, we don't care, as the ActionPanel handles the actual removal for us.
     * But, if all groups have been removed, then we trigger a UI reload to get
     * rid of our now-empty panel. The user can re-enable it by going back
     * to the "configure quick move destinations" dialog and picking some new nodes to show.
     */
    private void removeGroup() {
        if (actionPanel.getGroupCount() == 0) {
            // Note: it would be nice to show a confirmation prompt before removing the last
            //       group, but we don't receive this notification until AFTER the group
            //       has already been removed. Still, we can at least show a little prompt
            //       telling the user what we're doing and how they can undo it.
            JOptionPane.showMessageDialog(MainWindow.getInstance(),
                                          "All quick access destinations have been removed. " +
                                              "The quick access panel will now be hidden.\n\n"
                                              + "To show the quick access panel again, go to the " +
                                              "'Configure quick move destinations' dialog and select " +
                                              "some nodes to show.",
                                          "Quick access panel hidden",
                                          JOptionPane.INFORMATION_MESSAGE);
            extension.setNode(null); // the extension can handle the logic of triggering the reload
        }
    }

    /**
     * Forces a refresh of this panel and the ImagePanel, to ensure that any changes are reflected immediately.
     */
    private void refreshUI() {
        invalidate();
        revalidate();
        repaint();
        MainWindow.getInstance().redrawImagePanel();
    }

    /**
     * A simple action to execute a move operation to the given target directory.
     * This is used by the action buttons in the panel.
     */
    private static class MoveAction extends EnhancedAction {

        private final File targetDir;

        private MoveAction(String label, File targetDir) {
            super(label);
            if (label == null || targetDir == null) {
                throw new IllegalArgumentException("label and targetDir cannot be null");
            }
            if (label.isBlank()) {
                throw new IllegalArgumentException("label cannot be blank");
            }
            this.targetDir = targetDir;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ImageOperationHandler.moveImage(targetDir);
        }
    }
}
