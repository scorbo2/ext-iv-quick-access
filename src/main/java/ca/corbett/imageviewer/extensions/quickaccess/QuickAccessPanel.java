package ca.corbett.imageviewer.extensions.quickaccess;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.extras.LookAndFeelManager;
import ca.corbett.extras.ScrollUtil;
import ca.corbett.extras.actionpanel.ActionComponentType;
import ca.corbett.extras.actionpanel.ActionPanel;
import ca.corbett.extras.actionpanel.ColorOptions;
import ca.corbett.extras.actionpanel.ColorTheme;
import ca.corbett.extras.image.animation.BlurLayerUI;
import ca.corbett.imageviewer.AppConfig;
import ca.corbett.imageviewer.ImageOperationHandler;
import ca.corbett.imageviewer.QuickMoveManager;
import ca.corbett.imageviewer.ui.MainWindow;

import javax.swing.JLayer;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import java.awt.BorderLayout;
import java.awt.Color;
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

    private final ChangeListener lafListener;
    private final BlurLayerUI blurLayerUI;
    private final ActionPanel actionPanel;
    private final JScrollPane scrollPane;
    private final QuickAccessExtension extension;

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
        setActionPanelColors();

        // Set up a Look and Feel change listener so we can apply our workaround:
        lafListener = e -> applyLafWorkaround();
        LookAndFeelManager.addChangeListener(lafListener);

        // And force the workaround here for immediate effect:
        applyLafWorkaround();

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
     * Invoke this when the panel is no longer needed.
     */
    public void dispose() {
        LookAndFeelManager.removeChangeListener(lafListener);
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
     * Works around an unfortunate bug in swing-extras regarding button colors in certain Look and Feels.
     * The bug is that button borders are set to a very unpleasant default color for certain LaFs.
     * This bug was not fixed before swing-extras 2.8 was released :(
     */
    public void applyLafWorkaround() {
        // If a custom ActionPanel theme is in effect, tweak it:
        Color borderColor;
        ColorTheme appliedTheme = AppConfig.getInstance().getActionPanelTheme();
        if (appliedTheme != null) {
            borderColor = switch (appliedTheme) {
                case DEFAULT, MATRIX, DARK, ICE -> Color.DARK_GRAY;
                case LIGHT, GOT_THE_BLUES, SHADES_OF_GRAY -> Color.GRAY;
                case HOT_DOG_STAND -> Color.YELLOW; // because hot dog stands are ugly
            };
        }

        // If we're letting the Look and Feel decide our colors, then we'll
        // decide based on whether the current LaF is dark or light.
        // This isn't perfect, but it's better than the default:
        else {
            borderColor = LookAndFeelManager.isDark() ? Color.DARK_GRAY : Color.GRAY;
        }

        // Now overwrite the UIManager property in question:
        // (This will have effects beyond our own action panel, but it is what it is
        //  until this bug is properly addressed in swing-extras)
        UIManager.put("Button.default.startBorderColor", new ColorUIResource(borderColor));

        // Rebuild the action panel's UI without reloading contents from disk:
        actionPanel.setAutoRebuildEnabled(false);
        actionPanel.setAutoRebuildEnabled(true); // forces an immediate rebuild

        // This is what I want to do instead of the above,
        // but it won't work because of the way ActionPanel builds its buttons:
        //SwingUtilities.updateComponentTreeUI(actionPanel);

        invalidate();
        revalidate();
        repaint();
    }

    /**
     * Invoked recursively to process a node and its children.
     * The idea is that each node becomes a section header, and its
     * children become buttons below it. If a node has no children,
     * it simply becomes a button itself. This logic is complicated,
     * but the configuration options are pretty powerful when you
     * know how to set it up.
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
        final int iconSize = 18;
        ActionPanel panel = new ActionPanel();
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
