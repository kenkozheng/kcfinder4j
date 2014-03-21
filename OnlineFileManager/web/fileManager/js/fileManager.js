var ROOT_FOLDER = "/userFiles";       //表示当前文件操作的根目录，但不对用户显示，到了发送request的时候才拼接。不包括最后的"/"
var MANAGER_SERVLET = "../OnlineFileManagerServlet";    //指定管理功能的Servlet的路径
var UPLOAD_SERVLET = "../UploadServlet";                //指定上传功能的Servlet的路径

dtreePath = './dtree/';             //在dtree.js中加了这个变量，便于调整img路径
var tree;                           //tree必须为全局变量

//一直由$("#hiddenDirectory").attr("value")记录当前目录

//初始化工作
$(document).ready(function(){
    //设置ROOT_FOLDER
    ROOT_FOLDER = getUrlParam("rootFolder");
    //$("#fileInput").change(uploadFile);                     //上传控件选择的文件发生变化就上传，其实就是选择了文件后马上上传
    $("#uploadFileForm").attr("action", UPLOAD_SERVLET);    //设置处理上传请求的Servlet

    //绑定每个ajax动作开始到结束前都使用遮罩效果，缺点是当网速很快的时候就变了阻碍
    //        $(document).ajaxStart(function(){
    //            blockPage();
    //        }).ajaxStop(function(){
    //            unBlockPage();
    //        });

    $("#hiddenDirectory").attr("value", ROOT_FOLDER);     //必须重置,而且在列出目录之前
    refreshFiles(ROOT_FOLDER);
    refreshTree();
    
    //绑定按钮动作
    //    $("#refreshButton").click(function(){
    //        //            refreshFiles($("#hiddenDirectory").attr("value")); //在隐藏text中获取当前目录
    //    });
    $("#closeTipButton").click(function(){
        $("#operationTipDiv").hide();
    });
    $("#uploadButton").click(uploadFile);
});

/**
 * 获取新的文件列表
 * dir表示该文件夹的根目录路径
 */
function refreshFiles(dir) {
    //第二个括号内“command”是参数名，可以带双引号，也可以直接写
    //url加了date是为了解决ie缓存的问题
    $.getJSON(MANAGER_SERVLET+"?t="+new Date(),{
        command:"listFile",
        directory :dir
    } , function(data){
        $('#title').nextAll().remove(); //delete all old data in the table
        if(data == null || data == ""){
            $("#fileListDiv").hide();       //隐藏表格，显示“文件夹为空”提示
            $("#noFileMessageDiv").show();
            return;
        }else{
            $("#fileListDiv").show();
            $("#noFileMessageDiv").hide();
        }
        //遍历JSON中的每个entry,建立表格.(用JSONArray返回的json数据用each,如果用JSONObject返回的数据只有一层,就不用each)
        $.each(data,function(entryIndex,entry){
            var html='<tr>';
            var downloadHref = entry['contextPath'] + dir + '/' + entry['fileName'];
            html+='<td><a href="'+ downloadHref +'" target="_blank">'+entry['fileName']+'</a></td>';
            html+='<td>'+entry['uploadTime']+'</td>';
            html+='<td><img src="./image/delete.png" class="deleteButton" file="'+entry['fileName']+
            '" directory="'+dir+'" title="删除"/>&nbsp&nbsp'+
            '<img src="./image/rename.png" class="renameButton" file="'+entry['fileName']+
            '" directory="'+dir+'" title="重命名"/></td>';
            html+='</tr>';
            $('#title').after(html);
        });
        bindClickAction();  //绑定删除按钮和重命名按钮动作
        changeTableStyle();         //加入相间颜色显示
    }
    );
}

/**
* 绑定删除按钮和重命名按钮动作
 */
function bindClickAction() {
    $(".deleteButton").click(function(){
        var fileName = $(this).attr("file");
        var dir = $(this).attr("directory");
        jConfirm("确定要删除 "+ fileName +" 吗？", '', function(answer) {
            if(answer){
                deleteFile(fileName, dir);    //directory is a path including root folder
            }
        });
    });
    $(".renameButton").click(function(){
        var fileName = $(this).attr("file");
        var dir = $(this).attr("directory");
        //jPrompt: jquery plugin for alert dialogs et.
        jPrompt('请输入新的文件名（不包含后缀名）：', '', '重命名', function(newName) {
            if (newName){
                renameFile(fileName, dir, newName);
            }
        });
    });
}

/**
 * 加入相间颜色显示
 */
