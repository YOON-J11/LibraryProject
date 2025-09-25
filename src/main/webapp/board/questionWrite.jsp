<%@page import="Vo.boardVO"%>
<%@page import="java.util.Vector"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<%
request.setCharacterEncoding("UTF-8");
String contextPath = request.getContextPath();
String id = (String) session.getAttribute("id");
if (id == null) {
%>
<script>
  alert("로그인이 필요한 페이지입니다.");
  history.back();
</script>
<%
  return;
}
%>

<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<html>
<head>
  <title>문의글 글쓰기 - questionWrite.jsp</title>
  <script src="https://code.jquery.com/jquery-latest.min.js"></script>
  <style>
    .wri-mod-form {
      width:100%;
      max-width: 800px;
      margin: 20px auto;
      background-color: #fff;
      border-radius: 8px;
    }
    .form-title {
      display:flex;
      justify-content:space-between;
      align-items:center;
      margin-bottom:20px;
      padding-bottom:15px;
      border-bottom:2px solid #003c83;
    }
    .form-title h2 {
      margin:0;
      color:#003c83;
      font-size:24px;
      font-weight:bold;
    }
    input[type="text"], textarea {
      width:100%;
      padding:10px;
      margin-bottom:15px;
      border:1px solid #ccc;
      border-radius:4px;
      font-size:1rem;
      box-sizing:border-box;
      resize:vertical;
    }
    textarea { min-height:500px; }

    input[type="button"], input[type="submit"], button[type="button"] {
      padding:8px 18px;
      color:#fff;
      cursor:pointer;
    }
    input[type="submit"] { background-color:#003c83; color:#fff; }
    input[type="submit"]:hover { background-color:#002c66; }

    input[type="button"] { background:#f4f4f4; color:#424242; }
    input[type="button"]:hover { background:#f2f2f2; }

    button[type="button"] {
      background:#f4f4f4; color:#424242;
      font-size:.9rem; padding:8px 18px; margin-left:10px; vertical-align:middle;
      border:1px solid #dedede; border-radius:4px;
    }
    button[type="button"]:hover { background:#f2f2f2; }

    .file-group {
      margin-bottom:15px;
      border:1px dashed #ccc;
      padding:15px;
      border-radius:4px;
      background-color:#f9f9f9;
    }
    .file-group p {
      margin:0 0 5px 0;
      font-size:.9rem; color:#555;
    }
    .file-upload-label {
      display:inline-block; padding:8px 15px;
      background:#003c83; color:#fff; border-radius:4px; cursor:pointer;
      transition:background-color .3s ease; margin-right:10px; vertical-align:middle;
    }
    .file-upload-label:hover { background:#002c66; }
    .file-input { display:none; }
    .file-name { font-weight:bold; color:#555; vertical-align:middle; }

    .checkbox-group {
      margin-top:15px; margin-bottom:20px; padding:10px;
      background:#e9ecef; border-radius:4px; display:inline-block;
    }
    .checkbox-label { cursor:pointer; font-size:.95rem; color:#495057; }
    .checkbox-label input[type="checkbox"] { margin-right:5px; vertical-align:middle; }
  </style>
</head>
<body>
  <section class="wri-mod-form">
    <form name="questionWriteForm"
          method="post"
          action="${contextPath}/bbs/AddQuestion.do"
          enctype="multipart/form-data">
      <div class="form-title">
        <h2>문의글 글쓰기</h2>
        <div>
          <input type="button" value="취소" onclick="history.back();">
          <input type="submit" value="등록">
        </div>
      </div>

      <!-- 제목 -->
      <input type="text" name="title" placeholder="제목을 입력하세요" required>

      <!-- 첨부파일 -->
      <div class="file-group">
        <label for="file" class="file-upload-label">첨부파일 업로드</label>
        <span class="file-name" id="fileName">선택된 파일 없음</span>
        <input type="file" name="file" id="file" class="file-input">
        <button type="button" id="deleteFileBtn"
                onclick="deleteFile('file','fileName','deleteFileBtn')"
                style="display:none;">첨부파일 삭제</button>
      </div>

      <!-- 내용 -->
      <textarea name="content" placeholder="내용을 입력하세요" required></textarea>

      <!-- 비밀글 -->
      <div class="checkbox-group">
        <label for="secret" class="checkbox-label">
          <input type="checkbox" name="secret" id="secret">
          비밀글로 설정
        </label>
      </div>
    </form>
  </section>

  <script>
    function deleteFile(fileInputId, fileNameSpanId, deleteBtnId) {
      const fileInput = document.getElementById(fileInputId);
      const fileNameSpan = document.getElementById(fileNameSpanId);
      const deleteBtn = document.getElementById(deleteBtnId);
      fileInput.value = '';
      fileNameSpan.textContent = '선택된 파일 없음';
      deleteBtn.style.display = 'none';
    }

    function setupFileNameDisplay(inputId, spanId, buttonId) {
      const input = document.getElementById(inputId);
      const span = document.getElementById(spanId);
      const button = document.getElementById(buttonId);

      input.addEventListener('change', function () {
        if (input.files.length > 0) {
          span.textContent = input.files[0].name;
          if (button) button.style.display = 'inline-block';
        } else {
          span.textContent = '선택된 파일 없음';
          if (button) button.style.display = 'none';
        }
      });
    }

    document.addEventListener('DOMContentLoaded', function() {
      setupFileNameDisplay('file', 'fileName', 'deleteFileBtn');
    });
  </script>
</body>
</html>
