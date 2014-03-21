/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UI.FileManage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Kenko
 */
public class OnlineFileManagerServlet extends HttpServlet {

    static final Object lock = new Object(); //同步锁

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        request.setCharacterEncoding("UTF-8");
        String command = request.getParameter("command");

        // then you can add some check for security here, but no strict security now
        String dir = request.getParameter("directory");
        if (new SecurityChecker().isAuthorized(dir, command)) {
            //同步文件操作
            synchronized (lock) {
                if (command.equals(Commands.LIST_FILE)) {
                    listFiles(request, out);
                } else if (command.equals(Commands.DIRECTORY_TREE)) {
                    showDirectoryTree(request, out);
                } else if (command.equals(Commands.RENAME_FILE)) {
                    renameFile(request, out);
                } else if (command.equals(Commands.DELETE_FILE)) {
                    deleteFile(request, out);
                } else if (command.equals(Commands.ADD_FOLDER)) {
                    addFolder(request, out);
                } else if (command.equals(Commands.DELETE_FOLDER)) {
                    deleteFolder(request, out);
                } else if (command.equals(Commands.RENAME_FOLDER)) {
                    renameFolder(request, out);
                }
            }
        }
        try {
            out.close();
        } catch (Exception e) {
        }
    }

    private void deleteFile(HttpServletRequest request, PrintWriter out) {
        try {
            //前台html传递回来的dir不带开头和结尾的'/'
            String dir = getServletContext().getRealPath(request.getParameter("directory"));
            File file = new File(dir + File.separatorChar + request.getParameter("file"));
            JSONObject json = new JSONObject();
            json.put("success", file.delete());
            out.write(json.toString());
        } catch (Exception ex) {
            Logger.getLogger(OnlineFileManagerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void renameFile(HttpServletRequest request, PrintWriter out) {
        try {
            String dir = getServletContext().getRealPath(request.getParameter("directory"));
            File file = new File(dir + File.separatorChar + request.getParameter("file"));
            String suffix = file.getName().substring(file.getName().lastIndexOf("."));
            File newFile = new File(dir + File.separatorChar + request.getParameter("newFileName") + suffix);
            System.out.println(file.getAbsolutePath());
            System.out.println(newFile.getAbsolutePath());
            JSONObject json = new JSONObject();
            json.put("success", file.renameTo(newFile));
            out.write(json.toString());
        } catch (Exception ex) {
            Logger.getLogger(OnlineFileManagerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addFolder(HttpServletRequest request, PrintWriter out) {
        try {
            //前台html传递回来的dir不带开头和结尾的'/'
            String dir = getServletContext().getRealPath(request.getParameter("directory"));
            JSONObject json = new JSONObject();
            json.put("success", new File(dir).mkdirs());
            out.write(json.toString());
        } catch (Exception ex) {
            Logger.getLogger(OnlineFileManagerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void deleteFolder(HttpServletRequest request, PrintWriter out) {
        try {
            //前台html传递回来的dir不带开头和结尾的'/'
            String dir = getServletContext().getRealPath(request.getParameter("directory"));
            JSONObject json = new JSONObject();
            json.put("success", new File(dir).delete());
            out.write(json.toString());
        } catch (Exception ex) {
            Logger.getLogger(OnlineFileManagerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void renameFolder(HttpServletRequest request, PrintWriter out) {
        try {
            String dir = getServletContext().getRealPath(request.getParameter("directory"));
            String newDir = dir.substring(0, dir.lastIndexOf(File.separatorChar) + 1) + request.getParameter("newFolderName");
            JSONObject json = new JSONObject();
            json.put("success", new File(dir).renameTo(new File(newDir)));
            out.write(json.toString());
        } catch (Exception ex) {
            Logger.getLogger(OnlineFileManagerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * 列出所有文件，json返回
     * @param request
     * @param out
     */
    private void listFiles(HttpServletRequest request, PrintWriter out) {
        try {
            //前台html传递回来的dir不带开头和结尾的'/'
            String dir = getServletContext().getRealPath(request.getParameter("directory"));
            File directory = new File(dir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File[] files = directory.listFiles();
            Arrays.sort(files, new FileComparator()); //按名称排序
            //创建json数据
            JSONArray jsonFiles = new JSONArray();
            //因为json到了前台解释顺序问题，所以这里倒序
            for (int i = files.length - 1; i >= 0; i--) {
                if (files[i].isDirectory()) {
                    continue;
                }
                JSONObject jsonFile = new JSONObject();
                jsonFile.put("fileName", files[i].getName());
                jsonFile.put("contextPath", request.getContextPath());    //用于前台拼凑实际下载地址.contextPath means '/webapp' in "http://localhost/webapp/index.html"
                jsonFile.put("uploadTime", new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date(files[i].lastModified())));
                jsonFiles.put(jsonFile);
            }
            out.write(jsonFiles.toString());
        } catch (Exception ex) {
            Logger.getLogger(OnlineFileManagerServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }

    /**
     * 返回json表示的目录结构树，供前台dtree生成
     * @param request
     * @param out
     */
    private void showDirectoryTree(HttpServletRequest request, PrintWriter out) {
        try {
            //创建json数据
            JSONArray tree = new JSONArray();
            String rootFolder = request.getParameter("directory");
            String dir = getServletContext().getRealPath(rootFolder);
            File directory = new File(dir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            int rootFolderLength = dir.length();   //作用见listFolers函数
            listFolders(tree, directory, 0, rootFolderLength);     //递归调用
            out.write(tree.toString());
        } catch (Exception ex) {
            Logger.getLogger(OnlineFileManagerServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }

    /**
     * 列出指定目录内的所有文件夹，并递归执行
     * 调用者需要确保dir存在，而且必须为folder
     * 专门为showDirectoryTree服务
     * @param tree 调用者中的JSONArray，用于add新的JSONObject
     * @param dir
     * @param pid 父亲节点的id（dtree）
     * @param rootFolderLength 计算相对路径的时候
     */
    private void listFolders(JSONArray tree, File dir, int pid, int rootFolderLength) {
        try {
            //递归处理子目录
            File[] files = dir.listFiles();
            Arrays.sort(files, new FileComparator());     //按文件名排序
            for (File file : files) {
                if (file.isDirectory()) {
                    JSONObject node = new JSONObject();
                    int nodeID = tree.length() + 1; //计算新节点的id，按顺序递增
                    node.put("id", nodeID);
                    node.put("pid", pid);
                    node.put("name", file.getName());
                    //计算相对rootFolder的路径作为树形目录每个节点的tooltip（title）
                    //+1是为了去除开始的'/'，跟前边的listFiles函数设计一致
                    String relativePath = file.getAbsolutePath().substring(rootFolderLength + 1);
                    /*在windows平台，需要替换\，不然会在后边的listFiles中出错。在string中一个正斜杠用两个\表示，所以这里要用4个\
                     */
                    relativePath = relativePath.replaceAll("\\\\", "/");     //在windows平台，需要替换\，
                    node.put("url", "javascript:void(0)");
                    node.put("relativePath", relativePath);      //
                    tree.put(node);

                    if (file.listFiles().length != 0) {     //有子目录
                        listFolders(tree, file, nodeID, rootFolderLength);   //递归
                    }
                }
            }
        } catch (JSONException ex) {
            Logger.getLogger(OnlineFileManagerServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);

    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
