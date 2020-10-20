package org.jetbrains.jps.build.snapshot.dumper

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*


class CollectDialog(private var project: Project): DialogWrapper(true) {
    private val CHECKBOX_SELECTED = true

    val cachesCheckBox = JBCheckBox("Idea caches for current project", CHECKBOX_SELECTED)
    val buildLogsCheckBox = JBCheckBox("JPS build logs", CHECKBOX_SELECTED)
    val aboutInfoCheckBox = JBCheckBox("\"About Idea\" info (with system info)", CHECKBOX_SELECTED)
    val gitBranchCheckBox = JBCheckBox("Info about Git status, branch and commit", CHECKBOX_SELECTED)
    val gitPatchCheckBox = JBCheckBox("Patch with local changes", CHECKBOX_SELECTED)
    val kotlinDaemonLogsCheckBox = JBCheckBox("Kotlin daemon logs", CHECKBOX_SELECTED)
    val gradleDaemonLogsCheckBox = JBCheckBox("Gradle daemon logs", CHECKBOX_SELECTED)
    var projectFoldersTextField = JTextField(".idea, out, dist, buildSrc/build/classes/java")

    init {
        init()
        title = "Collect JPS build info"
    }

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout(10, 10))

        // Description
        val northPanel = JPanel(BorderLayout(10, 10))
        val label = JLabel(
            "<html>This action will collect JPS build artifacts. " +
                    "Choose those artifacts, which you want to share:</html>"
        )
        label.preferredSize = Dimension(400, 50)
        northPanel.add(label, BorderLayout.CENTER)

        val icon = JLabel(AllIcons.General.InformationDialog)
        northPanel.add(icon, BorderLayout.WEST)

        dialogPanel.add(northPanel, BorderLayout.NORTH)

        // Check box section
        val centralGridPanel = JPanel(GridBagLayout())
        var gbc = GridBagConstraints().apply {
            gridx = 0
            fill = GridBagConstraints.NONE
            anchor = GridBagConstraints.LINE_START
        }

        gbc.gridy = 0
        centralGridPanel.add(cachesCheckBox, gbc)

        gbc.gridy = 1
        centralGridPanel.add(buildLogsCheckBox, gbc)

        gbc.gridy = 2
        centralGridPanel.add(aboutInfoCheckBox, gbc)

        gbc.gridy = 3
        centralGridPanel.add(gitBranchCheckBox, gbc)

        gbc.gridy = 4
        centralGridPanel.add(gitPatchCheckBox, gbc)

        gbc.gridy = 5
        centralGridPanel.add(kotlinDaemonLogsCheckBox, gbc)

        gbc.gridy = 6
        centralGridPanel.add(gradleDaemonLogsCheckBox, gbc)

        dialogPanel.add(centralGridPanel, BorderLayout.WEST)

        // Project folders
        val southPanel = JPanel(GridBagLayout())

        val descriptionLabel = JLabel(
            "<html>Add relative paths from project root of folders you want to collect</html>"
        )
        gbc.fill = GridBagConstraints.NONE
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.LINE_START
        southPanel.add(descriptionLabel, gbc)

        projectFoldersTextField.font = descriptionLabel.font
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.gridy = 1
        southPanel.add(projectFoldersTextField, gbc)

        dialogPanel.add(southPanel, BorderLayout.SOUTH)

        okAction.putValue(Action.NAME, "Collect")

        pack()
        return dialogPanel
    }

    override fun doOKAction() {
        super.doOKAction()
        Collector(project, this).collectInBackground()
    }
}