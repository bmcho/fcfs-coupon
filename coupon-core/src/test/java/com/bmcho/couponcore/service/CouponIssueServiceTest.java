package com.bmcho.couponcore.service;

import com.bmcho.couponcore.TestConfig;
import com.bmcho.couponcore.exception.CouponIssueException;
import com.bmcho.couponcore.exception.ErrorCode;
import com.bmcho.couponcore.model.Coupon;
import com.bmcho.couponcore.model.CouponIssue;
import com.bmcho.couponcore.model.CouponType;
import com.bmcho.couponcore.repository.mysql.CouponIssueJpaRepository;
import com.bmcho.couponcore.repository.mysql.CouponIssueRepository;
import com.bmcho.couponcore.repository.mysql.CouponJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;


class CouponIssueServiceTest extends TestConfig {

    @Autowired
    CouponIssueService sut;

    @Autowired
    CouponIssueJpaRepository couponIssueJpaRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @BeforeEach
    void clean() {
        couponJpaRepository.deleteAll();
        couponIssueJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("쿠폰 발급 내역이 존재하면 예외를 반환한다")
    void saveCouponIssue_1() {
        // given
        CouponIssue couponIssue = CouponIssue.builder()
            .couponId(1L)
            .userId(1L)
            .build();
        couponIssueJpaRepository.save(couponIssue);
        // when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, () -> sut.saveCouponIssue(couponIssue.getCouponId(), couponIssue.getUserId()));
        assertEquals(exception.getErrorCode(), ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰 발급 내역이 존재하지 않는다면 쿠폰을 발급한다")
    void saveCouponIssue_2() {
        // given
        long couponId = 1L;
        long userId = 1L;
        // when
        CouponIssue result = sut.saveCouponIssue(couponId, userId);
        // then
        assertTrue(couponIssueJpaRepository.findById(result.getId()).isPresent());
    }

    @Test
    @DisplayName("발급 수량, 기한, 중복 발급 문제가 없다면 쿠폰을 발급한다")
    void issue_1() {
        // given
        long userId = 1;
        Coupon coupon = Coupon.builder()
            .couponType(CouponType.FIRST_COME_FIRST_SERVED)
            .title("선착순 테스트 쿠폰")
            .totalQuantity(100)
            .issuedQuantity(0)
            .dateIssueStart(LocalDateTime.now().minusDays(1))
            .dateIssueEnd(LocalDateTime.now().plusDays(1))
            .build();
        couponJpaRepository.save(coupon);
        // when
        sut.issue(coupon.getId(), userId);
        // then
        Coupon couponResult = couponJpaRepository.findById(coupon.getId()).get();
        assertEquals(couponResult.getIssuedQuantity(), 1);

        CouponIssue couponIssueResult = couponIssueRepository.findFirstCouponIssue(coupon.getId(), userId).orElse(null);
        assertNotNull(couponIssueResult);
    }


    @Test
    @DisplayName("발급 수량에 문제가 있다면 예외를 반환한다")
    void issue_2() {
        // given
        long userId = 1;
        Coupon coupon = Coupon.builder()
            .couponType(CouponType.FIRST_COME_FIRST_SERVED)
            .title("선착순 테스트 쿠폰")
            .totalQuantity(100)
            .issuedQuantity(100)
            .dateIssueStart(LocalDateTime.now().minusDays(1))
            .dateIssueEnd(LocalDateTime.now().plusDays(1))
            .build();
        couponJpaRepository.save(coupon);
        // when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, () -> {
            sut.issue(coupon.getId(), userId);
        });
        assertEquals(exception.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_QUANTITY);
    }

    @Test
    @DisplayName("발급 기한에 문제가 있다면 예외를 반환한다")
    void issue_3() {
        // given
        long userId = 1;
        Coupon coupon = Coupon.builder()
            .couponType(CouponType.FIRST_COME_FIRST_SERVED)
            .title("선착순 테스트 쿠폰")
            .totalQuantity(100)
            .issuedQuantity(0)
            .dateIssueStart(LocalDateTime.now().minusDays(2))
            .dateIssueEnd(LocalDateTime.now().minusDays(1))
            .build();
        couponJpaRepository.save(coupon);
        // when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, () -> {
            sut.issue(coupon.getId(), userId);
        });
        assertEquals(exception.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("중복 발급 검증에 문제가 있다면 예외를 반환한다")
    void issue_4() {
        // given
        long userId = 1;
        Coupon coupon = Coupon.builder()
            .couponType(CouponType.FIRST_COME_FIRST_SERVED)
            .title("선착순 테스트 쿠폰")
            .totalQuantity(100)
            .issuedQuantity(0)
            .dateIssueStart(LocalDateTime.now().minusDays(1))
            .dateIssueEnd(LocalDateTime.now().plusDays(1))
            .build();
        couponJpaRepository.save(coupon);

        CouponIssue couponIssue = CouponIssue.builder()
            .couponId(coupon.getId())
            .userId(userId)
            .build();
        couponIssueJpaRepository.save(couponIssue);

        // when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, () -> {
            sut.issue(coupon.getId(), userId);
        });
        assertEquals(exception.getErrorCode(), ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰이 존재하지 않는다면 예외를 반환한다")
    void issue_5() {
        // given
        long userId = 1;
        long couponId = 1;

        // when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, () -> sut.issue(couponId, userId));
        assertEquals(exception.getErrorCode(), ErrorCode.COUPON_NOT_EXIST);
    }

}