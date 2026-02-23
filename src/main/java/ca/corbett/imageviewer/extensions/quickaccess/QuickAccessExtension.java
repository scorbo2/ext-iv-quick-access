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

    /**
     * We can't have a single QuickAccessPanel, because we may legitimately be asked to create more
     * than one. For example, one on the main window, then another one in the fullscreen extension.
     * (Or in some other extension that hasn't been written yet.) So we need to keep track of all
     * the panels we've created, so we can update them all when needed. This unfortunately means
     * that we keep a reference to every panel we've ever created, but we don't get notified when
     * one is no longer needed, so it is what it is.
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
     */
    public void setNode(QuickMoveManager.TreeNode selectedNode) {
        currentNode = selectedNode;
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
    }

    @Override
    public void onActivate() {
        ReloadUIAction.getInstance().registerReloadable(this);
    }

    @Override
    public void onDeactivate() {
        ReloadUIAction.getInstance().unregisterReloadable(this);
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
     */
    @Override
    public JComponent getExtraPanelComponent(ExtraPanelPosition position) {
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
            QuickAccessPanel quickAccessPanel = new QuickAccessPanel();

            // Set current state if we've received any up to this point:
            if (currentNode != null) {
                quickAccessPanel.setNode(currentNode);
            }
            quickAccessPanel.setBackground(AppConfig.getInstance().getDefaultBackground());
            quickAccessPanels.add(quickAccessPanel);

            // Maybe a wonky case, but if we're currently in image set mode, then
            // we want to start with the panel NOT visible. It will be made visible
            // if and when the user switches back to filesystem mode.
            if (MainWindow.getInstance().getBrowseMode() == MainWindow.BrowseMode.IMAGE_SET) {
                quickAccessPanel.getActionPanel()
                                .setVisible(false); // don't use setQuickAccessVisibility here... panel may be empty
            }

            return quickAccessPanel;
        }
        return null;
    }

    /**
     * We only want our quick access panels visible when we're NOT in image set mode.
     */
    @Override
    public void browseModeChanged(MainWindow.BrowseMode newBrowseMode) {
        for (QuickAccessPanel quickAccessPanel : quickAccessPanels) {
            setQuickAccessVisibility(quickAccessPanel, newBrowseMode);
        }
    }

    /**
     * Sets visibility of the given quick access panel according to the given browse mode.
     * Also, we don't show the panel if it has no content.
     */
    private void setQuickAccessVisibility(QuickAccessPanel panel, MainWindow.BrowseMode browseMode) {
        if (panel == null) {
            return;
        }

        // If we're in image set mode, then hide the quick access panel:
        if (browseMode == MainWindow.BrowseMode.IMAGE_SET) {
            panel.getActionPanel().setVisible(false);
            return;
        }

        // Otherwise, only show it if it has content:
        panel.getActionPanel().setVisible(panel.hasContent());
    }

    @Override
    public void reloadUI() {
        for (QuickAccessPanel quickAccessPanel : quickAccessPanels) {
            quickAccessPanel.setBackground(AppConfig.getInstance().getDefaultBackground());
        }
    }
}
