/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UI.FileManage;

/**
 * 接收用户请求的目录，返回是否有权限
 * 暂时只禁止用户操作根目录，然后没有增删查改的检查
 * @author 郑高强
 */
public class SecurityChecker {

    //all commands are defined in Commands.java
    //dir begins with '/'
    public boolean isAuthorized(String dir, String command) {
        //if you want to do more specified dir checked, you should understand all request in fileManager.js
        //because requests for folder operation are not the same with those for file operation
        if (dir == null || dir.isEmpty()) {
            return false;     //目录为空表示有问题，至少不能让用户修改根目录的东西
        }

        String dirToCheck = dir + "/";  //原来dir没有'/'结尾
        //userFiles为网盘目录，attachments为跟系统业务相关的附件目录
        if (!(dirToCheck.startsWith("/userFiles/") || dirToCheck.startsWith("/attachments/"))) {
            return false;
        }
        return true;
    }
}
