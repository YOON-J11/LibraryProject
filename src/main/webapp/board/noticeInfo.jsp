<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<c:set var="fromPage" value="${param.fromPage != null ? param.fromPage : sessionScope.fromPage}" />

<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <title>공지사항 상세페이지 - noticeInfo.jsp</title>

  <style>
    /* Layout */
    body { background-color: #fff; margin: 0; }
    .content-box { display: flex; flex-direction: column; align-items: flex-start; max-width: 1000px; margin: 0 auto; padding: 40px 20px; }
    .content-box-top { margin: 10px 0; }
    .content-box-middle { align-self: center; width: 100%; margin: 0 auto; background-color: #fff; border-radius: 10px; padding: 32px; box-shadow: 0 2px 10px rgba(0,0,0,0.06); }
    .content-box-bottom { margin: 20px 0; display: flex; }
    .content-box-bottom-right { display: flex; }

    /* Pagination */
    .paging { display: flex; justify-content: flex-end; gap: 5px; }
    .paging form button,
    .back-list-btn,
    .top-btn {
      padding: 8px 16px;
      font-size: 14px;
      border-radius: 4px;
      color: #fff;
      text-decoration: none;
      cursor: pointer;
      box-sizing: border-box;
      border: none;
      outline: none;
      margin-left: 5px;
      background-color: #92B5DE;
      transition: background-color .2s ease;
    }
    .top-btn { background-color: #003c83; }
    .paging form button:hover,
    .back-list-btn:hover,
    .top-btn:hover { background-color: #002c66; }

    /* Title area */
    .title-area { display: flex; justify-content: space-between; border-bottom: 1px solid #eee; padding-bottom: 15px; gap: 12px; }
    .title-area .title-area-left { flex: 1 1 auto; }
    .title-area-left .title { font-weight: 700; font-size: 24px; margin: 0 0 6px; }
    .board-info { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .board-info .user-id { font-size: 13px; line-height: 15px; font-weight: 700; }
    .board-info .board-info-gray { font-size: 12px; line-height: 14px; color: #999; }

    .delet-modify { flex: 0 0 auto; display: flex; align-items: flex-start; gap: 6px; }
    .delet-modify a {
      background-color: #f4f4f4;
      padding: 8px 15px;
      border-radius: 4px;
      font-size: 14px;
      text-decoration: none;
      color: #424242;
    }
    .delet-modify a:hover { background-color: #eee; }

    /* Content area */
    .content-area { padding-top: 20px; display: flex; flex-direction: column; gap: 20px; }
    .file-area { text-align: right; }
    .download-link {
      display: inline-block;
      text-align: center;
      background-color: #fff;
      border: 0.5px solid #eee;
      padding: 12px 15px;
      font-size: 14px;
      font-weight: 700;
      border-radius: 4px;
    }
    .download-link span { padding: 0 8px; }
    .download-link:hover { background-color: #f4f4f4; }

    .banner-img { margin: 0 auto; width: 100%; }
    .banner-img img { width: 100%; height: auto; display: block; }

    .content-area p { line-height: 1.7; font-size: 15px; color: #555; word-break: break-word; margin: 0; }

    /* Responsive */
    @media (max-width: 768px) {
      .title-area { flex-direction: column; align-items: stretch; }
      .delet-modify { align-self: flex-end; }
    }
  </style>
</head>

<body>
  <!-- fromPage에 맞춰 이전/다음 상세 URL 결정 -->
  <c:choose>
    <c:when test="${fromPage == 'eventList'}">
      <c:set var="preBoardUrl" value="${contextPath}/bbs/eventInfo.do" />
      <c:set var="nextBoardUrl" value="${contextPath}/bbs/eventInfo.do" />
    </c:when>
    <c:otherwise>
      <c:set var="preBoardUrl" value="${contextPath}/bbs/noticeInfo.do" />
      <c:set var="nextBoardUrl" value="${contextPath}/bbs/noticeInfo.do" />
    </c:otherwise>
  </c:choose>

  <section class="content-box">
    <!-- Top: prev/next -->
    <div class="content-box-top">
      <div class="paging">
        <c:if test="${getPreBoardId > 0}">
          <form action="${preBoardUrl}" method="get">
            <input type="hidden" name="boardId" value="${getPreBoardId}" />
            <input type="hidden" name="fromPage" value="${fromPage}" />
            <button type="submit">이전글</button>
          </form>
        </c:if>

        <c:if test="${getNextBoardId > 0}">
          <form action="${nextBoardUrl}" method="get">
            <input type="hidden" name="boardId" value="${getNextBoardId}" />
            <input type="hidden" name="fromPage" value="${fromPage}" />
            <button type="submit">다음글</button>
          </form>
        </c:if>
      </div>
    </div>

    <!-- Middle: title/meta/content -->
    <div class="content-box-middle">
      <div class="title-area">
        <div class="title-area-left">
          <p class="title">${board.title}</p>
          <div class="board-info">
            <span class="user-id">${board.userId}</span>
            <span class="board-info-gray">
              <fmt:formatDate value="${board.createdAt}" pattern="yyyy-MM-dd HH:mm" />
            </span>
            <span class="board-info-gray">조회 ${board.views}</span>
          </div>
        </div>

        <div class="delet-modify">
          <c:if test="${sessionScope.id == 'admin'}">
            <a href="${contextPath}/bbs/noticeModifyForm.do?boardId=${board.boardId}">수정</a>
            <a href="#" onclick="fn_remove_board('${contextPath}/bbs/removeNotice.do', ${board.boardId}); return false;">삭제</a>
          </c:if>
        </div>
      </div>

      <div class="content-area">
        <!-- 첨부파일: Azure 공개 URL 바로 사용 -->
        <div class="file-area">
          <c:if test="${not empty board.file}">
            <c:set var="segments" value="${fn:split(board.file, '/')}" />
            <c:set var="fileNameOnly" value="${segments[fn:length(segments)-1]}" />
            <a href="${board.file}" class="download-link" target="_blank" rel="noopener">
              <span>💾</span><span>${fileNameOnly}</span><span>⭳</span>
            </a>
          </c:if>
        </div>

        <!-- 배너 이미지: Azure 공개 URL 바로 사용 -->
        <c:if test="${not empty board.bannerImg}">
          <div class="banner-img">
            <img src="${board.bannerImg}" alt="배너 이미지" loading="lazy" />
          </div>
        </c:if>

        <!-- 본문 -->
        <p style="white-space: pre-line;">${board.content}</p>
      </div>
    </div>

    <!-- Bottom: back & top -->
    <div class="content-box-bottom">
      <div class="content-box-bottom-left"></div>
      <div class="content-box-bottom-right">
        <c:choose>
          <c:when test="${fromPage == 'eventList'}">
            <form action="${contextPath}/bbs/eventList.do" method="get">
              <button type="submit" class="back-list-btn">목록</button>
            </form>
          </c:when>
          <c:otherwise>
            <form action="${contextPath}/bbs/noticeList.do" method="get">
              <button type="submit" class="back-list-btn">목록</button>
            </form>
          </c:otherwise>
        </c:choose>
        <button type="button" onclick="scrollToTop()" class="top-btn">TOP</button>
      </div>
    </div>
  </section>

  <script>
    function scrollToTop(){ window.scrollTo({ top: 0, behavior: 'smooth' }); }

    function fn_remove_board(url, boardId){
      if(!confirm("정말 삭제하시겠어요?")) return;
      fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ boardId: boardId })
      })
      .then(res => { if(!res.ok) throw new Error('HTTP ' + res.status); return res.json(); })
      .then(data => {
        if(data.result === 'success'){
          alert(data.message || "글을 삭제했습니다.");
          window.location.href = data.redirect || "${contextPath}/bbs/noticeList.do";
        } else {
          alert("삭제 중 문제가 발생했습니다."); console.error(data);
        }
      })
      .catch(err => { alert("삭제 요청 오류: " + err.message); console.error(err); });
    }
  </script>
</body>
</html>
