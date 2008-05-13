/*
 * Copyright (c) 2007-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
