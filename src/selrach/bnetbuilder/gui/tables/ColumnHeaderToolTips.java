package selrach.bnetbuilder.gui.tables;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

/**
 * Shows column tooltips
 * 
 * @author <a href="mailto:charleswrobertson@gmail.com">Charles Robertson</a>
 * 
 */
public class ColumnHeaderToolTips extends MouseMotionAdapter {

	int curIndex = -1;

	List<String> tips = new ArrayList<String>();
	String masterTip = "<html>";

	public void setMasterTooltip(String tip) {
		masterTip = "<html>" + tip;
	}

	public void setToolTip(int index, String tip) {
		while (index >= tips.size()) {
			tips.add("");
		}
		tips.set(index, tip);
	}

	public void setToolTips(List<String> tips) {
		this.tips = new ArrayList<String>(tips);
	}

	@Override
	public void mouseMoved(MouseEvent evt) {
		JTableHeader header = (JTableHeader) evt.getSource();
		JTable table = header.getTable();
		TableColumnModel colModel = table.getColumnModel();
		int vColIndex = colModel.getColumnIndexAtX(evt.getX());

		if (vColIndex != curIndex) {
			if (vColIndex < tips.size() && vColIndex > -1) {
				String tip = masterTip;
				tip += tips.get(vColIndex);
				curIndex = vColIndex;
				tip += "</html>";
				header.setToolTipText(tip);
			} else {
				header.setToolTipText(null);
			}
		}
	}
}
