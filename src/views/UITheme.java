package views;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;

/** Consistent, accessible light theme for the desktop client. */
final class UITheme {
    private UITheme() { }

    static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            Color navy = new Color(25, 48, 73);
            Color teal = new Color(0, 121, 140);
            Color canvas = new Color(245, 248, 251);
            UIManager.put("control", canvas);
            UIManager.put("nimbusBase", navy);
            UIManager.put("nimbusBlueGrey", new Color(211, 222, 231));
            UIManager.put("nimbusLightBackground", Color.WHITE);
            UIManager.put("text", navy);
            UIManager.put("controlText", navy);
            UIManager.put("nimbusFocus", teal);
            UIManager.put("defaultFont", new FontUIResource("SansSerif", Font.PLAIN, 14));
            for (String key : new String[]{"Button.font", "Label.font", "TextField.font", "PasswordField.font",
                    "ComboBox.font", "Table.font", "TableHeader.font", "TabbedPane.font"}) {
                UIManager.put(key, new FontUIResource(new Font("SansSerif", Font.PLAIN, 14)));
            }
            UIManager.put("Button.background", new Color(229, 239, 245));
            UIManager.put("Table.selectionBackground", new Color(202, 231, 239));
            UIManager.put("Table.selectionForeground", navy);
            UIManager.put("Table.gridColor", new Color(226, 233, 239));
            UIManager.put("Table.rowHeight", 30);
            UIManager.put("TableHeader.background", new Color(232, 239, 245));
            UIManager.put("TableHeader.foreground", navy);
            UIManager.put("TableHeader.font", new FontUIResource("SansSerif", Font.BOLD, 13));
            UIManager.put("Table.alternateRowColor", new Color(248, 250, 252));
            UIManager.put("ScrollPane.border", javax.swing.BorderFactory.createEmptyBorder());
        } catch (Exception ex) {
            System.err.println("Could not install the Forceps visual theme: " + ex.getMessage());
        }
    }
}
