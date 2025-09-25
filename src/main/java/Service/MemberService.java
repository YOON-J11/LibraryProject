package Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import Dao.MemberDao;
import Vo.KakaoTokenResponseVo;
import Vo.KakaoUserInfoVo;
import Vo.MemberVo;
import mail.NaverMailSend;

public class MemberService {
    private MemberDao memberDao;
    private ObjectMapper objectMapper;
    private HttpClient httpClient;

    // --- 카카오 API 관련 상수 ---
    private final String KAKAO_CLIENT_ID = "5a278aa0ad74ca4b39461b7e9208e622";
    private final String KAKAO_CLIENT_SECRET = "";
    private final String KAKAO_TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private final String KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    public MemberService() {
        memberDao = new MemberDao();
        objectMapper = new ObjectMapper();
        httpClient = HttpClient.newBuilder().build();
    }

    public String serviceLoginMember() {
        return "members/login.jsp";
    }

    public int serviceUserCheck(HttpServletRequest request) {
        String login_id = request.getParameter("id");
        String login_pass = request.getParameter("pass");
        if (login_id != null) login_id = login_id.trim();
        if (login_pass != null) login_pass = login_pass.trim();
        return memberDao.userCheck(login_id, login_pass);
    }


    public String serviceJoin(HttpServletRequest request) {
        return "members/join.jsp";
    }

    public Boolean serviceOverLappedId(HttpServletRequest request) {
        String id = request.getParameter("id");
        return memberDao.overlappedId(id);
    }

    public void serviceInsertMember(HttpServletRequest request) {
        String user_id = request.getParameter("id");
        String user_pass = request.getParameter("pass");
        String user_name = request.getParameter("name");
        String user_gender = request.getParameter("gender");
        String user_address = request.getParameter("address");
        String user_email = request.getParameter("email");
        String user_tel = request.getParameter("tel");

        MemberVo vo = new MemberVo(user_id, user_pass, user_name, user_gender, user_address, user_email, user_tel);
        memberDao.insertMember(vo);
    }

    public String serviceMypage(HttpServletRequest request) {
        return "members/mypage.jsp";
    }

    public String servicePassForm(HttpServletRequest request) {
        return "members/pass.jsp";
    }

    public String serviceUserModify(HttpServletRequest request) {
        return "members/modify.jsp";
    }

    public String serviceLeave(HttpServletRequest request) {
        return "members/leave.jsp";
    }

    public String serviceForgotIdform(HttpServletRequest request) {
        return "members/forgotIdForm.jsp";
    }

    public MemberVo serviceForgotId(HttpServletRequest request) {
        String email = request.getParameter("email");
        MemberVo member = memberDao.selectEmail(email);
        if (member != null) {
            System.out.println("찾은 회원 이메일: " + email);
            return member;
        } else {
            System.out.println("입력된 이메일의 회원 정보 없음: " + email);
            return null;
        }
    }

    public String serviceFindIdByEmail(HttpServletRequest request) {
        return "members/fondIdByEmail.jsp";
    }

    public String serviceForgotPwform(HttpServletRequest request) {
        return "members/forgotPwForm.jsp";
    }

    // 회원 정보 조회
    public MemberVo getMember(String id) {
        return memberDao.memberInfo(id);
    }

    // 회원 정보 수정 (마이페이지)
    public int serviceMemUpdate(HttpServletRequest request) {
        MemberVo memberVo = new MemberVo();

        memberVo.setId(request.getParameter("id"));
        memberVo.setPass(request.getParameter("pass"));
        memberVo.setName(request.getParameter("name"));
        memberVo.setGender(request.getParameter("gender"));
        memberVo.setAddress(request.getParameter("address"));
        memberVo.setEmail(request.getParameter("email"));
        memberVo.setTel(request.getParameter("tel"));

        return memberDao.memUpdate(memberVo);
    }

    // 회원 탈퇴
    public String serviceMemDelete(HttpServletRequest request) {
        String id = request.getParameter("id");
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return memberDao.memDelete(id);
    }

    public String serviceMemDeleteAdm(HttpServletRequest request) {
        String id = request.getParameter("id");
        return memberDao.memDelete(id);
    }

    // 카카오 로그인 관련 메소드
    public MemberVo loginOrRegisterKakaoUser(String code, String state, String sessionState, HttpServletRequest request)
            throws IOException, InterruptedException, SecurityException {

        if (sessionState == null || state == null || !sessionState.equals(state)) {
            System.out.println("카카오 로그인 오류: State 값이 일치하지 않습니다!");
            throw new SecurityException("유효하지 않은 접근입니다. (state 불일치)");
        }

        HttpSession session = request.getSession();
        session.removeAttribute("kakao_state");

        String accessToken = getKakaoAccessToken(code, request);
        if (accessToken == null) {
            System.out.println("카카오 로그인 오류: 액세스 토큰 발급에 실패했습니다.");
            return null;
        }

        KakaoUserInfoVo kakaoUserInfo = getKakaoUserInfo(accessToken);
        if (kakaoUserInfo == null || kakaoUserInfo.getId() == null) {
            System.out.println("카카오 로그인 오류: 사용자 정보 조회에 실패했습니다.");
            return null;
        }

        String kakaoId = String.valueOf(kakaoUserInfo.getId());
        MemberVo memberVo = memberDao.findMemberByKakaoId(kakaoId);

        if (memberVo == null) {
            System.out.println("카카오 신규 회원 감지...");
            memberVo = registerNewKakaoMember(kakaoUserInfo);

            if (memberVo == null) {
                System.out.println("카카오 로그인 오류: 신규 회원 등록에 실패했습니다.");
                return null;
            }
            System.out.println("카카오 신규 회원 등록 완료: ID=" + memberVo.getId());

        } else {
            System.out.println("기존 카카오 연동 회원 확인: ID=" + memberVo.getId());
        }

        return memberVo;
    }

