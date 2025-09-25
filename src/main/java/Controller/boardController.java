package Controller;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.simple.JSONObject;

import Service.boardService;
import Vo.boardVO;
import util.AzureBlobUtil;

@MultipartConfig(
	    fileSizeThreshold = 1024 * 1024,
	    maxFileSize = 50L * 1024L * 1024L,
	    maxRequestSize = 60L * 1024L * 1024L
	)
@WebServlet("/bbs/*")
public class boardController extends HttpServlet {

    private boardService boardService;
    private boardVO boardVO;

    @Override
    public void init() throws ServletException {
        super.init();
        boardService = new boardService();
        boardVO = new boardVO();
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try { doHandle(req, resp); } catch (FileUploadException e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try { doHandle(req, resp); } catch (FileUploadException e) { throw new ServletException(e); }
    }

    /** multipart 파싱 + temp 저장 */
    public Map<String, String> uploadFile(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, FileUploadException {

        Map<String, String> map = new HashMap<>();
        String root = request.getServletContext().getRealPath("/");
        String base = root + "board" + File.separator + "board_file_repo";
        File tempDir = new File(base + File.separator + "temp");
        if (!tempDir.exists()) tempDir.mkdirs();

        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(3 * 1024 * 1024);
        factory.setRepository(tempDir);
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setHeaderEncoding("utf-8");

        try {
            List<FileItem> items = upload.parseRequest(request);
            for (FileItem item : items) {
                String name = item.getFieldName();
                if (item.isFormField()) {
                    map.put(name, item.getString("utf-8"));
                } else {
                    if (item.getSize() > 0) {
                        String org = new File(item.getName()).getName();
                        String newName = System.currentTimeMillis() + "_" + org;
                        item.write(new File(tempDir, newName));
                        if ("file".equals(name)) {
                            map.put("file", newName);
                            map.put("newFileName", newName);
                        } else if ("bannerImage".equals(name)) {
                            map.put("bannerImage", newName);
                            map.put("newBannerName", newName);
                        }
                    } else {
                        if ("file".equals(name)) map.put("newFileName", "");
                        if ("bannerImage".equals(name)) map.put("newBannerName", "");
                    }
                }
            }
            map.putIfAbsent("originalFileName", "");
            map.putIfAbsent("originalBannerName", "");
            map.putIfAbsent("deleteFile", "false");
            map.putIfAbsent("deleteBanner", "false");

            if ("true".equals(map.get("deleteFile"))) map.put("newFileName", "");
            else if ("".equals(map.get("newFileName"))) map.put("newFileName", map.get("originalFileName"));

            if ("true".equals(map.get("deleteBanner"))) map.put("newBannerName", "");
            else if ("".equals(map.get("newBannerName"))) map.put("newBannerName", map.get("originalBannerName"));

        } catch (Exception e) {
            throw new ServletException("파일 업로드 실패", e);
        }
        return map;
    }

    private void doHandle(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, FileUploadException {

        String action = request.getPathInfo();
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        String nextPage = null;

        /* 공지 목록 */
        if ("/noticeList.do".equals(action)) {
            String searchType = nvl(request.getParameter("searchType"), "title");
            String searchKeyword = nvl(request.getParameter("searchKeyword"), "");
            int section = toInt(nvl(request.getParameter("section"), "1"));
            int pageNum = toInt(nvl(request.getParameter("pageNum"), "1"));

            Map<String, Object> result = boardService.getBoardList(0, section, pageNum, searchKeyword, searchType, null);
            request.setAttribute("searchKeyword", searchKeyword);
            request.setAttribute("searchType", searchType);
            request.setAttribute("boardList", result.get("boardList"));
            request.setAttribute("totalPage", result.get("totalPage"));
            request.setAttribute("totalSection", result.get("totalSection"));
            request.setAttribute("totalBoardCount", result.get("totalBoardCount"));
            request.setAttribute("section", section);
            request.setAttribute("pageNum", pageNum);
            request.setAttribute("center", "board/noticeList.jsp");
            nextPage = "/main.jsp";
        }

        /* 공지 글쓰기 화면 */
        if ("/noticeWrite.do".equals(action)) {
            request.setAttribute("center", "board/noticeWrite.jsp");
            nextPage = "/main.jsp";
        }

        /* 공지 등록 */
        if ("/AddNotice.do".equals(action)) {
            Map<String, String> m = uploadFile(request, response);

            // 1) 본문 먼저 저장하여 board_id 확보
            boardVO vo = new boardVO();
            vo.setCategory(0);
            vo.setTitle(m.get("title"));
            vo.setContent(m.get("content"));
            vo.setUserId((String) request.getSession().getAttribute("id"));
            vo.setFile(null);
            vo.setBannerImg(null);
            vo.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            vo.setViews(0);
            vo.setSecret(false); // 공지는 기본 공개

            int boardId = boardService.addBoard(vo);

            // 2) 업로드 (있을 때만)
            ServletContext ctx = request.getServletContext();
            String fileUrl   = toAzure(ctx, m.get("file"),        boardId, ctx.getMimeType(m.get("file")));
            String bannerUrl = toAzure(ctx, m.get("bannerImage"), boardId, ctx.getMimeType(m.get("bannerImage")));

            // 3) 파일/배너만 별도 UPDATE (있을 때만)
            if (fileUrl != null || bannerUrl != null) {
            	boolean secret = false;
            	boardService.updateBoardFiles(boardId, fileUrl, bannerUrl, secret);
            	
            }

            response.sendRedirect(request.getContextPath() + "/bbs/noticeInfo.do?boardId=" + boardId);
            return;
        }


        /* 공지 상세 */
        if ("/noticeInfo.do".equals(action)) {
            int boardId = requireInt(request.getParameter("boardId"));
            boardVO viewed = boardService.viewBoard(boardId);
            if (viewed == null) throw new ServletException("게시글 없음");
            request.setAttribute("board", viewed);
            request.setAttribute("getPreBoardId", boardService.getPreBoardId(boardId, viewed.getCategory()));
            request.setAttribute("getNextBoardId", boardService.getNextBoardId(boardId, viewed.getCategory()));
            request.setAttribute("center", "board/noticeInfo.jsp");
            nextPage = "/main.jsp";
        }
        
        /* 공지 수정 폼 진입 */
        if ("/noticeModifyForm.do".equals(action)) {
            int boardId = requireInt(request.getParameter("boardId"));
            boardVO viewed = boardService.viewBoard(boardId);
            if (viewed == null) throw new ServletException("게시글 없음");

            String uid = (String) request.getSession().getAttribute("id");
            if (uid == null || !(uid.equals(viewed.getUserId()) || "admin".equals(uid))) {
                response.getWriter().println("<script>alert('수정 권한이 없습니다.'); history.back();</script>");
                return;
            }

            request.setAttribute("board", viewed);
            request.setAttribute("center", "board/noticeModifyForm.jsp");
            nextPage = "/main.jsp";
        }


        /* 공지 수정 (temp→Azure) */
        if ("/noticeModify.do".equals(action)) {
            Map<String, String> m = uploadFile(request, response);
            int boardId = requireInt(m.get("boardId"));

            boardVO mod = new boardVO();
            mod.setBoardId(boardId);
            mod.setTitle(m.get("title"));
            mod.setContent(m.get("content"));
            mod.setSecret(false);

            // 새 파일이 들어온 경우에만 Azure 업로드 & URL 세팅
            ServletContext ctx = request.getServletContext();
            if (!empty(m.get("newFileName")) && !m.get("newFileName").equals(m.get("originalFileName"))) {
                String url = toAzure(ctx, m.get("newFileName"), boardId, getServletContext().getMimeType(m.get("newFileName")));
                mod.setFile(url);
            } else if ("true".equals(m.get("deleteFile"))) {
                mod.setFile(""); // 빈값 저장(서비스에서 빈값이면 null로 처리/삭제 플래그 등 정책에 맞게)
            }

            if (!empty(m.get("newBannerName")) && !m.get("newBannerName").equals(m.get("originalBannerName"))) {
                String url = toAzure(ctx, m.get("newBannerName"), boardId, getServletContext().getMimeType(m.get("newBannerName")));
                mod.setBannerImg(url);
            } else if ("true".equals(m.get("deleteBanner"))) {
                mod.setBannerImg("");
            }

            boardService.modifyBoard(mod);
            response.sendRedirect(request.getContextPath() + "/bbs/noticeInfo.do?boardId=" + boardId);
            return;
        }

        /* 문의 목록 */
        if ("/questionList.do".equals(action)) {
            String searchType = nvl(request.getParameter("searchType"), "title");
            String searchKeyword = nvl(request.getParameter("searchKeyword"), "");
            int section = toInt(nvl(request.getParameter("section"), "1"));
            int pageNum = toInt(nvl(request.getParameter("pageNum"), "1"));

            Map<String, Object> result = boardService.getBoardList(1, section, pageNum, searchKeyword, searchType, null);
            request.setAttribute("searchKeyword", searchKeyword);
            request.setAttribute("searchType", searchType);
            request.setAttribute("boardList", result.get("boardList"));
            request.setAttribute("totalPage", result.get("totalPage"));
            request.setAttribute("totalSection", result.get("totalSection"));
            request.setAttribute("totalBoardCount", result.get("totalBoardCount"));
            request.setAttribute("section", section);
            request.setAttribute("pageNum", pageNum);
            request.setAttribute("center", "board/questionList.jsp");
            nextPage = "/main.jsp";
        }

        /* 문의 글쓰기 화면 */
        if ("/questionWrite.do".equals(action)) {
            request.setAttribute("center", "board/questionWrite.jsp");
            nextPage = "/main.jsp";
        }

        /* 문의 등록 */
        if ("/AddQuestion.do".equals(action)) {
            Map<String, String> m = uploadFile(request, response);
            boolean isSecret = "on".equalsIgnoreCase(m.get("secret")) || "true".equalsIgnoreCase(m.get("secret"));

            // 1) 본문 먼저 저장
            boardVO vo = new boardVO();
            vo.setCategory(1);
            vo.setTitle(m.get("title"));
            vo.setContent(m.get("content"));
            vo.setUserId((String) request.getSession().getAttribute("id"));
            vo.setFile(null);
            vo.setBannerImg(null);
            vo.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            vo.setViews(0);
            vo.setSecret(isSecret);

            int boardId = boardService.addBoard(vo);

            // 2) 업로드 (있을 때만)
            ServletContext ctx = request.getServletContext();
            String fileUrl   = toAzure(ctx, m.get("file"),        boardId, ctx.getMimeType(m.get("file")));
            String bannerUrl = toAzure(ctx, m.get("bannerImage"), boardId, ctx.getMimeType(m.get("bannerImage")));

            // 3) 파일/배너만 별도 UPDATE (있을 때만)
            if (fileUrl != null || bannerUrl != null) {
            	boolean secret = false;
            	boardService.updateBoardFiles(boardId, fileUrl, bannerUrl, secret);
            }

            response.sendRedirect(request.getContextPath() + "/bbs/questionInfo.do?boardId=" + boardId);
            return;
        }


        /* 문의 상세 */
        if ("/questionInfo.do".equals(action)) {
            String currentUserId = (String) request.getSession().getAttribute("id");
            boolean isAdmin = "admin".equals(currentUserId);

            int boardId = requireInt(request.getParameter("boardId"));
            boardVO viewed = boardService.viewBoard(boardId);
            if (viewed == null) throw new ServletException("게시글 없음");

            if (Boolean.TRUE.equals(viewed.getSecret())) {
                String author = viewed.getUserId();
                if (!(isAdmin || (currentUserId != null && currentUserId.equals(author)))) {
                    response.getWriter().println("<script>alert('해당 게시글을 볼 권한이 없습니다!');location.href='"
                            + request.getContextPath() + "/bbs/questionList.do';</script>");
                    return;
                }
            }

            request.setAttribute("board", viewed);
            request.setAttribute("getPreBoardId", boardService.getPreBoardId(boardId, viewed.getCategory()));
            request.setAttribute("getNextBoardId", boardService.getNextBoardId(boardId, viewed.getCategory()));
            request.setAttribute("center", "board/questionInfo.jsp");
            nextPage = "/main.jsp";
        }
        
        /* 문의 수정 폼 진입 */
        if ("/questionModifyForm.do".equals(action)) {
            int boardId = requireInt(request.getParameter("boardId"));
            boardVO viewed = boardService.viewBoard(boardId);
            if (viewed == null) throw new ServletException("게시글 없음");

            String uid = (String) request.getSession().getAttribute("id");
            if (uid == null || !(uid.equals(viewed.getUserId()) || "admin".equals(uid))) {
                response.getWriter().println("<script>alert('수정 권한이 없습니다.'); history.back();</script>");
                return;
            }

            request.setAttribute("board", viewed);
            request.setAttribute("center", "board/questionModifyForm.jsp");
            nextPage = "/main.jsp";
        }


        /* 문의 수정 (temp→Azure) */
        if ("/questionModify.do".equals(action)) {
            Map<String, String> m = uploadFile(request, response);
            int boardId = requireInt(m.get("boardId"));
            boolean isSecret = "on".equals(m.get("secret"));

            boardVO mod = new boardVO();
            mod.setBoardId(boardId);
            mod.setTitle(m.get("title"));
            mod.setContent(m.get("content"));
            mod.setSecret(isSecret);

            ServletContext ctx = request.getServletContext();
            if (!empty(m.get("newFileName")) && !m.get("newFileName").equals(m.get("originalFileName"))) {
                String url = toAzure(ctx, m.get("newFileName"), boardId, getServletContext().getMimeType(m.get("newFileName")));
                mod.setFile(url);
            } else if ("true".equals(m.get("deleteFile"))) {
                mod.setFile("");
            }

            if (!empty(m.get("newBannerName")) && !m.get("newBannerName").equals(m.get("originalBannerName"))) {
                String url = toAzure(ctx, m.get("newBannerName"), boardId, getServletContext().getMimeType(m.get("newBannerName")));
                mod.setBannerImg(url);
            } else if ("true".equals(m.get("deleteBanner"))) {
                mod.setBannerImg("");
            }

            boardService.modifyBoard(mod);
            response.sendRedirect(request.getContextPath() + "/bbs/questionInfo.do?boardId=" + boardId);
            return;
        }
        
        /* 문의글 답변 등록/수정 */
        if ("/reply.do".equals(action)) {
            response.setContentType("application/json; charset=UTF-8");
            JSONObject json = new JSONObject();

            try {
                String adminId = (String) request.getSession().getAttribute("id");
                if (!"admin".equals(adminId)) {
                    json.put("result", "fail");
                    json.put("message", "관리자만 답변 등록/수정이 가능합니다.");
                    response.getWriter().print(json.toString());
                    return;
                }

                int boardId = requireInt(request.getParameter("boardId"));
                String reply = nvl(request.getParameter("reply"), "").trim();

                if (reply.isEmpty()) {
                    json.put("result", "fail");
                    json.put("message", "답변 내용을 입력해주세요.");
                    response.getWriter().print(json.toString());
                    return;
                }

                boolean ok = boardService.updateReply(boardId, reply);
                if (ok) {
                    json.put("result", "success");
                    json.put("boardId", boardId);
                    json.put("reply", reply);
                } else {
                    json.put("result", "fail");
                    json.put("message", "DB 업데이트 실패");
                }
                response.getWriter().print(json.toString());
                return;

            } catch (Exception e) {
                json.put("result", "fail");
                json.put("message", e.getMessage());
                response.getWriter().print(json.toString());
                return;
            }
        }

        /* 문의글 답변 삭제 */
        if ("/replyDelete.do".equals(action)) {
            response.setContentType("application/json; charset=UTF-8");
            JSONObject json = new JSONObject();

            try {
                String adminId = (String) request.getSession().getAttribute("id");
                if (!"admin".equals(adminId)) {
                    json.put("result", "fail");
                    json.put("message", "관리자만 답변 삭제가 가능합니다.");
                    response.getWriter().print(json.toString());
                    return;
                }

                int boardId = requireInt(request.getParameter("boardId"));
                boolean ok = boardService.deleteReply(boardId);
                if (ok) {
                    json.put("result", "success");
                    json.put("boardId", boardId);
                } else {
                    json.put("result", "fail");
                    json.put("message", "DB 업데이트 실패");
                }
                response.getWriter().print(json.toString());
                return;

            } catch (Exception e) {
                json.put("result", "fail");
                json.put("message", e.getMessage());
                response.getWriter().print(json.toString());
                return;
            }
        }

        /* 내서평 목록 */
        if ("/myReviewList.do".equals(action)) {
            String uid = (String) request.getSession().getAttribute("id");
            if (empty(uid)) { response.sendRedirect(request.getContextPath() + "/member/login"); return; }

            String searchType = nvl(request.getParameter("searchType"), "title");
            String searchKeyword = nvl(request.getParameter("searchKeyword"), "");
            int section = toInt(nvl(request.getParameter("section"), "1"));
            int pageNum = toInt(nvl(request.getParameter("pageNum"), "1"));

            Map<String, Object> result = boardService.getBoardList(2, section, pageNum, searchKeyword, searchType, uid);
            request.setAttribute("searchKeyword", searchKeyword);
            request.setAttribute("searchType", searchType);
            request.setAttribute("boardList", result.get("boardList"));
            request.setAttribute("totalPage", result.get("totalPage"));
            request.setAttribute("totalSection", result.get("totalSection"));
            request.setAttribute("totalBoardCount", result.get("totalBoardCount"));
            request.setAttribute("section", section);
            request.setAttribute("pageNum", pageNum);
            request.setAttribute("center", "board/myReviewList.jsp");
            nextPage = "/main.jsp";
        }

        /* 내서평 상세 */
        if ("/myReviewInfo.do".equals(action)) {
            String uid = (String) request.getSession().getAttribute("id");
            if (empty(uid)) { response.sendRedirect(request.getContextPath() + "/member/login"); return; }
            int boardId = requireInt(request.getParameter("boardId"));
            boardVO viewed = boardService.viewBoard(boardId);
            if (viewed == null) throw new ServletException("게시글 없음");

            request.setAttribute("board", viewed);
            request.setAttribute("getPreBoardId", boardService.getPreBoardId(boardId, viewed.getCategory()));
            request.setAttribute("getNextBoardId", boardService.getNextBoardId(boardId, viewed.getCategory()));
            request.setAttribute("center", "board/myReviewInfo.jsp");
            nextPage = "/main.jsp";
        }

        /* 서평 더보기 상세(일반) */
        if ("/reviewDetail.do".equals(action)) {
            int boardId = requireInt(request.getParameter("boardId"));
            boardVO viewed = boardService.viewBoard(boardId);
            if (viewed == null) throw new ServletException("게시글 없음");
            request.setAttribute("board", viewed);
            request.setAttribute("getPreBoardId", boardService.getPreBoardId(boardId, viewed.getCategory()));
            request.setAttribute("getNextBoardId", boardService.getNextBoardId(boardId, viewed.getCategory()));
            request.setAttribute("center", "board/reviewInfo.jsp");
            nextPage = "/main.jsp";
        }

        /* 서평 등록 (본문만) */
        if ("/myReviewWrite.do".equals(action)) {
            String title = request.getParameter("title");
            String content = request.getParameter("content");
            int bookNo = requireInt(request.getParameter("bookNo"));
            String uid = (String) request.getSession().getAttribute("id");

            boardVO vo = new boardVO();
            vo.setCategory(2);
            vo.setTitle(title);
            vo.setContent(content);
            vo.setUserId(uid);
            vo.setBookNo(bookNo);
            vo.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            vo.setViews(0);
            vo.setSecret(false);

            int boardId = boardService.addBoard(vo);
            response.sendRedirect(request.getContextPath() + "/books/bookDetail.do?bookNo=" + bookNo);
            return;
        }

        /* 행사 리스트 */
        if ("/eventList.do".equals(action)) {
            String searchType = nvl(request.getParameter("searchType"), "title");
            String searchKeyword = nvl(request.getParameter("searchKeyword"), "");
            int section = toInt(nvl(request.getParameter("section"), "1"));
            int pageNum = toInt(nvl(request.getParameter("pageNum"), "1"));

            Map<String, Object> result = boardService.getEventBoardList(section, pageNum, searchKeyword, searchType);
            request.setAttribute("searchKeyword", searchKeyword);
            request.setAttribute("searchType", searchType);
            request.setAttribute("boardList", result.get("boardList"));
            request.setAttribute("totalPage", result.get("totalPage"));
            request.setAttribute("totalSection", result.get("totalSection"));
            request.setAttribute("totalBoardCount", result.get("totalBoardCount"));
            request.setAttribute("section", section);
            request.setAttribute("pageNum", pageNum);
            request.setAttribute("center", "board/eventList.jsp");
            nextPage = "/main.jsp";
        }

        /* 행사 상세 */
        if ("/eventInfo.do".equals(action)) {
            int boardId = requireInt(request.getParameter("boardId"));
            boardVO viewed = boardService.viewBannerBoard(boardId);
            if (viewed == null) throw new ServletException("게시글 없음");

            request.setAttribute("board", viewed);
            request.setAttribute("getPreBoardId", boardService.getPreBannerBoardId(boardId));
            request.setAttribute("getNextBoardId", boardService.getNextBannerBoardId(boardId));
            request.setAttribute("center", "board/eventInfo.jsp");
            nextPage = "/main.jsp";
        }

        /* 공통 삭제(공지/문의/내서평 리스트 진입 루트) */
        Map<String, String> redirect = new HashMap<>();
        redirect.put("/removeQuestion.do", "/bbs/questionList.do");
        redirect.put("/removeNotice.do", "/bbs/noticeList.do");
        redirect.put("/removeMyReviewList.do", "/bbs/myReviewList.do");

        if (redirect.containsKey(action)) {
            int boardId = requireInt(request.getParameter("boardId"));
            int deleted = boardService.removeBoard(boardId); // 파일은 Azure이므로 로컬 삭제 로직 제거
            JSONObject json = new JSONObject();
            json.put("result", "success");
            json.put("message", "게시글이 삭제되었습니다.");
            json.put("redirect", request.getContextPath() + redirect.get(action));
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().print(json.toString());
            return;
        }

        /* 서평 단독 삭제(상세→삭제) */
        if ("/removeReviewList.do".equals(action)) {
            int boardId = requireInt(request.getParameter("boardId"));
            int bookNo = requireInt(request.getParameter("bookNo"));
            boardService.removeBoard(boardId);
            JSONObject json = new JSONObject();
            json.put("result", "success");
            json.put("message", "게시글이 삭제되었습니다.");
            json.put("redirect", request.getContextPath() + "/books/bookDetail.do?bookNo=" + bookNo);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().print(json.toString());
            return;
        }

        if (nextPage != null) {
            RequestDispatcher rd = request.getRequestDispatcher(nextPage);
            rd.forward(request, response);
        }
    }

    /* === helpers === */
    private static boolean empty(String s) { return s == null || s.isEmpty(); }
    private static String nvl(String s, String d) { return empty(s) ? d : s; }
    private static int toInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 1; } }
    private static int requireInt(String s) {
        if (empty(s)) throw new IllegalArgumentException("required int param missing");
        return Integer.parseInt(s);
    }

    /** temp 파일을 Azure에 올리고 공개 URL 반환 */
    private String toAzure(ServletContext ctx, String tempName, int boardId, String contentType) throws IOException {
        if (empty(tempName)) return null;
        String root = ctx.getRealPath("/");
        File f = new File(root + "board" + File.separator + "board_file_repo" + File.separator + "temp" + File.separator + tempName);
        if (!f.exists()) return null;
        long size = f.length();
        try (java.io.InputStream in = new java.io.FileInputStream(f)) {
            String folder = "board/board_file_repo/" + boardId;
            AzureBlobUtil.upload(folder, tempName, nvl(contentType, "application/octet-stream"), size, in);
        }
        try { f.delete(); } catch (Exception ignore) {}
        return AzureBlobUtil.publicUrl("board/board_file_repo/" + boardId, tempName);
    }
}
