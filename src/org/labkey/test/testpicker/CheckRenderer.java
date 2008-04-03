package org.labkey.test.testpicker;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.plaf.ColorUIResource;
import javax.swing.tree.TreeCellRenderer;

/**
@author Nobuo Tamemasa
@editedBy Erik Ulberg
*/
public class CheckRenderer extends JPanel implements TreeCellRenderer {
    protected JCheckBox _check;
    protected JLabel _label;

    public CheckRenderer()
    {
        setLayout(null);
        add(_check = new JCheckBox());
        add(_label = new JLabel());
        _check.setBackground(Color.white);
        _label.setForeground(new Color(102, 102, 102));
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected,
             boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
        String stringValue = tree.convertValueToText(value, isSelected, expanded, leaf, row, hasFocus);
        setEnabled(tree.isEnabled());
        _check.setSelected(((CheckNode)value).isSelected());
        _label.setFont(tree.getFont());
        _label.setText(stringValue);
        return this;
    }

    public Dimension getPreferredSize()
    {
        Dimension d_check = _check.getPreferredSize();
        Dimension d_label = _label.getPreferredSize();
        return new Dimension(d_check.width  + d_label.width,
                (d_check.height < d_label.height ? d_label.height : d_check.height));
    }

    public void doLayout()
    {
        Dimension d_check = _check.getPreferredSize();
        Dimension d_label = _label.getPreferredSize();
        int y_check = 0;
        int y_label = 0;
        if (d_check.height < d_label.height)
        {
            y_check = (d_label.height - d_check.height)/2;
        }
        else
        {
            y_label = (d_check.height - d_label.height)/2;
        }
        _check.setLocation(0,y_check);
        _check.setBounds(0,y_check,d_check.width,d_check.height);
        _label.setLocation(d_check.width,y_label);
        _label.setBounds(d_check.width,y_label,d_label.width,d_label.height);
    }

    public void setBackground(Color color)
    {
        if (color instanceof ColorUIResource)
            color = null;
        super.setBackground(color);
    }

    public int getCheckboxWidth()
    {
        if (_check != null)
        {
            return (int) _check.getSize().getWidth();
        }
        return 21;
    }
}
