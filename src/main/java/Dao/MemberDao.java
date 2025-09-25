package Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Vo.MemberVo;

public class MemberDao {
    Connection con;
    PreparedStatement pstmt;
    ResultSet rs;
    
    private Connection tryConnect(String url, String user, String pass) throws SQLException {
        Connection c = java.sql.DriverManager.getConnection(url, user, pass);

        try (PreparedStatement ps = c.prepareStatement("SELECT DATABASE() db, CURRENT_USER() cu");
             ResultSet r = ps.executeQuery()) {
            if (r.next()) {
                System.out.println("[DB DEBUG] db=" + r.getString("db") + ", user=" + r.getString("cu"));
            }
        }
        return c;
    }
    

    private Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        // 라이브러리 프로젝트 DB
        final String url  = "jdbc:mysql://jiwonserver2.mysql.database.azure.com:3306/library_project"
            + "?useSSL=true&requireSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true";

        final String userPlain = "yjw5619";
        final String userWithServer = "yjw5619@jiwonserver2"; // 폴백용
        final String pass = "wl184545W";

        try {

            System.out.println("[DB TRY] user=" + userPlain);
            return tryConnect(url, userPlain, pass);
        } catch (java.sql.SQLException e) {

            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("Access denied")) {
                System.out.println("[DB TRY] Access denied with '" + userPlain + "', retry with '" + userWithServer + "'");

                return tryConnect(url, userWithServer, pass);
            }
            throw e; // 다른 예외는 그대로
        }
    }

    // 모든 회원 정보 조회 (관리자용)
    public List<MemberVo> selectAllMembers() {
        List<MemberVo> memberList = new ArrayList<>();
        String sql = "select * from member order by joinDate desc";
        try {
            con = this.getConnection();
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                MemberVo memberVo = new MemberVo();
                memberVo.setId(rs.getString("id"));
                memberVo.setPass("********"); // 마스킹처리
                memberVo.setName(rs.getString("name"));
                memberVo.setGender(rs.getString("gender"));
                memberVo.setAddress(rs.getString("address"));
                memberVo.setEmail(rs.getString("email"));
                memberVo.setTel(rs.getString("tel"));
                memberVo.setJoinDate(rs.getDate("joinDate"));
                memberVo.setKakaoId(rs.getString("kakao_id"));
                memberList.add(memberVo);
            }
        } catch (Exception e) {
            System.out.println("selectAllMembers 메소드 내부에서 오류!");
            e.printStackTrace();
        } finally {
            ResourceClose();
        }
        return memberList;
    }

    // 회원가입 시 아이디 중복 확인
    public Boolean overlappedId(String id) {
        boolean result = false;
        String sql = "select 1 from member where id = ?";
        try {
            con = this.getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) result = true;
        } catch (Exception e) {
            System.out.println("MemberDAO.overlappedId() 메소드 오류: " + e);
            e.printStackTrace();
        } finally {
            ResourceClose();
        }
        return result;
    }

    // 회원 추가 (회원가입)
    public void insertMember(MemberVo vo) {
        try {
            con = this.getConnection();
            String sql = "insert into member(id, pass, name, gender, address, email, tel, joinDate, kakao_id) "
                       + "values(?,?,?,?,?,?,?,?,?)";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, vo.getId());
            pstmt.setString(2, vo.getPass());
            pstmt.setString(3, vo.getName());
            pstmt.setString(4, vo.getGender());
            pstmt.setString(5, vo.getAddress());
            pstmt.setString(6, vo.getEmail());
            pstmt.setString(7, vo.getTel());
            pstmt.setTimestamp(8, new java.sql.Timestamp(System.currentTimeMillis())); // joinDate = NOW()
            pstmt.setString(9, vo.getKakaoId());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("insertMember 메소드 내부에서 오류!");
            e.printStackTrace();
        } finally {
            ResourceClose();
        }
    }

    
    public int userCheck(String login_id, String login_pass) {
        int check = -1;
        try {
            con = this.getConnection();
            String sql = "SELECT pass FROM member WHERE id=?";
            pstmt = con.prepareStatement(sql);
            String safeId = (login_id == null) ? "" : login_id.trim();
            pstmt.setString(1, safeId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbPass = rs.getString("pass");
                boolean passMatch = (login_pass != null && login_pass.trim().equals(dbPass));
                System.out.println("[LOGIN DEBUG] id found=" + safeId + ", passMatch=" + passMatch);
                check = passMatch ? 1 : 0;
            } else {
                System.out.println("[LOGIN DEBUG] id not found=" + safeId);
                check = -1;
            }
        } catch (Exception e) {
            System.out.println("MemberDAO.userCheck() 오류: " + e);
            e.printStackTrace();
        } finally {
            ResourceClose();
        }
        return check;
    }

    // 회원정보 수정에 불러올 vo
    public MemberVo memberInfo(String id) {
        MemberVo memberVo = null;
        try {
            con = this.getConnection();
            String sql = "select * from member where id=?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                memberVo = new MemberVo();
                memberVo.setId(rs.getString("id"));
                memberVo.setPass("********");
                memberVo.setName(rs.getString("name"));
                memberVo.setGender(rs.getString("gender"));
                memberVo.setAddress(rs.getString("address"));
                memberVo.setEmail(rs.getString("email"));
                memberVo.setTel(rs.getString("tel"));
                memberVo.setJoinDate(rs.getDate("joinDate"));
                memberVo.setKakaoId(rs.getString("kakao_id"));
            }
        } catch (Exception e) {
            System.out.println("MemberDao.memberInfo() 메소드 오류 : " + e);
        } finally {
            ResourceClose();
        }
        return memberVo;
    }

    // 회원정보 수정 요청 (마이페이지)
    public int memUpdate(MemberVo memberVo) {
        int result = 0;
        try {
            con = this.getConnection();
            String sql = "update member set pass=?, gender=?, address=?, email=?, tel=? where id=?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberVo.getPass());
            pstmt.setString(2, memberVo.getGender());
            pstmt.setString(3, memberVo.getAddress());
            pstmt.setString(4, memberVo.getEmail());
            pstmt.setString(5, memberVo.getTel());
            pstmt.setString(6, memberVo.getId());
            result = pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("MemberDao.memUpdate() 메소드 오류 : " + e);
        } finally {
            ResourceClose();
        }
        return result;
    }

    // 회원 탈퇴(삭제)
    public String memDelete(String id) {
        String result = null;
        try {
            con = this.getConnection();
            String sql = "delete from member where id=?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, id);
            int val = pstmt.executeUpdate();

            if (val == 1) {
                result = (id != null && id.equals("admin")) ? "관리자 계정 삭제 성공" : "회원 탈퇴 성공";
            } else {
                result = "탈퇴실패";
            }
        } catch (Exception e) {
            System.out.println("MemberDao.memDelete() 메소드 오류 : " + e);
            result = "회원 삭제 처리 중 시스템 오류 발생";
            e.printStackTrace();
        } finally {
            ResourceClose();
        }
        return result;
    }

    // 아이디+이메일로 존재 확인
    public MemberVo selectMember(String id, String email) {
        MemberVo memberVo = null;
        try {
            con = this.getConnection();
            String sql = "select id, email from member where id=? and email=?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.setString(2, email);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                memberVo = new MemberVo();
                memberVo.setId(id);
                memberVo.setEmail(email);
            }
        } catch (Exception e) {
            System.out.println("MemberDao / selectMember(id,email) 메소드 오류 : " + e);
        } finally {
            ResourceClose();
        }
        return memberVo;
    }

    // 이메일로 아이디 조회
    public MemberVo selectEmail(String email) {
        MemberVo memberVo = null;
        try {
            con = this.getConnection();
            String sql = "select id, email from member where email=?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, email);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                memberVo = new MemberVo();
                memberVo.setId(rs.getString("id"));
                memberVo.setEmail(email);
            }
        } catch (Exception e) {
            System.out.println("MemberDao / selectEmail 메소드 오류 : " + e);
        } finally {
            ResourceClose();
        }
        return memberVo;
    }

    // 카카오 ID를 사용하여 회원 정보를 조회
    public MemberVo findMemberByKakaoId(String kakaoId) {
        MemberVo memberVo = null;
        String sql = "SELECT * FROM member WHERE kakao_id = ?";
        try {
            con = this.getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, kakaoId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                memberVo = new MemberVo();
                memberVo.setId(rs.getString("id"));
                memberVo.setPass(rs.getString("pass"));
                memberVo.setName(rs.getString("name"));
                memberVo.setGender(rs.getString("gender"));
                memberVo.setAddress(rs.getString("address"));
                memberVo.setEmail(rs.getString("email"));
                memberVo.setTel(rs.getString("tel"));
                memberVo.setKakaoId(rs.getString("kakao_id"));
            }
        } catch (Exception e) {
            System.out.println("MemberDAO.findMemberByKakaoId() 메소드 오류: " + e);
            e.printStackTrace();
        } finally {
            ResourceClose();
        }
        return memberVo;
    }

    // 비밀번호 수정
    public int updatePw(String id, String newPw) {
        int updateResult = 0;
        try {
            con = this.getConnection();
            String sql = "update member set pass=? where id=?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, newPw);
            pstmt.setString(2, id);
            updateResult = pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("MemberDAO.updatePw() 메소드 오류 : " + e);
        } finally {
            ResourceClose();
        }
        return updateResult;
    }

    // ---- 관리자 페이지 검색
    public List<MemberVo> selectMemberList(Map<String, String> searchCriteria) {
        List<MemberVo> memberList = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = this.getConnection();
            StringBuilder sql = new StringBuilder(
                "SELECT id, pass, name, gender, address, email, tel, joinDate, kakao_id FROM member"
            );
            List<Object> params = new ArrayList<>();
            boolean whereAdded = false;

            if (searchCriteria.containsKey("searchId") && !searchCriteria.get("searchId").isEmpty()) {
                sql.append(" WHERE id LIKE ?");
                params.add("%" + searchCriteria.get("searchId") + "%");
                whereAdded = true;
            }
            if (searchCriteria.containsKey("searchName") && !searchCriteria.get("searchName").isEmpty()) {
                sql.append(whereAdded ? " AND" : " WHERE").append(" name LIKE ?");
                params.add("%" + searchCriteria.get("searchName") + "%");
                whereAdded = true;
            }
            if (searchCriteria.containsKey("searchEmail") && !searchCriteria.get("searchEmail").isEmpty()) {
                sql.append(whereAdded ? " AND" : " WHERE").append(" email LIKE ?");
                params.add("%" + searchCriteria.get("searchEmail") + "%");
            }

            sql.append(" ORDER BY joinDate DESC");
            System.out.println("DAO Search SQL: " + sql);

            pstmt = con.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            rs = pstmt.executeQuery();

            while (rs.next()) {
                MemberVo member = new MemberVo();
                member.setId(rs.getString("id"));
                member.setPass(rs.getString("pass"));
                member.setName(rs.getString("name"));
                member.setGender(rs.getString("gender"));
                member.setAddress(rs.getString("address"));
                member.setEmail(rs.getString("email"));
                member.setTel(rs.getString("tel"));
                member.setJoinDate(rs.getDate("joinDate"));
                member.setKakaoId(rs.getString("kakao_id"));
                memberList.add(member);
            }
        } catch (Exception e) {
            System.out.println("MemberDao / selectMemberList 메소드 오류: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (pstmt != null) pstmt.close(); } catch (SQLException ignored) {}
            try { if (con != null) con.close(); } catch (SQLException ignored) {}
        }
        return memberList;
    }

    // 회원 한 명 정보 가져오기 (아이디)
    public MemberVo selectMember(String memberId) {
        MemberVo memberVo = null;
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = this.getConnection();
            String sql = "SELECT id, pass, name, gender, address, email, tel, joinDate, kakao_id FROM member WHERE id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                memberVo = new MemberVo();
                memberVo.setId(rs.getString("id"));
                memberVo.setPass(rs.getString("pass"));
                memberVo.setName(rs.getString("name"));
                memberVo.setGender(rs.getString("gender"));
                memberVo.setAddress(rs.getString("address"));
                memberVo.setEmail(rs.getString("email"));
                memberVo.setTel(rs.getString("tel"));
                memberVo.setJoinDate(rs.getDate("joinDate"));
                memberVo.setKakaoId(rs.getString("kakao_id"));
            }
        } catch (Exception e) {
            System.out.println("MemberDao / selectMember(id) 메소드 오류: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (pstmt != null) pstmt.close(); } catch (SQLException ignored) {}
            try { if (con != null) con.close(); } catch (SQLException ignored) {}
        }
        return memberVo;
    }

    // 회원 정보 수정 메소드 (관리자용 업데이트)
    public int updateMember(MemberVo member) {
        int updateCount = 0;
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = this.getConnection();
            String sql = "UPDATE member SET pass=?, name=?, gender=?, address=?, email=?, tel=? WHERE id=?";
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, member.getPass());
            pstmt.setString(2, member.getName());
            pstmt.setString(3, member.getGender());
            pstmt.setString(4, member.getAddress());
            pstmt.setString(5, member.getEmail());
            pstmt.setString(6, member.getTel());
            pstmt.setString(7, member.getId());

            updateCount = pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("MemberDao / updateMember 메소드 오류: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (pstmt != null) pstmt.close(); } catch (SQLException ignored) {}
            try { if (con != null) con.close(); } catch (SQLException ignored) {}
        }
        return updateCount;
    }

    // 최근 7일 이내 가입한 회원
    public List<MemberVo> selectRecentMembers() {
        List<MemberVo> recentMemberList = new ArrayList<>();
        String sql = "SELECT * FROM member WHERE joinDate >= (NOW() - INTERVAL 7 DAY) ORDER BY joinDate DESC";
        try {
            con = this.getConnection();
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                MemberVo memberVo = new MemberVo();
                memberVo.setId(rs.getString("id"));
                memberVo.setPass("********");
                memberVo.setName(rs.getString("name"));
                memberVo.setGender(rs.getString("gender"));
                memberVo.setAddress(rs.getString("address"));
                memberVo.setEmail(rs.getString("email"));
                memberVo.setTel(rs.getString("tel"));
                memberVo.setJoinDate(rs.getDate("joinDate"));
                memberVo.setKakaoId(rs.getString("kakao_id"));
                recentMemberList.add(memberVo);
            }
        } catch (Exception e) {
            System.out.println("selectRecentMembers 메소드 내부에서 오류!");
            e.printStackTrace();
        } finally {
            ResourceClose();
        }
        System.out.println(">>> [DAO] selectRecentMembers 메소드 종료. 반환 리스트 크기: " + recentMemberList.size());
        return recentMemberList;
    }

    // 자원 해제 (필드 자원용)
    public void ResourceClose() {
        try { if (pstmt != null) pstmt.close(); } catch (Exception e) { e.printStackTrace(); }
        try { if (rs != null) rs.close(); } catch (Exception e) { e.printStackTrace(); }
        try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
    }
}
