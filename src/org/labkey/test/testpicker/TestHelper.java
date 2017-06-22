/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.labkey.api.util.FileUtil;
import org.labkey.test.SuiteBuilder;
import org.labkey.test.TestConfig;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestSet;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Continue;
import org.labkey.test.categories.Test;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class TestHelper
{
    public static final String DEFAULT_PORT = WebTestHelper.getWebPort().toString();
    public static final String DEFAULT_CONTEXT_PATH = WebTestHelper.getContextPath();
    public static final String DEFAULT_SERVER = WebTestHelper.getTargetServer();
    public static final String DEFAULT_ROOT = TestFileUtils.getLabKeyRoot();

    private static Thread awtThread = null;
    private static String _saveFileName = "savedConfigs.idx";
    private static String _prevTestConfig = "previous_config";

    private File _saveFile;
    private List<TestConfig> _savedConfigs;
    private JFrame _window;
    private CheckNode _treeRoot;
    private CheckRenderer _renderer;
    private String _rootName = "Test Suites";
    private JCheckBox _clean;
    private JCheckBox _linkCheck;
    private JCheckBox _memCheck;
    private JCheckBox _loop ;
    private JCheckBox _haltOnError;
    private DependentCheckBox _cleanOnly ;
    private JTextField _port;
    private JTextField _contextPath;
    private JTextField _server;
    private JTextField _root;
    private JRadioButton _bestBrowserButton;
    private JRadioButton _chromeButton;
    private JRadioButton _firefoxButton;
    private JRadioButton _ieButton;
    private JComboBox _configDropDown;
    private JTree _testTree;

    private ResultPair _result;

    /** Displays test picker UI and blocks until closed. */
    public static ResultPair run()
    {
        TestHelper ui = new TestHelper();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    awtThread = Thread.currentThread();
                }
            });
            awtThread.join();
        }
        catch (java.lang.reflect.InvocationTargetException ite) {
            System.err.println("invocation exception: " + ite.getMessage());
        }
        catch (InterruptedException ie) {
            System.err.println("interrupted: " + ie.getMessage());
        }

        return ui._result;
    }

    public TestHelper()
    {
        startTestHelper();
        loadTestConfig(_prevTestConfig);
    }

    private void setResult(TestSet set, List<String> testNames)
    {
        _result = new ResultPair(set, testNames);
    }

    private void startTestHelper()
    {
        _saveFile = findSaveFile();
        _savedConfigs = getSavedConfigs();
        _window = new JFrame();
        _renderer = new CheckRenderer();

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.white);

        panel.add(createHeader(), BorderLayout.NORTH);
        Box body = Box.createVerticalBox();
        body.add(createTestOptions());
        body.add(createBody());
        panel.add(body, BorderLayout.CENTER);

        _window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        _window.add(panel);
        _window.setTitle("LabKey Automated Test Suite");
        _window.pack();
        _window.setVisible(true);
    }

    private Component createHeader()
    {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.white);
        header.setBorder(new MatteBorder(3, 10, 3, 10, Color.white));

        Box headerTitle = Box.createHorizontalBox();
        headerTitle.setBackground(Color.white);

        JLabel headerImage = new JLabel(new ImageIcon("../internal/webapp/_images/defaultlogosmall.gif"));
        headerTitle.add(headerImage);

        JLabel title = new JLabel(" LabKey Automated Test Suite");
        title.setFont(new Font("Arial", Font.PLAIN, 24));
        title.setForeground(new Color(102, 102, 102));
        headerTitle.add(title);

        header.add(headerTitle, BorderLayout.WEST);
        return header;
    }

    private Component createTestOptions()
    {
        Box options = Box.createVerticalBox();
        options.setBorder(new MatteBorder(10, 10, 5, 10, new Color(176, 196, 222)));

        JPanel debugHeader = new JPanel();
        JLabel debugText = new JLabel("To debug this test, attach the debugger before clicking 'Run'");
        debugHeader.add(debugText);
        options.add(debugHeader);

        GridBagConstraints gbcChecks = new GridBagConstraints();
        gbcChecks.weightx = 1.0;

        JPanel optionsChecks = new JPanel(new GridBagLayout());
        _clean = new JCheckBox("Clean");
        _clean.setBackground(Color.white);
        _clean.setSelected(true);
        _linkCheck = new JCheckBox("Link Check");
        _linkCheck.setBackground(Color.white);
        _memCheck = new JCheckBox("Mem Check");
        _memCheck.setBackground(Color.white);
        _loop = new JCheckBox("Loop");
        _loop.setBackground(Color.white);
        _haltOnError = new JCheckBox("Halt on Error");
        _haltOnError.setBackground(Color.white);
        _cleanOnly = new DependentCheckBox("Clean Only", _clean);
        _cleanOnly.setBackground(Color.white);
        optionsChecks.add(_clean, gbcChecks);
        optionsChecks.add(_linkCheck, gbcChecks);
        optionsChecks.add(_memCheck, gbcChecks);
        optionsChecks.add(_loop, gbcChecks);
//        optionsChecks.add(_haltOnError, gbcChecks);
        optionsChecks.add(_cleanOnly, gbcChecks);
        optionsChecks.setBackground(Color.white);
        options.add(optionsChecks);

        GridBagConstraints gbcShort = new GridBagConstraints();
        gbcShort.anchor = GridBagConstraints.WEST;
        gbcShort.insets = new Insets(0, 0, 0, 5);
        GridBagConstraints gbcLong = new GridBagConstraints();
        gbcLong.anchor = GridBagConstraints.WEST;
        gbcLong.gridwidth = GridBagConstraints.REMAINDER;
        gbcLong.weightx = 1.0;
        gbcLong.fill = GridBagConstraints.HORIZONTAL;

        JPanel optionsText = new JPanel(new GridBagLayout());
        optionsText.setBorder(new MatteBorder(5, 5, 5, 5, Color.white));
        JLabel labkeyContextPathName = new JLabel("Context Path:");
        _contextPath = new JTextField(DEFAULT_CONTEXT_PATH, 6);
        optionsText.add(labkeyContextPathName, gbcShort);
        optionsText.add(_contextPath, gbcShort);
        JLabel labkeyServerName = new JLabel("Target Server:");
        _server = new JTextField(DEFAULT_SERVER);
        optionsText.add(labkeyServerName, gbcShort);
        optionsText.add(_server, gbcLong);
        optionsText.setBackground(Color.white);
        options.add(optionsText);

        optionsText.setBorder(new MatteBorder(5, 5, 5, 5, Color.white));
        JLabel portName = new JLabel("Port:");
        _port = new JTextField(DEFAULT_PORT, 6);
        optionsText.add(portName, gbcShort);
        optionsText.add(_port, gbcShort);
        JLabel labkeyRootName = new JLabel("LabKey Root:");

        File rootFile = new File(DEFAULT_ROOT);
        String rootPath = FileUtil.getAbsoluteCaseSensitiveFile(rootFile).getAbsolutePath();
        _root = new JTextField(rootPath);
        optionsText.add(labkeyRootName, gbcShort);
        optionsText.add(_root, gbcLong);
        optionsText.setBackground(Color.white);

        JLabel browserName = new JLabel("Browser:");
        optionsText.add(browserName);
        ButtonGroup browser = new ButtonGroup();

        _bestBrowserButton = new JRadioButton("Fastest", true);
        _bestBrowserButton.setBackground(Color.white);
        _bestBrowserButton.setToolTipText("Individual tests will run on the fastest browser they are able\nUsually Firefox, sometims Chrome");
        browser.add(_bestBrowserButton);
        optionsText.add(_bestBrowserButton);

        _chromeButton = new JRadioButton("Chrome", true);
        _chromeButton.setBackground(Color.white);
        browser.add(_chromeButton);
        optionsText.add(_chromeButton);

        _firefoxButton = new JRadioButton("Firefox", false);
        _firefoxButton.setBackground(Color.white);
        browser.add(_firefoxButton);
        optionsText.add(_firefoxButton);

        _ieButton = new JRadioButton("Internet Explorer", false);
        _ieButton.setBackground(Color.white);
        browser.add(_ieButton);
        optionsText.add(_ieButton);

        options.add(optionsText);

        return options;
    }

    private void createMainTree()
    {
        _treeRoot = new CheckNode(_rootName);
        _testTree = new JTree(_treeRoot);

        SuiteBuilder suiteBuilder = SuiteBuilder.getInstance();

        List<String> suites = new ArrayList<>(suiteBuilder.getSuites());
        Collections.sort(suites);

        for (String suite : suites)
        {
            TestSet testSet = suiteBuilder.getTestSet(suite);
            if (!testSet.getTestNames().isEmpty())
            {
                CheckNode suiteNode = new CheckNode(testSet.getSuite());
                List<String> testNames = testSet.getTestNames();
                Collections.sort(testNames);
                for (String test : testNames)
                {
                    CheckNode testNode = new CheckNode(test, false, false);
                    suiteNode.add(testNode);
                }
                _treeRoot.add(suiteNode);
            }
        }

        _testTree.setCellRenderer(_renderer);
        _testTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        _testTree.addMouseListener(new NodeSelectionListener(_testTree));
        _testTree.expandRow(0);
    }

    private Component createConfigLoader()
    {
        JPanel loadPanel = new JPanel(new BorderLayout());
        loadPanel.setBackground(Color.white);

        JLabel saved = new JLabel("Saved Configurations:");
        saved.setBorder(new MatteBorder(0, 0, 0, 10, Color.white));
        loadPanel.add(saved, BorderLayout.WEST);

        String[] loadConfigs = new String[_savedConfigs.size() + 1];
        loadConfigs[0] = "";
        for (int i = 0; i < _savedConfigs.size(); i++)
        {
            loadConfigs[i + 1] = _savedConfigs.get(i).getName();
        }
        _configDropDown = new JComboBox(loadConfigs);
        _configDropDown.setBorder(new MatteBorder(0, 0, 0, 10, Color.white));
        _configDropDown.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                loadTestConfig((String) _configDropDown.getSelectedItem());
            }
        });
        loadPanel.add(_configDropDown, BorderLayout.CENTER);

        return loadPanel;
    }

    private Component createButtonBar()
    {
        // Create top row with config loader and buttons
        JPanel buttonBarTop = new JPanel(new BorderLayout());
        buttonBarTop.add(createConfigLoader(), BorderLayout.CENTER);
        buttonBarTop.setBorder(new MatteBorder(5, 10, 10, 10, Color.white));

        JPanel buttonBarTopButtons = new JPanel();
        buttonBarTopButtons.setBackground(Color.white);
        buttonBarTopButtons.setLayout(new BoxLayout(buttonBarTopButtons, BoxLayout.LINE_AXIS));

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                String name = (String) _configDropDown.getSelectedItem();
                if (!name.equals(""))
                {
                    deleteTestConfig(name);
                    _configDropDown.removeItem(name);
                }
            }
        });

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                String name = JOptionPane.showInputDialog(_window, "Enter a name:",
                                    "Save Run", JOptionPane.PLAIN_MESSAGE);
                if (validate(name))
                {
                    _configDropDown.removeItem(name);
                    saveTestConfig(name);
                    _configDropDown.addItem(name);
                    _configDropDown.setSelectedItem(name);
                }
            }
        });

        buttonBarTopButtons.add(deleteButton);
        buttonBarTopButtons.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonBarTopButtons.add(saveButton);

        buttonBarTop.add(buttonBarTopButtons, BorderLayout.EAST);

        // Create bottom row of buttons
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel buttonBarBottom = new JPanel(new GridBagLayout());
        buttonBarBottom.setBackground(Color.white);
        buttonBarBottom.setBorder(new MatteBorder(5, 10, 10, 10, Color.white));

        JButton runButton = new JButton("Run");
        runButton.addActionListener(new RunActionListener(_treeRoot));

        JButton continueButton = new JButton("Continue");
        continueButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                setResult(SuiteBuilder.getInstance().getTestSet(Continue.class.getSimpleName()), new ArrayList<String>());
                _window.dispose();
            }
        });

        buttonBarBottom.add(runButton, gbc);
        buttonBarBottom.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonBarBottom.add(continueButton, gbc);
        buttonBarBottom.add(Box.createRigidArea(new Dimension(10, 0)));

        JPanel buttonBar = new JPanel(new BorderLayout());

        buttonBar.add(buttonBarTop, BorderLayout.NORTH);
        buttonBar.add(buttonBarBottom, BorderLayout.SOUTH);

        return buttonBar;
    }

    private Component createBody()
    {
        createMainTree();
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(new MatteBorder(10, 10, 10, 10, new Color(176, 196, 222)));

        JPanel testHeader = new JPanel();

        JButton userPropsButton = new JButton("User Props");
        userPropsButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                loadUserProps();
            }
        });

        testHeader.add(userPropsButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                reloadPage(new TestConfig());
            }
        });

        testHeader.add(clearButton);

        JLabel testText = new JLabel("Select the tests you would like to run");
        testHeader.add(testText);
        body.add(testHeader, BorderLayout.NORTH);

        JScrollPane bodyPane = new JScrollPane(_testTree);
        bodyPane.setPreferredSize(new Dimension(400, 400));
        body.add(bodyPane, BorderLayout.CENTER);

        body.add(createButtonBar(), BorderLayout.SOUTH);

        return body;
    }

    /**
     * @return a list of only the selected tests in the tree
     */
    public List<String> getSelectedTests(CheckNode root)
    {
        List<String> selectedNodes = new ArrayList<>();
        selectedNodes.addAll(getChecked(root, true));
        return selectedNodes;
    }

    /**
     *  @return a list of paths to all the nodes in the tree that are checked in the form "root/node/.../node"
      */
    public List<String> getCheckedNodes(CheckNode root)
    {
        List<String> selectedNodes = new ArrayList<>();
        selectedNodes.addAll(getChecked(root, false));
        return selectedNodes;
    }

    /**
     *
     * @param testsOnly: see return
     * @return If testsOnly, returns a list of the names of the leaves that are checked, else, returns a list
     * of the paths of all checked nodes in the form "node/node/.../node"
     */
    private List<String> getChecked(CheckNode node, boolean testsOnly)
    {
        List<String> selected = new ArrayList<>();
        if (node.isSelected())
        {
            if (testsOnly)
            {
                 if (node.isLeaf())
                    selected.add(node.toString());
            }
            else
            {
                TreeNode[] path = node.getPath();
                StringBuilder pathString = new StringBuilder();
                for (int i = 0; i < path.length; i++)
                {
                    pathString.append(path[i].toString());
                    if (i < path.length - 1)
                        pathString.append("/");
                }
                selected.add(pathString.toString());
            }
        }

        Enumeration<TreeNode> childNodes = node.children();
        while (childNodes.hasMoreElements())
        {
            selected.addAll(getChecked((CheckNode)childNodes.nextElement(), testsOnly));
        }
        return selected;
    }

    private boolean validate(String name)
    {
        if (name == null || name.equals(""))
        {
            JOptionPane.showMessageDialog(_window, "You did not enter a name. The test configuration was not saved.");
            return false;
        }
        return true;
    }

    private void saveTestConfig(String name)
    {
        TestConfig config = new TestConfig(name, _clean.isSelected(), _linkCheck.isSelected(), _memCheck.isSelected(), _loop.isSelected(), _cleanOnly.isSelected(), _bestBrowserButton.isSelected(), _chromeButton.isSelected(), _firefoxButton.isSelected(),
                _ieButton.isSelected(), _port.getText().trim(), _contextPath.getText().trim(), _server.getText().trim(), _root.getText().trim(), getCheckedNodes(_treeRoot), _haltOnError.isSelected());

        _savedConfigs = deleteTestConfigFromList(name, _savedConfigs);
        _savedConfigs.add(config);
        writeConfigs(_savedConfigs);
    }

    /**
     * @return returns the list with the item removed
     */
    private List<TestConfig> deleteTestConfigFromList(String name, List<TestConfig> list)
    {
        if (list.size() > 0)
        {
            for (TestConfig config : list)
            {
                if (config.getName().equals(name))
                {
                    list.remove(config);
                    return list;
                }
            }
        }
        return list;
    }

    private void deleteTestConfig(String name)
    {
        _savedConfigs = deleteTestConfigFromList(name, _savedConfigs);
        writeConfigs(_savedConfigs);
    }

    /**
     * Writes the serialized form of the list configs to _saveFile
     */
    private void writeConfigs(List<TestConfig> configsToSave)
    {
        try
        {
            OutputStream out = new FileOutputStream(_saveFile);
            ObjectOutputStream objectOut = new ObjectOutputStream(new BufferedOutputStream(out));
            objectOut.writeObject(configsToSave);
            objectOut.flush();
            objectOut.close();
        }
        catch (IOException e)
        {
            e.printStackTrace(System.out);
        }
    }

    /**
     * @param name, if name is provided, loads the default page, else loads the page with the settings
     */
    private void loadTestConfig(String name)
    {
        if (name == null || name.equals(""))
        {
            reloadPage(new TestConfig());
        }
        else
        {
            for (TestConfig config : _savedConfigs)
            {
                if (config.getName() != null && config.getName().equals(name))
                {
                    reloadPage(config);
                    return;
                }
            }
        }
    }

    /**
     * @return the list of configs in _saveFile, or returns an empty list if it doesn't find the file
     */
    private List<TestConfig> getSavedConfigs()
    {
        List<TestConfig> savedConfigs = new ArrayList<>();
        try
        {
            if (_saveFile.isFile())
            {
                InputStream in = new FileInputStream(_saveFile);
                ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(in));
                savedConfigs = (List<TestConfig>) objectIn.readObject();
                objectIn.close();
            }
        }
        catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace(System.out);
        }
        return savedConfigs;
    }

    /**
     * Updates which config is being displayed
     */
    private void reloadPage(TestConfig config)
    {
        _clean.setSelected(config.isClean());
        _clean.setEnabled(!config.isCleanOnly());
        _linkCheck.setSelected(config.isLinkCheck());
        _memCheck.setSelected(config.isMemCheck());
        _haltOnError.setSelected(config.isHaltOnError());
        _loop.setSelected(config.isLoop());
        _cleanOnly.setSelected(config.isCleanOnly());
        _bestBrowserButton.setSelected(config.isBestBrowser());
        _chromeButton.setSelected(config.isChrome());
        _firefoxButton.setSelected(config.isFirefox());
        _ieButton.setSelected(config.isIe());
        _port.setText(config.getPort());
        _contextPath.setText(config.getContextPath());
        _server.setText(config.getServer());
        //_root.setText(config.getRoot());  jgarms: do not persist the root across branches
        checkNodes(config.getConfigCheckedNodes());
    }

    /**
     * Loads the applicable user props to the UI, if they are set
     */
    private void loadUserProps()
    {
        _clean.setSelected(Boolean.valueOf(System.getProperty("clean")));
        _clean.setEnabled(!Boolean.valueOf(System.getProperty("cleanOnly")));
        _cleanOnly.setSelected(Boolean.valueOf(System.getProperty("cleanOnly")));
        _loop.setSelected(Boolean.valueOf(System.getProperty("loop")));
        _linkCheck.setSelected(Boolean.valueOf(System.getProperty("linkCheck")));
        _memCheck.setSelected(Boolean.valueOf(System.getProperty("memCheck")));
        _haltOnError.setSelected(Boolean.valueOf(System.getProperty("haltOnError")));
        String port = System.getProperty("labkey.port");
        if (port != null && !port.equals(""))
            _port.setText(port);
        String contextPath = System.getProperty("labkey.contextpath");
        if (contextPath != null && !contextPath.equals(""))
            _contextPath.setText(contextPath);
        String server = System.getProperty("labkey.server");
        if (server != null && !server.equals(""))
            _server.setText(server);
    }

    /**
     * @param paths, checks all the nodes in the list paths using their paths and unchecks any that are not in the list
     * Uses "/" as a delimiter between path node names
     */
    private void checkNodes(List<String> paths)
    {
        check(_treeRoot, paths);
        _testTree.revalidate();
        _testTree.repaint();
    }

    /**
     * Recursive helper method for checkNodes, which determines if it should check or uncheck a node
     */
    private void check(CheckNode node, List<String> paths)
    {
        TreeNode[] path = node.getPath();
        StringBuilder pathString = new StringBuilder();
        for (int i = 0; i < path.length; i++)
        {
            pathString.append(path[i].toString());
            if (i < path.length - 1)
                pathString.append("/");
        }
        if (paths.contains(pathString.toString()))
        {
            node.setSelected(true);
            expandNode(path);
        }
        else
            node.setSelected(false);

        Enumeration<TreeNode> childNodes = node.children();
        while (childNodes.hasMoreElements())
        {
            check((CheckNode)childNodes.nextElement(), paths);
        }
    }

    /**
     * Expands the path of a node.
     * @param path
     */
    private void expandNode(TreeNode[] path)
    {
        TreeNode[] parentPath = new TreeNode[path.length-1];
        System.arraycopy(path, 0, parentPath, 0, path.length - 1);
        if (parentPath.length > 0)
        {
            _testTree.expandPath(new TreePath(parentPath));
        }
    }

    private static File verifyDir(String dirName)
    {
        if (dirName != null)
        {
            File dir = new File(dirName);
            if (dir.exists())
                return dir;
        }
        return null;
    }

    private static File findSaveFile()
    {
        File dir = verifyDir(System.getProperty("user.home"));
        if (dir == null)
        {
            System.out.println("User home couldn't be found.  Using working directory instead.");
            dir = verifyDir(System.getProperty("user.dir"));
        }
        if (dir == null)
            throw new IllegalStateException("System property for user.home or user.dir must be set to enable configuration storage.");

        return new File(dir, _saveFileName);
    }

    /**
     * Determines whether the user clicked on the checkbox or the name of a CheckNode. Expands if name was
     * clicked or clicks checkbox if checkbox was clicked
     */
    private class NodeSelectionListener extends MouseAdapter
    {
        JTree _tree;

        NodeSelectionListener(JTree tree)
        {
          this._tree = tree;
        }

        public void mouseClicked(MouseEvent e)
        {
            int x = e.getX();
            int y = e.getY();
            int row = _tree.getRowForLocation(x, y);
            TreePath path = _tree.getPathForRow(row);
            if (path != null)
            {
                CheckNode node = (CheckNode)path.getLastPathComponent();
                Rectangle bounds = _tree.getRowBounds(row);
                if (x - bounds.getMinX() <= _renderer.getCheckboxWidth())
                    node.setSelected(!node.isSelected());
                else if (!node.isLeaf())
                {
                    boolean isExpanded = _tree.isExpanded(path);
                    if (isExpanded)
                        _tree.collapsePath(path);
                    else
                        _tree.expandPath(path);
                }
                ((DefaultTreeModel) _tree.getModel()).nodeChanged(node);
                _tree.revalidate();
                _tree.repaint();
            }
        }
      }

    /**
     * Collects settings and runs Selenium test
     */
    class RunActionListener implements ActionListener
    {
        CheckNode _treeRoot;

        RunActionListener(final CheckNode root)
        {
            _treeRoot = root;
        }

        public void actionPerformed(ActionEvent e)
        {
            List<String> selectedTests = getSelectedTests(_treeRoot);
            System.setProperty("clean", String.valueOf(_clean.isSelected()));
            System.setProperty("linkCheck", String.valueOf(_linkCheck.isSelected()));
            System.setProperty("memCheck", String.valueOf(_memCheck.isSelected()));
            System.setProperty("loop", String.valueOf(_loop.isSelected()));
            System.setProperty("cleanOnly", String.valueOf(_cleanOnly.isSelected()));
            System.setProperty("haltOnError", String.valueOf(_haltOnError.isSelected()));
            System.setProperty("labkey.port", _port.getText().trim());
            System.setProperty("labkey.contextpath", _contextPath.getText().trim());
            System.setProperty("labkey.server", _server.getText().trim());
            System.setProperty("labkey.root", _root.getText().trim());
            if (_bestBrowserButton.isSelected())
            {
                System.setProperty("selenium.browser", "*best");
            }
            else if (_chromeButton.isSelected())
            {
                System.setProperty("selenium.browser", "*googlechrome");
            }
            if (_firefoxButton.isSelected())
            {
                System.setProperty("selenium.browser", "*firefox");
            }
            else if (_ieButton.isSelected())
            {
                System.setProperty("selenium.browser", "*ie");
            }
            saveTestConfig(_prevTestConfig);

            if (selectedTests.size() != 0 )
            {
                setResult(SuiteBuilder.getInstance().getTestSet(Test.class.getSimpleName()), selectedTests);
            }
            _window.dispose();
        }
    }

    /**
     * A checkbox that has another checkbox that must be selected if this checkbox is. Disables and checks
     * dependent checkbox if selected.
     */
    private class DependentCheckBox extends JCheckBox
    {
        JCheckBox _dependentCheckBox;

        public DependentCheckBox(String name, JCheckBox checkBox)
        {
            super(name);
            _dependentCheckBox = checkBox;
            addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e)
                {
                    if (isSelected())
                    {
                        _dependentCheckBox.setSelected(true);
                        _dependentCheckBox.setEnabled(false);
                    }
                    if (!isSelected())
                    {
                        _dependentCheckBox.setEnabled(true);
                    }
                }
            });
        }
    }

    public static class ResultPair
    {
        public TestSet set;
        public List<String> testNames;

        public ResultPair(TestSet set, List<String> testNames)
        {
            this.set = set;
            this.testNames = testNames;
        }
    }

    public static void main(String[] args)
    {
        ResultPair pair = TestHelper.run();
        if (pair == null)
        {
            System.out.println("pair is null");
        }
        else
        {
            System.out.println("pair.set = " + pair.set);
            for (String test : pair.testNames)
                System.out.println("pair.test = " + test);
        }
    }
}
