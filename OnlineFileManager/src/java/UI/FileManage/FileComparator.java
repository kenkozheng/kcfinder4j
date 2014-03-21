/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package UI.FileManage;

import java.io.File;
import java.util.Comparator;
import utility.chinese.PinyinComparator;

/**
 * 文件列表如何排序，在这里设置
 * @author 郑高强
 */
public class FileComparator implements Comparator<File>{

    public int compare(File file1, File file2) {
        return new PinyinComparator().compare(file1.getName(), file2.getName());
    }

}
