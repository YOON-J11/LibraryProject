package Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import Vo.boardVO;

public class boardDAO {
    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    public boardDAO() {}

    /** 공통 게시글 목록 (검색/페이징) */
    public List<boardVO> getBoardList(int category, int startRow, int endRow,
                                      String searchKeyword, String searchType, String currentUserId) {
        List<boardVO> boardList = new ArrayList<>();
        try {
            con = DbcpBean.getConnection();

            StringBuilder sb = new StringBuilder("SELECT * FROM board WHERE category = ? ");
            boolean useUserFilter = (category == 2 && currentUserId != null && !currentUserId.isEmpty());
            if (useUserFilter) sb.append(" AND user_id = ? ");
            if (searchType != null && !searchType.isEmpty()) sb.append(" AND ").append(searchType).append(" LIKE ? ");
            sb.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

            pstmt = con.prepareStatement(sb.toString());
            int i = 1;
            pstmt.setInt(i++, category);
            if (useUserFilter) pstmt.setString(i++, currentUserId);
            pstmt.setString(i++, "%" + searchKeyword + "%");
            pstmt.setInt(i++, endRow - startRow);
            pstmt.setInt(i++, startRow);

            rs = pstmt.executeQuery();
            while (rs.next()) {
                boardVO vo = new boardVO(
                    rs.getInt("board_id"),
                    rs.getInt("category"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("user_id"),
                    rs.getInt("book_no"),
                    rs.getString("file"),
                    rs.getString("banner_img"),
                    rs.getTimestamp("created_at"),
                    rs.getInt("views"),
                    rs.getBoolean("secret"),
                    rs.getString("reply")
                );
                boardList.add(vo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return boardList;
    }

    /** 전체 개수 */
    public int getTotalBoardCount(int category, String searchKeyword, String searchType) {
        int totalCount = 0;
        try {
            con = DbcpBean.getConnection();
            String sql = "SELECT COUNT(*) FROM board WHERE category = ? AND " + searchType + " LIKE ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, category);
            pstmt.setString(2, "%" + searchKeyword + "%");
            rs = pstmt.executeQuery();
            if (rs.next()) totalCount = rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return totalCount;
    }

    /** 글 등록 (board_id 반환) */
    public int insertBoard(boardVO vo) {
        try {
            con = DbcpBean.getConnection();

            int category = vo.getCategory();
            String title = vo.getTitle();
            String content = vo.getContent();
            String userId = vo.getUserId();
            int bookNo = vo.getBookNo();
            String file = vo.getFile();
            String bannerImg = vo.getBannerImg();
            Timestamp createdAt = vo.getCreatedAt();
            boolean secret = Boolean.TRUE.equals(vo.getSecret());

            String sql = "INSERT INTO board (category, title, content, user_id, book_no, file, banner_img, created_at, secret) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            pstmt.setInt(i++, category);
            pstmt.setString(i++, title);
            pstmt.setString(i++, content);
            pstmt.setString(i++, userId);
            if (bookNo == 0) pstmt.setNull(i++, java.sql.Types.INTEGER);
            else pstmt.setInt(i++, bookNo);
            pstmt.setString(i++, file);
            pstmt.setString(i++, bannerImg);
            pstmt.setTimestamp(i++, createdAt);
            pstmt.setBoolean(i++, secret);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt);
        }
        return 0;
    }

    /** 조회수 +1 */
    public void increaseViewCount(int boardId) {
        try {
            con = DbcpBean.getConnection();
            String sql = "UPDATE board SET views = views + 1 WHERE board_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, boardId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt);
        }
    }

    /** 글 단건 조회(책 썸네일 포함) */
    public boardVO selectBoard(int boardId) {
        boardVO out = null;
        try {
            con = DbcpBean.getConnection();
            String sql = "SELECT b.*, bo.thumbnail FROM board b LEFT JOIN book bo ON b.book_no = bo.book_no WHERE b.board_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, boardId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                out = new boardVO(
                    rs.getInt("board_id"),
                    rs.getInt("category"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("user_id"),
                    rs.getInt("book_no"),
                    rs.getString("file"),
                    rs.getString("banner_img"),
                    rs.getTimestamp("created_at"),
                    rs.getInt("views"),
                    rs.getBoolean("secret"),
                    rs.getString("reply")
                );
                out.setThumbnail(rs.getString("thumbnail"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return out;
    }

    /** 배너 있는 공지 단건 */
    public boardVO selectBannerBoard(int boardId) {
        boardVO out = null;
        try {
            con = DbcpBean.getConnection();
            String sql = "SELECT * FROM board WHERE board_id = ? AND category = 0 AND banner_img IS NOT NULL";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, boardId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                out = new boardVO(
                    rs.getInt("board_id"),
                    rs.getInt("category"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("user_id"),
                    rs.getInt("book_no"),
                    rs.getString("file"),
                    rs.getString("banner_img"),
                    rs.getTimestamp("created_at"),
                    rs.getInt("views"),
                    rs.getBoolean("secret"),
                    rs.getString("reply")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return out;
    }

    /** 이전/다음 글 (카테고리 기준) */
    public int getPreBoardId(int currentBoardId, int category) {
        int boardId = 0;
        try {
            con = DbcpBean.getConnection();
            String sql = "SELECT board_id FROM board WHERE board_id < ? AND category = ? ORDER BY board_id DESC LIMIT 1";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, currentBoardId);
            pstmt.setInt(2, category);
            rs = pstmt.executeQuery();
            if (rs.next()) boardId = rs.getInt("board_id");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return boardId;
    }

    public int getNextBoardId(int currentBoardId, int category) {
        int boardId = 0;
        try {
            con = DbcpBean.getConnection();
            String sql = "SELECT board_id FROM board WHERE board_id > ? AND category = ? ORDER BY board_id ASC LIMIT 1";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, currentBoardId);
            pstmt.setInt(2, category);
            rs = pstmt.executeQuery();
            if (rs.next()) boardId = rs.getInt("board_id");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return boardId;
    }

    /** 행사게시판 이전/다음 (배너 존재 조건) */
    public int getPreBannerBoardId(int currentBoardId) {
        int boardId = 0;
        try {
            con = DbcpBean.getConnection();
            String sql = "SELECT board_id FROM board WHERE board_id < ? AND category = 0 AND banner_img IS NOT NULL ORDER BY board_id DESC LIMIT 1";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, currentBoardId);
            rs = pstmt.executeQuery();
            if (rs.next()) boardId = rs.getInt("board_id");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return boardId;
    }

    public int getNextBannerBoardId(int currentBoardId) {
        int boardId = 0;
        try {
            con = DbcpBean.getConnection();
            String sql = "SELECT board_id FROM board WHERE board_id > ? AND category = 0 AND banner_img IS NOT NULL ORDER BY board_id ASC LIMIT 1";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, currentBoardId);
            rs = pstmt.executeQuery();
            if (rs.next()) boardId = rs.getInt("board_id");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return boardId;
    }

    /** 전체 수정(제목/내용/비밀/파일/배너) */
    public void updateBoard(boardVO modVO) {
        int boardId = modVO.getBoardId();
        String title = modVO.getTitle();
        String content = modVO.getContent();
        String file = modVO.getFile();
        String bannerImg = modVO.getBannerImg();
        boolean secret = Boolean.TRUE.equals(modVO.getSecret());

        try {
            con = DbcpBean.getConnection();
            String sql = "UPDATE board SET " +
                         "title = ?, " +
                         "content = ?, " +
                         "secret = ?, " +
                         "file = COALESCE(?, file), " +              // ✅ null이면 기존 값 유지
                         "banner_img = COALESCE(?, banner_img) " +  // ✅ null이면 기존 값 유지
                         "WHERE board_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.setBoolean(3, secret);
            pstmt.setString(4, file);        // null이면 기존값 유지됨
            pstmt.setString(5, bannerImg);   // null이면 기존값 유지됨
            pstmt.setInt(6, boardId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt);
        }
    }


    /**
     * 파일/배너만 부분 수정 (null이 아닌 항목만 SET)
     * - fileUrl, bannerUrl, secret 중 전달된 값만 업데이트
     */
    public void updateBoardFiles(int boardId, String fileUrl, String bannerUrl, Boolean secret) {
        StringBuilder sb = new StringBuilder("UPDATE board SET ");
        List<Object> params = new ArrayList<>();

        if (fileUrl != null) {
            sb.append("file = ?, ");
            params.add(fileUrl);
        }
        if (bannerUrl != null) {
            sb.append("banner_img = ?, ");
            params.add(bannerUrl);
        }
        if (secret != null) {
            sb.append("secret = ?, ");
            params.add(secret.booleanValue());
        }

        // 변경할 항목이 없으면 종료
        if (params.isEmpty()) return;

        // 마지막 콤마/공백 제거 후 WHERE 추가
        sb.setLength(sb.length() - 2);
        sb.append(" WHERE board_id = ?");

        try {
            con = DbcpBean.getConnection();
            pstmt = con.prepareStatement(sb.toString());
            int i = 1;
            for (Object p : params) {
                if (p instanceof String) pstmt.setString(i++, (String) p);
                else if (p instanceof Boolean) pstmt.setBoolean(i++, (Boolean) p);
            }
            pstmt.setInt(i, boardId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt);
        }
    }

    /** 글 삭제 */
    public void deletBoard(int boardID) {
        try {
            con = DbcpBean.getConnection();
            String sql = "DELETE FROM board WHERE board_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, boardID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt);
        }
    }

    /** 답변 등록/삭제 */
    public boolean updateReply(int boardId, String reply) {
        String sql = "UPDATE board SET reply = ? WHERE board_id = ?";
        try {
            con = DbcpBean.getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, reply);
            pstmt.setInt(2, boardId);
            return pstmt.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt);
        }
        return false;
    }

    public boolean deleteReply(int boardId) {
        String sql = "UPDATE board SET reply = NULL WHERE board_id = ?";
        try {
            con = DbcpBean.getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, boardId);
            return pstmt.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt);
        }
        return false;
    }

    /** 배너 있는 공지 목록(검색/페이징) */
    public List<boardVO> getBannerList(int startRow, int endRow, String searchKeyword, String searchType) {
        List<boardVO> boardList = new ArrayList<>();
        try {
            con = DbcpBean.getConnection();

            // WHERE → ORDER BY → LIMIT 순서로 구성
            StringBuilder sb = new StringBuilder("SELECT * FROM board WHERE category = 0 AND banner_img IS NOT NULL ");
            if (searchKeyword != null && !searchKeyword.isEmpty()) {
                if ("title".equals(searchType)) sb.append(" AND title LIKE ? ");
                else if ("content".equals(searchType)) sb.append(" AND content LIKE ? ");
                else if ("userId".equals(searchType)) sb.append(" AND user_id LIKE ? ");
            }
            sb.append(" ORDER BY board_id DESC LIMIT ? OFFSET ?");

            pstmt = con.prepareStatement(sb.toString());
            int i = 1;
            if (searchKeyword != null && !searchKeyword.isEmpty()) {
                pstmt.setString(i++, "%" + searchKeyword + "%");
            }
            pstmt.setInt(i++, endRow - startRow); // LIMIT
            pstmt.setInt(i++, startRow);          // OFFSET

            rs = pstmt.executeQuery();
            while (rs.next()) {
                boardVO board = new boardVO();
                board.setBoardId(rs.getInt("board_id"));
                board.setTitle(rs.getString("title"));
                board.setContent(rs.getString("content"));
                board.setUserId(rs.getString("user_id"));
                board.setBannerImg(rs.getString("banner_img"));
                boardList.add(board);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return boardList;
    }

    /** 배너 있는 공지 총 개수 */
    public int getTotalBannerCount(String searchKeyword, String searchType) {
        int totalCount = 0;
        String sql = "SELECT COUNT(*) FROM board WHERE category = 0 AND banner_img IS NOT NULL";
        try {
            con = DbcpBean.getConnection();
            if (searchKeyword != null && !searchKeyword.isEmpty()) {
                if ("title".equals(searchType)) sql += " AND title LIKE ?";
                else if ("content".equals(searchType)) sql += " AND content LIKE ?";
                else if ("userId".equals(searchType)) sql += " AND user_id LIKE ?";
                pstmt = con.prepareStatement(sql);
                pstmt.setString(1, "%" + searchKeyword + "%");
            } else {
                pstmt = con.prepareStatement(sql);
            }
            rs = pstmt.executeQuery();
            if (rs.next()) totalCount = rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return totalCount;
    }

    /** 책별 서평 목록 */
    public List<boardVO> getReviewsByBookNo(int bookNo) {
        List<boardVO> reviewList = new ArrayList<>();
        try {
            con = DbcpBean.getConnection();
            String sql = "SELECT * FROM board WHERE category = 2 AND book_no = ? ORDER BY created_at DESC";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, bookNo);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                boardVO vo = new boardVO(
                    rs.getInt("board_id"),
                    rs.getInt("category"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("user_id"),
                    rs.getInt("book_no"),
                    rs.getString("file"),
                    rs.getString("banner_img"),
                    rs.getTimestamp("created_at"),
                    rs.getInt("views"),
                    rs.getBoolean("secret"),
                    rs.getString("reply")
                );
                reviewList.add(vo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return reviewList;
    }

    /** 메인 배너 N개 */
    public List<boardVO> getLatestEventBanners(int limit) {
        List<boardVO> list = new ArrayList<>();
        try {
            con = DbcpBean.getConnection();
            String q = "SELECT board_id, category, title, content, user_id, book_no, file, banner_img, created_at, views, secret, reply " +
                       "FROM board WHERE category = 0 AND banner_img IS NOT NULL ORDER BY created_at DESC LIMIT ?";
            pstmt = con.prepareStatement(q);
            pstmt.setInt(1, limit);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                boardVO b = new boardVO(
                    rs.getInt("board_id"),
                    rs.getInt("category"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("user_id"),
                    rs.getInt("book_no"),
                    rs.getString("file"),
                    rs.getString("banner_img"),
                    rs.getTimestamp("created_at"),
                    rs.getInt("views"),
                    rs.getBoolean("secret"),
                    rs.getString("reply")
                );
                list.add(b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return list;
    }

    /** 최신 공지 N개 */
    public List<boardVO> getLatestNotices(int i) {
        List<boardVO> noticeList = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DbcpBean.getConnection();
            String sql = "SELECT * FROM board WHERE category = 0 ORDER BY created_at DESC LIMIT ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, i);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                boardVO vo = new boardVO(
                    rs.getInt("board_id"),
                    rs.getInt("category"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("user_id"),
                    rs.getInt("book_no"),
                    rs.getString("file"),
                    rs.getString("banner_img"),
                    rs.getTimestamp("created_at"),
                    rs.getInt("views"),
                    rs.getBoolean("secret"),
                    rs.getString("reply")
                );
                noticeList.add(vo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbcpBean.close(con, pstmt, rs);
        }
        return noticeList;
    }
    
    
}
