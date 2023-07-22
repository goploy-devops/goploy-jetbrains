package com.goploy.toolwindow

import com.intellij.ui.JBColor
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.LineBorder
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer


/**
 * The ButtonColumn class provides a renderer and an editor that looks like a
 * JButton. The renderer and editor will then be used for a specified column
 * in the table. The TableModel will contain the String to be displayed on
 * the button.
 * The button can be invoked by a mouse click or by pressing the space bar
 * when the cell has focus. Optionally a mnemonic can be set to invoke the
 * button. When the button is invoked the provided Action is invoked. The
 * source of the Action will be the table. The action command will contain
 * the model row number of the button that was clicked.
 *
 */
class ButtonColumn(private val table: JTable, private val action: Action, column: Int) : AbstractCellEditor(),
    TableCellRenderer, TableCellEditor, ActionListener, MouseListener {
    var mnemonic = 0
        /**
         * The mnemonic to activate the button when the cell has focus
         *
         * @param mnemonic the mnemonic
         */
        set(mnemonic) {
            field = mnemonic
            renderButton.mnemonic = mnemonic
            editButton.mnemonic = mnemonic
        }
    private val originalBorder: Border
    private var focusBorder: Border? = null
    private val renderButton: JButton = JButton()
    private val editButton: JButton = JButton()
    private var editorValue: Any? = null
    private var isButtonColumnEditor = false

    /**
     * Create the ButtonColumn to be used as a renderer and editor. The
     * renderer and editor will automatically be installed on the TableColumn
     * of the specified column.
     *
     * @param table the table containing the button renderer/editor
     * @param action the Action to be invoked when the button is invoked
     * @param column the column to which the button renderer/editor is added
     */
    init {
        editButton.isFocusPainted = false
        editButton.addActionListener(this)
        originalBorder = editButton.border
        setFocusBorder(LineBorder(JBColor.BLUE))
        val columnModel = table.columnModel
        columnModel.getColumn(column).cellRenderer = this
        columnModel.getColumn(column).cellEditor = this
        table.addMouseListener(this)
    }

    /**
     * Get foreground color of the button when the cell has focus
     *
     * @return the foreground color
     */
    fun getFocusBorder(): Border? {
        return focusBorder
    }

    /**
     * The foreground color of the button when the cell has focus
     *
     * @param focusBorder the foreground color
     */
    fun setFocusBorder(focusBorder: Border?) {
        this.focusBorder = focusBorder
        editButton.border = focusBorder
    }

    override fun getTableCellEditorComponent(
        table: JTable, value: Any, isSelected: Boolean, row: Int, column: Int
    ): Component {
        if (value == null) {
            editButton.text = ""
            editButton.icon = null
        } else if (value is Icon) {
            editButton.text = ""
            editButton.icon = value
        } else {
            editButton.text = value.toString()
            editButton.icon = null
        }
        editorValue = value
        return editButton
    }

    override fun getCellEditorValue(): Any {
        return editorValue!!
    }

    //
    //  Implement TableCellRenderer interface
    //
    override fun getTableCellRendererComponent(
        table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component {
        if (isSelected) {
            renderButton.foreground = table.selectionForeground
            renderButton.background = table.selectionBackground
        } else {
            renderButton.foreground = table.foreground
            renderButton.background = UIManager.getColor("Button.background")
        }
        if (hasFocus) {
            renderButton.border = focusBorder
        } else {
            renderButton.border = originalBorder
        }

//		renderButton.setText( (value == null) ? "" : value.toString() );
        if (value == null) {
            renderButton.text = ""
            renderButton.icon = null
        } else if (value is Icon) {
            renderButton.text = ""
            renderButton.icon = value
        } else {
            renderButton.text = value.toString()
            renderButton.icon = null
        }
        return renderButton
    }

    //
    //  Implement ActionListener interface
    //
    /*
     *	The button has been pressed. Stop editing and invoke the custom Action
     */
    override fun actionPerformed(e: ActionEvent) {
        val row = table.convertRowIndexToModel(table.editingRow)
        fireEditingStopped()

        //  Invoke the Action
        val event = ActionEvent(
            table,
            ActionEvent.ACTION_PERFORMED,
            "" + row
        )
        action.actionPerformed(event)
    }

    //
    //  Implement MouseListener interface
    //
    /*
     *  When the mouse is pressed the editor is invoked. If you then then drag
     *  the mouse to another cell before releasing it, the editor is still
     *  active. Make sure editing is stopped when the mouse is released.
     */
    override fun mousePressed(e: MouseEvent) {
        if (table.isEditing
            && table.cellEditor === this
        ) isButtonColumnEditor = true
    }

    override fun mouseReleased(e: MouseEvent) {
        if (isButtonColumnEditor
            && table.isEditing
        ) table.cellEditor.stopCellEditing()
        isButtonColumnEditor = false
    }

    override fun mouseClicked(e: MouseEvent) {}
    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}
}