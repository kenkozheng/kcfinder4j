package UI.FileManage;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class UploadServlet extends HttpServlet {

    @SuppressWarnings("unchecked")
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        String errorMessage = null;
        try {
            DiskFileItemFactory fac = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(fac);
            upload.setHeaderEncoding("utf-8");
            List fileList = null;
            fileList = upload.parseRequest(request);

            String dir = ((FileItem) fileList.get(1)).getString();   //获取directory参数，指从根目录的路径，带前边的/
            // then you can add some check for security here, but no strict security now
            if (!(new SecurityChecker().isAuthorized(dir, Commands.UPLOAD_FILE))) {
                return;     
            }
            String savePath = getServletContext().getRealPath(dir);

            //平时就在最上方request.setCharacterEncoding("GB2312")即可，但这个上传控件不给力，只能这里独立把编码从网络的iso-8859-1换一下
            savePath = new String(savePath.getBytes("iso-8859-1"), "UTF-8");
            File f1 = new File(savePath);
            if (!f1.exists()) {
                f1.mkdirs();
            }

            Iterator<FileItem> it = fileList.iterator();
            String name = "";
            while (it.hasNext()) {
                FileItem item = it.next();
                if (!item.isFormField()) {
                    name = item.getName();
                    //为了防止name包含整个客户端路径，只截取最后部分。其实是针对IE
                    if (name.contains("\\")) {
                        name = name.substring(name.lastIndexOf('\\'));
                    }
                    File file = new File(savePath + File.separatorChar + name);
                    if (file.exists()) {
                        errorMessage = "该文件已经存在！请重命名后再上传。";
                    }
                    item.write(file);
                    item.delete();  //释放输出流
                }
            }
            if (errorMessage == null) {
                response.getWriter().print("<script> parent.finishUploading(1);</script>");
            } else {
                response.getWriter().print("<script> parent.finishUploading(0,'" + errorMessage + "');</script>");
            }
        } catch (Exception exception) {
            response.getWriter().print("<script> parent.finishUploading(0,'上传失败，请重试。');</script>");
        } finally {
            response.getWriter().close();
        }
    }
}
