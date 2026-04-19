package ca.corbett.imageviewer.extensions.quickaccess;

import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.ComboProperty;
import ca.corbett.imageviewer.AppConfig;
import ca.corbett.imageviewer.QuickMoveManager;
import ca.corbett.imageviewer.extensions.ImageViewerExtension;
import ca.corbett.imageviewer.ui.MainWindow;
import ca.corbett.imageviewer.ui.UIReloadable;
import ca.corbett.imageviewer.ui.actions.ReloadUIAction;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * An extension to ImageViewer that allows a panel of quick-access buttons to be placed to
 * the left or to the right of an ImagePanel. Each button represents a single quick-move destination,
 * and clicking it will immediately move the current image to that destination. This is a very
 * convenient shorthand for commonly-used destinations, and can be customized to show only
 * certain selected destinations, grouped and organized as desired.
 * <p>
 *     This extension is compatible with the FullScreenExtension! Even in fullscreen mode,
 *     you will have access to the quick access panel, if it is defined and enabled.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class QuickAccessExtension extends ImageViewerExtension implements UIReloadable {

    private static final Logger log = Logger.getLogger(QuickAccessExtension.class.getName());

    private static final String extInfoLocation = "/ca/corbett/imageviewer/extensions/quickaccess/extInfo.json";
    private static final String positionPropName = "Quick Move.Quick Access Extension.position";

    private static final String WRONG_MODE_WARNING = "Not available\nin ImageSet mode.";

    /**
     * We can't have a single QuickAccessPanel, because we may legitimately be asked to create more
     * than one. For example, one on the main window, then another one in the fullscreen extension.
     * (Or in some other extension that hasn't been written yet.) So we need to keep track of all
     * the panels we've created, so we can update them all when needed. This unfortunately means
     * that we keep a reference to every panel we've ever created. Worse, we don't get notified when
     * one is no longer needed, so we can't properly clean these up until we are deactivated.
     */
    private final List<QuickAccessPanel> quickAccessPanels;

    private final AppExtensionInfo extInfo;
    private QuickMoveManager.TreeNode currentNode;

    public QuickAccessExtension() {
        extInfo = AppExtensionInfo.fromExtensionJar(getClass(), extInfoLocation);
        if (extInfo == null) {
            throw new RuntimeException("QuickAccessExtension: can't parse extInfo.json!");
        }
        quickAccessPanels = new ArrayList<>();
    }

    /**
     * Discards any previous node, and sets the given node as the current one. This will
     * update all existing QuickAccessPanels created by this extension so far.
     * <p>
     *     Passing null to this method will trigger a UI reload, so that we can
     *     remove our panel (since we no longer have content).
     * </p>
     */
    public void setNode(QuickMoveManager.TreeNode selectedNode) {
        currentNode = selectedNode;

        // Passing null to this method is perfectly valid.
        // It means that we should clear all of our content.
        if (currentNode == null) {
            handlePanelCleanup();

            // Now that we have no content, trigger a UI reload so that any existing panels will be removed:
            ReloadUIAction.getInstance().actionPerformed(null);
            return;
        }

        // If we had no content, but we were just given a non-null node, then
        // we need to trigger a UI reload to pick up the new content:
        if (quickAccessPanels.isEmpty()) {
            ReloadUIAction.getInstance().actionPerformed(null);
            return;
        }

        // If we get here, it means we already had content, and we've just been
        // given new, non-null content. In that case, we can just update our
        // existing panels without needing to trigger a full UI reload:
        for (QuickAccessPanel panel : quickAccessPanels) {
            panel.setNode(currentNode);
        }
    }

    @Override
    public AppExtensionInfo getInfo() {
        return extInfo;
    }

    @Override
    public void loadJarResources() {
        // Nothing to load here.
    }

    @Override
    public void onActivate() {
        ReloadUIAction.getInstance().registerReloadable(this);
    }

    @Override
    public void onDeactivate() {
        ReloadUIAction.getInstance().unregisterReloadable(this);
        handlePanelCleanup();
    }

    @Override
    protected List<AbstractProperty> createConfigProperties() {
        return List.of(
            // We can't use EnumProperty because we don't support all enum options...
            // we only support Left and Right. So, use String-based combo instead.
            new ComboProperty<>(positionPropName,
                                "Panel position:",
                                List.of("Left", "Right"),
                                0,
                                false)
        );
    }

    /**
     * I *think* this is the only use of getQuickMoveDialogsActions in any IV extension so far.
     */
    @Override
    public List<AbstractAction> getQuickMoveDialogActions() {
        return List.of(new QuickAccessAction(this));
    }

    /**
     * Invoked by the parent application when it's building out an image panel and wants to know
     * if any extension has additional panels to go around that image panel.
     * In our case, our quick access panel can be configured to go to the left or to the right
     * of the image panel.
     * <p>
     *     <b>Note:</b> Even if quick access is enabled in user prefs, we may still return null
     *     here if we have no content! Enabling it in application preferences is basically
     *     the equivalent of saying "show it if the user has selected any quick access destinations."
     *     The user can populate and show our panel in the configured position by going to
     *     the "configure quick move destinations" dialog, picking a node, and selecting
     *     the "Quick Access" button.
     * </p>
     */
    @Override
    public JComponent getExtraPanelComponent(ExtraPanelPosition position) {
        // If we have no content, return nothing:
        // User must visit "configure quick move destinations" dialog and select something.
        if (currentNode == null) {
            return null;
        }

        // It shouldn't happen that our property will ever fail to return from AppConfig,
        // but let's guard against it anyway:
        AbstractProperty prop = AppConfig.getInstance().getPropertiesManager().getProperty(positionPropName);
        if (!(prop instanceof ComboProperty<?> positionProp)) {
            log.warning("QuickAccessExtension: configuration property '"
                            + positionPropName
                            + "' is missing or of wrong type.");
            return null;
        }

        // Our combo only supports two options:
        ExtraPanelPosition configPosition = positionProp.getSelectedIndex() == 0 ?
            ExtraPanelPosition.Left :
            ExtraPanelPosition.Right;

        // If this is our configured position, then create and return a QuickAccessPanel:
        if (position == configPosition) {
            QuickAccessPanel quickAccessPanel = new QuickAccessPanel(this);

            // Set current state if we've received any up to this point:
            quickAccessPanel.setNode(currentNode);
            quickAccessPanels.add(quickAccessPanel);

            // This panel is only valid in filesystem mode:
            if (MainWindow.getInstance().getBrowseMode() == MainWindow.BrowseMode.IMAGE_SET) {
                quickAccessPanel.blur(WRONG_MODE_WARNING);
            }

            return quickAccessPanel;
        }
        return null;
    }

    /**
     * We only want our quick access panels accessible when we're NOT in image set mode.
     */
    @Override
    public void browseModeChanged(MainWindow.BrowseMode newBrowseMode) {
        for (QuickAccessPanel quickAccessPanel : quickAccessPanels) {
            setQuickAccessAccessibility(quickAccessPanel, newBrowseMode);
        }
    }

    /**
     * Sets accessibility of the given quick access panel according to the given browse mode.
     * If we're in the wrong mode, we'll use the BlurLayerUI from swing-extras to blur the entire
     * panel, and put a text overlay on top of it explaining why it's unavailable.
     */
    private void setQuickAccessAccessibility(QuickAccessPanel panel, MainWindow.BrowseMode browseMode) {
        if (panel == null) {
            return;
        }

        // If we're in image set mode, then blur the quick access panel:
        if (browseMode == MainWindow.BrowseMode.IMAGE_SET) {
            panel.blur(WRONG_MODE_WARNING);
            return;
        }

        // Otherwise, unblur it (this is safe to invoke even if it wasn't already blurred):
        panel.unblur();
    }

    /**
     * Overridden here so we can respond to changes in the application's look and feel.
     */
    @Override
    public void reloadUI() {
        for (QuickAccessPanel quickAccessPanel : quickAccessPanels) {
            quickAccessPanel.setActionPanelColors();
        }
    }

    /**
     * Invoked internally to properly clean our list of quick access panels.
     */
    private void handlePanelCleanup() {
        for (QuickAccessPanel panel : quickAccessPanels) {
            panel.dispose();
        }
        quickAccessPanels.clear();
    }
}
