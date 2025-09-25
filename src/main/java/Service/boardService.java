package Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Dao.boardDAO;
import Vo.boardVO;

public class boardService {

    private boardDAO boardDao;

    public boardService() {
        boardDao = new boardDAO();
    }

    /** 게시글 목록 (페이징 + 검색) */
    public Map<String, Object> getBoardList(int category, int section, int pageNum, String searchKeyword, String searchType, String currentUserId) {
        int pageSize = 10;
        int sectionSize = 5;
        int startRow = (pageNum - 1) * pageSize;
        int endRow = startRow + pageSize;

        List<boardVO> boardList = boardDao.getBoardList(category, startRow, endRow, searchKeyword, searchType, currentUserId);
        int totalBoardCount = boardDao.getTotalBoardCount(category, searchKeyword, searchType);
        int totalPage = (int) Math.ceil(totalBoardCount / (double) pageSize);
        int totalSection = (int) Math.ceil(totalPage / (double) sectionSize);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("boardList", boardList);
        resultMap.put("totalPage", totalPage);
        resultMap.put("totalSection", totalSection);
        resultMap.put("totalBoardCount", totalBoardCount);

        return resultMap;
    }

    /** 게시글 등록 */
    public int addBoard(boardVO boardVO) {
        return boardDao.insertBoard(boardVO);
    }

    /** 게시글 상세 보기 */
    public boardVO viewBoard(int boardId) {
        boardVO board = boardDao.selectBoard(boardId);
        if (board != null) boardDao.increaseViewCount(boardId);
        return board;
    }

    /** 배너 있는 게시글 상세 보기 */
    public boardVO viewBannerBoard(int boardId) {
        boardVO board = boardDao.selectBannerBoard(boardId);
        if (board != null) boardDao.increaseViewCount(boardId);
        return board;
    }

    /** 이전/다음 글 */
    public int getPreBoardId(int currentBoardId, int category) {
        return boardDao.getPreBoardId(currentBoardId, category);
    }
    public int getNextBoardId(int currentBoardId, int category) {
        return boardDao.getNextBoardId(currentBoardId, category);
    }

    /** 행사 게시판 이전/다음 글 */
    public int getPreBannerBoardId(int currentBoardId) {
        return boardDao.getPreBannerBoardId(currentBoardId);
    }
    public int getNextBannerBoardId(int currentBoardId) {
        return boardDao.getNextBannerBoardId(currentBoardId);
    }

    /** 게시글 전체 수정 */
    public void modifyBoard(boardVO modVO) {
        boardDao.updateBoard(modVO);
    }

	// 파일/배너 이미지 및 비밀글 여부를 업데이트
    public void updateBoardFiles(int boardId, String fileUrl, String bannerUrl, Boolean secret) {
        boardDao.updateBoardFiles(boardId, fileUrl, bannerUrl, secret);
    }

    /** 게시글 삭제 */
    public int removeBoard(int boardID) {
        boardDao.deletBoard(boardID);
        return boardID;
    }

    /** 답변 등록/삭제 */
    public boolean updateReply(int boardId, String reply) {
        return boardDao.updateReply(boardId, reply);
    }
    public boolean deleteReply(int boardId) throws Exception {
        if (boardId <= 0) throw new IllegalArgumentException("잘못된 게시글 ID입니다.");
        return boardDao.deleteReply(boardId);
    }

    /** 이벤트 게시글 목록 (배너 있는 글만) */
    public Map<String, Object> getEventBoardList(int section, int pageNum, String searchKeyword, String searchType) {
        int pageSize = 12;
        int sectionSize = 5;
        int startRow = (pageNum - 1) * pageSize;
        int endRow = startRow + pageSize;

        List<boardVO> boardList = boardDao.getBannerList(startRow, endRow, searchKeyword, searchType);
        int totalBoardCount = boardDao.getTotalBannerCount(searchKeyword, searchType);
        int totalPage = (int) Math.ceil(totalBoardCount / (double) pageSize);
        int totalSection = (int) Math.ceil(totalPage / (double) sectionSize);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("boardList", boardList);
        resultMap.put("totalPage", totalPage);
        resultMap.put("totalSection", totalSection);
        resultMap.put("totalBoardCount", totalBoardCount);

        return resultMap;
    }

    /** 책별 서평 목록 */
    public List<boardVO> getReviewsByBookNo(int bookNo) {
        return boardDao.getReviewsByBookNo(bookNo);
    }

    /** 메인 화면용 최신 데이터 */
    public List<boardVO> getLatestEventBanners(int i) {
        return boardDao.getLatestEventBanners(i);
    }
    public List<boardVO> getLatestNotices(int i) {
        return boardDao.getLatestNotices(i);
    }
    

}