    // 카카오 액세스 토큰 발급
    private String getKakaoAccessToken(String code, HttpServletRequest request)
            throws IOException, InterruptedException {
        String redirectUri = generateRedirectUri(request);

        Map<Object, Object> data = new HashMap<>();
        data.put("grant_type", "authorization_code");
        data.put("client_id", KAKAO_CLIENT_ID);
        data.put("redirect_uri", redirectUri);
        data.put("code", code);
        if (KAKAO_CLIENT_SECRET != null && !KAKAO_CLIENT_SECRET.isEmpty()) {
            data.put("client_secret", KAKAO_CLIENT_SECRET);
        }

        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(KAKAO_TOKEN_URI))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(buildFormData(data)))
                .build();

        HttpResponse<String> response = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            try {
                KakaoTokenResponseVo tokenResponse = objectMapper.readValue(response.body(), KakaoTokenResponseVo.class);
                System.out.println("카카오 액세스 토큰 수신 성공.");
                return tokenResponse.getAccessToken();
            } catch (Exception e) {
                System.out.println("카카오 토큰 응답 JSON 파싱 오류: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        } else {
            System.out.println("카카오 토큰 발급 실패. 상태 코드: " + response.statusCode() + ", 응답 본문: " + response.body());
            return null;
        }
    }

    // 카카오 사용자 정보 조회
    private KakaoUserInfoVo getKakaoUserInfo(String accessToken) throws IOException, InterruptedException {
        HttpRequest userInfoRequest = HttpRequest.newBuilder()
                .uri(URI.create(KAKAO_USER_INFO_URI))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(userInfoRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            try {
                KakaoUserInfoVo userInfo = objectMapper.readValue(response.body(), KakaoUserInfoVo.class);
                System.out.println("카카오 사용자 정보 수신 성공: ID=" + userInfo.getId() + ", Email="
                        + (userInfo.getKakaoAccount() != null ? userInfo.getKakaoAccount().getEmail() : "N/A"));
                return userInfo;
            } catch (Exception e) {
                System.out.println("카카오 사용자 정보 응답 JSON 파싱 오류: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        } else {
            System.out.println("카카오 사용자 정보 조회 실패. 상태 코드: " + response.statusCode() + ", 응답 본문: " + response.body());
            return null;
        }
    }

    // 카카오 신규 회원 등록
    private MemberVo registerNewKakaoMember(KakaoUserInfoVo kakaoUserInfo) {
        MemberVo newMember = new MemberVo();
        String kakaoId = String.valueOf(kakaoUserInfo.getId());

        String email = null;
        String nickname = null;

        if (kakaoUserInfo.getKakaoAccount() != null) {
            email = kakaoUserInfo.getKakaoAccount().getEmail();
            if (kakaoUserInfo.getKakaoAccount().getProfile() != null) {
                nickname = kakaoUserInfo.getKakaoAccount().getProfile().getNickname();
            }
        }

        String generatedId = null;

        if (email != null && email.length() <= 12 && !memberDao.overlappedId(email)) {
            generatedId = email;
            System.out.println("카카오 회원 ID 생성: 이메일 사용 -> " + generatedId);
        } else {
            generatedId = "k_" + kakaoId;
            if (generatedId.length() > 12) {
                generatedId = generatedId.substring(0, 12);
            }
            System.out.println("카카오 회원 ID 생성 시도: 카카오ID 기반 -> " + generatedId);
            int tryCount = 0;
            while (memberDao.overlappedId(generatedId) && tryCount < 5) {
                String baseId = generatedId.substring(0, Math.min(11, generatedId.length()));
                generatedId = baseId + (tryCount + 1);
                System.out.println("ID 중복 발생, 재시도 -> " + generatedId);
                tryCount++;
            }
            if (memberDao.overlappedId(generatedId)) {
                System.out.println("카카오 회원 고유 ID 생성 실패: " + kakaoId);
                return null;
            }
        }

        newMember.setId(generatedId);
        newMember.setPass(UUID.randomUUID().toString());
        newMember.setName(nickname != null ? nickname : "카카오회원");
        newMember.setEmail(email);
        newMember.setKakaoId(kakaoId);
        newMember.setGender(null);
        newMember.setAddress(null);
        newMember.setTel(null);

        try {
            memberDao.insertMember(newMember);
            return memberDao.findMemberByKakaoId(kakaoId);
        } catch (Exception e) {
            System.out.println("신규 카카오 회원 DB 삽입 오류: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // --- Helper Methods ---
    private String generateRedirectUri(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String redirectUriPath = contextPath + "/member/kakaoCallback.me";
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String scheme = request.getScheme();
        String portString = "";

        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            portString = ":" + serverPort;
        }
        return scheme + "://" + serverName + portString + redirectUriPath;
        }

    private String buildFormData(Map<Object, Object> data) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) builder.append("&");
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    // 메일 인증번호 받기
    public String serviceForgotPw(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("id") != null) {
            session.invalidate();
            PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('접근 권한이 없습니다.');");
            out.println("location.href='" + request.getContextPath() + "/main.jsp';");
            out.println("</script>");
        }

        String email = request.getParameter("email");
        String id = request.getParameter("id");

        // DAO 직접 호출로 교체 가능하지만, 기존 흐름 유지
        MemberVo member = getEmail(id, email);
        if (member == null || !email.equals(member.getEmail())) {
            PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('회원 정보가 존재하지 않습니다.');");
            out.println("history.back(-1);");
            out.println("</script>");
        } else {
            NaverMailSend sendMail = new NaverMailSend();
            String authenticationCode = sendMail.sendEmail(email);

            session = request.getSession(true);
            session.setAttribute("authenticationCode", authenticationCode);
            request.setAttribute("id", id);
            request.setAttribute("email", email);
        }
        return "/member/pwdChange.do";
    }

    // 아이디와 이메일 조회
    public MemberVo getEmail(String id, String email) {
        return memberDao.selectMember(id, email);
    }

    // 인증코드 이용, 새 비밀번호 변경 페이지 요청
    public void serviceAuthenCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        String authenCode1 = session != null ? (String) session.getAttribute("authenticationCode") : null;
        String newAuthenCode = request.getParameter("newAuthenCode");

        if (authenCode1 != null && authenCode1.equals(newAuthenCode)) {
            String id = request.getParameter("id");
            String newPw = request.getParameter("newPw");

            int updateResult = memberDao.updatePw(id, newPw);
            if (updateResult == 1) {
                out.println("<script>");
                out.println("alert('정상적으로 수정되었습니다. 로그인 페이지로 돌아갑니다.');");
                out.println("location.href='" + request.getContextPath() + "/member/login';");
                out.println("</script>");
            } else {
                out.println("<script>");
                out.println("alert('비밀번호 수정에 실패했습니다.');");
                out.println("history.back(-1);");
                out.println("</script>");
            }
        } else {
            response.setContentType("text/html;charset=utf-8");
            out.println("<script>");
            out.println("alert('잘못된 접근 권한입니다.');");
            out.println("location.href='" + request.getContextPath() + "/index.jsp';");
            out.println("</script>");
        }
    }

    // ------ 관리자용 ------
    public List<MemberVo> serviceMemberSearch(HttpServletRequest request) {
        String searchId = request.getParameter("searchId");
        String searchName = request.getParameter("searchName");
        String searchEmail = request.getParameter("searchEmail");

        Map<String, String> searchCriteria = new HashMap<>();
        if (searchId != null && !searchId.trim().isEmpty()) searchCriteria.put("searchId", searchId.trim());
        if (searchName != null && !searchName.trim().isEmpty()) searchCriteria.put("searchName", searchName.trim());
        if (searchEmail != null && !searchEmail.trim().isEmpty()) searchCriteria.put("searchEmail", searchEmail.trim());

        return memberDao.selectMemberList(searchCriteria);
    }

    public MemberVo serviceGetMember(HttpServletRequest request) {
        String memberId = request.getParameter("id");
        if (memberId == null || memberId.trim().isEmpty()) {
            System.out.println("Service / serviceGetMember: 아이디 파라미터가 누락되었습니다.");
            return null;
        }
        return memberDao.selectMember(memberId.trim());
    }

    // (관리자) 회원 정보 수정
    public boolean serviceUpdateMember(HttpServletRequest request) {
        String id = request.getParameter("id");
        String pass = request.getParameter("pass");
        String name = request.getParameter("name");
        String gender = request.getParameter("gender");
        String address = request.getParameter("address");
        String email = request.getParameter("email");
        String tel = request.getParameter("tel");

        if (id == null || id.trim().isEmpty() ||
            pass == null || pass.trim().isEmpty() ||
            name == null || name.trim().isEmpty()) {
            System.out.println("Service / serviceUpdateMember: 필수 수정 정보 누락");
            return false;
        }

        MemberVo member = new MemberVo();
        member.setId(id.trim());
        member.setPass(pass.trim());
        member.setName(name.trim());
        member.setGender(gender != null ? gender.trim() : null);
        member.setAddress(address != null ? address.trim() : null);
        member.setEmail(email != null ? email.trim() : null);
        member.setTel(tel != null ? tel.trim() : null);

        int updateCount = memberDao.updateMember(member);
        return updateCount > 0;
    }

    public List<MemberVo> getRecentMembers() {
        return this.memberDao.selectRecentMembers();
    }
}
