package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PointService 테스트")
class PointServiceTest {

    private PointService pointService;
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Test
    @DisplayName("사용자 포인트 조회 - 처음 조회시 0포인트를 반환한다")
    void getUserPoint_WhenFirstTime_ShouldReturnZeroPoint() {
        // given
        long userId = 1L;

        // when
        UserPoint userPoint = pointService.getUserPoint(userId);

        // then
        assertThat(userPoint.id()).isEqualTo(userId);
        assertThat(userPoint.point()).isEqualTo(0L);
    }

    @Test
    @DisplayName("사용자 포인트 조회 - 존재하지 않는 사용자도 0포인트로 조회된다")
    void getUserPoint_WhenNonExistentUser_ShouldReturnZeroPoint() {
        // given
        long nonExistentUserId = 999999L;

        // when
        UserPoint userPoint = pointService.getUserPoint(nonExistentUserId);

        // then
        assertThat(userPoint.id()).isEqualTo(nonExistentUserId);
        assertThat(userPoint.point()).isEqualTo(0L);
        assertThat(userPoint.updateMillis()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("포인트 충전 - 정상적으로 포인트가 충전된다")
    void chargePoint_WhenValidAmount_ShouldChargeSuccessfully() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;

        // when
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // then
        assertThat(result.point()).isEqualTo(1000L);
        assertThat(result.id()).isEqualTo(userId);
    }

    @Test
    @DisplayName("포인트 충전 - 여러 번 충전시 누적된다")
    void chargePoint_WhenMultipleTimes_ShouldAccumulate() {
        // given
        long userId = 1L;

        // when
        pointService.chargePoint(userId, 1000L);
        UserPoint result = pointService.chargePoint(userId, 500L);

        // then
        assertThat(result.point()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("포인트 충전 - 0 이하의 금액으로 충전시 예외가 발생한다")
    void chargePoint_WhenNegativeOrZeroAmount_ShouldThrowException() {
        // given
        long userId = 1L;

        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("충전 금액은 0보다 커야 합니다");

        assertThatThrownBy(() -> pointService.chargePoint(userId, -100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("충전 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("포인트 사용 - 잔고가 충분할 때 정상적으로 차감된다")
    void usePoint_WhenSufficientBalance_ShouldDeductSuccessfully() {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 1000L); // 1000포인트 충전

        // when
        UserPoint result = pointService.usePoint(userId, 300L);

        // then
        assertThat(result.point()).isEqualTo(700L);
    }

    @Test
    @DisplayName("포인트 사용 - 잔고가 부족할 때 예외가 발생한다")
    void usePoint_WhenInsufficientBalance_ShouldThrowException() {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 500L); // 500포인트만 충전

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, 1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔고가 부족합니다");
    }

    @Test
    @DisplayName("포인트 사용 - 0 이하의 금액으로 사용시 예외가 발생한다")
    void usePoint_WhenNegativeOrZeroAmount_ShouldThrowException() {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 1000L);

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용 금액은 0보다 커야 합니다");

        assertThatThrownBy(() -> pointService.usePoint(userId, -100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("포인트 내역 조회 - 처음 조회시 빈 리스트를 반환한다")
    void getUserPointHistories_WhenNoHistory_ShouldReturnEmptyList() {
        // given
        long userId = 1L;

        // when
        List<PointHistory> histories = pointService.getUserPointHistories(userId);

        // then
        assertThat(histories).isEmpty();
    }

    @Test
    @DisplayName("포인트 내역 조회 - 충전/사용 내역이 올바르게 기록된다")
    void getUserPointHistories_WhenHasTransactions_ShouldReturnCorrectHistories() {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 1000L);
        pointService.usePoint(userId, 300L);

        // when
        List<PointHistory> histories = pointService.getUserPointHistories(userId);

        // then
        assertThat(histories).hasSize(2);
        
        PointHistory chargeHistory = histories.get(0);
        assertThat(chargeHistory.userId()).isEqualTo(userId);
        assertThat(chargeHistory.amount()).isEqualTo(1000L);
        assertThat(chargeHistory.type()).isEqualTo(TransactionType.CHARGE);

        PointHistory useHistory = histories.get(1);
        assertThat(useHistory.userId()).isEqualTo(userId);
        assertThat(useHistory.amount()).isEqualTo(300L);
        assertThat(useHistory.type()).isEqualTo(TransactionType.USE);
    }
}
