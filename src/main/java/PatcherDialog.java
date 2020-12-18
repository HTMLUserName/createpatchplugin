import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rino Ning
 * @description Patcher dialog
 * @date 2020/9/16 17:29
 */
public class PatcherDialog extends JDialog {

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;

    private JTextField savePath;
    private JButton savePathChooseBtn;
    private JPanel filePanel;
    private JButton patchPathChooseBtn;
    private JTextField patchPath;
    private JCheckBox javaCheckBox;
    private JCheckBox classCheckBox;
    private AnActionEvent event;
    private JBList fieldList;
    private boolean isHaveFile = false;
    private boolean isHaveFolder = false;

    private static PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();

    PatcherDialog(final AnActionEvent event) {
        this.event = event;
        setTitle("Create Patcher Dialog");
        if (!isHaveFile && isHaveFolder) {
            String userDir = System.getProperty("user.home");
            patchPath.setText(userDir + "/" + "Desktop" + "/unnamed.patch");
        }
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // Save route button event
        savePathChooseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userDir = System.getProperty("user.home");
                JFileChooser fileChooser = new JFileChooser(userDir + "/" + "Desktop");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int flag = fileChooser.showOpenDialog(null);
                if (flag == JFileChooser.APPROVE_OPTION) {
                    String absolutePath = fileChooser.getSelectedFile().getAbsolutePath();
                    savePath.setText(absolutePath);
                    propertiesComponent.setValue("exportLocal_path", absolutePath);
                }
            }
        });
        // Patch path button event
        patchPathChooseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userDir = System.getProperty("user.home");
                JFileChooser fileChooser = new JFileChooser(userDir + "/" + "Desktop");
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int flag = fileChooser.showOpenDialog(null);
                if (flag == JFileChooser.APPROVE_OPTION) {
                    String absolutePath = fileChooser.getSelectedFile().getAbsolutePath();
                    patchPath.setText(absolutePath);
                    propertiesComponent.setValue("patchLocal_path", absolutePath);
                }
            }
        });
        this.savePath.setText(StringUtils.isNoneBlank(new CharSequence[]{propertiesComponent.getValue("exportLocal_path")}) ? propertiesComponent.getValue("exportLocal_path") : "");
        this.patchPath.setText(StringUtils.isNoneBlank(new CharSequence[]{propertiesComponent.getValue("patchLocal_path")}) ? propertiesComponent.getValue("patchLocal_path") : "");
    }

    private void onOK() {
        List<String> patchFileList = new ArrayList<>();
        // Condition check
        if (!javaCheckBox.isSelected() && !classCheckBox.isSelected()) {
            Messages.showErrorDialog(this, "Choose at least one of java or class!", "Error");
            return;
        }

        if (StringUtils.isEmpty(savePath.getText())) {
            Messages.showErrorDialog(this, "Please Select Save Path!", "Error");
            return;
        }

        if (isHaveFolder && !isHaveFile) {
            //If the selected file is empty, directly export the files in the entire directory
            //The patch file must be selected to obtain the modified file
            if (StringUtils.isEmpty(patchPath.getText())) {
                Messages.showErrorDialog(this, "If Select Folder,Please Select Patch Path!", "Error");
                return;
            }
            //Get the files modified by the patch file
            try {
                patchFileList = getPatchFileList();
            } catch (Exception e) {
                Messages.showErrorDialog(this, "Get Patch File Error!" + CommonUtil.getStackInfo(e), "Error");
                return;
            }
        }

        // Export directory
        String exportPath = savePath.getText();
        ListModel<VirtualFile> model = this.fieldList.getModel();
        try {
            ModuleManager moduleManager = ModuleManager.getInstance(this.event.getProject());
            Module[] modules = moduleManager.getModules();
            boolean javaCheckBoxSelected = this.javaCheckBox.isSelected();
            boolean classCheckBoxSelected = this.classCheckBox.isSelected();
            Module module = event.getData(LangDataKeys.MODULE);
            if (isHaveFile && !isHaveFolder) {
                //Multiple file export
                //select files
                List<String> selectFiles = new ArrayList<>();
                for (int i = 0; i < model.getSize(); i++) {
                    VirtualFile element = model.getElementAt(i);
                    selectFiles.add(element.getPath());
                }
                //export file
                exportFile(module, selectFiles, javaCheckBoxSelected, classCheckBoxSelected, exportPath);
            } else if (isHaveFolder && !isHaveFile) {
                //Folder export modified files
                //export file
                exportFile(module, patchFileList, javaCheckBoxSelected, classCheckBoxSelected, exportPath);
            }

        } catch (Exception e) {
            Messages.showErrorDialog(this, "Create Patcher Error!" + CommonUtil.getStackInfo(e), "Error");
            FileUtil.delete(new File(exportPath));
            return;
        }
        Messages.showInfoMessage("Create Patcher Success", "Info");
        dispose();
    }

    /**
     * export file common method
     *
     * @param module
     * @param pathList
     * @param javaCheckBoxSelected
     * @param classCheckBoxSelected
     * @param exportPath
     * @throws Exception
     */
    private void exportFile(Module module, List<String> pathList, boolean javaCheckBoxSelected, boolean classCheckBoxSelected, String exportPath) throws Exception {
        try {
            String moduleName = module.getName();
            String srcPath = moduleName + "/src/main/java";
            String resPath = moduleName + "/src/main/resources";
            for (String path : pathList) {
                if (path.indexOf(moduleName) != -1) {
                    int srcPathPos = path.indexOf(srcPath);
                    if (srcPathPos != -1) {
                        String packPath =
                                StringUtils.substring(path, srcPathPos + srcPath.length() + 1);

                        if (javaCheckBoxSelected) {
                            exportJavaSource(path, exportPath + "/" + moduleName + "/src/main/java/" + packPath);
                        }

                        if (classCheckBoxSelected) {
                            exportJavaClass(
                                    "/target/classes",
                                    path,
                                    exportPath + "/" + moduleName + "/WEB-INF/classes" + "/" + packPath);
                        }
                    } else {
                        int resPathPos = path.indexOf(resPath);
                        String packPath =
                                StringUtils.substring(
                                        path, path.indexOf(moduleName));
                        String toResPath = exportPath + "/" + packPath;
                        if (resPathPos != -1) {
                            if (classCheckBoxSelected) {
                                toResPath =
                                        (exportPath + "/" + packPath)
                                                .replace(resPath.replace(moduleName, ""), "/WEB-INF/classes");
                                FileUtil.copyFileOrDir(new File(path), new File(toResPath));
                            }
                            if (javaCheckBoxSelected) {
                                toResPath = exportPath + "/" + packPath;
                                FileUtil.copyFileOrDir(new File(path), new File(toResPath));
                            }
                        } else {
                            FileUtil.copyFileOrDir(new File(path), new File(toResPath));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private void exportJavaClass(String targetPath, String elementPath, String packPath) throws IOException {
        String toTargetPath = StringUtils.replace(elementPath, "/src/main/java", targetPath);
        if (toTargetPath.endsWith(".java")) {
            String classPath = StringUtils.substring(toTargetPath, 0, toTargetPath.lastIndexOf("/") + 1);
            String className = StringUtils.substring(toTargetPath, toTargetPath.lastIndexOf("/") + 1, toTargetPath.lastIndexOf("."));
            String[] list = (new File(classPath)).list((dir, name) -> name.startsWith(className));
            if (list == null) {
                return;
            }
            for (String cname : list) {
                String localPath = StringUtils.substring(packPath, 0, packPath.lastIndexOf("/") + 1) + cname;
                FileUtil.copyFileOrDir(new File(classPath + cname), new File(localPath));
            }
        } else {
            FileUtil.copyFileOrDir(new File(toTargetPath), new File(packPath));
        }
    }

    private void exportJavaSource(String elementPath, String packPath) throws IOException {
        File srcFrom = new File(elementPath);
        File srcTo = new File(packPath);
        FileUtil.copyFileOrDir(srcFrom, srcTo);
    }

    private void onCancel() {
        dispose();
    }

    /****
     * Read the patch configuration file to parse out the modified file and return to the list collection
     * @return
     * @throws Exception
     */
    private List<String> getPatchFileList() throws Exception {
        //Project path
        Module module = event.getData(LangDataKeys.MODULE);
        String[] split = module.getModuleFilePath().split("/");
        String projectUrl = module.getModuleFilePath().replace(split[split.length - 1], "");

        List<String> fileList = new ArrayList<String>();
        FileInputStream f = new FileInputStream(patchPath.getText());
        BufferedReader dr = new BufferedReader(new InputStreamReader(f, "utf-8"));
        String line;
        while ((line = dr.readLine()) != null) {
            if (line.indexOf("Index:") != -1) {
                line = line.replaceAll(" ", "");
                line = line.substring(line.indexOf(":") + 1, line.length());
            } else if (line.indexOf("diff --git") != -1) {
                line = line.substring(line.indexOf("--git") + 6, line.length());
                if (line.contains(" ")) {
                    line = line.split(" ")[0];
                }
                line = line.replaceAll(" ", "");
            } else {
                continue;
            }
            String moduleName = line.split("/")[0];
            if (!projectUrl.contains(moduleName)) {
                continue;
            } else {
                line = projectUrl + line.replaceAll(moduleName + "/", "");
                fileList.add(line);
            }
        }
        dr.close();
        return fileList;
    }

    private void createUIComponents() {
        VirtualFile[] datas = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        for (VirtualFile data : datas) {
            if (isHaveFile && isHaveFolder) {
                break;
            }
            File file = new File(data.getPath());
            if (file.isDirectory()) {
                isHaveFolder = true;
            }
            if (file.isFile()) {
                isHaveFile = true;
            }
        }
        if (isHaveFile && isHaveFolder) {
            Messages.showErrorDialog(this, "Cannot select files and folders at the same time!", "Error");
        } else if (!isHaveFile && isHaveFolder && datas.length > 1) {
            Messages.showErrorDialog(this, "Cannot select multiple modules!", "Error");
        } else {
            fieldList = new JBList(datas);
            fieldList.setEmptyText("No file or folder selected!");
            ToolbarDecorator decorator = ToolbarDecorator.createDecorator(fieldList);
            filePanel = decorator.createPanel();
        }
    }
}
