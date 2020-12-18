import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
/**
 * @author Huilong.Ning
 * @description Create patch action
 * @date 2020/9/16 17:29
 */
public class CreatePatcherAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        PatcherDialog dialog = new PatcherDialog(e);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        dialog.requestFocus();
    }
}
