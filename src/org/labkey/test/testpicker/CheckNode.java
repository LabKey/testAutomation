package org.labkey.test.testpicker;

import java.util.*;
import javax.swing.tree.*;

public class CheckNode extends DefaultMutableTreeNode {
    protected boolean isSelected;

    public CheckNode(Object userObject)
    {
        this(userObject, true, false);
    }

    public CheckNode(Object userObject, boolean allowsChildren, boolean isSelected)
    {
        super(userObject, allowsChildren);
        this.isSelected = isSelected;
    }

    public void setSelected(boolean isSelected)
    {
        this.isSelected = isSelected;
        if (children != null)
        {
            Enumeration nodes = children.elements();
            while (nodes.hasMoreElements())
            {
                CheckNode node = (CheckNode)nodes.nextElement();
                node.setSelected(isSelected);
            }
        }
    }

    public boolean isSelected()
    {
        return isSelected;
    }

}
