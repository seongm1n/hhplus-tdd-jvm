package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PointController 단위 테스트")
class PointControllerTest {

    private PointController pointController;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        UserPointTable userPointTable = new UserPointTable();
        PointHistoryTable pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(userPointTable, pointHistoryTable);
        pointController = new PointController(pointService);
    }

    @Test
    @DisplayName("사용자 포인트 조회")
    void getUserPoint() {
        UserPoint result = pointController.point(1L);
        
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.point()).isEqualTo(0L);
    }

    @Test
    @DisplayName("포인트 내역 조회")
    void getUserPointHistories() {
        var result = pointController.history(1L);
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("포인트 충전")
    void chargePoint() {
        UserPoint result = pointController.charge(1L, 1000L);
        
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.point()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("포인트 사용")
    void usePoint() {
        pointController.charge(1L, 1000L);
        UserPoint result = pointController.use(1L, 300L);
        
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.point()).isEqualTo(700L);
    }

    @Test
    @DisplayName("잔고 부족시 예외 발생")
    void usePoint_InsufficientBalance() {
        assertThatThrownBy(() -> pointController.use(1L, 1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔고가 부족합니다");
    }
}
