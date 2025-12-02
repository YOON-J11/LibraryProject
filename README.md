# 📚 도서관 웹 사이트 제작 프로젝트 (Library Web Project)

Java Servlet & JSP 기반의 도서관 사이트입니다.  
게시판 중심 기능을 포함하여 웹 애플리케이션의 주요 기능들을 직접 설계하고 구현하였습니다.

<div style="border:4px solid #003366; display:inline-block; padding:4px;">
  <img width="2416" height="1794" alt="image" src="https://github.com/user-attachments/assets/7ce10769-0f55-4e15-bfa4-dd923135eae9" />
</div>

---

## 📝 프로젝트 개요

- **프로젝트명**: 도서관 웹 사이트  
- **개발 기간**: 2025.04.23 ~ 2025.05.23 (1개월)  
- **참여 인원**: 4명  
- 🌐 실제 서비스 링크: [https://cinemoa.yoonj11.site  ](https://library.yoonj11.site/view/main)
- ☁️ Azure App Service와 MySQL(Azure Database for MySQL)을 기반으로 **클라우드 환경에 배포**되어 있습니다.

👉 위 주소로 접속하면 실제 배포된 서비스를 바로 체험할 수 있습니다.

---

## 🌟 개발 환경

### 👀 Frontend  
  ![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white)
  ![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white)
  ![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
  ![JQuery](https://img.shields.io/badge/JQuery-0769AD?style=for-the-badge&logo=jquery&logoColor=white)  

### 🧠 Backend
  ![Apache Tomcat](https://img.shields.io/badge/Tomcat-005571?style=for-the-badge&logo=apachetomcat&logoColor=white)
  ![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)
  ![JSP](https://img.shields.io/badge/JSP-007396?style=for-the-badge&logo=java&logoColor=white)
  ![Servlet](https://img.shields.io/badge/Servlet-2C2255?style=for-the-badge&logo=java&logoColor=white)
  ![JDK](https://img.shields.io/badge/JDK-007396?style=for-the-badge&logo=java&logoColor=white)  

### 🛢 Database
  ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)  
  - Azure MySQL Database를 이용하여 클라우드 환경에서 데이터 관리  
  - 서버 배포 후에도 동일한 DB를 사용하도록 연동

### ☁️ 클라우드 / 저장소
  ![Azure](https://img.shields.io/badge/Microsoft_Azure-0089D6?style=for-the-badge&logo=microsoftazure&logoColor=white)  
  - **Azure Web App**으로 서버를 배포  
  - **Azure Blob Storage**를 연동하여 게시글 첨부파일 및 배너 이미지를 클라우드에 저장  
  - 서버와 스토리지를 분리하여 관리 및 성능 효율성 확보

### 🧰 개발 도구
  ![Eclipse](https://img.shields.io/badge/Eclipse-2C2255?style=for-the-badge&logo=eclipse&logoColor=white)
  ![SQL Developer](https://img.shields.io/badge/SQL_Developer-0F4B7F?style=for-the-badge&logo=oracle&logoColor=white)
  ![Git](https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white)  

---

## 📦 DB 구성

아래는 프로젝트에서 사용된 데이터베이스 테이블 간 관계(ERD)입니다.

<div style="border:4px solid #003366; display:inline-block; padding:4px;">
  <img width="1236" height="965" alt="image" src="https://github.com/user-attachments/assets/216b9f76-128c-4d05-b6be-cbd2ee9fdc87" />
</div>

---

## 👩‍💻 담당 업무 (윤지원)

### 📌 게시판 모듈 전체 구현  
공지사항, 서평, 문의, 배너 게시판 등 전체 게시판 기능을 기획하고 구현하였습니다.

<div style="border:4px solid #003366; display:inline-block; padding:4px;">
  <img width="2262" height="1591" alt="image" src="https://github.com/user-attachments/assets/44604435-172a-41c8-948c-5815ecf84abe" />
</div>

<div style="border:4px solid #003366; display:inline-block; padding:4px;">
  <img width="2690" height="3010" alt="image" src="https://github.com/user-attachments/assets/183b3162-fac6-47d5-bbf4-b3e9fb942cf7" />
</div>

<div style="border:4px solid #003366; display:inline-block; padding:4px;">
  <img width="2313" height="1600" alt="image" src="https://github.com/user-attachments/assets/02cf254e-aa4e-4046-af94-987dd0e1f332" />
</div>

---

### 📌 게시글 등록 / 수정 / 삭제 / 조회 기능 구현

<div style="border:4px solid #003366; display:inline-block; padding:4px;">
  <img width="1311" height="1305" alt="image" src="https://github.com/user-attachments/assets/69ac37ef-62b0-476d-b2f0-050c6b8b7dbe" />
</div>

<div style="border:4px solid #003366; display:inline-block; padding:4px;">
  <img width="1263" height="1641" alt="image" src="https://github.com/user-attachments/assets/b2c821ce-dfb7-4c10-980d-1f96e0b1015e" />
</div>

---

### 📌 게시판별 리스트 & 상세 페이지 및 데이터 연동 처리


---

### 📌 첨부파일 업로드 기능 구현  
- 서버 로컬 저장 대신 **Azure Blob Storage**에 저장되도록 구현  
- 업로드된 파일은 공용 URL로 접근 가능하며, 게시글에서 다운로드 및 미리보기 지원

---

### 📌 메인 페이지에 최신 게시글 5건 노출

<div style="border:4px solid #003366; display:inline-block; padding:4px;">
 <img width="1433" height="499" alt="image" src="https://github.com/user-attachments/assets/58deaaef-7043-493c-9ad5-e5cd9f1dfc14" />
</div>

---

### 📌 게시판용 테이블 설계 및 DB 연동  
- MySQL 기반 테이블 설계 및 CRUD 구현  
- Azure MySQL Database를 사용하여 클라우드 환경에서 데이터 관리

---

### 📌 JSP/Servlet 기반 아키텍처 설계 및 유지보수  
- MVC 구조 내에서 게시판 기능 중심의 흐름 설계 및 유지

---

## 📄 프로젝트 소개서

팀원별 기능, DB 설계, 전체 흐름 등을 정리한 프로젝트 발표 자료입니다.  
아래 버튼을 눌러 PDF 파일을 다운로드할 수 있습니다.

[📥 PDF 다운로드](https://github.com/YOON-J11/LibraryProject/blob/main/도서관프로젝트ppt.pdf?raw=1)

---

✅ **배포된 웹사이트 보기:**