function changeTableStyle() {
    $("tr:even").addClass("evenTr");
    $("tr:odd").addClass("oddTr");
}

function refreshTree() {
    //生成新的树，ajax方式获取最新tree，每个json item表示一个节点
    //url加了date是为了解决ie缓存的问题
    $.getJSON(MANAGER_SERVLET+"?t="+new Date(), {
        command:"directoryTree",
        directory: ROOT_FOLDER
    }, function(data){
        tree = new dTree('tree');   //参数tree，表示生成的html代码中id的标记，不能修改，涉及到很多函数，如bindClickActionToTree
        tree.add(0, -1, '总目录', 'javascript:void(0)','');     //树根
        var selectedNodeNum = 0;    //设置要选中的节点
        //遍历JSON中的每个entry
        $.each(data,function(entryIndex,entry){
            //id和pid为了组建树，name是显示的文字，另外再绑定点击的动作，不用url
            //relativePath是相对路径，不包括ROOT_FOLDER，因为不显示ROOT_FOLDER给用户看
            tree.add(entry['id'], entry['pid'], entry['name'], "javascript:void(0)", entry['relativePath']);
            //如果等于当前目录，就要设置选中这个节点。因为前者包括ROOT_FOLDER，所以需要用indexOf，而不能直接比较
            if($("#hiddenDirectory").attr("value") == (ROOT_FOLDER + "/" + entry['relativePath'])){
                selectedNodeNum = entry['id'];  
            }
        }
        );
        $("#directoryTreeDiv").html(tree.toString());   //画出树
        tree.openAll(); //需要在树画出来后调用
        tree.s(selectedNodeNum);

        bindContextMenuToTree();
        bindClickActionToTree();
    });
}

/**
 * 添加点击操作到树节点上，不用dtree默认的href，href设置为void(0)
 */
function bindClickActionToTree() {
    $(".node, .nodeSel").click(function(){
        //this是node
        var newDir = ROOT_FOLDER + "/" + this.title;
        $("#hiddenDirectory").attr("value", newDir);   //为上传做准备
        refreshFiles(newDir);
    })
}

/**
 * 绑定右键操作，jquery.contextmenu.r2.js
 * 设置参考：http://www.blogjava.net/supercrsky/articles/250091.html
 */
function bindContextMenuToTree() {
    //为根节点绑定动作。因为调用此函数前用tree.s(0)选中了根节点，根节点class为nodeSel，其他节点class为node
    $("#stree0").contextMenu('menuForTreeRoot',
    {
        //菜单项样式
        itemStyle: {
            padding: '1px',
            "font-size": '12px' //font-size不用引号包住就出错
        },
        bindings: {
            //add之类的是id
            'addFolderAtRoot': function(nodeClicked) {
                //jPrompt: jquery plugin for alert dialogs et.
                jPrompt("在总目录下建立新目录。请输入目录名称：", '', '', function(dirName) {
                    if (dirName){
                        addFolder(ROOT_FOLDER, dirName);
                    }
                });
            }
        }

    });
    //选中树中其他节点(树生成后节点的class为node). jquery选择器参考：http://www.w3school.com.cn/jquery/jquery_ref_selectors.asp
    $(".node:not(#stree0),.nodeSel:not(#stree0)").contextMenu('menuForTreeLeaf',
    {
        //菜单项样式
        itemStyle: {
            padding: '1px',
            "font-size": '12px' //font-size不用引号包住就出错
        },
        bindings: {
            //add之类的是id
            'add': function(nodeClicked) {
                var parentDir = nodeClicked.title;  //建树的时候设置title为相对路径
                //jPrompt: jquery plugin for alert dialogs et.
                jPrompt("在目录 "+parentDir+" 下建立新目录。请输入目录名称：", '', '', function(dirName) {
                    if (dirName){
                        addFolder(ROOT_FOLDER + "/" + parentDir, dirName);
                    }
                });
            },
            'delete': function(nodeClicked) {
                var dir = nodeClicked.title;
                jConfirm("确定要删除 "+dir+" 吗？", '', function(answer) {
                    if(answer){
                        var dir = ROOT_FOLDER + "/" + nodeClicked.title;
                        deleteFolder(dir);
                    }
                });
            },
            'rename': function(nodeClicked) {
                var dir = nodeClicked.title;
                //jPrompt: jquery plugin for alert dialogs et.
                jPrompt("重命名目录："+dir+"。请输入目录名称：", '', '', function(newName) {
                    if (newName){
                        renameFolder(ROOT_FOLDER + "/" + dir, newName);
                    }
                });
            }
        }

    });
}
//parentDir : 上层目录路径，dirName：新建目录名称
function addFolder(parentDir, dirName) {
    $.post(MANAGER_SERVLET, {
        command:"addFolder",
        directory : parentDir + "/" + dirName
    },
    function(data){
        if(data == null || data['success'] != true){
            jAlert('error', '添加失败，可能存在同名目录或新目录名含有非法字符(? \\ / : * \" < > |)，请重试。');
        }
        refreshTree();
    }, "json");
}

