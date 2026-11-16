package kr.sunsetcoast.domain.managementfee

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import kr.sunsetcoast.domain.managementfee.vo.FeeDecision
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeBasis
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.room.entity.RoomEntity
import org.mockito.Mockito.mock
import java.math.BigDecimal

class ManagementFeePaymentTest : FunSpec({
    fun fee(amount: Int) = ManagementFeeEntity(
        reservation = mock(ReservationEntity::class.java),
        room = mock(RoomEntity::class.java),
        basis = if (amount > 0) ManagementFeeBasis.CHARGED else ManagementFeeBasis.CANCELLED,
        calculatedAmount = BigDecimal(amount),
    )

    test("입금 완료로 표시하면 그때 지급액을 남긴다") {
        val fee = fee(30_000)
        fee.markPaid(userId = 1)
        fee.paid shouldBe true
        fee.paidAmount shouldBe BigDecimal(30_000)
        fee.paidBy shouldBe 1L
        fee.amountChangedAfterPayment shouldBe false
    }

    test("입금 뒤 재계산으로 금액이 바뀌면 입금 상태는 그대로, 차액을 알 수 있다") {
        val fee = fee(30_000)
        fee.markPaid(userId = 1)
        fee.apply(FeeDecision(ManagementFeeBasis.CANCELLED, BigDecimal.ZERO))
        fee.paid shouldBe true
        fee.amountChangedAfterPayment shouldBe true
    }

    test("0원 관리비는 입금 완료로 표시할 수 없다") {
        shouldThrow<IllegalArgumentException> { fee(0).markPaid(userId = 1) }
    }

    test("입금 완료된 관리비는 금액을 고칠 수 없고, 취소하면 고칠 수 있다") {
        val fee = fee(30_000)
        fee.markPaid(userId = 1)
        shouldThrow<IllegalStateException> { fee.overrideManually(BigDecimal(90_000), "한달살기") }
        fee.unmarkPaid()
        fee.paid shouldBe false
        fee.paidAmount shouldBe null
        fee.overrideManually(BigDecimal(90_000), "한달살기")
        fee.amount shouldBe BigDecimal(90_000)
    }

    test("확인: 확인한 금액을 남기고, 금액이 바뀌면 다시 확인 필요, 다시 확인하면 새 금액으로") {
        val fee = fee(30_000)
        fee.verify(userId = 1)
        fee.verified shouldBe true
        fee.amountChangedAfterVerification shouldBe false

        fee.apply(FeeDecision(ManagementFeeBasis.CHARGED, BigDecimal(45_000)))
        fee.amountChangedAfterVerification shouldBe true

        fee.verify(userId = 1)
        fee.verifiedAmount shouldBe BigDecimal(45_000)
        fee.amountChangedAfterVerification shouldBe false

        fee.unverify()
        fee.verified shouldBe false
    }
})
