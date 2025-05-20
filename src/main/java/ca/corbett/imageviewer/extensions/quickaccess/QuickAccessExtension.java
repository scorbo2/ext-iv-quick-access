package ca.corbett.imageviewer.extensions.quickaccess;

import ca.corbett.imageviewer.AppConfig;
import ca.corbett.imageviewer.QuickMoveManager;
import ca.corbett.imageviewer.extensions.ImageViewerExtension;
import ca.corbett.extensions.AppExtensionInfo;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.ComboProperty;
import ca.corbett.extras.properties.LabelProperty;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An extension to ImageViewer that allows a panel of quick-access buttons to be placed to
 * the left or to the right of an ImagePanel, to provide very quick access to quick move
 * options for a given quick move tree node. The obvious use of this extension is for
 * the ImagePanel on the main window, but it also works well with the full screen extension.
 *
 * @author scorbo2
 */
public class QuickAccessExtension extends ImageViewerExtension {

    private static final String[] validPositions = {"Left", "Right"};
    private static final String extInfoLocation = "/ca/corbett/imageviewer/extensions/quickaccess/extInfo.json";
    private static final String positionPropName = "UI.Quick Access.position";
    private static final String warningPropName = "UI.Quick Access.warningLabel";

    private final List<QuickAccessPanel> quickAccessPanels;
    private final AppExtensionInfo extInfo;
    private QuickMoveManager.TreeNode currentNode;
    private Color currentBgColor;

    public QuickAccessExtension() {
        extInfo = AppExtensionInfo.fromExtensionJar(getClass(), extInfoLocation);
        if (extInfo == null) {
            throw new RuntimeException("QuickAccessExtension: can't parse extInfo.json!");
        }
        quickAccessPanels = new ArrayList<>();
    }

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
    public List<AbstractProperty> getConfigProperties() {
        List<AbstractProperty> list = new ArrayList<>();
        list.add(new LabelProperty(warningPropName, "Note: restart is required to change panel position."));
        list.add(new ComboProperty(positionPropName, "Panel position:", Arrays.asList(validPositions),0, false));
        return list;
    }

    /**
     * I *think* this is the only use of getQuickMoveDialogsActions in any IV extension so far.
     */
    @Override
    public List<AbstractAction> getQuickMoveDialogActions() {
        List<AbstractAction> list = new ArrayList<>();
        list.add(new QuickAccessAction(this));
        return list;
    }

    @Override
    public JComponent getExtraPanelComponent(ExtraPanelPosition position) {
        ComboProperty positionProp = (ComboProperty)AppConfig.getInstance().getPropertiesManager().getProperty(positionPropName);

        ExtraPanelPosition configPosition;
        if (positionProp.getSelectedIndex() == 0) {
            configPosition = ExtraPanelPosition.Left;
        }
        else {
            configPosition = ExtraPanelPosition.Right;
        }
        if (position == configPosition) {
            QuickAccessPanel quickAccessPanel = new QuickAccessPanel();

            // Set current state if we've received any up to this point:
            if (currentNode != null) {
                quickAccessPanel.setNode(currentNode);
            }
            if (currentBgColor != null) {
                quickAccessPanel.setBackground(currentBgColor);
            }
            quickAccessPanels.add(quickAccessPanel);

            // Create a scroll pane for it:
            JScrollPane quickAccessScrollPane = new JScrollPane(quickAccessPanel);
            quickAccessScrollPane.setBorder(null);
            quickAccessScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            quickAccessScrollPane.getVerticalScrollBar().setUnitIncrement(20);
            return quickAccessScrollPane;
        }
        return null;
    }

    @Override
    public void imagePanelBackgroundChanged(Color newColor) {
        currentBgColor = newColor;
        for (QuickAccessPanel quickAccessPanel : quickAccessPanels) {
            quickAccessPanel.setBackground(currentBgColor);
        }
    }

}
