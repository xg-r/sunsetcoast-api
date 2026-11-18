package kr.sunsetcoast.domain.managementfee

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kr.sunsetcoast.domain.managementfee.entity.ManagementFeeEntity
import kr.sunsetcoast.domain.managementfee.vo.ManagementFeeBasis
import kr.sunsetcoast.domain.property.entity.PropertyEntity
import kr.sunsetcoast.domain.reservation.entity.ReservationEntity
import kr.sunsetcoast.domain.reservation.vo.RoomChange
import kr.sunsetcoast.domain.room.entity.RoomEntity
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal

class ManagementFeeRoomChangeTest : FunSpec({
    fun property(id: Long): PropertyEntity = mock(PropertyEntity::class.java).also { `when`(it.id).thenReturn(id) }
    fun room(id: Long, name: String, property: PropertyEntity): RoomEntity = mock(RoomEntity::class.java).also {
        `when`(it.id).thenReturn(id)
        `when`(it.name).thenReturn(name)
        `when`(it.property).thenReturn(property)
    }

    val camping = property(1)
    val haean = room(10, "해안", camping)
    val nolmeong = room(11, "놀멍", camping)
    val resort = room(20, "RESORT_201", property(2))

    fun fee() = ManagementFeeEntity(
        reservation = mock(ReservationEntity::class.java),
        room = haean,
        basis = ManagementFeeBasis.CHARGED,
        calculatedAmount = BigDecimal(40_000),
    )

    test("옮긴 객실과 차액을 남기고, 관리비 단가는 옮긴 객실로 정한다") {
        val fee = fee()
        fee.changeRoom(nolmeong, BigDecimal(120_000), " 온수 고장 ")
        fee.movedRoom shouldBe nolmeong
        fee.feeRoom shouldBe nolmeong
        fee.roomChangeDifference shouldBe BigDecimal(120_000)
        fee.roomChangeMemo shouldBe "온수 고장"
        fee.room shouldBe haean

        RoomChange.of(listOf(fee)).single().let {
            it.from shouldBe haean
            it.to shouldBe nolmeong
        }
    }

    test("차액 0원은 없는 것으로") {
        val fee = fee()
        fee.changeRoom(nolmeong, BigDecimal.ZERO, null)
        fee.roomChangeDifference shouldBe null
    }

    test("취소하면 예약한 객실로 돌아간다") {
        val fee = fee()
        fee.changeRoom(nolmeong, BigDecimal(120_000), null)
        fee.clearRoomChange()
        fee.movedRoom shouldBe null
        fee.feeRoom shouldBe haean
        fee.roomChangeDifference shouldBe null
        RoomChange.of(listOf(fee)) shouldBe emptyList()
    }

    test("같은 객실, 다른 업소 객실로는 옮길 수 없다") {
        shouldThrow<IllegalArgumentException> { fee().changeRoom(haean, null, null) }
        shouldThrow<IllegalArgumentException> { fee().changeRoom(resort, null, null) }
    }

    test("입금 완료된 관리비는 객실 변경을 고칠 수 없다") {
        val fee = fee()
        fee.markPaid(userId = 1)
        shouldThrow<IllegalStateException> { fee.changeRoom(nolmeong, null, null) }
        shouldThrow<IllegalStateException> { fee.clearRoomChange() }
    }
})
