package org.jetbrains.jps.build.snapshot.dumper

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.jdesktop.swingx.prompt.PromptSupport
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*


class CollectDialog(private var project: Project): DialogWrapper(true) {
    private var textField = JTextField()

    init {
        init()
        title = "Collect JPS build info"
    }

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout(10, 10))

        val label = JLabel(
            "<html>This action will collect all JPS build artifacts: " +
                    "all build output (like out, dist, buildSrc folders), " +
                    "logs, build caches, git info and local changes.</html>"
        )

        label.preferredSize = Dimension(400, 50)
        dialogPanel.add(label, BorderLayout.CENTER)

        val icon = JLabel(AllIcons.General.InformationDialog)
        dialogPanel.add(icon, BorderLayout.WEST)

        okAction.putValue(Action.NAME, "Collect")

        textField.font = label.font
        PromptSupport.setPrompt("(Optional) Add relative paths of folders to collect here", textField)
        dialogPanel.add(textField, BorderLayout.SOUTH)

        pack()
        return dialogPanel
    }

    override fun doOKAction() {
        super.doOKAction()
        val foldersToCollect = textField.text
        Collector(project, foldersToCollect).collectInBackground()
    }
}