//dir是相对根目录的路径，newName只有最后一个文件夹名称
function renameFolder(dir, newName) {
    $.post(MANAGER_SERVLET, {
        command:"renameFolder",
        directory:dir,
        newFolderName : newName
    },
    function(data){
        if(data ==null || data['success'] != true){
            jAlert('error', "重命名失败，可能存在同名目录或新目录名含有非法字符(? \\ / : * \" < > |)，请重试。");
            return;
        }
        //如果重命名的目录正好是当前目录路径上，dir包括ROOT_FOLDER
        var currentDir = $("#hiddenDirectory").attr("value").toString();
        if(currentDir.indexOf(dir) == 0){
            var newDir = dir.substring(0, dir.lastIndexOf('/')+1) + newName;
            $("#hiddenDirectory").attr("value", currentDir.replace(dir, newDir));   //如果是当前目录，正好全替换；如果是父目录就替换前半段
        }
        refreshTree();
    }, "json");
}
//dir : directory,including ROOT_FOLDER in the path(根目录路径)
function deleteFolder(dir) {
    $.post(MANAGER_SERVLET, {
        command:"deleteFolder",
        directory:dir
    },
    function(data){
        if(data ==null || data['success'] != true){
            jAlert('error', "删除失败，可能该目录内有其他文件。");
            return;
        }
        refreshTree();
        //如果删除的目录正好是当前目录
        var currentDir = $("#hiddenDirectory").attr("value");
        if(currentDir == dir){
            $("#hiddenDirectory").attr("value", ROOT_FOLDER);   //如果是当前目录，就回去根目录
            refreshFiles(ROOT_FOLDER);
        }
    }, "json");
}
/**
* delete a file
* file : file name
* dir : directory,including ROOT_FOLDER in the path(根目录路径)
*/
function deleteFile(fileName, dir) {
    $.post(MANAGER_SERVLET, {
        command:"deleteFile",
        file:fileName,
        directory:dir
    },
    function(data){
        if(data ==null || data['success'] != true){
            jAlert('error', "删除失败，请重试。");
        }
        refreshFiles(dir);
    }, "json");
}
/**
* rename a file
* file : file name
* dir : directory,including ROOT_FOLDER in the path(根目录路径)
*/
function renameFile(fileName, dir, newName) {
    $.post(MANAGER_SERVLET, {
        command:"renameFile",
        file:fileName,
        directory:dir,
        newFileName : newName
    },
    function(data){
        if(data == null || data['success'] != true){
            jAlert('error', "重命名失败，可能存在同名文件或新文件名含有非法字符(? \\ / : * \" < > |)，请重试。");
        }
        refreshFiles(dir);
    }, "json");
}
/**
* 上传文件，非刷新上传，上传后局部刷新
*/
function uploadFile() {
    if($("#fileInput").attr("value")==""){
        jAlert('error', "请先选择文件");
        return;
    }
    blockPage();
    $('#uploadFileForm').submit();
}
/**
* finish uploading
*/
function finishUploading(status, message) {
    unBlockPage();
    if(status == 1){
        refreshFiles($("#hiddenDirectory").attr("value"));  //在隐藏text中获取当前目录
        jAlert('success', "上传成功", "成功提示");
    }
    else{
        jAlert('error', message);
    }
}

/**
* 遮罩效果，msg是需要显示的文字
*/
function blockPage() {
    $.blockUI({
        message: '<img src="./image/loading.gif"/>',
        //消息框外框的样式
        css : {
            border: "none",
            background: "transparent"
        }
    }
    );
}
/**
* 去除遮罩效果
*/
function unBlockPage() {
    $.unblockUI();
}



/**
 * //获取request参数
 */
function getUrlParam(name){
    var reg = new RegExp("(^|&)"+ name +"=([^&]*)(&|$)");
    var r = window.location.search.substr(1).match(reg);
    if (r!=null) return unescape(r[2]);
    return null;      
}
