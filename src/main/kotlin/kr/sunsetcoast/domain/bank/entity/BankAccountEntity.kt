package kr.sunsetcoast.domain.bank.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/** 거래내역을 올린 계좌. 파일 머리글의 계좌번호로 찾거나 만든다 */
@Entity
@Table(name = "bank_accounts")
class BankAccountEntity(
    /** 파일에 계좌번호가 없었으면 '번호없음:{이름}' (나중에 번호가 있는 파일이 오면 채운다) */
    @Column(name = "account_number", nullable = false, length = 50)
    var accountNumber: String,

    /** 파일 머리글 괄호 안 이름 (예: 선셋코스트) */
    @Column(name = "holder_name", length = 100)
    var holderName: String?,

    /** 이 계좌의 사업자. 관리비 비교에 쓴다 */
    @Column(name = "business_owner_id")
    var businessOwnerId: Long?,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
