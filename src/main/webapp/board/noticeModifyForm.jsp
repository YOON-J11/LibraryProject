<%@page import="Vo.boardVO"%>
<%@page import="java.util.Vector"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<%
request.setCharacterEncoding("UTF-8");
String contextPath = request.getContextPath();
String id = (String) session.getAttribute("id");
if (id == null || !"admin".equals(id)) {
%>
<script>
  alert("접근 권한이 없습니다.");
  history.back();
</script>
<%
  return;
}
%>

<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<html>
<head>
<title>공지사항 글수정 - noticeModifyForm.jsp</title>
<script src="https://code.jquery.com/jquery-latest.min.js"></script>
<style>
/* noticeWrite.jsp와 동일한 톤 */
.wri-mod-form {
  width:100%;
  max-width: 800px;
  margin: 20px auto;
  background-color: #fff;
  border-radius: 8px;
}
.form-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #003c83;
}
.form-title h2 {
  margin: 0;
  color: #003c83;
  font-size: 24px;
  font-weight: bold;
}
input[type="text"], textarea {
  width: 100%;
  padding: 10px;
  margin-bottom: 15px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 1rem;
  box-sizing: border-box;
  resize: vertical;
}
textarea { min-height: 500px; }

input[type="button"], input[type="submit"], button[type="button"] {
  padding: 8px 18px;
  color: #fff;
  cursor: pointer;
}
input[type="submit"] {
  background-color: #003c83; color: #fff;
}
input[type="submit"]:hover { background-color: #002c66; }

input[type="button"] {
  background-color: #f4f4f4; color: #424242;
}
input[type="button"]:hover { background-color: #f2f2f2; }

button[type="button"] {
  background-color: #f4f4f4; color: #424242;
  font-size: 0.9rem;
  padding: 8px 18px;
  margin-left: 10px;
  vertical-align: middle;
  border: 1px solid #dedede;
  border-radius: 4px;
}
button[type="button"]:hover { background-color: #f2f2f2; }

.file-group {
  margin-bottom: 15px;
  border: 1px dashed #ccc;
  padding: 15px;
  border-radius: 4px;
  background-color: #f9f9f9;
}
.file-group p {
  margin-top: 0; margin-bottom: 5px;
  font-size: 0.9rem; color: #555;
}

.file-upload-label {
  display: inline-block;
  padding: 8px 15px;
  background-color: #003c83; color: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color .3s ease;
  margin-right: 10px; vertical-align: middle;
}
.file-upload-label:hover { background-color: #002c66; }

.file-input { display: none; }
.file-name {
  font-weight: bold; color: #555; vertical-align: middle;
}

#bannerPreview img {
  max-width: 100%; height: auto; display: block;
  margin-top: 10px; border: 1px solid #ddd; padding: 5px; background: #fff;
}
</style>
</head>
<body>
<section class="wri-mod-form">
  <form name="noticeModifyForm" method="post"
        action="${contextPath}/bbs/noticeModify.do"
        enctype="multipart/form-data">
    <div class="form-title">
      <h2>공지사항 글 수정</h2>
      <div>
        <input type="button" value="취소"
               onclick="location.href='${contextPath}/bbs/noticeInfo.do?boardId=${board.boardId}'">
        <input type="submit" value="수정">
      </div>
    </div>

    <!-- 제목 -->
    <input type="text" name="title" placeholder="제목을 입력하세요"
           value="${board.title}" required>

    <!-- 첨부파일 -->
    <div class="file-group">
      <label for="file" class="file-upload-label">첨부파일 업로드</label>
      <span class="file-name" id="fileName">
        <c:choose>
          <c:when test="${not empty board.file}">
            <!-- board.file은 Azure URL일 가능성이 높음: 전체 링크를 그대로 노출 -->
            <a href="${board.file}" target="_blank" style="text-decoration:none;color:#555;">
              ${board.file}
            </a>
          </c:when>
          <c:otherwise>선택된 파일 없음</c:otherwise>
        </c:choose>
      </span>
      <button type="button" id="deleteFileBtn"
              style="<c:if test='${empty board.file}'>display:none;</c:if>">
        첨부파일 삭제
      </button>
      <input type="file" name="file" id="file" class="file-input">
    </div>

    <!-- 내용 -->
    <textarea name="content" placeholder="내용을 입력하세요" required>${board.content}</textarea>

    <!-- 배너 이미지 -->
    <div class="file-group">
      <p>배너 이미지를 등록하면 행사안내 게시판과 메인슬라이드에 노출됩니다.</p>
      <p>배너 이미지 권장 사이즈 1200px * 900px (4:3)</p>

      <label for="bannerImage" class="file-upload-label">배너이미지 업로드</label>
      <span class="file-name" id="bannerFileName">
        <c:choose>
          <c:when test="${not empty board.bannerImg}">
            <!-- 배너는 이미지 URL로 바로 보기 -->
            ${board.bannerImg}
          </c:when>
          <c:otherwise>선택된 파일 없음</c:otherwise>
        </c:choose>
      </span>
      <input type="file" name="bannerImage" id="bannerImage" class="file-input" accept="image/*">
      <button type="button" id="deleteBannerBtn"
              style="<c:if test='${empty board.bannerImg}'>display:none;</c:if>">
        배너이미지 삭제
      </button>

      <div id="bannerPreview">
        <c:if test="${not empty board.bannerImg}">
          <!-- Azure URL을 직접 미리보기 -->
          <img src="${board.bannerImg}" alt="배너 미리보기">
        </c:if>
      </div>
    </div>

    <!-- hidden -->
    <input type="hidden" name="boardId" value="${board.boardId}">
    <input type="hidden" name="originalFileName" value="${board.file}">
    <input type="hidden" name="originalBannerName" value="${board.bannerImg}">
    <input type="hidden" id="deleteFile" name="deleteFile" value="false">
    <input type="hidden" id="deleteBanner" name="deleteBanner" value="false">
  </form>
</section>

<script>
// 공통 삭제 처리
function deleteFile(fileInputId, fileNameSpanId, deleteBtnId, previewId) {
  const fileInput = document.getElementById(fileInputId);
  const fileNameSpan = document.getElementById(fileNameSpanId);
  const deleteBtn = document.getElementById(deleteBtnId);

  fileInput.value = '';
  fileNameSpan.textContent = '선택된 파일 없음';
  deleteBtn.style.display = 'none';

  if (previewId) {
    const previewContainer = document.getElementById(previewId);
    if (previewContainer) previewContainer.innerHTML = '';
  }
  if (fileInputId === 'file') {
    document.getElementById('deleteFile').value = 'true';
  } else if (fileInputId === 'bannerImage') {
    document.getElementById('deleteBanner').value = 'true';
  }
}

// 파일 선택 시 UI 반영
function setupFileNameDisplay(inputId, spanId, buttonId) {
  const input = document.getElementById(inputId);
  const span = document.getElementById(spanId);
  const button = document.getElementById(buttonId);

  input.addEventListener('change', function() {
    if (input.files.length > 0) {
      span.textContent = input.files[0].name;
      button.style.display = 'inline-block';
    } else {
      span.textContent = '선택된 파일 없음';
      button.style.display = 'none';
    }
    // 새 파일 선택 시 삭제 플래그 해제
    if (inputId === 'file') document.getElementById('deleteFile').value = 'false';
    if (inputId === 'bannerImage') document.getElementById('deleteBanner').value = 'false';

    if (inputId === 'bannerImage') readURL(input);
  });
}

// 배너 미리보기
function readURL(input) {
  if (input.files && input.files[0]) {
    const reader = new FileReader();
    reader.onload = function(e) {
      const preview = document.getElementById('bannerPreview');
      preview.innerHTML = '';
      const img = document.createElement('img');
      img.src = e.target.result;
      img.style.maxWidth = '100%';
      img.style.height = 'auto';
      img.style.border = '1px solid #ddd';
      img.style.marginTop = '10px';
      img.style.padding = '5px';
      preview.appendChild(img);
    };
    reader.readAsDataURL(input.files[0]);
  }
}

document.addEventListener('DOMContentLoaded', function() {
  setupFileNameDisplay('file', 'fileName', 'deleteFileBtn');
  setupFileNameDisplay('bannerImage', 'bannerFileName', 'deleteBannerBtn');

  const deleteFileBtn = document.getElementById('deleteFileBtn');
  if (deleteFileBtn) {
    deleteFileBtn.addEventListener('click', function() {
      deleteFile('file', 'fileName', 'deleteFileBtn');
    });
    // 기존 파일 있으면 삭제 버튼 보이기
    const fileNameSpan = document.getElementById('fileName');
    if (fileNameSpan && fileNameSpan.textContent.trim() !== '선택된 파일 없음' && fileNameSpan.textContent.trim() !== '') {
      deleteFileBtn.style.display = 'inline-block';
    }
  }

  const deleteBannerBtn = document.getElementById('deleteBannerBtn');
  if (deleteBannerBtn) {
    deleteBannerBtn.addEventListener('click', function() {
      deleteFile('bannerImage', 'bannerFileName', 'deleteBannerBtn', 'bannerPreview');
    });
    const bannerNameSpan = document.getElementById('bannerFileName');
    if (bannerNameSpan && bannerNameSpan.textContent.trim() !== '선택된 파일 없음' && bannerNameSpan.textContent.trim() !== '') {
      deleteBannerBtn.style.display = 'inline-block';
    }
  }
});
</script>
</body>
</html>
